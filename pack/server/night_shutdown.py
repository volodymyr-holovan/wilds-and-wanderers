"""Нічне вимкнення: the TrueNAS app that switches the Wilds & Wanderers server box off at night.

At the set time (00:00 by default, in the TZ of the container), when the switch is on:
  1. Crafty is stopped with a long grace period: Crafty stops the Minecraft server the proper way ("stop": players
     are saved, the world is saved), waiting up to the server's shutdown timeout;
  2. once no Minecraft process is left, TrueNAS is shut down through its middleware (system.shutdown), which stops
     the remaining apps and powers off cleanly.
If Minecraft does not stop, TrueNAS is NOT switched off (a half-saved world is worse than a night of power).

The switch, the time and a log are on the app's web page (the 🌐 button of the app's dashboard widget). Stopping the
app also switches the feature off.

Runs in a privileged container with the host's PID namespace (pid: host): host commands (docker, midclt) run through
nsenter as root. The page only flips settings; it runs no command a visitor could choose.
"""
import datetime
import html
import http.server
import json
import os
import subprocess
import threading
import time
import urllib.parse

DATA = os.environ.get("DATA_DIR", "/data")
STATE_FILE = os.path.join(DATA, "state.json")
LOG_FILE = os.path.join(DATA, "log.txt")
PORT = int(os.environ.get("PORT", "30210"))
CRAFTY = os.environ.get("CRAFTY_NAME", "crafty")           # part of the Crafty app's container name
STOP_TIMEOUT = int(os.environ.get("STOP_TIMEOUT", "900"))  # seconds Crafty gets to stop Minecraft
SERVERS_DIR = os.environ.get("MC_DIR_MARK", "/crafty/servers/")
WINDOW = 10 * 60                                           # a late start (after a restart) still counts within this
SETTLE = 10 * 60                                           # nothing happens in the first minutes after a boot
STARTED = time.time()
LOCK = threading.Lock()
BUSY = threading.Lock()


def now():
    return datetime.datetime.now()


def load():
    state = {"enabled": True, "time": "00:00", "last_run": "", "skip": ""}
    try:
        with open(STATE_FILE, encoding="utf-8") as f:
            state.update(json.load(f))
    except (OSError, ValueError):
        pass
    return state


def save(state):
    tmp = STATE_FILE + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(state, f, ensure_ascii=False, indent=1)
    os.replace(tmp, STATE_FILE)


def log(message):
    line = f"{now():%Y-%m-%d %H:%M:%S}  {message}"
    print(line, flush=True)
    with LOCK:
        lines = []
        try:
            with open(LOG_FILE, encoding="utf-8") as f:
                lines = f.read().splitlines()[-299:]
        except OSError:
            pass
        lines.append(line)
        with open(LOG_FILE, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")


def host(*cmd, timeout=120):
    """a command on TrueNAS itself (root, its own namespaces)"""
    return subprocess.run(["nsenter", "--target", "1", "--mount", "--uts", "--ipc", "--net", "--pid", "--", *cmd],
                          capture_output=True, text=True, timeout=timeout)


def minecraft_pids():
    """Minecraft servers Crafty runs: Java processes working in a folder under /crafty/servers/"""
    found = []
    for pid in filter(str.isdigit, os.listdir("/proc")):
        try:
            with open(f"/proc/{pid}/cmdline", "rb") as f:
                cmd = f.read().replace(b"\0", b" ").decode(errors="replace")
            if "java" not in cmd:
                continue
            if SERVERS_DIR in os.readlink(f"/proc/{pid}/cwd") or SERVERS_DIR in cmd:
                found.append(int(pid))
        except OSError:
            continue
    return found


def crafty_containers():
    r = host("docker", "ps", "--format", "{{.ID}} {{.Names}}")
    if r.returncode != 0:
        raise RuntimeError("docker ps: " + (r.stderr or r.stdout).strip()[:200])
    return [line.split(" ", 1) for line in r.stdout.splitlines() if CRAFTY in line.split(" ", 1)[-1]]


def shutdown_truenas():
    reason = "Нічне вимкнення (Wilds & Wanderers)"
    for cmd in (["midclt", "call", "system.shutdown", reason], ["midclt", "call", "system.shutdown"]):
        r = host(*cmd, timeout=60)
        if r.returncode == 0:
            return True
        log("midclt: " + (r.stderr or r.stdout).strip()[:300])
    return False


def run(dry):
    if not BUSY.acquire(blocking=False):
        log("вже виконується")
        return
    try:
        log("Перевірка (нічого не вимикається)…" if dry else "Нічне вимкнення почалося")
        try:
            pids, containers = minecraft_pids(), crafty_containers()
        except Exception as e:
            log("Помилка: " + str(e))
            return
        log(f"Сервер Minecraft: {'працює (процес ' + ', '.join(map(str, pids)) + ')' if pids else 'не запущено'}; "
            f"Crafty: {', '.join(name for _, name in containers) or 'не знайдено'}")
        nsenter_ok = host("midclt", "call", "system.ready", timeout=30)
        log("Доступ до TrueNAS: " + ("є" if nsenter_ok.returncode == 0 else "НЕМАЄ — " + (nsenter_ok.stderr or "").strip()[:200]))
        if dry:
            log("Перевірку завершено: опівночі все це спрацює так само, але насправді.")
            return
        for cid, name in containers:
            log(f"Зупиняю {name}: Crafty зберігає світ і зупиняє Minecraft (до {STOP_TIMEOUT // 60} хв)…")
            r = host("docker", "stop", "-t", str(STOP_TIMEOUT), cid, timeout=STOP_TIMEOUT + 120)
            log(f"{name} зупинено" if r.returncode == 0 else f"{name}: " + (r.stderr or r.stdout).strip()[:300])
        for _ in range(60):
            if not minecraft_pids():
                break
            time.sleep(2)
        left = minecraft_pids()
        if left:
            log(f"Minecraft досі працює ({left}): TrueNAS НЕ вимикаю, щоб не зіпсувати світ.")
            return
        log("Світ збережено, Minecraft зупинено. Вимикаю TrueNAS…")
        if not shutdown_truenas():
            log("Не вдалося вимкнути TrueNAS.")
    finally:
        BUSY.release()


def target_today(state):
    hh, mm = (int(x) for x in state["time"].split(":"))
    return now().replace(hour=hh, minute=mm, second=0, microsecond=0)


def scheduler():
    while True:
        try:
            state = load()
            t = target_today(state)
            today = f"{t:%Y-%m-%d}"
            due = 0 <= (now() - t).total_seconds() < WINDOW
            if state["enabled"] and due and state["last_run"] != today and time.time() - STARTED > SETTLE:
                state["last_run"] = today
                save(state)
                if state.get("skip") == today:
                    log("Сьогоднішнє вимкнення пропущено (так вирішили на сторінці).")
                else:
                    run(False)
        except Exception as e:
            log("Помилка планувальника: " + str(e))
        time.sleep(15)


def next_run(state):
    if not state["enabled"]:
        return None
    t = target_today(state)
    if now() >= t or state["last_run"] == f"{t:%Y-%m-%d}":
        t += datetime.timedelta(days=1)
    return t


PAGE = """<!doctype html>
<html lang="uk"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Нічне вимкнення</title>
<style>
:root{color-scheme:dark}
body{margin:0;font:15px/1.5 "Segoe UI",system-ui,sans-serif;background:#0d1417;color:#ece6d8}
main{max-width:640px;margin:40px auto;padding:0 16px}
h1{font:600 28px Georgia,serif;color:#e2b35a;margin:0 0 6px}
.sub{color:#9aa49d;margin:0 0 28px}
.card{background:#151e21;border:1px solid #2b3a36;border-radius:12px;padding:22px;margin-bottom:16px}
.row{display:flex;align-items:center;gap:16px;flex-wrap:wrap}
.state{font-size:20px;font-weight:600}
.on{color:#6fd39b}.off{color:#e5735f}
button{font:inherit;border:0;border-radius:8px;padding:10px 18px;cursor:pointer}
.big{font-size:17px;font-weight:600;padding:14px 26px}
.go{background:#e2b35a;color:#16130c}.stop{background:#3a2a26;color:#f0b3a6}
.ghost{background:#22302c;color:#ece6d8}
input[type=time]{font:inherit;background:#0d1417;color:#ece6d8;border:1px solid #2b3a36;border-radius:8px;padding:8px}
pre{white-space:pre-wrap;font:12px/1.5 Consolas,monospace;color:#b8c2bb;max-height:340px;overflow:auto;margin:0}
.muted{color:#9aa49d;font-size:13px}
form{display:inline}
</style></head><body><main>
<h1>Нічне вимкнення</h1>
<p class="sub">Сервер Wilds &amp; Wanderers: спершу Minecraft зберігає світ і зупиняється, потім вимикається TrueNAS.</p>
<div class="card"><div class="row">
<div class="state %(cls)s">%(state)s</div>
<form method="post" action="/toggle"><button class="big %(btn)s">%(action)s</button></form>
</div><p class="muted">%(detail)s</p></div>
<div class="card"><div class="row">
<form method="post" action="/time">Час вимкнення: <input type="time" name="time" value="%(time)s" required> <button class="ghost">Зберегти</button></form>
</div><p class="muted">Часовий пояс: %(tz)s. Зараз %(clock)s.</p>
<div class="row">
<form method="post" action="/skip"><button class="ghost">%(skip)s</button></form>
<form method="post" action="/check"><button class="ghost">Перевірити зараз (нічого не вимикає)</button></form>
</div></div>
<div class="card"><pre>%(log)s</pre></div>
</main></body></html>"""


def render():
    s = load()
    nxt = next_run(s)
    today = f"{now():%Y-%m-%d}"
    skipped = nxt is not None and s.get("skip") == f"{nxt:%Y-%m-%d}"
    if s["enabled"]:
        detail = (f"Наступне вимкнення: {nxt:%d.%m о %H:%M}" + (" — пропускається" if skipped else "") + ". "
                  "Гравців на сервері в цей момент буде відключено.")
    else:
        detail = "Сервер працюватиме, доки його не вимкнуть вручну."
    try:
        with open(LOG_FILE, encoding="utf-8") as f:
            tail = "\n".join(f.read().splitlines()[-40:][::-1])
    except OSError:
        tail = "Поки що записів немає."
    return PAGE % dict(
        cls="on" if s["enabled"] else "off", state="Увімкнено" if s["enabled"] else "Вимкнено",
        btn="stop" if s["enabled"] else "go", action="Вимкнути функцію" if s["enabled"] else "Увімкнути функцію",
        detail=html.escape(detail), time=html.escape(s["time"]), tz=html.escape(os.environ.get("TZ", "UTC")),
        clock=f"{now():%d.%m.%Y %H:%M}", log=html.escape(tail),
        skip="Не пропускати цю ніч" if skipped else "Пропустити цю ніч")


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path.split("?")[0] not in ("/", "/index.html"):
            self.send_error(404)
            return
        body = render().encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        length = min(int(self.headers.get("Content-Length") or 0), 4096)
        form = urllib.parse.parse_qs(self.rfile.read(length).decode("utf-8", "replace"))
        s = load()
        path = self.path.split("?")[0]
        if path == "/toggle":
            s["enabled"] = not s["enabled"]
            save(s)
            log("Функцію " + ("увімкнено" if s["enabled"] else "вимкнено") + " на сторінці")
        elif path == "/time":
            value = (form.get("time") or [""])[0]
            try:
                hh, mm = (int(x) for x in value.split(":")[:2])
                if not (0 <= hh < 24 and 0 <= mm < 60):
                    raise ValueError
                s["time"] = f"{hh:02d}:{mm:02d}"
                save(s)
                log("Час вимкнення: " + s["time"])
            except ValueError:
                pass
        elif path == "/skip":
            nxt = next_run(s)
            if nxt is not None:
                day = f"{nxt:%Y-%m-%d}"
                s["skip"] = "" if s.get("skip") == day else day
                save(s)
                log(("Пропущу" if s["skip"] else "Не пропускатиму") + f" вимкнення {nxt:%d.%m}")
        elif path == "/check":
            threading.Thread(target=run, args=(True,), daemon=True).start()
            time.sleep(3)
        else:
            self.send_error(404)
            return
        self.send_response(303)
        self.send_header("Location", "/")
        self.end_headers()

    def log_message(self, *args):
        pass


def main():
    os.makedirs(DATA, exist_ok=True)
    log(f"Запущено. Вимкнення {'увімкнено' if load()['enabled'] else 'вимкнено'}, час {load()['time']} ({os.environ.get('TZ', 'UTC')})")
    threading.Thread(target=scheduler, daemon=True).start()
    http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()


if __name__ == "__main__":
    main()
