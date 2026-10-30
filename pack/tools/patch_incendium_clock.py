"""Incendium's datapack clock (incendium:clocks/main) runs every tick and walks every entity of every dimension several
times; on the server it cost ~5% of the whole tick (vanilla /perf, 29.10). Most of those walks only notice NEW entities
(give them an id, run their one-time init) or clear a shield's poison/wither: doing that every 4 ticks instead of every
tick changes nothing a player can see, while weapons, projectiles, freezing and the castle and boss logic keep their
every-tick clock. This writes the split clock into pack/overrides-server/kubejs/data (KubeJS data wins over the mod's
own; server only and not in the repository: it is a modified copy of Incendium's file). Run again after updating
Incendium: it stops if the clock no longer has the lines it moves.

  python pack/tools/patch_incendium_clock.py
"""
import glob, os, sys, zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "pack", "overrides-server", "kubejs", "data", "incendium", "function", "clocks")
MAIN = "data/incendium/function/clocks/main.mcfunction"
# the walks that only look for entities the clock has not seen yet, and the shield clean-up
SLOW = [
    "execute as @e[type=#incendium:mobs_no_player, tag=!in.checked] at @s run function incendium:entity/mob/init",
    "execute as @e[type=#incendium:mobs_no_player] unless score @s in.eid matches 0..32767 run function incendium:technical/entity_id/check",
    "execute as @e[type=#incendium:other, tag=!in.checked] at @s run function incendium:entity/other/init",
    "execute if entity @e[scores={in.cd_shield=1..},predicate=incendium:holding/radiation_shield] run effect clear @e[scores={in.cd_shield=1..},predicate=incendium:holding/radiation_shield] poison",
    "execute if entity @e[scores={in.cd_shield=1..},predicate=incendium:holding/necrotic_shield] run effect clear @e[scores={in.cd_shield=1..},predicate=incendium:holding/necrotic_shield] wither",
]
EVERY = 4

jars = glob.glob(os.path.join(ROOT, "server", "mods", "Incendium_*.jar"))
if not jars:
    sys.exit("Incendium jar not found in server/mods")
src = zipfile.ZipFile(jars[0]).read(MAIN).decode("utf-8")
lines = src.splitlines()
missing = [s for s in SLOW if s not in lines]
if missing:
    sys.exit("Incendium's clock changed, check it by hand; not found:\n  " + "\n  ".join(missing))

kept = [l for l in lines if l not in SLOW]
# one counter in Incendium's own dummy objective (created by incendium:load) decides the slow tick
kept += ["", f"# Wilds & Wanderers: the walks over all entities for new ones run every {EVERY} ticks (clocks/wilds_slow)",
         "scoreboard players add $wilds_slow in.dummy 1",
         f"execute if score $wilds_slow in.dummy matches {EVERY}.. run function incendium:clocks/wilds_slow"]
slow = [f"# from: clocks/main every {EVERY} ticks (Wilds & Wanderers, pack/tools/patch_incendium_clock.py)",
        "scoreboard players set $wilds_slow in.dummy 0", ""] + SLOW

os.makedirs(OUT, exist_ok=True)
for name, body in (("main", kept), ("wilds_slow", slow)):
    with open(os.path.join(OUT, name + ".mcfunction"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(body) + "\n")
print("written", OUT, f"({len(SLOW)} lines moved to the {EVERY}-tick clock)")
