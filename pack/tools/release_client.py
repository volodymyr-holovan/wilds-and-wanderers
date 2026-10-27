"""Publish the pack for players' computers (the Wilds & Wanderers program in launcher/src installs and updates from it).

  python pack/tools/release_client.py          build, create the GitHub release pack-<version>, commit and push the manifest
  python pack/tools/release_client.py --dry    build only: launcher/dist/<version>/ and launcher/manifest.json
  python pack/tools/release_client.py --app    also build the program and publish it (release app-v<version>); config.json
                                               then points every installed copy to it (they update themselves)

What a player's computer gets:
  - every client mod (docs/mods.json: both/client/opt-client) straight from Modrinth/CurseForge, checked by SHA-1;
    nothing of other authors is re-uploaded;
  - the pack's own mods (wilds_core, wilds_wiki) and an archive of pack/overrides (+ overrides-initial: copied once,
    then the player's) as assets of the release;
  - the shader packs the presets use, from Modrinth; NeoForge's installer from NeoForge's maven.
The manifest lists every file with its hash, so the program fetches only what changed.
Signing: set WILDS_SIGN_PFX (and WILDS_SIGN_PASSWORD) to sign the program with signtool.
"""
import datetime, hashlib, json, os, re, shutil, subprocess, sys, urllib.request, zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
REPO = "volodymyr-holovan/wilds-and-wanderers"
LAUNCHER = os.path.join(ROOT, "launcher")
NEOFORGE = "21.1.264"
MINECRAFT = "1.21.1"
# the shader packs of the presets (launcher/presets.json names them by file)
SHADERS = [
    ("ComplementaryReimagined_r5.9.3.zip", "https://cdn.modrinth.com/data/HVnmMxH1/versions/Bqen1mJX/ComplementaryReimagined_r5.9.3.zip",
     "838139b54cddb56b2e83cd260d8efd960ac536d6"),
    ("ComplementaryUnbound_r5.9.3.zip", "https://cdn.modrinth.com/data/R6NEzAwj/versions/B1kyfoUZ/ComplementaryUnbound_r5.9.3.zip",
     "2ee08300e1d6f039e63eae8484dddf57b3aaaf67"),
]
UA = {"User-Agent": "wilds-and-wanderers-pack/2.0"}


def sha1(data):
    return hashlib.sha1(data).hexdigest()


def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120).read()


def run(*cmd, cwd=ROOT):
    print(">", " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, check=True)


def next_version():
    today = datetime.date.today().strftime("%Y.%m.%d")
    old = os.path.join(LAUNCHER, "manifest.json")
    n = 1
    if os.path.exists(old):
        v = json.load(open(old, encoding="utf-8")).get("version", "")
        if v.startswith(today + "."):
            n = int(v.rsplit(".", 1)[1]) + 1
    return f"{today}.{n}"


def overrides_zip(path):
    """pack/overrides as overrides/..., pack/overrides-initial as initial/...; the same input gives the same bytes"""
    entries = {}
    for prefix, src in (("overrides", "overrides"), ("initial", "overrides-initial")):
        base = os.path.join(ROOT, "pack", src)
        listed = []
        for dp, _, fs in os.walk(base):
            for f in fs:
                full = os.path.join(dp, f)
                rel = os.path.relpath(full, base).replace(os.sep, "/")
                data = open(full, "rb").read()
                entries[f"{prefix}/{rel}"] = data
                listed.append({"path": rel, "sha1": sha1(data), "size": len(data)})
        yield prefix, sorted(listed, key=lambda e: e["path"])
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, entries[name])


def build(version):
    dist = os.path.join(LAUNCHER, "dist", version)
    shutil.rmtree(dist, ignore_errors=True)
    os.makedirs(dist)
    base = f"https://github.com/{REPO}/releases/download/pack-{version}/"

    mods = []
    for m in json.load(open(os.path.join(ROOT, "docs", "mods.json"), encoding="utf-8")):
        if m["where"] in ("both", "client", "opt-client"):
            if not m.get("sha1") or not m.get("url"):
                sys.exit(f"{m['file']}: no hash or link in docs/mods.json")
            mods.append({"file": m["file"], "url": m["url"], "sha1": m["sha1"], "size": m["size"]})
    # the pack's own mods: built locally (mods-src), shipped as release assets
    for fn in sorted(os.listdir(os.path.join(ROOT, "client", "mods"))):
        if fn.startswith("wilds_") and fn.endswith(".jar") and not fn.startswith("wilds_dump"):
            data = open(os.path.join(ROOT, "client", "mods", fn), "rb").read()
            shutil.copy2(os.path.join(ROOT, "client", "mods", fn), os.path.join(dist, fn))
            mods.append({"file": fn, "url": base + fn, "sha1": sha1(data), "size": len(data)})
    names = [m["file"] for m in mods]
    if len(names) != len(set(names)):
        sys.exit("a mod file is listed twice")
    client_jars = {f for f in os.listdir(os.path.join(ROOT, "client", "mods")) if f.endswith(".jar")}
    missing = client_jars - set(names)
    if missing:
        sys.exit("in client/mods but not in the release (add them to docs/mods.json): " + ", ".join(sorted(missing)))

    zip_path = os.path.join(dist, "overrides.zip")
    listed = dict(overrides_zip(zip_path))
    zip_data = open(zip_path, "rb").read()

    installer = os.path.join(ROOT, "_tools", f"neoforge-{NEOFORGE}-installer.jar")
    nf_url = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE}/neoforge-{NEOFORGE}-installer.jar"
    nf_data = open(installer, "rb").read() if os.path.exists(installer) else fetch(nf_url)
    if fetch(nf_url + ".sha1").decode().strip().split()[0] != sha1(nf_data):
        sys.exit("the NeoForge installer here differs from NeoForge's maven")

    shaders = []
    for fn, url, h in SHADERS:
        cached = os.path.join(LAUNCHER, "dist", "shaders", fn)
        if not os.path.exists(cached):
            os.makedirs(os.path.dirname(cached), exist_ok=True)
            open(cached, "wb").write(fetch(url))
        data = open(cached, "rb").read()
        if sha1(data) != h:
            sys.exit(f"{fn}: hash differs from the one recorded")
        shaders.append({"file": fn, "url": url, "sha1": h, "size": len(data)})
    presets = json.load(open(os.path.join(LAUNCHER, "presets.json"), encoding="utf-8"))
    for p in presets["presets"]:
        if p.get("shader") and p["shader"] not in {s["file"] for s in shaders}:
            sys.exit(f"preset {p['id']}: unknown shader pack {p['shader']}")

    removed = [l.strip() for l in open(os.path.join(ROOT, "pack", "removed.txt"), encoding="utf-8") if l.strip() and not l.startswith("#")]
    manifest = {
        "version": version,
        "minecraft": MINECRAFT,
        "neoforge": {"version": NEOFORGE, "installer": {"file": os.path.basename(nf_url), "url": nf_url, "sha1": sha1(nf_data), "size": len(nf_data)}},
        "mods": sorted(mods, key=lambda m: m["file"].lower()),
        "files": {
            "zip": {"file": "overrides.zip", "url": base + "overrides.zip", "sha1": sha1(zip_data), "size": len(zip_data)},
            "overrides": listed["overrides"],
            "initial": listed["initial"],
        },
        "shaders": shaders,
        "removed": removed,
    }
    json.dump(manifest, open(os.path.join(LAUNCHER, "manifest.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    size = sum(m["size"] for m in mods)
    print(f"pack {version}: {len(mods)} mods ({size // 1048576} MB), {len(listed['overrides'])} configs, "
          f"{len(listed['initial'])} first-time files, archive {len(zip_data) // 1024} KB")
    return dist


def build_app():
    proj = os.path.join(LAUNCHER, "src", "WildsLauncher")
    version = re.search(r"<Version>([^<]+)</Version>", open(os.path.join(proj, "WildsLauncher.csproj"), encoding="utf-8").read())[1]
    out = os.path.join(LAUNCHER, "dist", "app", version)
    shutil.rmtree(out, ignore_errors=True)
    run("dotnet", "publish", proj, "-c", "Release", "-o", out, "-nologo", "-v", "q")
    exe = os.path.join(out, "WildsAndWanderers.exe")
    pfx = os.environ.get("WILDS_SIGN_PFX")
    if pfx:
        run("signtool", "sign", "/fd", "SHA256", "/td", "SHA256", "/tr", "http://timestamp.digicert.com", "/f", pfx,
            *(["/p", os.environ["WILDS_SIGN_PASSWORD"]] if os.environ.get("WILDS_SIGN_PASSWORD") else []), exe)
    return version, exe


def main():
    dry = "--dry" in sys.argv
    version = next_version()
    dist = build(version)
    if dry:
        print("dry run: nothing published")
        return
    if "--app" in sys.argv:
        app_version, exe = build_app()
        tag = f"app-v{app_version}"
        run("gh", "release", "create", tag, exe, "--repo", REPO, "--title", f"Wilds & Wanderers {app_version} (програма)",
            "--notes", "Програма встановлення й оновлення збірки Wilds & Wanderers для офіційного лаунчера Minecraft. "
                       "Завантажте WildsAndWanderers.exe і запустіть: вона сама встановить NeoForge, моди й налаштування.")
        cfg_path = os.path.join(LAUNCHER, "config.json")
        cfg = json.load(open(cfg_path, encoding="utf-8"))
        cfg["updater"] = {"version": app_version, "url": f"https://github.com/{REPO}/releases/download/{tag}/WildsAndWanderers.exe",
                          "sha256": hashlib.sha256(open(exe, "rb").read()).hexdigest()}
        json.dump(cfg, open(cfg_path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    assets = [os.path.join(dist, f) for f in sorted(os.listdir(dist))]
    run("gh", "release", "create", f"pack-{version}", *assets, "--repo", REPO, "--title", f"Збірка {version}",
        "--notes", "Файли збірки для програми Wilds & Wanderers (власні моди й налаштування). Встановлювати вручну не потрібно.")
    run("git", "add", "-A")
    run("git", "commit", "-q", "-m", f"Pack release {version}\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>")
    run("git", "push", "-q")


if __name__ == "__main__":
    main()
