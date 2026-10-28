"""Copy pack/overrides into a game folder (server or client). Only files from overrides are touched.

pack/overrides-initial holds player-tweakable defaults (video, minimap...): copied only when missing,
so updates never overwrite what a player changed. Pass --no-initial for servers (they also get pack/overrides-server).
"""
import os, shutil, sys

src = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "overrides")
dst = sys.argv[1]
n = 0
# what the pack stopped shipping (pack/removed.txt) is moved aside, never deleted
removed = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "removed.txt")
for line in open(removed, encoding="utf-8") if os.path.exists(removed) else []:
    rel = line.strip()
    if not rel or rel.startswith("#") or not os.path.exists(os.path.join(dst, rel)):
        continue
    aside = os.path.join(dst, "_removed_by_pack", rel)
    # an earlier copy is already set aside (the pack shipped it again in between): number the new one
    k, first = 1, aside
    while os.path.exists(aside):
        aside = f"{first}.old{k if k > 1 else ''}"
        k += 1
    os.makedirs(os.path.dirname(aside), exist_ok=True)
    shutil.move(os.path.join(dst, rel), aside); n += 1
# servers (--no-initial) also get pack/overrides-server: data only a server reads, made from other mods' files
# (pack/tools/fix_structure_attributes.py), kept out of the repository and of the players' pack
sources = [src] + ([os.path.join(os.path.dirname(src), "overrides-server")] if "--no-initial" in sys.argv else [])
for top in sources:
    for base, _, files in os.walk(top):
        rel = os.path.relpath(base, top)
        os.makedirs(os.path.join(dst, rel), exist_ok=True)
        for f in files:
            s, d = os.path.join(base, f), os.path.join(dst, rel, f)
            if not os.path.exists(d) or open(s, "rb").read() != open(d, "rb").read():
                shutil.copy2(s, d); n += 1
init = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "overrides-initial")
if "--no-initial" not in sys.argv and os.path.isdir(init):
    for base, _, files in os.walk(init):
        rel = os.path.relpath(base, init)
        os.makedirs(os.path.join(dst, rel), exist_ok=True)
        for f in files:
            d = os.path.join(dst, rel, f)
            if not os.path.exists(d):
                shutil.copy2(os.path.join(base, f), d); n += 1
print(f"synced {n} changed files into {dst}")
