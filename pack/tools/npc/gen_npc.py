"""Build the wandering quest givers from their stories (quests_*.py):

  python gen_npc.py

Writes
  pack/overrides/kubejs/data/wilds_core/npc_quests/all.json   - what Wilds Core reads (persona, place, task, reward, lines)
  pack/tools/quests/chapters/c90_errands.py                    - the quest book chapter «Доручення»: for every quest an
                                                                 invisible gate and the quest itself, hidden until the
                                                                 gate is done (Wilds Core completes it when the player
                                                                 accepts the job); the giver hands out the reward
Every item, mob and loot table named is checked against the server registries.
"""
import glob, importlib.util, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
REG = os.path.join(ROOT, "docs", "registries")
ITEMS = set(open(os.path.join(REG, "item.txt"), encoding="utf-8").read().split())
ENTITIES = set(open(os.path.join(REG, "entity_type.txt"), encoding="utf-8").read().split())
STRUCTURES = set(open(os.path.join(REG, "worldgen_structure.txt"), encoding="utf-8").read().split())
LOOT = os.path.join(ROOT, "pack", "overrides", "kubejs", "data", "wilds", "loot_table")
OUT_DATA = os.path.join(ROOT, "pack", "overrides", "kubejs", "data", "wilds_core", "npc_quests", "all.json")
OUT_CHAPTER = os.path.join(ROOT, "pack", "tools", "quests", "chapters", "c90_errands.py")

sys.path.insert(0, HERE)
quests, errors, chains = [], [], {}
for f in sorted(glob.glob(os.path.join(HERE, "quests_*.py"))):
    spec = importlib.util.spec_from_file_location(os.path.basename(f)[:-3], f)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    quests += mod.QUESTS
    chains.update(getattr(mod, "CHAINS", {}))

# chains: a giver with several quests in a row; the follow-ups take the giver's name, look and place
by_key = {q["key"]: q for q in quests}
in_chain = {}
for persona, order in chains.items():
    if persona not in by_key or by_key[persona].get("follow_up"):
        errors.append(f"chain {persona}: no such giver")
        continue
    if persona not in order:
        errors.append(f"chain {persona}: the giver's own quest is not in the chain")
    for stage, k in enumerate(order, 1):
        if k not in by_key:
            errors.append(f"chain {persona}: unknown quest {k}")
            continue
        if k in in_chain:
            errors.append(f"{k}: in two chains")
        in_chain[k] = persona
        q = by_key[k]
        q["persona"], q["stage"], q["stages"] = persona, stage, len(order)
        if q.get("follow_up"):
            q["npc"], q["where"] = dict(by_key[persona]["npc"]), dict(by_key[persona]["where"])
for q in quests:
    if q.get("follow_up") and q["key"] not in in_chain:
        errors.append(f"{q['key']}: a follow-up quest outside any chain")
    q.setdefault("persona", q["key"])
    q.setdefault("stage", 1)
    q.setdefault("stages", 1)
    # lying is possible only where it is worth the risk: the most valuable jobs (and only with the skill «Красномовство»)
    rw = q["reward"]
    q["valuable"] = bool(q["lie"]) and (rw["xp"] >= 400 or (q["stages"] >= 4 and q["stage"] == q["stages"]))
# a giver's quests stand together in the book: order by persona, then stage
order = {k: i for i, k in enumerate(q["key"] for q in quests if q["stage"] == 1)}
quests.sort(key=lambda q: (order.get(q["persona"], 0), q["stage"]))

seen = set()
for q in quests:
    k = q["key"]
    if k in seen:
        errors.append(f"{k}: duplicate key")
    seen.add(k)
    if len(q["title"]) > 26:
        errors.append(f"{k}: title longer than 26: {q['title']}")
    kind, _, rest = q["task_spec"].partition(":")
    ident = rest.split("*")[0]
    if kind == "item" and ident not in ITEMS:
        errors.append(f"{k}: unknown item {ident}")
    if kind == "kill" and ident not in ENTITIES:
        errors.append(f"{k}: unknown mob {ident}")
    if kind == "struct" and ident not in STRUCTURES:
        errors.append(f"{k}: unknown structure {ident}")
    if q["task"]["count"] < 1:
        errors.append(f"{k}: count must be at least 1")
    for spec in q["reward"]["items"]:
        if spec.split("*")[0] not in ITEMS:
            errors.append(f"{k}: unknown reward item {spec}")
    if q["npc"]["item"] not in ITEMS:
        errors.append(f"{k}: unknown held item {q['npc']['item']}")
    loot = q["reward"]["loot"]
    if loot and not os.path.exists(os.path.join(LOOT, loot.split(":", 1)[1] + ".json")):
        errors.append(f"{k}: unknown loot table {loot}")
    for line in q["lines"]:
        if line not in ("greet", "ask", "accept", "decline", "remind", "done", "thanks", "lie_ok", "lie_fail", "haggle_ok", "haggle_fail", "dead", "after", "task"):
            errors.append(f"{k}: unknown line {line}")

data = []
for q in quests:
    d = {k: v for k, v in q.items() if k not in ("task_spec", "title", "follow_up", "lie")}
    d["quest_title"] = q["title"]
    data.append(d)
os.makedirs(os.path.dirname(OUT_DATA), exist_ok=True)
json.dump(data, open(OUT_DATA, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

# the quest book chapter: one visible introduction, then gate + quest per giver
lines = ['"""Generated by pack/tools/npc/gen_npc.py from the quest givers\' stories; do not edit by hand."""', "", "CHAPTER = {",
         '    "key": "errands", "title": "&eДоручення", "group": "Мандрівники", "icon": "minecraft:writable_book",',
         '    "subtitle": "Справи, які вам доручили мандрівники",', '    "quests": [']
intro = {"key": "intro", "title": "Мандрівники з дорученнями", "icon": "minecraft:writable_book", "shape": "gear", "size": 1.5, "pos": (0.0, 0.0),
         "desc": ["У спорудах світу — від сільських хат до підземних катакомб і палуб розбитих кораблів — трапляються &eмандрівники&r із золотим &6«!»&r над головою.",
                  "", "Підійдіть ближче — вони заговорять самі. Вислухайте історію, розпитайте, а тоді &aвізьміться&r за справу чи відмовтеся.",
                  "Можна спробувати &dвиторгувати&r більшу плату — шанс росте з гілкою «Дипломат» у навичках (K). Навичка &dКрасномовство&r відкриває ризиковану брехню — лише в найцінніших дорученнях.",
                  "", "&6«!»&r — у мандрівника є для вас справа. &6«?»&r — справу виконано, поверніться по нагороду. Сірий &7«?»&r — ви ще працюєте над нею. Коли ви виконали всі його доручення, знак зникає.",
                  "Дехто з мандрівників, переконавшись, що вам можна довіряти, дає наступну справу — більшу й ціннішу (до п'яти поспіль).",
                  "", "Узяте доручення з'являється в цьому розділі. Виконайте його й поверніться до мандрівника по нагороду. Кожен гравець отримує й виконує доручення окремо.",
                  "&cМандрівники смертні.&r Якщо замовник загине раніше, ніж ви повернетеся, — доручення провалено."],
         "tasks": ["check"], "sources": False}
lines.append("        " + repr(intro) + ",")
per_row = 9
sys.path.insert(0, os.path.join(HERE, "..", "quests"))
from quest_data import Facts  # noqa: E402
FACTS = Facts()


def pay_line(rw):
    """«Обіцяна плата: ...» as text components, so item names show in the player's language"""
    parts = [{"text": "Обіцяна плата: ", "color": "gold"}]
    for spec in rw["items"]:
        item, _, n = spec.partition("*")
        if len(parts) > 1:
            parts.append({"text": ", ", "color": "gray"})
        parts += [{"translate": FACTS.item_key(item), "color": "aqua"}, {"text": f" ×{n}" if n and n != "1" else "", "color": "gray"}]
    extra = [t for t in (f"{rw['xp']} досвіду" if rw["xp"] else "", "скриня здобичі" if rw.get("loot") else "",
                         f"{rw['skill_xp']} очок шляху" if rw.get("skill_xp") else "") if t]
    if extra:
        parts.append({"text": (", " if len(parts) > 1 else "") + ", ".join(extra), "color": "gray"})
    return json.dumps(["", *parts], ensure_ascii=False)


for i, q in enumerate(quests):
    row, col = divmod(i, per_row)
    gate = {"key": "g_" + q["key"], "title": q["title"], "invisible": True, "no_rewards": True, "tasks": ["check"],
            "pos": (-6.0, float(i) * 0.5), "sources": False}
    step = f" &8(доручення {q['stage']} з {q['stages']})&r" if q["stages"] > 1 else ""
    # the book keeps what the giver said: the whole story, the request, and the promised pay
    desc = [f"&6{q['npc']['name']}&r, {q['npc']['title']}{step}:"] + [f"&o«{s}»&r" for s in q["story"]]
    if q["lines"].get("ask"):
        desc.append("&f" + q["lines"]["ask"])
    desc += ["", pay_line(q["reward"]), "&7Плату видає замовник: поверніться до нього, коли виконаєте."]
    entry = {"key": q["key"], "title": q["title"], "gate": "g_" + q["key"], "no_rewards": True, "icon": q["npc"]["item"],
             "pos": (float(col * 1.6 - 6.4), float(2.0 + row * 1.6)), "desc": desc, "tasks": [q["task_spec"]]}
    lines.append("        " + repr(gate) + ",")
    lines.append("        " + repr(entry) + ",")
lines += ["    ],", "}", ""]
open(OUT_CHAPTER, "w", encoding="utf-8").write("\n".join(lines))

print(f"{len(quests)} quest givers -> {OUT_DATA}")
print(f"chapter -> {OUT_CHAPTER}")
for e in errors:
    print("  ERROR", e)
sys.exit(1 if errors else 0)
