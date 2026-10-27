"""Turn what the test server saw (wilds_dump mod: /wildsdump biomes|structures|trades) into generated/world.json for the wiki.

  python gen_world.py

Reads testserver/wilds_dump/{biomes.json, structures.jsonl, trades.json} and generated/loot.json. Writes generated/world.json:
  biomes[id]      dims, structures (most specific first), mobs by spawn group (most common first), blocks (characteristic)
  structures[id]  dims, biomes, size, spacing, icon, bosses, mobs, loot tables, items of those tables, blocks (characteristic)
  mob_biomes, mob_structures, item_structures, block_biomes, block_structures, trades   (reverse lookups for other pages)
"Characteristic" blocks are the ones a biome or structure has a lot of and few others have (tf-idf), so a desert shows sand
and cactus, not stone; the photo list for PhotoMode is written next to the dump (photos.json).
"""
import glob, json, math, os, re, zipfile
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
DUMP = os.path.join(ROOT, "testserver", "wilds_dump")
GEN = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "generated")
REG = os.path.join(ROOT, "docs", "registries")
ITEMS = set(open(os.path.join(REG, "item.txt"), encoding="utf-8").read().split())
NOT_BLOCKS = {"minecraft:water", "minecraft:lava", "minecraft:bedrock", "minecraft:barrier", "minecraft:structure_void", "minecraft:light",
              "minecraft:bubble_column", "minecraft:fire", "minecraft:soul_fire", "minecraft:jigsaw", "minecraft:structure_block"}
GROUPS = ["monster", "creature", "ambient", "water_creature", "water_ambient", "underground_water_creature", "axolotls"]

biomes = json.load(open(os.path.join(DUMP, "biomes.json"), encoding="utf-8"))
structs = {}
if os.path.exists(os.path.join(DUMP, "structures.jsonl")):
    for line in open(os.path.join(DUMP, "structures.jsonl"), encoding="utf-8"):
        if line.strip():
            o = json.loads(line)
            structs[o["id"]] = o
trades = json.load(open(os.path.join(DUMP, "trades.json"), encoding="utf-8")) if os.path.exists(os.path.join(DUMP, "trades.json")) else {}
loot = json.load(open(os.path.join(GEN, "loot.json"), encoding="utf-8"))

# chest tables -> items, and which mobs are real mobs (have a loot table, a spawn egg, spawn somewhere or are bosses)
table_items = defaultdict(set)
mobs_known = set()
for item, srcs in loot["sources"].items():
    for kind, src in srcs:
        if kind in ("chest", "archaeology", "other"):
            table_items[src].add(item)
        if kind in ("entity", "skeleton"):
            mobs_known.add(src)
for i in ITEMS:
    if i.endswith("_spawn_egg"):
        mobs_known.add(i[:-10])
for b in biomes.values():
    for g, l in b.get("spawns", {}).items():
        for e in l:
            mobs_known.add(e[0])
bosses = set()
for jar in glob.glob(os.path.join(ROOT, "client", "mods", "*.jar")):
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if re.match(r"data/(c|forge|neoforge)/tags/entity_types?/bosses\.json$", n):
                try:
                    for v in json.loads(z.read(n)).get("values", []):
                        v = v["id"] if isinstance(v, dict) else v
                        if not v.startswith("#"):
                            bosses.add(v)
                except ValueError:
                    pass
bosses |= {"minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian"}


def is_block(b):
    return b not in NOT_BLOCKS and b in ITEMS and not b.endswith("_air")


def tfidf(docs, limit, min_count=3, max_df=0.35):
    """docs: name -> Counter(block -> count). The blocks each doc has a lot of and few docs have."""
    n = max(1, len(docs))
    df = Counter()
    for c in docs.values():
        for b in c:
            df[b] += 1
    out = {}
    for name, c in docs.items():
        total = sum(c.values()) or 1
        scored = []
        for b, k in c.items():
            if k < min_count or not is_block(b) or df[b] / n > max_df:
                continue
            scored.append(((k / total) * math.log(n / df[b]), b))
        scored.sort(reverse=True)
        out[name] = [b for _, b in scored[:limit]]
    return out


# ---------------------------------------------------------------- biomes
named_biomes = {b for b, o in biomes.items() if o.get("dimensions")}
samples = {b: Counter(o["sample"]["blocks"]) for b, o in biomes.items() if isinstance(o.get("sample"), dict) and b in named_biomes}
for b, o in biomes.items():  # a block the features place and the sample saw at least once counts as common enough
    if b in samples:
        for fb in o.get("feature_blocks", []):
            if fb in samples[b]:
                samples[b][fb] += 2
biome_blocks = tfidf(samples, 20)

struct_biomes = {s: o.get("biomes", []) for s, o in structs.items()}
out_biomes = {}
for b in sorted(named_biomes):
    o = biomes[b]
    here = [s for s, bl in struct_biomes.items() if b in bl]
    here.sort(key=lambda s: (len(struct_biomes[s]), s))
    mobs = {}
    for g in GROUPS:
        l = sorted(o.get("spawns", {}).get(g, []), key=lambda e: -e[1])
        if l:
            mobs[g] = [[e[0], e[1]] for e in l]
    out_biomes[b] = {"dims": o["dimensions"], "structures": here, "mobs": mobs, "blocks": biome_blocks.get(b, [])}

# ---------------------------------------------------------------- structures
struct_blocks = tfidf({s: Counter(o.get("blocks", {})) for s, o in structs.items() if o.get("blocks")}, 16, min_count=4, max_df=0.5)
table_df = Counter(i for t in table_items.values() for i in t)
out_structs = {}
for s, o in sorted(structs.items()):
    dims = sorted({d for b in o.get("biomes", []) for d in biomes.get(b, {}).get("dimensions", [])})
    mobs = set(o.get("entities", {})) | set(o.get("spawners", {}))
    for g, l in o.get("spawn_overrides", {}).items():
        mobs |= {e[0] for e in l}
    mobs = {m for m in mobs if m in mobs_known}
    tables = sorted(o.get("loot", {}), key=lambda t: -o["loot"][t])
    items = set()
    for t in tables:
        items |= table_items.get(t, set())
    items = sorted(items, key=lambda i: (table_df[i], i))[:48]
    rec = {"dims": dims, "biomes": o.get("biomes", []), "bosses": sorted(m for m in mobs if m in bosses),
           "mobs": sorted(m for m in mobs if m not in bosses), "loot": tables, "items": items, "blocks": struct_blocks.get(s, [])}
    if "box" in o:
        x0, y0, z0, x1, y1, z1 = o["box"]
        rec["size"] = [x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1]
    if o.get("placement", {}).get("spacing"):
        rec["spacing"] = o["placement"]["spacing"]
    icon = next((b for b in rec["blocks"] if b in ITEMS), None)
    if icon:
        rec["icon"] = icon
    out_structs[s] = rec
# structures without a dump record yet still get a page (biomes from the registry are unknown then)
for s in open(os.path.join(REG, "worldgen_structure.txt"), encoding="utf-8").read().split():
    out_structs.setdefault(s, {"dims": [], "biomes": []})

# ---------------------------------------------------------------- reverse lookups
mob_biomes, mob_structures, item_structures, block_biomes, block_structures = (defaultdict(list) for _ in range(5))
for b, o in out_biomes.items():
    for g, l in o["mobs"].items():
        for e, w in l:
            mob_biomes[e].append((w, b))
    for bl in o["blocks"]:
        block_biomes[bl].append(b)
for s, o in out_structs.items():
    for m in o.get("bosses", []) + o.get("mobs", []):
        mob_structures[m].append(s)
    for i in o.get("items", []):
        item_structures[i].append(s)
    for bl in o.get("blocks", []):
        block_structures[bl].append(s)
# also items whose tables matter but fell outside the top 48 of a structure
for s, o in structs.items():
    for t in o.get("loot", {}):
        for i in table_items.get(t, ()):
            if s not in item_structures[i]:
                item_structures[i].append(s)
world = {
    "biomes": out_biomes,
    "structures": out_structs,
    "mob_biomes": {e: [b for _, b in sorted(l, key=lambda x: -x[0])] for e, l in mob_biomes.items()},
    "mob_structures": {e: sorted(l) for e, l in mob_structures.items()},
    "item_structures": {i: sorted(l)[:40] for i, l in item_structures.items()},
    "block_biomes": {b: sorted(l) for b, l in block_biomes.items()},
    "block_structures": {b: sorted(l)[:40] for b, l in block_structures.items()},
}
tr = defaultdict(list)
for who, levels in trades.items():
    for lv, offers in levels.items():
        for res, n, a, na, b, nb in offers:
            if res in ITEMS and res != "minecraft:emerald":
                tr[res].append([who, int(lv), a, na, b, nb, n])
world["trades"] = {i: sorted(l, key=lambda x: (x[0] != "wandering_trader", x[1]))[:12] for i, l in tr.items()}
json.dump(world, open(os.path.join(GEN, "world.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))

# ---------------------------------------------------------------- camera points for PhotoMode
shots = []
for s, o in sorted(structs.items()):
    if "box" not in o or o.get("error"):
        continue
    x0, y0, z0, x1, y1, z1 = o["box"]
    cx, cy, cz = (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2
    r = max(6.0, 0.5 * math.sqrt((x1 - x0) ** 2 + (y1 - y0) ** 2 + (z1 - z0) ** 2))
    yaw, pitch = -45.0, 28.0  # looking south-east and down
    d = r / math.sin(math.radians(36)) * 0.92
    vx = -math.sin(math.radians(yaw)) * math.cos(math.radians(pitch))
    vy = -math.sin(math.radians(pitch))
    vz = math.cos(math.radians(yaw)) * math.cos(math.radians(pitch))
    ns, path = s.split(":", 1)
    shots.append({"name": f"structure/{ns}/{path}", "dim": "wilds_dump:photo", "x": cx - vx * d, "y": cy - vy * d, "z": cz - vz * d,
                  "yaw": yaw, "pitch": pitch})
for b, o in sorted(biomes.items()):
    smp = o.get("sample")
    if not isinstance(smp, dict) or smp.get("cave") or smp.get("columns", 0) < 200:
        continue
    ns, path = b.split(":", 1)
    shots.append({"name": f"biome/{ns}/{path}", "dim": smp["dimension"], "x": smp["x"] + 0.5, "y": smp["y"] + 14, "z": smp["z"] + 0.5,
                  "yaw": 35.0, "pitch": 14.0})
json.dump(shots, open(os.path.join(DUMP, "photos.json"), "w", encoding="utf-8"), indent=0)
print(len(shots), "camera points ->", os.path.join(DUMP, "photos.json"))
print(f"biomes {len(out_biomes)} (sampled {len(samples)}), structures {len(out_structs)} (scanned {len(structs)}), trades for {len(world['trades'])} items,"
      f" {os.path.getsize(os.path.join(GEN, 'world.json')) // 1024} KB")
