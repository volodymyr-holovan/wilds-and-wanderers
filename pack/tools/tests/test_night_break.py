"""Checks the night break app (pack/server/night_shutdown.py) without TrueNAS: a fake clock and a fake docker.

  python pack/tools/tests/test_night_break.py

Scenarios: TrueNAS switched on by day, at night and right after midnight; the 00:00 stop and the 06:00 start by the
scheduler; a skipped night; the feature switched off; the state file of the old night shutdown; and that TrueNAS is
never powered off.
"""
import datetime, importlib.util, json, os, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "server", "night_shutdown.py")


def fresh(clock, containers_running=True, state=None):
    d = tempfile.mkdtemp()
    os.environ["DATA_DIR"] = d
    os.environ["BOOT_SETTLE"] = "0"
    if state is not None:
        json.dump(state, open(os.path.join(d, "state.json"), "w"))
    spec = importlib.util.spec_from_file_location("nb", SRC)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    world = {"running": {"ix-crafty-crafty-1": containers_running}, "calls": [], "mc": containers_running}

    def host(*cmd, timeout=120):
        world["calls"].append(" ".join(cmd))
        class R: returncode, stdout, stderr = 0, "", ""
        if cmd[:2] == ("docker", "ps"):
            R.stdout = "\n".join(f"id{i} {n}" for i, (n, up) in enumerate(world["running"].items()) if up or "-a" in cmd)
        elif cmd[:2] == ("docker", "stop"):
            for n in world["running"]: world["running"][n] = False
            world["mc"] = False
        elif cmd[:2] == ("docker", "start"):
            world["running"][cmd[2]] = True
            world["mc"] = True
        elif "midclt" in cmd and "system.shutdown" in cmd:
            world["shutdown"] = True
        return R
    m.host = host
    m.minecraft_pids = lambda: [4242] if world["mc"] else []
    m.now = lambda: clock[0]
    m.time.sleep = lambda s: None
    m.docker_ready = lambda: True
    return m, world


def at(h, mi=0, day=25):
    return datetime.datetime(2026, 11, day, h, mi, 5)


fails = []


def check(name, cond):
    print(("ok   " if cond else "FAIL ") + name)
    if not cond: fails.append(name)


# 1. switched on by day while Crafty is stopped (the night break left it stopped, then a power cut): it starts
clock = [at(9)]
m, w = fresh(clock, containers_running=False, state={"restart": ["ix-crafty-crafty-1"]})
m.at_boot()
check("boot by day starts Crafty", w["running"]["ix-crafty-crafty-1"] and w["mc"])

# 2. switched on at 03:00: Crafty came up by itself and started Minecraft -> closed for the night
clock = [at(3)]
m, w = fresh(clock, containers_running=True)
m.at_boot()
check("boot at night stops Minecraft", not w["running"]["ix-crafty-crafty-1"] and not w["mc"])
check("the stopped container is remembered", m.load()["restart"] == ["ix-crafty-crafty-1"])

# 3. switched on at 23:59 (still open) -> starts; at 00:00 the scheduler stops it
clock = [at(23, 59)]
m, w = fresh(clock, containers_running=False)
m.at_boot()
check("boot at 23:59 starts Crafty", w["mc"])
clock[0] = at(0, 0, day=26)
m.tick()
check("00:00 stops Minecraft", not w["mc"])
m.tick()
check("the stop runs once", sum("docker stop" in c for c in w["calls"]) == 1)

# 4. 06:00 the scheduler starts it again, once
clock[0] = at(6, 0, day=26)
m.tick(); m.tick()
check("06:00 starts Minecraft", w["mc"])
check("the start runs once", sum(c.startswith("docker start") for c in w["calls"]) == 2)

# 5. a skipped night: 00:00 does nothing
clock = [at(23)]
m, w = fresh(clock, containers_running=True, state={"skip": "2026-11-26"})
clock[0] = at(0, 0, day=26)
m.tick()
check("a skipped night keeps Minecraft", w["mc"])

# 6. the feature off: neither boot nor 00:00 touch anything
clock = [at(3)]
m, w = fresh(clock, containers_running=True, state={"enabled": False})
m.at_boot(); clock[0] = at(0, 0, day=26); m.tick()
check("switched off: nothing happens", w["mc"] and not any("docker stop" in c or "docker start" in c for c in w["calls"]))

# 7. the old night shutdown's state file: its time becomes the stop time
clock = [at(12)]
m, w = fresh(clock, state={"enabled": True, "time": "23:30", "last_run": "2026-11-24", "skip": "", "restart": []})
s = m.load()
check("old state migrates", s["stop"] == "23:30" and s["start"] == "06:00" and "time" not in s)

# 8. open hours
s = {"start": "06:00", "stop": "00:00"}
check("open 06:00-23:59", all(m.is_open(s, at(h)) for h in range(6, 24)) and not any(m.is_open(s, at(h)) for h in range(0, 6)))
s = {"start": "08:00", "stop": "22:00"}
check("open 08:00-21:59 (same day)", m.is_open(s, at(8)) and m.is_open(s, at(21, 59)) and not m.is_open(s, at(22)) and not m.is_open(s, at(7)))

# 9. TrueNAS is never powered off
check("TrueNAS never shut down", not any("system.shutdown" in c for c in w["calls"]))

# 10. a late stop (the app restarted at 00:05) still runs; one at 00:15 does not
clock = [at(0, 5, day=26)]
m, w = fresh(clock, containers_running=True)
m.tick()
check("a stop 5 min late still runs", not w["mc"])

print("all good" if not fails else f"{len(fails)} FAILED")
sys.exit(1 if fails else 0)
