"""Нічна перерва: the TrueNAS app that closes the Wilds & Wanderers Minecraft server for the night (25.11).

TrueNAS itself stays on all night (until 25.11 this app switched the whole box off). When the switch is on:
  * at the stop time (00:00 by default, in the TZ of the container) Crafty is stopped with a long grace period:
    Crafty stops Minecraft the proper way ("stop": players saved, world saved), so nobody can join until morning;
  * at the start time (06:00) Crafty is started again, and Crafty starts Minecraft itself (its server has auto start);
  * when TrueNAS is switched on (this app starts with it) or the app restarts, the clock decides: inside the open
    hours (06:00-00:00) Crafty is started if it is not running; inside the night it is stopped, because Crafty would
    otherwise start Minecraft on its own.
Only these moments act: in between, a server an admin starts or stops by hand is left as it is.

The switch, both times, "skip tonight", "start now" and a log are on the app's web page (the 🌐 button of the app's
dashboard widget). Stopping the app also switches the feature off (and leaves Crafty as it is).

Runs in a privileged container with the host's PID namespace (pid: host): host commands (docker) run through nsenter
as root. The page only flips settings; it runs no command a visitor could choose.
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
WINDOW = 10 * 60                                           # a stop or start up to this late still runs (the app restarted)
BOOT_SETTLE = int(os.environ.get("BOOT_SETTLE", "60"))     # at night: let Crafty start before it is stopped again
LOCK = threading.Lock()
BUSY = threading.Lock()


def now():
    return datetime.datetime.now()


def load():
    state = {"enabled": True, "stop": "00:00", "start": "06:00", "last_stop": "", "last_start": "", "skip": "", "restart": []}
    try:
        with open(STATE_FILE, encoding="utf-8") as f:
            old = json.load(f)
        if "time" in old and "stop" not in old:      # the night shutdown before 25.11 kept one time
            old["stop"] = old.pop("time")
        old.pop("time", None)
        old.pop("last_run", None)
        state.update(old)
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


def crafty_containers(running_only=True):
    """[(id, name)] of the Crafty app's containers (the running ones, or all of them)"""
    args = ["docker", "ps", "--format", "{{.ID}} {{.Names}}"] + ([] if running_only else ["-a"])
    r = host(*args)
    if r.returncode != 0:
        raise RuntimeError("docker ps: " + (r.stderr or r.stdout).strip()[:200])
    return [line.split(" ", 1) for line in r.stdout.splitlines() if CRAFTY in line.split(" ", 1)[-1]]


def docker_ready():
    try:
        return host("docker", "info", "--format", "{{.ServerVersion}}", timeout=30).returncode == 0
    except Exception:
        return False


def minutes(value):
    hh, mm = (int(x) for x in value.split(":")[:2])
    return hh * 60 + mm


def is_open(state, t=None):
    """inside the open hours: from the start time to the stop time (across midnight when the stop comes first)"""
    t = t or now()
    m, start, stop = t.hour * 60 + t.minute, minutes(state["start"]), minutes(state["stop"])
    if start == stop:
        return True
    return start <= m < stop if start < stop else (m >= start or m < stop)


def open_minecraft(why):
    """start every Crafty container that is not running; Crafty then starts Minecraft itself"""
    if not BUSY.acquire(blocking=False):
        log("вже виконується інша дія")
        return False
    try:
        state = load()
        running = {name for _, name in crafty_containers()}
        names = sorted(set(state.get("restart") or []) | {name for _, name in crafty_containers(False)})
        ok = True
        for name in names:
            if name in running:
                continue
            r = host("docker", "start", name, timeout=300)
            if r.returncode == 0:
                log(f"{name} запущено ({why}); Crafty сам запустить Minecraft")
            else:
                ok = False
                log(f"{name} не запустився: " + (r.stderr or r.stdout).strip()[:300])
        if ok:
            state = load()
            state["restart"] = []
            save(state)
            if names and all(n in running for n in names):
                log(f"Crafty уже працює ({why})")
        return ok
    finally:
        BUSY.release()


def close_minecraft(why, dry=False):
    """stop the Crafty containers with a long grace period: Crafty saves the world and stops Minecraft"""
    if not BUSY.acquire(blocking=False):
        log("вже виконується інша дія")
        return False
    try:
        pids, containers = minecraft_pids(), crafty_containers()
        log(f"Сервер Minecraft: {'працює (процес ' + ', '.join(map(str, pids)) + ')' if pids else 'не запущено'}; "
            f"Crafty: {', '.join(name for _, name in containers) or 'не запущено'}")
        if dry:
            log("Перевірку завершено: о часі перерви все це спрацює так само, але насправді.")
            return True
        if not containers:
            log(f"Нічого зупиняти ({why}).")
            return True
        state = load()
        state["restart"] = sorted(set(state.get("restart") or []) | {name for _, name in containers})
        save(state)
        for cid, name in containers:
            log(f"Зупиняю {name} ({why}): Crafty зберігає світ і зупиняє Minecraft (до {STOP_TIMEOUT // 60} хв)…")
            r = host("docker", "stop", "-t", str(STOP_TIMEOUT), cid, timeout=STOP_TIMEOUT + 120)
            log(f"{name} зупинено" if r.returncode == 0 else f"{name}: " + (r.stderr or r.stdout).strip()[:300])
        for _ in range(60):
            if not minecraft_pids():
                break
            time.sleep(2)
        left = minecraft_pids()
        log(f"Minecraft досі працює ({left}) — перевірте Crafty вручну." if left
            else "Світ збережено, Minecraft зупинено. TrueNAS працює далі.")
        return not left
    finally:
        BUSY.release()


def at_boot():
    """TrueNAS was switched on, or the app restarted: put Minecraft where the clock says (docker may still be coming up)"""
    for _ in range(90):
        if docker_ready():
            break
        time.sleep(10)
    else:
        log("Docker так і не запустився: нічого не роблю.")
        return
    state = load()
    if not state["enabled"]:
        log("Старт: функцію вимкнено, Minecraft лишаю як є.")
        return
    try:
        if is_open(state):
            open_minecraft("TrueNAS увімкнено вдень")
        else:
            # Crafty starts Minecraft by itself when its container comes up: let it, then close for the night
            time.sleep(BOOT_SETTLE)
            close_minecraft("TrueNAS увімкнено вночі")
    except Exception as e:
        log("Старт: " + str(e))


def moment_today(value):
    hh, mm = (int(x) for x in value.split(":")[:2])
    return now().replace(hour=hh, minute=mm, second=0, microsecond=0)


def due(state, key, last_key):
    """(is the stop or start of today due and not done yet, today's date)"""
    t = moment_today(state[key])
    day = f"{t:%Y-%m-%d}"
    return 0 <= (now() - t).total_seconds() < WINDOW and state.get(last_key) != day, day


def tick():
    """one look at the clock: the stop or the start when its moment has come"""
    state = load()
    if not state["enabled"]:
        return
    stop_due, day = due(state, "stop", "last_stop")
    if stop_due:
        state["last_stop"] = day
        save(state)
        if state.get("skip") == day:
            log("Сьогоднішню нічну перерву пропущено (так вирішили на сторінці).")
        else:
            close_minecraft("нічна перерва")
    state = load()
    start_due, day = due(state, "start", "last_start")
    if start_due:
        state["last_start"] = day
        save(state)
        open_minecraft("ранок")


def scheduler():
    while True:
        try:
            tick()
        except Exception as e:
            log("Помилка планувальника: " + str(e))
        time.sleep(15)


def next_moment(state, key, last_key):
    t = moment_today(state[key])
    if now() >= t or state.get(last_key) == f"{t:%Y-%m-%d}":
        t += datetime.timedelta(days=1)
    return t


PAGE = """<!doctype html>
<html lang="uk"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Нічна перерва</title>
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
<h1>Нічна перерва</h1>
<p class="sub">Сервер Wilds &amp; Wanderers: уночі Minecraft зберігає світ і зупиняється, зранку запускається сам. TrueNAS працює весь час.</p>
<div class="card"><div class="row">
<div class="state %(cls)s">%(state)s</div>
<form method="post" action="/toggle"><button class="big %(btn)s">%(action)s</button></form>
</div><p class="muted">%(detail)s</p></div>
<div class="card"><form method="post" action="/times"><div class="row">
<label>Вимкнути о <input type="time" name="stop" value="%(stop)s" required></label>
<label>Увімкнути о <input type="time" name="start" value="%(start)s" required></label>
<button class="ghost">Зберегти</button></div></form>
<p class="muted">Часовий пояс: %(tz)s. Зараз %(clock)s — %(phase)s.</p>
<div class="row">
<form method="post" action="/skip"><button class="ghost">%(skip)s</button></form>
<form method="post" action="/open"><button class="ghost">Запустити Minecraft зараз</button></form>
<form method="post" action="/check"><button class="ghost">Перевірити (нічого не вимикає)</button></form>
</div></div>
<div class="card"><pre>%(log)s</pre></div>
</main></body></html>"""


def render():
    s = load()
    nstop, nstart = next_moment(s, "stop", "last_stop"), next_moment(s, "start", "last_start")
    skipped = s.get("skip") == f"{nstop:%Y-%m-%d}"
    if s["enabled"]:
        detail = (f"Наступна перерва: {nstop:%d.%m о %H:%M}" + (" — пропускається" if skipped else "") +
                  f", запуск: {nstart:%d.%m о %H:%M}. Гравців на сервері в момент перерви буде відключено.")
    else:
        detail = "Minecraft працюватиме, доки його не зупинять вручну."
    try:
        with open(LOG_FILE, encoding="utf-8") as f:
            tail = "\n".join(f.read().splitlines()[-40:][::-1])
    except OSError:
        tail = "Поки що записів немає."
    return PAGE % dict(
        cls="on" if s["enabled"] else "off", state="Увімкнено" if s["enabled"] else "Вимкнено",
        btn="stop" if s["enabled"] else "go", action="Вимкнути функцію" if s["enabled"] else "Увімкнути функцію",
        detail=html.escape(detail), stop=html.escape(s["stop"]), start=html.escape(s["start"]),
        tz=html.escape(os.environ.get("TZ", "UTC")), clock=f"{now():%d.%m.%Y %H:%M}",
        phase="сервер має працювати" if is_open(s) else "нічна перерва", log=html.escape(tail),
        skip="Не пропускати цю ніч" if skipped else "Пропустити цю ніч")


def valid(value):
    hh, mm = (int(x) for x in value.split(":")[:2])
    if not (0 <= hh < 24 and 0 <= mm < 60):
        raise ValueError
    return f"{hh:02d}:{mm:02d}"


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
        elif path == "/times":
            try:
                s["stop"], s["start"] = valid((form.get("stop") or [""])[0]), valid((form.get("start") or [""])[0])
                save(s)
                log(f"Перерва: з {s['stop']} до {s['start']}")
            except ValueError:
                pass
        elif path == "/skip":
            day = f"{next_moment(s, 'stop', 'last_stop'):%Y-%m-%d}"
            s["skip"] = "" if s.get("skip") == day else day
            save(s)
            log(("Пропущу" if s["skip"] else "Не пропускатиму") + f" перерву {day}")
        elif path == "/open":
            threading.Thread(target=open_minecraft, args=("запущено на сторінці",), daemon=True).start()
            time.sleep(3)
        elif path == "/check":
            threading.Thread(target=close_minecraft, args=("перевірка", True), daemon=True).start()
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
    s = load()
    save(s)
    log(f"Запущено. Нічна перерва {'увімкнена' if s['enabled'] else 'вимкнена'}: з {s['stop']} до {s['start']} "
        f"({os.environ.get('TZ', 'UTC')}); TrueNAS більше не вимикається")
    threading.Thread(target=at_boot, daemon=True).start()
    threading.Thread(target=scheduler, daemon=True).start()
    http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()


if __name__ == "__main__":
    main()
