"""Start the installed NeoForge client directly (offline name), optionally auto-joining a server.

Uses the version files the official launcher already downloaded (.minecraft/versions, libraries, assets).
This is the core a future custom launcher needs; for now it is used for unattended join tests.

  python launch_client.py --game-dir DIR --name WW_Test [--join host:port] [--java PATH] [--xmx 6G]
"""
import argparse, json, os, platform, re, subprocess, uuid

MC = os.path.join(os.environ["APPDATA"], ".minecraft")
VERSION = "neoforge-21.1.264"


def load(vid):
    return json.load(open(os.path.join(MC, "versions", vid, vid + ".json"), encoding="utf-8"))


def allowed(rules):
    if not rules:
        return True
    ok = False
    for r in rules:
        osr, feats = r.get("os"), r.get("features")
        if feats:
            continue  # demo / custom resolution / quick play features: we pass those ourselves
        if osr and osr.get("name") and osr["name"] != "windows":
            continue
        if osr and osr.get("arch") and osr["arch"] != ("x86" if platform.architecture()[0] == "32bit" else "x64"):
            continue
        ok = r["action"] == "allow"
    return ok


def lib_path(lib):
    art = lib.get("downloads", {}).get("artifact")
    if art and art.get("path"):
        return os.path.join(MC, "libraries", art["path"])
    g, a, v, *cls = lib["name"].split(":")
    fn = f"{a}-{v}" + (f"-{cls[0]}" if cls else "") + ".jar"
    return os.path.join(MC, "libraries", *g.split("."), a, v, fn)


def flatten(args):
    out = []
    for a in args:
        if isinstance(a, str):
            out.append(a)
        elif allowed(a.get("rules")):
            v = a["value"]
            out.extend(v if isinstance(v, list) else [v])
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-dir", required=True)
    ap.add_argument("--name", default="WW_Test")
    ap.add_argument("--join")
    ap.add_argument("--java", default="java")
    ap.add_argument("--xmx", default="6G")
    ap.add_argument("--jvm-arg", action="append", default=[], help="extra JVM argument, e.g. -Dwilds_wiki.selftest=true")
    ap.add_argument("--size", help="window size WxH")
    o = ap.parse_args()

    child, parent = load(VERSION), load(load(VERSION)["inheritsFrom"])
    libs, seen = [], set()
    for lib in child["libraries"] + parent["libraries"]:
        key = re.sub(r":[^:]+$", "", lib["name"]) if lib["name"].count(":") == 2 else lib["name"]
        if key in seen or not allowed(lib.get("rules")):
            continue
        seen.add(key)
        libs.append(lib_path(lib))
    # no vanilla client jar on the classpath: NeoForge loads its patched copy, a second one breaks module resolution
    missing = [p for p in libs if not os.path.exists(p)]
    if missing:
        raise SystemExit("missing libraries (start the profile once from the launcher): " + "; ".join(missing[:5]))

    subst = {
        "natives_directory": os.path.join(MC, "versions", VERSION, "natives"), "launcher_name": "wilds", "launcher_version": "1",
        "classpath": os.pathsep.join(libs), "classpath_separator": os.pathsep, "library_directory": os.path.join(MC, "libraries"),
        "version_name": VERSION, "auth_player_name": o.name, "game_directory": o.game_dir, "assets_root": os.path.join(MC, "assets"),
        "assets_index_name": parent["assetIndex"]["id"], "auth_uuid": str(uuid.uuid3(uuid.NAMESPACE_OID, o.name)).replace("-", ""),
        "auth_access_token": "0", "clientid": "0", "auth_xuid": "0", "user_type": "legacy", "version_type": "release",
    }
    fill = lambda s: re.sub(r"\$\{(\w+)\}", lambda m: subst.get(m.group(1), m.group(0)), s)
    jvm = [fill(a) for a in flatten(parent["arguments"]["jvm"]) + flatten(child["arguments"].get("jvm", []))]
    game = [fill(a) for a in flatten(parent["arguments"]["game"]) + flatten(child["arguments"].get("game", []))]
    cmd = [o.java, f"-Xmx{o.xmx}", "-Xms2G", *o.jvm_arg, *jvm, child["mainClass"], *game]
    if o.size:
        w, h = o.size.lower().split("x")
        cmd += ["--width", w, "--height", h]
    if o.join:
        cmd += ["--quickPlayMultiplayer", o.join]
    os.makedirs(o.game_dir, exist_ok=True)
    log = open(os.path.join(o.game_dir, "launch_client.out"), "w", encoding="utf-8")
    p = subprocess.Popen(cmd, cwd=o.game_dir, stdout=log, stderr=subprocess.STDOUT)
    print("started pid", p.pid)


if __name__ == "__main__":
    main()
