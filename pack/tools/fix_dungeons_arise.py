"""When Dungeons Arise 2.1.68 has a typo: the Mechanical Nest's jigsaw blocks ask for the template pool
"dungeons_arise:mechanical_nest/mechanical_nest_decoration", the mod ships it as "...decorations". Every nest then
logs "Couldn't find template pool reference" and stays without its decorations. This writes the same pool under the
name the jigsaws use into pack/overrides-server/kubejs/data (server only and not in the repository: it is a copy of the
mod's file). Run again after updating the mod (it stops when the mod has fixed its typo or renamed the pool).

  python pack/tools/fix_dungeons_arise.py
"""
import glob, os, sys, zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
POOLS = "data/dungeons_arise/worldgen/template_pool/mechanical_nest/"
OUT = os.path.join(ROOT, "pack", "overrides-server", "kubejs", POOLS.replace("/", os.sep))

jars = glob.glob(os.path.join(ROOT, "server", "mods", "DungeonsArise-1.21.1-*.jar"))
if not jars:
    sys.exit("When Dungeons Arise not found in server/mods")
z = zipfile.ZipFile(jars[0])
names = set(z.namelist())
if POOLS + "mechanical_nest_decoration.json" in names:
    sys.exit("the mod ships mechanical_nest_decoration itself now: nothing to do (delete the old copy in overrides-server)")
if POOLS + "mechanical_nest_decorations.json" not in names:
    sys.exit("mechanical_nest_decorations.json is gone: check the pool names by hand")
os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "mechanical_nest_decoration.json"), "wb") as f:
    f.write(z.read(POOLS + "mechanical_nest_decorations.json"))
print("written", os.path.join(OUT, "mechanical_nest_decoration.json"))
