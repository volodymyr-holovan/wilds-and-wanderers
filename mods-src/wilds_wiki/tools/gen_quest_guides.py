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


def flat(s):
    """a quest line written as a JSON text component (the errands' promised pay) -> plain wiki text: its texts in a row,
    each translated name as a wiki link to that item (22.11: the wiki showed the raw JSON)"""
    t = s.strip()
    if not (t.startswith("[") or t.startswith("{")):
        return s
    try:
        import json
        parts = json.loads(t)
    except ValueError:
        return s
    out = []

    def walk(p):
        if isinstance(p, str):
            out.append(p)
        elif isinstance(p, list):
            for x in p: walk(x)
        elif isinstance(p, dict):
            if "text" in p:
                out.append(("**" + p["text"] + "**") if p.get("color") == "gold" and p["text"].strip() else p["text"])
            if "translate" in p:
                m = re.match(r"(?:item|block|entity)\.([a-z0-9_]+)\.([a-z0-9_./]+)$", p["translate"])
                out.append(f"[[{m.group(1)}:{m.group(2)}]]" if m else "")
            for x in p.get("extra", []): walk(x)
    walk(parts)
    return re.sub(r"\*\*\s*\*\*", "", "".join(out))


def md(s):
    """FTB colour codes -> wiki markup: &a/&e/&6 highlighted words become bold, the rest is dropped"""
    s = flat(s)
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
        first = next((md(d) for d in q.get("desc", []) if d.strip()), "")
        first = re.sub(r"[*`\[\]]", "", first)
        lines = ["---", "for: " + ", ".join(tg), "keywords: " + md(q["title"]), "category: q_" + ch["key"], "summary: " + first[:110], "---",
                 f"# Квест «{md(q['title'])}»",
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
        open(os.path.join(OUT, f"{ch['key']}_{q['key']}.md"), "w", encoding="utf-8", newline="\n").write("\n".join(lines).rstrip() + "\n")
        n += 1
print(n, "quest guides written")
