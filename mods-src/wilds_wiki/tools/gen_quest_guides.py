"""Every FTB quest of the pack becomes a short wiki article on the pages of what the quest asks for (its items and mobs):
the quest text and its guide (where, how dangerous, what to bring, how to find, a tip) were researched once for the quest
book, the wiki shows the same, so the two never disagree.

  python gen_quest_guides.py   ->  wilds_wiki/uk_ua/quest_guides/<chapter>_<quest>.md
"""
import glob, importlib.util, os, re, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
CHAPTERS = os.path.join(ROOT, "pack", "tools", "quests", "chapters")
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "wilds_wiki", "uk_ua", "quest_guides")
REG = os.path.join(ROOT, "docs", "registries")
ITEMS = set(open(os.path.join(REG, "item.txt"), encoding="utf-8").read().split())
ENTITIES = set(open(os.path.join(REG, "entity_type.txt"), encoding="utf-8").read().split())
DUMP = os.path.join(ROOT, "testserver", "wilds_dump", "biomes.json")
DIM_BIOMES = {}  # a quest that asks to enter a dimension tells every biome there how to get in
if os.path.exists(DUMP):
    import json
    for b, o in json.load(open(DUMP, encoding="utf-8")).items():
        for d in o.get("dimensions", []):
            if d not in ("minecraft:overworld",):
                DIM_BIOMES.setdefault(d, []).append("biome:" + b)
STARS = "★★★★★"
LABEL = {"where": "Де", "danger": "Складність", "gear": "Що взяти", "find": "Як знайти", "tip": "Порада"}


def md(s):
    """FTB colour codes -> wiki markup: &a/&e/&6 highlighted words become bold, the rest is dropped"""
    s = re.sub(r"&[aeb6](.+?)(&r|$)", r"**\1**", s)
    s = re.sub(r"&[0-9a-fk-or]", "", s)
    return s.replace("****", "").strip()


def targets(q):
    out = []
    for t in q.get("tasks", []):
        kind, _, rest = t.partition(":")
        rid = rest.split("*")[0].split("@")[0]
        if kind == "item" and rid in ITEMS:
            out.append(rid)
        elif kind == "kill" and rid in ENTITIES:
            out.append("entity:" + rid)
        elif kind == "dim":
            out.extend(DIM_BIOMES.get(rid, []))
    return out


shutil.rmtree(OUT, ignore_errors=True)
os.makedirs(OUT)
n = 0
for f in sorted(glob.glob(os.path.join(CHAPTERS, "c*.py"))):
    spec = importlib.util.spec_from_file_location(os.path.basename(f)[:-3], f)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    ch = mod.CHAPTER
    chapter = md(ch["title"])
    for q in ch["quests"]:
        tg = targets(q)
        if not tg:
            continue
        lines = ["---", "for: " + ", ".join(tg), "keywords: " + md(q["title"]), "---", f"# Квест «{md(q['title'])}»",
                 f"`Книга квестів → {chapter}`", ""]
        for d in q.get("desc", []):
            lines.append(md(d) if d.strip() else "")
        g = q.get("guide") or {}
        if g:
            lines.append("")
            for k in ("where", "danger", "gear", "find", "tip"):
                if k not in g:
                    continue
                v = STARS[:int(g[k])] + "☆" * (5 - int(g[k])) if k == "danger" else md(str(g[k]))
                lines.append(f"- **{LABEL[k]}:** {v}")
        open(os.path.join(OUT, f"{ch['key']}_{q['key']}.md"), "w", encoding="utf-8").write("\n".join(lines).rstrip() + "\n")
        n += 1
print(n, "quest guides written")
