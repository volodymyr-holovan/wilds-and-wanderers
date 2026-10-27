"""Generate the data files the wiki reads at runtime from the mod jars of the pack.

  python gen_data.py

Writes into src/main/resources/assets/wilds_wiki/generated/:
  loot.json      - for every item: where it drops (mob, block, chest, fishing, archaeology...) and for every mob/block what it drops
  changed_recipes.json - recipe ids the pack added or modified (KubeJS), marked in JEI
  loot_tables.txt - every chest/other loot table id, the list the Ukrainian names in lang are written for
"""
import glob, json, os, re, zipfile
from collections import defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "wilds_wiki", "generated")
MC = os.path.join(os.environ["APPDATA"], ".minecraft")
JARS = sorted(set(glob.glob(os.path.join(ROOT, "server", "mods", "*.jar")) + glob.glob(os.path.join(ROOT, "client", "mods", "*.jar"))),
              key=os.path.basename)
VANILLA = os.path.join(MC, "versions", "1.21.1", "1.21.1.jar")
KUBE_DATA = os.path.join(ROOT, "pack", "overrides", "kubejs", "data")

tables, tags, recipes = {}, defaultdict(list), {}


def read_data(name, raw):
    m = re.match(r"data/([^/]+)/(loot_table|loot_tables)/(.+)\.json$", name)
    if m:
        try:
            tables[f"{m[1]}:{m[3]}"] = json.loads(raw)
        except ValueError:
            pass
        return
    m = re.match(r"data/([^/]+)/tags/(?:item|items)/(.+)\.json$", name)
    if m:
        try:
            tags[f"{m[1]}:{m[2]}"].append(json.loads(raw))
        except ValueError:
            pass
        return
    m = re.match(r"data/([^/]+)/(?:recipe|recipes)/(.+)\.json$", name)
    if m:
        recipes[f"{m[1]}:{m[2]}"] = raw.decode("utf-8", "replace")


for jar in [VANILLA] + JARS:
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.startswith("data/") and n.endswith(".json"):
                read_data(n, z.read(n))
for f in glob.glob(os.path.join(KUBE_DATA, "**", "*.json"), recursive=True):
    read_data("data/" + os.path.relpath(f, KUBE_DATA).replace(os.sep, "/"), open(f, "rb").read())


def tag_items(tag, seen=None):
    seen = seen or set()
    if tag in seen:
        return set()
    seen.add(tag)
    out = set()
    for t in tags.get(tag, []):
        for v in t.get("values", []):
            v = v["id"] if isinstance(v, dict) else v
            if v.startswith("#"):
                out |= tag_items(v[1:], seen)
            else:
                out.add(v)
    return out


def entry_items(e, stack):
    t = e.get("type", "").replace("minecraft:", "")
    if t == "item":
        return {e["name"]}
    if t == "tag":
        return tag_items(e["name"])
    if t in ("alternatives", "group", "sequence"):
        out = set()
        for c in e.get("children", []):
            out |= entry_items(c, stack)
        return out
    if t == "loot_table":
        v = e.get("value", e.get("name"))
        if isinstance(v, dict):
            return table_items_raw(v, stack)
        return table_items(v, stack) if isinstance(v, str) else set()
    return set()


def table_items_raw(t, stack):
    out = set()
    for p in t.get("pools", []):
        for e in p.get("entries", []):
            out |= entry_items(e, stack)
    return out


def table_items(tid, stack=()):
    if tid in stack or tid not in tables:
        return set()
    return table_items_raw(tables[tid], stack + (tid,))


ENTITIES = set(open(os.path.join(ROOT, "docs", "registries", "entity_type.txt"), encoding="utf-8").read().split())


def entity_of(ns, parts):
    """The mob a loot table under entities/ belongs to: entities/dragon/fire_dragon_male -> iceandfire:fire_dragon."""
    for cand in reversed(parts[1:]):
        for c in (cand, re.sub(r"_(male|female|skeleton|corpse|baby|adult|giant|\d+)$", "", cand),
                  re.sub(r"^(red|tan|white|forest|frost|mountain|[a-z]+?)_", "", cand)):
            if f"{ns}:{c}" in ENTITIES:
                return f"{ns}:{c}"
    for c in reversed(parts[1:]):  # deathworm_red -> death_worm, troll_forest -> troll
        for e in ENTITIES:
            if e.startswith(ns + ":") and e.split(":")[1].replace("_", "") in c.replace("_", ""):
                return e
    return None


def classify(tid):
    ns, path = tid.split(":", 1)
    parts = path.split("/")
    if parts[0] in ("entities", "entity") and len(parts) >= 2:
        mob = entity_of(ns, parts)
        if mob and re.search(r"skeleton|corpse", parts[-1]):
            return "skeleton", mob  # broken skeletons and harvested corpses, not a kill
        if mob:
            return "entity", mob
        return "other", tid
    if parts[0] == "blocks" and len(parts) >= 2:
        return "block", f"{ns}:{'/'.join(parts[1:])}"
    if parts[0] == "chests" or "chest" in path:
        return "chest", tid
    if "fishing" in path:
        return "fishing", tid
    if parts[0] == "archaeology" or "archaeology" in path:
        return "archaeology", tid
    if "bartering" in path:
        return "barter", tid
    if "hero_of_the_village" in path:
        return "gift", tid
    if parts[0] in ("gameplay",) or "shearing" in path or "brush" in path:
        return "other", tid
    return None, tid


item_sources, source_drops, named_tables = defaultdict(set), defaultdict(set), set()
for tid in tables:
    kind, src = classify(tid)
    if not kind:
        continue
    items = {i for i in table_items(tid) if i and i != "minecraft:air"}
    for it in items:
        if kind == "block" and it == src:
            continue  # a block dropping itself says nothing new
        item_sources[it].add((kind, src))
        source_drops[f"{kind}|{src}"].add(it)
    if kind not in ("entity", "block", "skeleton") and items:
        named_tables.add(tid)

ORDER = {"entity": 0, "skeleton": 0, "block": 1, "chest": 2, "archaeology": 3, "fishing": 4, "barter": 5, "gift": 6, "other": 7}
loot = {
    "sources": {it: [[k, s] for k, s in sorted(v, key=lambda x: (ORDER[x[0]], x[1]))][:60] for it, v in sorted(item_sources.items())},
    "drops": {k: sorted(v)[:80] for k, v in sorted(source_drops.items()) if k.split("|")[0] in ("entity", "block")},
}

# Recipes KubeJS changed: everything in the wilds namespace is new; replaceInput touched the recipes of these mods
REPLACED = {"iceandfire": "iceandfire:dragonbone", "alexscaves": "alexscaves:heavy_bone", "twilightforest": "twilightforest:fiery_blood"}
NOTES = {"iceandfire:dragonbone": "ancient_bones", "alexscaves:heavy_bone": "ancient_bones", "twilightforest:fiery_blood": "fiery_blood"}
changed = {}
for rid, text in sorted(recipes.items()):
    ns = rid.split(":")[0]
    if ns not in REPLACED:
        continue
    try:
        r = json.loads(text)
    except ValueError:
        continue
    for k in ("result", "results", "output", "outputs"):
        r.pop(k, None)  # replaceInput changes ingredients only
    if '"' + REPLACED[ns] + '"' in json.dumps(r):
        changed[rid] = "modified:" + NOTES[REPLACED[ns]]
# exact list: wilds_links.js logs every recipe it modifies (the scan above is the fallback before the first server start)
LOG = os.path.join(ROOT, "testserver", "logs", "kubejs", "server.log")
logged = re.findall(r"\[wilds-changed\] (\S+) (\S+)", open(LOG, encoding="utf-8", errors="replace").read()) if os.path.exists(LOG) else []
if logged:
    changed = dict(logged)
links = open(os.path.join(ROOT, "pack", "overrides", "kubejs", "server_scripts", "wilds_links.js"), encoding="utf-8").read()
for rid in re.findall(r"\.id\('(wilds:[^']+)'\)", links):
    changed[rid] = "added"

os.makedirs(OUT, exist_ok=True)
json.dump(loot, open(os.path.join(OUT, "loot.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
json.dump(changed, open(os.path.join(OUT, "changed_recipes.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
open(os.path.join(OUT, "loot_tables.txt"), "w", encoding="utf-8").write("\n".join(sorted(named_tables)) + "\n")
print(f"tables {len(tables)}, items with sources {len(item_sources)}, named tables {len(named_tables)}, changed recipes {len(changed)}")
