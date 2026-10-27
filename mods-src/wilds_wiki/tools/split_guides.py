"""Split the guide bundles in guides_src/*.txt into articles: guides_src/<mod>.txt -> wilds_wiki/uk_ua/guides/<mod>/<name>.md.

A bundle holds many short articles separated by "=== name.md" lines; one file per mod is easier to write and review.
Every [[link]] and !directive id is checked against the registries, so a typo shows up here and not in the game log.

  python split_guides.py
"""
import glob, os, re, shutil, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
SRC = os.path.join(HERE, "..", "guides_src")
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "wilds_wiki", "uk_ua", "guides")
REG = os.path.join(ROOT, "docs", "registries")
known = {"item": set(), "entity": set(), "structure": set(), "biome": set()}
for kind, f in (("item", "item.txt"), ("entity", "entity_type.txt"), ("structure", "worldgen_structure.txt"), ("biome", "worldgen_biome.txt")):
    p = os.path.join(REG, f)
    if os.path.exists(p):
        known[kind] = set(open(p, encoding="utf-8").read().split())


def check(ref, where, bad):
    ref = ref.strip()
    if "*" in ref:  # a pattern must match at least one item
        pat = re.compile(re.escape(ref.split(":", 1)[1] if ref.startswith("item:") else ref).replace(r"\*", "[a-z0-9_/]*") + "$")
        if not any(pat.match(i) for i in known["item"]):
            bad.append(f"{where}: {ref} (matches nothing)")
        return
    kind, _, rest = ref.partition(":")
    if kind in known and ":" in rest:
        ok = rest in known[kind] or (kind == "item" and rest in known["item"])
    else:
        rest = ref
        ok = any(ref in s for s in known.values())
    if not ok:
        bad.append(f"{where}: {ref}")


shutil.rmtree(OUT, ignore_errors=True)
bad, count = [], 0
for bundle in sorted(glob.glob(os.path.join(SRC, "*.txt"))):
    mod = os.path.splitext(os.path.basename(bundle))[0]
    parts = re.split(r"^=== (\S+\.md)\s*$", open(bundle, encoding="utf-8").read(), flags=re.M)
    for name, body in zip(parts[1::2], parts[2::2]):
        where = f"{mod}/{name}"
        m = re.search(r"^for:\s*(.+)$", body, re.M)
        for t in (m[1].split(",") if m else []):
            check(t, where, bad)
        for ref in re.findall(r"\[\[([^\]|]+)(?:\|[^\]]*)?\]\]", body):
            check(ref, where, bad)
        for line in re.findall(r"^!(?:items|icons|recipe|uses|entity)\s+(.+)$", body, re.M):
            for ref in line.split():
                if not ref.isdigit():
                    check(ref if ":" in ref else ref, where, bad)
        os.makedirs(os.path.join(OUT, mod), exist_ok=True)
        open(os.path.join(OUT, mod, name), "w", encoding="utf-8").write(body.strip() + "\n")
        count += 1
print(count, "guides written")
if bad:
    print("unknown ids:")
    for b in bad:
        print(" ", b)
    sys.exit(1)
