"""All item ids that exist in the pack: every assets/<ns>/models/item/<path>.json in the client jars plus vanilla.

  python item_ids.py [--check id id ...] [--ns namespace]
Writes docs/registries/items_models.txt. With --check prints which ids are missing.
"""
import glob, os, re, sys, zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MC = os.path.join(os.environ["APPDATA"], ".minecraft", "versions", "1.21.1", "1.21.1.jar")
ids = set()
for jar in [MC] + glob.glob(os.path.join(ROOT, "client", "mods", "*.jar")) + glob.glob(os.path.join(ROOT, "server", "mods", "*.jar")):
    try:
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                m = re.match(r"assets/([^/]+)/models/item/(.+)\.json$", n)
                if m:
                    ids.add(f"{m[1]}:{m[2]}")
    except zipfile.BadZipFile:
        pass
open(os.path.join(ROOT, "docs", "registries", "items_models.txt"), "w").write("\n".join(sorted(ids)) + "\n")
args = sys.argv[1:]
if "--check" in args:
    for i in args[args.index("--check") + 1:]:
        if i not in ids:
            print("MISSING", i)
elif "--ns" in args:
    ns = args[args.index("--ns") + 1]
    print(" ".join(sorted(i.split(":")[1] for i in ids if i.startswith(ns + ":"))))
else:
    print(len(ids), "item ids")
