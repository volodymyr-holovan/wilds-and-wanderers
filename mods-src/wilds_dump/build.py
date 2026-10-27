"""Build the Wilds Dump dev mod (test server only) without Gradle: javac against the libraries the launcher installed.

  python build.py [--install]

--install copies the jar into testserver/mods (never into the pack).
The classpath comes from the NeoForge version json (the same files launch_client.py starts the game with),
the patched Minecraft client jar (Mojang names, as NeoForge runs them) and JEI from client/mods.
"""
import glob, json, os, re, shutil, subprocess, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
MC = os.path.join(os.environ["APPDATA"], ".minecraft")
VERSION = "neoforge-21.1.264"
JDK = r"C:\Program Files\Java\jdk-24\bin"
INSTANCE = r"C:\Users\ketch\Games\Wilds-and-Wanderers-1.21.1\mods"
MOD_VERSION = re.search(r'^version="([^"]+)"', open(os.path.join(HERE, "src", "main", "resources", "META-INF", "neoforge.mods.toml"),
                                                    encoding="utf-8").read(), re.M)[1]


def lib_path(lib):
    art = lib.get("downloads", {}).get("artifact")
    if art and art.get("path"):
        return os.path.join(MC, "libraries", art["path"])
    g, a, v, *cls = lib["name"].split(":")
    return os.path.join(MC, "libraries", *g.split("."), a, v, f"{a}-{v}" + (f"-{cls[0]}" if cls else "") + ".jar")


def classpath():
    child = json.load(open(os.path.join(MC, "versions", VERSION, VERSION + ".json"), encoding="utf-8"))
    parent = json.load(open(os.path.join(MC, "versions", child["inheritsFrom"], child["inheritsFrom"] + ".json"), encoding="utf-8"))
    cp = [lib_path(l) for l in child["libraries"] + parent["libraries"]]
    nf = os.path.join(MC, "libraries", "net", "neoforged", "neoforge", "21.1.264")
    # patched classes first, then the rest of Minecraft remapped to Mojang names by the installer
    cp += [os.path.join(nf, "neoforge-21.1.264-client.jar"), os.path.join(nf, "neoforge-21.1.264-universal.jar")]
    cp += glob.glob(os.path.join(MC, "libraries", "net", "minecraft", "client", "1.21.1-*", "client-1.21.1-*-srg.jar"))
    cp += glob.glob(os.path.join(ROOT, "client", "mods", "jei-1.21.1-neoforge-*.jar"))
    cp += glob.glob(os.path.join(ROOT, "client", "mods", "reeses-sodium-options-*.jar"))
    return [p for p in dict.fromkeys(cp) if os.path.exists(p)]


def main():
    build = os.path.join(HERE, "build")
    classes = os.path.join(build, "classes")
    shutil.rmtree(classes, ignore_errors=True)
    os.makedirs(classes)
    sources = glob.glob(os.path.join(HERE, "src", "main", "java", "**", "*.java"), recursive=True)
    argfile = os.path.join(build, "javac.args")
    with open(argfile, "w", encoding="utf-8") as f:
        f.write("-cp\n\"" + os.pathsep.join(classpath()).replace("\\", "/") + "\"\n")
        f.write("\n".join('"' + s.replace("\\", "/") + '"' for s in sources))
    r = subprocess.run([os.path.join(JDK, "javac.exe"), "--release", "21", "-encoding", "UTF-8", "-nowarn", "-Xmaxerrs", "200",
                        "-d", classes, "@" + argfile])
    if r.returncode:
        sys.exit("javac failed")
    out = os.path.join(build, f"wilds_dump-{MOD_VERSION}.jar")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nImplementation-Title: wilds_dump\n"
                                           f"Implementation-Version: {MOD_VERSION}\n")
        langs = {}
        for base in (classes, os.path.join(HERE, "src", "main", "resources")):
            for dp, _, fs in os.walk(base):
                for fn in fs:
                    p = os.path.join(dp, fn)
                    rel = os.path.relpath(p, base).replace(os.sep, "/")
                    m = re.match(r"(assets/[^/]+/lang/[a-z]{2}_[a-z]{2})(\.[\w-]+)?\.json$", rel)
                    if m:  # uk_ua.json and its generated parts (uk_ua.loot.json) become one lang file
                        langs.setdefault(m[1] + ".json", {}).update(json.load(open(p, encoding="utf-8")))
                        continue
                    z.write(p, rel)
        for rel, d in langs.items():
            z.writestr(rel, json.dumps(d, ensure_ascii=False, indent=1))
    print("built", out, os.path.getsize(out) // 1024, "KB")
    if "--install" in sys.argv:
        for d in (os.path.join(ROOT, "testserver", "mods"),):
            for old in glob.glob(os.path.join(d, "wilds_dump-*.jar")):
                os.remove(old)
            shutil.copy2(out, d)
            print("installed into", d)


main()
