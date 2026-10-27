"""Find a seed whose world spawn is on a big continent, not on an island, using the local test server (all worldgen mods).

  python seed_probe.py [--tries 8] [--seed S ...]

For each seed: stop the test server, delete its world, set level-seed, start it, read the spawn from level.dat and
sample biomes on a grid around the spawn over RCON (execute if biome ... #minecraft:is_ocean, no chunks generated).
Prints the share of land within 500 / 1000 / 1500 blocks and the nearest ocean. The last world stays in testserver/world.
"""
import argparse, gzip, io, os, random, re, shutil, socket, struct, subprocess, sys, time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TS = os.path.join(ROOT, "testserver")
PW = open(os.path.join(ROOT, "_tools", "test_rcon_password.txt")).read().strip()
JAVA = os.path.join(ROOT, "_tools", "java21", "bin", "java.exe")


class Rcon:
    def __init__(self):
        self.s = socket.create_connection(("127.0.0.1", 25580), timeout=60)
        self.send(3, PW)
        if self.recv()[0] == -1:
            raise SystemExit("rcon auth failed")

    def send(self, t, body):
        data = struct.pack("<ii", 7, t) + body.encode() + b"\0\0"
        self.s.sendall(struct.pack("<i", len(data)) + data)

    def recv(self):
        size = struct.unpack("<i", self._exact(4))[0]
        d = self._exact(size)
        return struct.unpack("<i", d[:4])[0], d[8:-2].decode("utf-8", "replace")

    def _exact(self, n):
        b = b""
        while len(b) < n:
            c = self.s.recv(n - len(b))
            if not c:
                raise ConnectionError("closed")
            b += c
        return b

    def cmd(self, c):
        self.send(2, c)
        return self.recv()[1]


def nbt_spawn(path):
    """SpawnX / SpawnZ from level.dat (gzipped NBT): a tiny reader that walks just enough of the format."""
    data = gzip.open(path).read()
    f = io.BytesIO(data)
    out = {}

    def rd(n):
        return f.read(n)

    def name():
        return rd(struct.unpack(">H", rd(2))[0]).decode("utf-8", "replace")

    def payload(t, key):
        if t == 1: rd(1)
        elif t == 2: rd(2)
        elif t == 3:
            v = struct.unpack(">i", rd(4))[0]
            if key in ("SpawnX", "SpawnY", "SpawnZ"):
                out[key] = v
        elif t == 4: rd(8)
        elif t == 5: rd(4)
        elif t == 6: rd(8)
        elif t == 7: rd(struct.unpack(">i", rd(4))[0])
        elif t == 8: name()
        elif t == 9:
            et = rd(1)[0]
            for _ in range(struct.unpack(">i", rd(4))[0]):
                payload(et, None)
        elif t == 10:
            while True:
                tt = rd(1)[0]
                if tt == 0:
                    break
                payload(tt, name())
        elif t == 11: rd(4 * struct.unpack(">i", rd(4))[0])
        elif t == 12: rd(8 * struct.unpack(">i", rd(4))[0])

    t = rd(1)[0]
    name()
    payload(t, None)
    return out.get("SpawnX"), out.get("SpawnZ")


def server_running():
    r = subprocess.run(["powershell", "-NoProfile", "-Command",
                        "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*win_args.txt*' }).Count"],
                       capture_output=True, text=True)
    return r.stdout.strip() not in ("", "0")


def stop():
    try:
        Rcon().cmd("stop")
    except Exception:
        pass
    for _ in range(60):
        if not server_running():
            return
        time.sleep(2)
    subprocess.run(["powershell", "-NoProfile", "-Command",
                    "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*win_args.txt*' } | "
                    "ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"])
    time.sleep(3)


def start():
    log = open(os.path.join(TS, "console.log"), "w", encoding="utf-8")
    subprocess.Popen([JAVA, "@user_jvm_args.txt", "@libraries/net/neoforged/neoforge/21.1.264/win_args.txt", "nogui"], cwd=TS,
                     stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    for _ in range(200):
        time.sleep(3)
        text = open(os.path.join(TS, "console.log"), encoding="utf-8", errors="replace").read()
        if "Done (" in text:
            return True
        if "Crash report saved" in text or "Mod loading has failed" in text:
            return False
    return False


def set_seed(seed):
    p = os.path.join(TS, "server.properties")
    s = open(p, encoding="utf-8").read()
    s = re.sub(r"^level-seed=.*$", f"level-seed={seed}", s, flags=re.M)
    open(p, "w", encoding="utf-8").write(s)


def probe(seed):
    stop()
    shutil.rmtree(os.path.join(TS, "world"), ignore_errors=True)
    set_seed(seed)
    if not start():
        return None
    r = Rcon()
    r.cmd("save-all flush")  # the world spawn is in level.dat only after the first save
    time.sleep(2)
    sx, sz = nbt_spawn(os.path.join(TS, "world", "level.dat"))
    # "execute if biome" only works in loaded chunks; "locate biome" reads the biome source directly.
    # Distance to the nearest ocean from the spawn and from 8 points on rings of 400 and 900 blocks:
    # 0 means that point is ocean; an island spawn has ocean close by in most directions.
    def ocean_distance(x, z):
        out = r.cmd(f"execute positioned {x} 64 {z} run locate biome #minecraft:is_ocean")
        m = re.search(r"\((\d+) blocks away\)", out)
        return int(m[1]) if m else 99999

    import math
    d0 = ocean_distance(sx, sz)
    rings = {}
    for radius in (400, 900):
        ds = []
        for i in range(8):
            a = math.tau * i / 8
            ds.append(ocean_distance(int(sx + radius * math.cos(a)), int(sz + radius * math.sin(a))))
        rings[radius] = ds
    return {"seed": seed, "spawn": (sx, sz), "nearest_ocean": d0,
            "land400": sum(1 for d in rings[400] if d > 0), "land900": sum(1 for d in rings[900] if d > 0),
            "min400": min(rings[400]), "min900": min(rings[900])}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tries", type=int, default=8)
    ap.add_argument("--seed", action="append", default=[])
    o = ap.parse_args()
    seeds = o.seed or [str(random.randint(-2 ** 63, 2 ** 63 - 1)) for _ in range(o.tries)]
    best = None
    for s in seeds:
        res = probe(s)
        print(res, flush=True)
        if res and res["nearest_ocean"] > 0:
            score = min(res["nearest_ocean"], 1500) / 300 + res["land400"] + res["land900"] * 0.7 + min(res["min400"], 600) / 300
            if best is None or score > best[0]:
                best = (score, res)
    print("BEST:", best)


main()
