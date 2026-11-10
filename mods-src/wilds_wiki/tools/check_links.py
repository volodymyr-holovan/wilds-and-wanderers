"""Checks every link of every article against what exists: [[refs]], "for:" targets, !items, pages of categories.json.

  python check_links.py     (exit code 1 when something is broken)
"""
import glob, json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
U = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "wilds_wiki", "uk_ua")
REG = os.path.join(ROOT, "docs", "registries")


def reg(name):
    return set(open(os.path.join(REG, name), encoding="utf-8").read().split())


items, entities, biomes, structures = reg("item.txt"), reg("entity_type.txt"), reg("worldgen_biome.txt"), reg("worldgen_structure.txt")
topics, cats = set(), set()
arts = {}
for f in glob.glob(os.path.join(U, "**", "*.md"), recursive=True):
    rel = os.path.relpath(f, U).replace("\\", "/")[:-3]
    text = open(f, encoding="utf-8").read()
    arts[rel] = text
    m = re.search(r"^topic:\s*(\S+)", text, re.M)
    topics.add("wilds_wiki:" + (m.group(1) if m else rel))
cat = json.load(open(os.path.join(U, "categories.json"), encoding="utf-8"))
for c in cat["categories"]:
    topics.add("wilds_wiki:cat/" + c["id"])
    cats.add(c["id"])
mods = json.load(open(os.path.join(U, "mods.json"), encoding="utf-8"))
namespaces = {i.split(":")[0] for i in items | entities}


def ok(ref):
    ref = ref.strip()
    if "*" in ref:
        return True
    kind = None
    m = re.match(r"(item|entity|block|structure|biome|effect|enchantment|topic):(.+)", ref)
    if m:
        kind, ref = m.group(1), m.group(2)
    if ref.startswith("wilds_wiki:mod/"):
        return ref[len("wilds_wiki:mod/"):] in namespaces
    if kind in ("effect", "enchantment", "block"):
        return True   # not in the dumped registries
    if kind == "item":
        return ref in items
    if kind == "entity":
        return ref in entities
    if kind == "biome":
        return ref in biomes
    if kind == "structure":
        return ref in structures
    return ref in items or ref in entities or ref in biomes or ref in structures or ref in topics


bad = 0
for rel, text in arts.items():
    refs = set(re.findall(r"\[\[([^\]|]+)", text))
    m = re.search(r"^for:\s*(.+)$", text, re.M)
    if m:
        refs |= {t.strip() for t in m.group(1).split(",") if t.strip()}
    for line in re.findall(r"^!(?:items|icons)\s+(.+)$", text, re.M):
        refs |= set(line.split())
    for r in sorted(refs):
        if not ok(r):
            print(f"{rel}: unknown {r}")
            bad += 1
    m = re.search(r"^category:\s*(.+)$", text, re.M)
for c in cat["categories"]:
    for s in c.get("sections", []):
        for p in s.get("pages", []):
            if not ok(p if ":" in p else "wilds_wiki:" + p):
                print(f"category {c['id']}: unknown page {p}")
                bad += 1
        for ch in s.get("children", []):
            if ch not in cats:
                print(f"category {c['id']}: unknown child {ch}")
                bad += 1
for b in cat["bosses"]:
    if b not in entities:
        print("boss list: unknown", b)
        bad += 1
print(len(arts), "articles,", bad, "broken links")
sys.exit(1 if bad else 0)
