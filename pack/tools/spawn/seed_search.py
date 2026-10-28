"""Find an overworld seed and a place in it for the pack's world: land all around the spawn (no ocean within MIN_LAND blocks, so the spawn
is not on an island) and ground calm enough to set a ~100-block spawn build on it.

For each seed the LOCAL test server (testserver/) creates a throwaway world seedtest_<n>, then:
  - the world spawn is read from level.dat;
  - `locate biome #minecraft:is_ocean` from the spawn gives the nearest ocean;
  - the chunks around the spawn are force-loaded and saved, and the surface heights and biomes are read straight from
    the region files.
The test server's own world and settings are put back at the end.

usage: python pack/tools/spawn/seed_search.py [count] [--seeds a,b,c]     (log: _tools/seed_search.log)
"""
import io, math, os, random, re, struct, subprocess, sys, time, zlib
import numpy as np
import nbtlib

ROOT = r"E:\Minecraft Server 1.21.1"
TS = os.path.join(ROOT, "testserver")
PROPS = os.path.join(TS, "server.properties")
LOG = os.path.join(ROOT, "_tools", "seed_search.log")
RC = open(os.path.join(ROOT, "_tools", "test_rcon_password.txt")).read().strip()
MIN_LAND = 600
AREA = 64          # half-size of the spawn area that is checked for calm ground


def log(*a):
    line = " ".join(str(x) for x in a)
    print(line, flush=True)
    with open(LOG, "a", encoding="utf-8") as f:
        f.write(line + "\n")


def rcon(cmd, timeout=300):
    r = subprocess.run([sys.executable, "-I", os.path.join(ROOT, "pack", "tools", "rcon.py"), "127.0.0.1", "25580", RC, cmd],
                       capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
    return "\n".join(l for l in r.stdout.splitlines() if l.strip() and not l.startswith(">"))


def java_running():
    out = subprocess.run(["powershell", "-NoProfile", "-Command",
                          "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*win_args.txt*' }).Count"],
                         capture_output=True, text=True).stdout.strip()
    return out not in ("", "0")


def stop_server():
    try:
        rcon("stop", timeout=30)
    except Exception:
        pass
    for _ in range(90):
        if not java_running():
            return
        time.sleep(2)
    subprocess.run(["powershell", "-NoProfile", "-Command",
                    "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*win_args.txt*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"])
    time.sleep(3)


def start_server():
    console = open(os.path.join(TS, "console.log"), "w", encoding="utf-8")
    java = os.path.join(ROOT, "_tools", "java21", "bin", "java.exe")
    subprocess.Popen([java, "@user_jvm_args.txt", "@libraries/net/neoforged/neoforge/21.1.264/win_args.txt", "nogui"],
                     cwd=TS, stdout=console, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                     creationflags=subprocess.CREATE_NEW_PROCESS_GROUP)
    for _ in range(300):
        time.sleep(3)
        text = open(os.path.join(TS, "console.log"), encoding="utf-8", errors="replace").read()
        if "Done (" in text:
            return True
        if "Crash report saved" in text or "Mod loading has failed" in text:
            return False
    return False


def set_props(**kv):
    lines = open(PROPS, encoding="utf-8").read().splitlines()
    for i, l in enumerate(lines):
        k = l.split("=", 1)[0]
        if k in kv:
            lines[i] = f"{k}={kv[k]}"
    open(PROPS, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")


def get_props(*keys):
    d = dict(l.split("=", 1) for l in open(PROPS, encoding="utf-8").read().splitlines() if "=" in l and not l.startswith("#"))
    return {k: d.get(k, "") for k in keys}


# --- region files ------------------------------------------------------------------------------------------------------
def read_chunk(region_dir, cx, cz):
    path = os.path.join(region_dir, f"r.{cx >> 5}.{cz >> 5}.mca")
    if not os.path.exists(path):
        return None
    with open(path, "rb") as f:
        head = f.read(4096)
        i = 4 * ((cx & 31) + (cz & 31) * 32)
        off = int.from_bytes(head[i:i + 3], "big")
        if off == 0:
            return None
        f.seek(off * 4096)
        length, comp = struct.unpack(">iB", f.read(5))
        data = f.read(length - 1)
    raw = zlib.decompress(data) if comp == 2 else data
    return nbtlib.File.parse(io.BytesIO(raw))


def unpack(longs, bits, count):
    per = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for v in longs:
        v = int(v) & 0xFFFFFFFFFFFFFFFF
        for j in range(per):
            out.append((v >> (j * bits)) & mask)
            if len(out) == count:
                return out
    return out


def surface(region_dir, x0, z0, x1, z1):
    """Top solid (non-leaf) block Y over [x0..x1]x[z0..z1] and the biomes at that height."""
    h = np.full((z1 - z0 + 1, x1 - x0 + 1), -999, dtype=np.int32)
    biomes = {}
    for cx in range(x0 >> 4, (x1 >> 4) + 1):
        for cz in range(z0 >> 4, (z1 >> 4) + 1):
            c = read_chunk(region_dir, cx, cz)
            if c is None or "Heightmaps" not in c or "MOTION_BLOCKING_NO_LEAVES" not in c["Heightmaps"]:
                continue
            hm = unpack(c["Heightmaps"]["MOTION_BLOCKING_NO_LEAVES"], 9, 256)
            for i, v in enumerate(hm):
                x, z = cx * 16 + (i & 15), cz * 16 + (i >> 4)
                if x0 <= x <= x1 and z0 <= z <= z1:
                    h[z - z0, x - x0] = v - 64 - 1
            # biome of the section that holds the chunk's middle surface
            y = hm[8 * 16 + 8] - 65
            for s in c.get("sections", []):
                if int(s["Y"]) == y >> 4 and "biomes" in s:
                    for b in s["biomes"]["palette"]:
                        biomes[str(b)] = biomes.get(str(b), 0) + 1
    return h, biomes


def ocean_distance(x, z):
    out = rcon(f"execute in minecraft:overworld positioned {x} 100 {z} run locate biome #minecraft:is_ocean")
    m = re.search(r"\((\d+) blocks away\)", out)
    return int(m.group(1)) if m else 99999


def evaluate(n, seed):
    """the world spawn and 16 points on two rings around the origin; the one farthest from any ocean is measured"""
    name = f"seedtest_{seed}"
    set_props(**{"level-name": name, "level-seed": seed})
    if not start_server():
        log(f"{seed}: server failed to start (see testserver/console.log)")
        return None
    rcon("save-all flush")
    time.sleep(2)
    lvl = nbtlib.load(os.path.join(TS, name, "level.dat"))["Data"]
    points = [(int(lvl["SpawnX"]), int(lvl["SpawnZ"]))]
    for r in (1200, 2400):
        for k in range(8):
            a = k * math.pi / 4
            points.append((int(r * math.cos(a)), int(r * math.sin(a))))
    dists = [(ocean_distance(x, z), (x, z)) for x, z in points]
    dists.sort(reverse=True)
    ocean, (sx, sz) = dists[0]
    rcon(f"forceload add {sx - AREA} {sz - AREA} {sx + AREA} {sz + AREA}")
    time.sleep(10)
    rcon("save-all flush")
    time.sleep(3)
    h, biomes = surface(os.path.join(TS, name, "region"), sx - AREA, sz - AREA, sx + AREA, sz + AREA)
    rcon("forceload remove all")
    valid = h[h > -999]
    inner = h[AREA - 50:AREA + 51, AREA - 50:AREA + 51]
    inner = inner[inner > -999]
    res = dict(seed=seed, spawn=(sx, sz), ocean=ocean, others=[d for d, _ in dists[1:4]],
               y_med=int(np.median(valid)) if valid.size else None,
               inner_std=round(float(inner.std()), 1) if inner.size else None,
               p10_p90=(int(np.percentile(inner, 10)), int(np.percentile(inner, 90))) if inner.size else None,
               biomes=sorted(biomes, key=lambda b: -biomes[b])[:4], coverage=f"{valid.size}/{h.size}")
    log(res)
    stop_server()
    return res


def main():
    count = int(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1].isdigit() else 10
    seeds = []
    if "--seeds" in sys.argv:
        seeds = sys.argv[sys.argv.index("--seeds") + 1].split(",")
    rnd = random.Random()
    while len(seeds) < count:
        seeds.append(str(rnd.randint(-2**63, 2**63 - 1)))
    original = get_props("level-name", "level-seed")
    log(f"=== seed search {time.strftime('%Y-%m-%d %H:%M')}, keeping {original}")
    stop_server()
    good = []
    try:
        for n, seed in enumerate(seeds):
            r = evaluate(n, seed)
            if r and r["ocean"] >= MIN_LAND and r["p10_p90"] and r["p10_p90"][1] - r["p10_p90"][0] <= 10:
                good.append(r)
                log(f"  GOOD #{len(good)}")
    finally:
        stop_server()
        set_props(**original)
    log("=== good:", *good, sep="\n")


if __name__ == "__main__":
    main()
