"""Generate FTB Quests (2101) files from the chapter definitions in chapters/*.py.

Each chapter module defines CHAPTER = {"key", "title", "subtitle", "group", "icon", "quests": [...]}.
A quest is a dict:
    key       unique within the chapter (used for ids and dependencies)
    title     Ukrainian title
    desc      list of description lines ("" = empty line); &-codes allowed
    tasks     list of task specs (see parse_task)
    rewards   list of reward specs (see parse_reward); every quest also feeds the skill tree
    deps      list of quest keys (same chapter) or "chapter:key"
    guide     optional dict rendered under the description as fixed lines, so every adventure quest answers the
              same questions: where (Де), danger 1-5 (Складність), gear (Спорядження), find (Як знайти), tip (Порада)
    pos       optional (x, y); otherwise placed automatically by dependency depth
    icon, shape, size, optional
    how       optional list of steps, shown numbered under «Як»
    pin       true: pinned on screen for every new player (the onboarding chapter)
    gate      key of an invisible quest this one waits for (quests NPCs hand out: Wilds Core completes the gate)
    sources   false: no automatic «Де взяти» block (the text explains it already)
The description is composed: the text, then «Завдання» (each task with the item/mob name in the player's language),
«Де взяти» (how the items are made and where they drop, from the wiki's data), «Як», the guide block and a wiki link.
Titles are short (quest <= 26, chapter <= 16 characters) so the chapter panel stays narrow.
All item/entity/biome/structure ids are checked against the dumped server registries.
"""
import hashlib, importlib.util, json, os, re, shutil, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from quest_data import Facts  # noqa: E402 (runs with python -I)

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.abspath(os.path.join(HERE, "..", ".."))
REG = os.path.join(PACK, "..", "docs", "registries")
OUT = os.path.join(PACK, "overrides", "config", "ftbquests", "quests")
SKILL_CATEGORY = "wilds:path"


def load_registry(name):
    p = os.path.join(REG, name)
    return set(open(p, encoding="utf-8").read().split()) if os.path.exists(p) else set()


ITEMS, ENTITIES = load_registry("item.txt"), load_registry("entity_type.txt")
BIOMES, STRUCTURES = load_registry("worldgen_biome.txt"), load_registry("worldgen_structure.txt")
DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
              "twilightforest:twilight_forest", "aether:the_aether"}
LOOT_DIR = os.path.join(PACK, "overrides", "kubejs", "data", "wilds", "loot_table")
LOOT_TITLES = {"explorer": "Торба мандрівника", "sea": "Морська скриня", "magic": "Скриня мага", "tech": "Ящик інженера",
               "fantasy": "Гоблінська здобич", "boss": "Скарб переможця"}
DANGER = ["", "легко", "помірно", "небезпечно", "дуже небезпечно", "смертельно"]
ERRORS = []
FACTS = Facts()
# Wilds Core reads these (pins for new players, tutorial events completing custom tasks, NPC quest gates)
CORE_DATA = os.path.join(PACK, "overrides", "kubejs", "data", "wilds_core", "quests")
TUTORIAL, PINS, GATES = {}, [], {}
DIM_NAMES = {"minecraft:overworld": "Звичайний світ", "minecraft:the_nether": "Незер", "minecraft:the_end": "Енд",
             "twilightforest:twilight_forest": "Сутінковий ліс", "aether:the_aether": "Етер"}
MAX_QUEST_TITLE, MAX_CHAPTER_TITLE = 26, 16
# what a custom task (completed by Wilds Core) is called in the book; FTB would say «Спеціальне»
CUSTOM_TITLES = {"open_quests": "Відкрити книгу квестів (J)", "open_wiki": "Відкрити вікі (O)", "open_skills": "Відкрити дерево навичок (K)",
                 "open_map": "Відкрити мапу світу (M)", "open_inventory": "Відкрити інвентар (E)", "open_backpack": "Відкрити ранець (B)",
                 "open_recipes": "Подивитися рецепт у JEI", "open_curios": "Вдягнути щось у слот спорядження", "use_waystone": "Скористатися путівним каменем",
                 "open_browser": "Відкрити браузер", "open_spellbook": "Відкрити книгу заклинань", "open_ars_book": "Відкрити книгу Ars Nouveau",
                 "talk_npc": "Поговорити з мандрівником", "pin_quest": "Закріпити квест"}
TASK_TITLES = {}
GROUP_ORDER = ["Початок", "Мандрівники", "Пригоди", "Магія", "Техніка", "Поселення"]
STAT_NAMES = {"minecraft:horse_one_cm": "Проїхати верхи", "minecraft:boat_one_cm": "Пропливти човном", "minecraft:aviate_one_cm": "Пролетіти на елітрах",
              "minecraft:walk_one_cm": "Пройти пішки", "minecraft:swim_one_cm": "Пропливти", "minecraft:animals_bred": "Розвести тварин",
              "minecraft:fish_caught": "Впіймати риби", "minecraft:traded_with_villager": "Обмінятися з селянами", "minecraft:raid_win": "Відбити рейдів"}


def hid(*parts):
    """Stable 16-hex FTB id derived from names, so ids survive regeneration."""
    h = hashlib.sha1("/".join(parts).encode()).hexdigest()[:16].upper()
    # FTB reads ids in its language tables and progress with Long.parseLong: an id with the top bit set (8...F...)
    # loads as a quest but loses its title, text and saved progress. First digit 1-7 (0 would look odd).
    return "%X" % ((int(h[0], 16) & 7) or 1) + h[1:]


def check(kind, value, where):
    pool = {"item": ITEMS, "entity": ENTITIES, "biome": BIOMES, "structure": STRUCTURES, "dimension": DIMENSIONS}[kind]
    v = value.lstrip("#")
    if value.startswith("#"):
        return
    if pool and v not in pool:
        ERRORS.append(f"{where}: unknown {kind} {value}")


def snbt(v, ind=0):
    """Serialize Python values to FTB's SNBT flavour."""
    pad = "\t" * ind
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, int):
        return str(v)
    if isinstance(v, float):
        return f"{v}d"
    if isinstance(v, str):
        return json.dumps(v, ensure_ascii=False)
    if isinstance(v, tuple) and v and v[0] == "L":
        return f"{v[1]}L"
    if isinstance(v, list):
        if not v:
            return "[ ]"
        return "[\n" + "\n".join(pad + "\t" + snbt(x, ind + 1) for x in v) + "\n" + pad + "]"
    if isinstance(v, dict):
        lines = []
        for k in sorted(v):
            key = k if all(c.isalnum() or c in "_." for c in k) else json.dumps(k)
            lines.append(f"{pad}\t{key}: {snbt(v[k], ind + 1)}")
        return "{\n" + "\n".join(lines) + "\n" + pad + "}"
    raise TypeError(v)


def ftb_text(v):
    """FTB formatting treats '&' as a colour prefix and rejects '& ' (the whole title turns into an error):
    a literal ampersand must be written as '\\&'. JSON component lines are left alone."""
    if isinstance(v, list):
        return [ftb_text(x) for x in v]
    if v.lstrip().startswith(("[", "{")):
        return v
    return re.sub(r"(?<!\\)&(?=\s|$)", r"\\&", v)


def item_stack(spec):
    item, _, count = spec.partition("*")
    return item, int(count or 1)


def parse_task(spec, qid, n, where):
    kind, _, rest = spec.partition(":")
    tid = hid(qid, "task", str(n))
    if kind == "item":
        item, count = item_stack(rest)
        if item.startswith("#"):
            check("item", item, where)
            return {"id": tid, "type": "item", "count": ("L", count),
                    "item": {"id": "ftbfiltersystem:smart_filter", "count": 1,
                             "components": {"ftbfiltersystem:filter": f"item_tag({item[1:]})"}}}
        check("item", item, where)
        t = {"id": tid, "type": "item", "item": {"id": item, "count": 1}}
        if count > 1:
            t["count"] = ("L", count)
        return t
    if kind == "kill":
        ent, count = item_stack(rest)
        check("entity", ent, where)
        return {"id": tid, "type": "kill", "entity": ent, "value": ("L", count)}
    if kind == "dim":
        check("dimension", rest, where)
        return {"id": tid, "type": "dimension", "dimension": rest}
    if kind == "biome":
        check("biome", rest, where)
        return {"id": tid, "type": "biome", "biome": rest}
    if kind == "struct":
        check("structure", rest, where)
        return {"id": tid, "type": "structure", "structure": rest}
    if kind == "check":
        return {"id": tid, "type": "checkmark"}
    if kind == "custom":
        # completed by Wilds Core when the player does it (opens the wiki, the skill tree...); see CORE_DATA/tutorial.json
        TUTORIAL.setdefault(rest, []).append(tid)
        TASK_TITLES[tid] = CUSTOM_TITLES.get(rest, "Зробити це в грі")
        return {"id": tid, "type": "custom", "max_progress": ("L", 1)}
    if kind == "stat":
        # "stat:minecraft:horse_one_cm*100000" - a statistic grown by that much
        stat_id, count = item_stack(rest)
        return {"id": tid, "type": "stat", "stat": stat_id, "value": count}
    if kind == "adv":
        return {"id": tid, "type": "advancement", "advancement": rest, "criterion": ""}
    if kind == "npc":
        # Easy NPC x FTB Quests: completed when a dialogue with this label is opened on any NPC
        return {"id": tid, "type": "easynpc_ftbq_compat:easy_npc_task", "dialog": rest}
    raise ValueError(f"{where}: bad task {spec}")


def parse_reward(spec, qid, n, where):
    kind, _, rest = spec.partition(":")
    rid = hid(qid, "reward", str(n))
    if kind == "xp":
        return {"id": rid, "type": "xp", "xp": int(rest)}
    if kind == "skill":
        return {"id": rid, "type": "command", "elevate_perms": True, "silent": True,
                "command": f"/puffish_skills experience add {{p}} {SKILL_CATEGORY} {int(rest)}",
                "title": f"+{int(rest)} досвіду навичок"}
    if kind == "item":
        item, count = item_stack(rest)
        check("item", item, where)
        r = {"id": rid, "type": "item", "item": {"id": item, "count": 1}}
        if count > 1:
            r["count"] = count
        return r
    if kind == "loot":
        # a few random rolls from one of the pack's reward tables (pack/tools/gen_loot_inject.py)
        ns, _, path = rest.partition(":")
        if ns != "wilds" or not os.path.exists(os.path.join(LOOT_DIR, path + ".json")):
            ERRORS.append(f"{where}: unknown loot table {rest}")
        return {"id": rid, "type": "command", "elevate_perms": True, "silent": True,
                "command": f"/loot give {{p}} loot {rest}", "title": LOOT_TITLES.get(path.split("/")[-1], "Скриня здобичі"),
                "icon": {"id": "minecraft:chest"}}
    raise ValueError(f"{where}: bad reward {spec}")


def jline(*parts):
    """one description line made of text components: FTB parses a line starting with [ as JSON"""
    return json.dumps(["", *parts], ensure_ascii=False)


def task_line(spec):
    kind, _, rest = spec.partition(":")
    name, count = item_stack(rest) if kind in ("item", "kill") else (rest, 1)
    n = {"text": f" ×{count}"} if count > 1 else {"text": ""}
    if kind == "item":
        if name.startswith("#"):
            return jline({"text": f"• Будь-який предмет групи {name}", "color": "gray"}, n)
        return jline({"text": "• Мати в інвентарі: ", "color": "gray"}, {"translate": FACTS.item_key(name), "color": "aqua"}, n)
    if kind == "kill":
        return jline({"text": "• Перемогти: ", "color": "gray"}, {"translate": FACTS.entity_key(name), "color": "red"}, n)
    if kind == "stat":
        sid, cnt = item_stack(rest)
        what = STAT_NAMES.get(sid, sid)
        return jline({"text": f"• {what}: ", "color": "gray"}, {"text": f"{cnt // 100 if sid.endswith('_cm') else cnt}" + (" блоків" if sid.endswith("_cm") else ""), "color": "yellow"})
    if kind == "dim":
        return jline({"text": "• Потрапити у вимір: ", "color": "gray"}, {"text": DIM_NAMES.get(rest, rest), "color": "light_purple"})
    if kind == "biome":
        return jline({"text": "• Побувати в біомі: ", "color": "gray"}, {"translate": FACTS.biome_key(rest), "color": "green"})
    if kind == "struct":
        return jline({"text": "• Знайти споруду: ", "color": "gray"}, {"translate": FACTS.structure_key(rest), "color": "gold"})
    return None


def names(kind, ids, limit=3):
    parts = []
    for i, x in enumerate(ids[:limit]):
        if i:
            parts.append({"text": ", "})
        key = {"entity": FACTS.entity_key, "structure": FACTS.structure_key, "biome": FACTS.biome_key}[kind](x)
        parts.append({"translate": key, "color": "white"})
    if len(ids) > limit:
        parts.append({"text": f" і ще {len(ids) - limit}"})
    return parts


def source_lines(items):
    """«Де взяти» for the items a quest asks for, from recipes, loot tables and the world dump"""
    out = []
    for item in items:
        made = FACTS.made_in(item)
        src = FACTS.sources(item)
        bits = []
        if made:
            bits.append([{"text": "робиться: ", "color": "gray"}, {"text": ", ".join(made[:3]), "color": "white"}])
        if src["mobs"]:
            bits.append([{"text": "падає з: ", "color": "gray"}, *names("entity", src["mobs"])])
        if src["skeleton"]:
            bits.append([{"text": "зі скелетів: ", "color": "gray"}, *names("entity", src["skeleton"])])
        if src["structures"]:
            bits.append([{"text": "у скринях: ", "color": "gray"}, *names("structure", src["structures"])])
        if src["biomes"] and not made:
            bits.append([{"text": "у природі: ", "color": "gray"}, *names("biome", src["biomes"])])
        if src["trades"]:
            bits.append([{"text": "продають торговці", "color": "gray"}])
        if not bits:
            continue
        out.append(jline({"translate": FACTS.item_key(item), "color": "aqua"}))
        for b in bits[:4]:
            out.append(jline({"text": "   "}, *b))
    return out


def wiki_target(q):
    for t in q["tasks"]:
        kind, _, rest = t.partition(":")
        if kind == "item" and not rest.startswith("#"):
            return item_stack(rest)[0]
        if kind == "kill":
            return "entity:" + item_stack(rest)[0]
    return q.get("wiki") or q.get("icon")


def compose_desc(q, where):
    out = list(q.get("desc", []))
    tl = [l for l in (task_line(t) for t in q["tasks"]) if l]
    if tl:
        out += ["", "&eЗавдання:"] + tl
    items = []
    for t in q["tasks"]:
        kind, _, rest = t.partition(":")
        if kind == "item" and not rest.startswith("#"):
            it = item_stack(rest)[0]
            if it not in items:
                items.append(it)
    if items and q.get("sources", True):
        sl = source_lines(items[:3])
        if sl:
            out += ["", "&6Де взяти:"] + sl
    if q.get("how"):
        out += ["", "&6Як:"] + [f"&7{i + 1}.&r {step}" for i, step in enumerate(q["how"])]
    if q.get("guide"):
        out += guide_lines(q["guide"], where)
    target = wiki_target(q)
    if target:
        out += ["", jline({"text": "➜ Відкрити у вікі", "color": "dark_aqua", "underlined": True,
                           "clickEvent": {"action": "run_command", "value": "/wiki " + target}})]
    return out


def guide_lines(g, where):
    """The fixed «where / how hard / what to bring / how to find / tip» block of an adventure quest."""
    out = [""]
    if g.get("where"):
        out.append(f"&6Де:&r {g['where']}")
    if g.get("danger"):
        n = int(g["danger"])
        if not 1 <= n <= 5:
            ERRORS.append(f"{where}: danger must be 1-5")
        out.append(f"&6Складність:&r &c{'★' * n}&8{'★' * (5 - n)}&r — {DANGER[n]}")
    if g.get("gear"):
        out.append(f"&6Спорядження:&r {g['gear']}")
    if g.get("find"):
        out.append(f"&6Як знайти:&r {g['find']}")
    if g.get("tip"):
        out.append(f"&6Порада:&r {g['tip']}")
    for k in g:
        if k not in ("where", "danger", "gear", "find", "tip"):
            ERRORS.append(f"{where}: unknown guide field {k}")
    return out


def auto_layout(quests):
    """Place quests left-to-right by dependency depth, siblings spread vertically."""
    depth = {}
    keys = {q["key"] for q in quests}

    def d(q):
        if q["key"] in depth:
            return depth[q["key"]]
        local = [k for k in q.get("deps", []) if ":" not in k and k in keys]
        depth[q["key"]] = 0 if not local else 1 + max(d(next(x for x in quests if x["key"] == k)) for k in local)
        return depth[q["key"]]
    for q in quests:
        d(q)
    cols = {}
    for q in quests:
        cols.setdefault(depth[q["key"]], []).append(q)
    for col, qs in cols.items():
        for i, q in enumerate(qs):
            q.setdefault("pos", (col * 2.0, (i - (len(qs) - 1) / 2) * 1.75))


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    chapters = []
    cdir = os.path.join(HERE, "chapters")
    for fn in sorted(os.listdir(cdir)):
        if fn.endswith(".py"):
            spec = importlib.util.spec_from_file_location(fn[:-3], os.path.join(cdir, fn))
            mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
            chapters.append(mod.CHAPTER)

    groups = {}
    lang = {}               # key -> value (string or list)
    quest_ids = {}          # "chapter:key" -> id
    for ch in chapters:
        for q in ch["quests"]:
            quest_ids[f'{ch["key"]}:{q["key"]}'] = hid(ch["key"], q["key"])
    if len(set(quest_ids.values())) != len(quest_ids):
        ERRORS.append("two quests got the same id")

    os.makedirs(os.path.join(OUT, "chapters"), exist_ok=True)
    for order, ch in enumerate(chapters):
        cid = hid("chapter", ch["key"])
        if len(re.sub(r"&.", "", ch["title"])) > MAX_CHAPTER_TITLE:
            ERRORS.append(f'{ch["key"]}: chapter title longer than {MAX_CHAPTER_TITLE}: {ch["title"]}')
        gid = groups.setdefault(ch["group"], hid("group", ch["group"]))
        lang[f"chapter.{cid}.title"] = ch["title"]
        if ch.get("subtitle"):
            lang[f"chapter.{cid}.chapter_subtitle"] = [ch["subtitle"]]
        auto_layout(ch["quests"])
        out_quests = []
        for q in ch["quests"]:
            where = f'{ch["key"]}:{q["key"]}'
            qid = quest_ids[where]
            tasks = [parse_task(t, qid, i, where) for i, t in enumerate(q["tasks"])]
            rewards = [parse_reward(r, qid, i, where) for i, r in enumerate(q.get("rewards", []))]
            if not q.get("no_rewards") and not any("puffish_skills" in r.get("command", "") for r in rewards):
                rewards.append(parse_reward(f"skill:{q.get('skill', 25)}", qid, 99, where))
            deps = []
            for k in q.get("deps", []):
                full = k if ":" in k and k.split(":")[0] in {c["key"] for c in chapters} else f'{ch["key"]}:{k}'
                if full not in quest_ids:
                    ERRORS.append(f"{where}: unknown dependency {k}")
                else:
                    deps.append(quest_ids[full])
            x, y = q["pos"]
            entry = {"id": qid, "x": float(x), "y": float(y), "tasks": tasks, "rewards": rewards}
            if deps:
                entry["dependencies"] = deps
            if q.get("icon"):
                check("item", q["icon"], where)
                entry["icon"] = {"id": q["icon"]}
            if q.get("shape"):
                entry["shape"] = q["shape"]
            if q.get("size"):
                entry["size"] = float(q["size"])
            out_quests.append(entry)
            if len(re.sub(r"&.", "", q["title"])) > MAX_QUEST_TITLE:
                ERRORS.append(f"{where}: title longer than {MAX_QUEST_TITLE}: {q['title']}")
            if q.get("optional"):
                entry["optional"] = True
            if q.get("pin"):
                PINS.append(qid)
            if q.get("invisible"):
                entry["invisible"] = True
            if q.get("gate"):
                gate_id = quest_ids.get(ch["key"] + ":" + q["gate"])
                if not gate_id:
                    ERRORS.append(f"{where}: unknown gate {q['gate']}")
                else:
                    entry.setdefault("dependencies", []).append(gate_id)
                    entry["hide_until_deps_complete"] = True
                    GATES[q["key"]] = {"quest": qid, "gate": gate_id}
            lang[f"quest.{qid}.title"] = q["title"]
            if q.get("subtitle"):
                lang[f"quest.{qid}.quest_subtitle"] = q["subtitle"]
            lang[f"quest.{qid}.quest_desc"] = compose_desc(q, where)
        check("item", ch["icon"], ch["key"])
        chapter = {"id": cid, "filename": ch["key"], "group": gid, "order_index": order,
                   "icon": {"id": ch["icon"]}, "default_quest_shape": "", "default_hide_dependency_lines": False,
                   "quest_links": [], "quests": out_quests}
        open(os.path.join(OUT, "chapters", ch["key"] + ".snbt"), "w", encoding="utf-8").write(snbt(chapter) + "\n")

    group_names = {}
    for ch in chapters:
        group_names[groups[ch["group"]]] = ch["group"]
    # the side panel lists groups in this order (the travellers' errands right after the start)
    ordered = sorted(groups, key=lambda g: GROUP_ORDER.index(g) if g in GROUP_ORDER else len(GROUP_ORDER))
    open(os.path.join(OUT, "chapter_groups.snbt"), "w", encoding="utf-8").write(
        snbt({"chapter_groups": [{"id": groups[g]} for g in ordered]}) + "\n")
    data = {"version": 13, "default_reward_team": False, "default_consume_items": False,
            "default_autoclaim_rewards": "disabled", "default_quest_shape": "circle", "default_quest_disable_jei": False,
            "emergency_items_cooldown": 300, "drop_loot_crates": False, "disable_gui": False, "grid_scale": 0.5,
            "pause_game": False, "lock_message": "", "progression_mode": "flexible", "detection_delay": 20,
            "show_lock_icons": True, "icon": {"id": "minecraft:compass"},
            "loot_crate_no_drop": {"boss": 0, "monster": 600, "passive": 4000}}
    open(os.path.join(OUT, "data.snbt"), "w", encoding="utf-8").write(snbt(data) + "\n")

    # text: Ukrainian is the pack language; the same text is the fallback for every other locale
    # FTB Quests 2101.1.37 reads one table per locale: lang/<locale>.snbt. A per-folder lang/<locale>/ left by an older
    # version wins over the file (pack/removed.txt moves those aside in game folders)
    table = {"file.0000000000000001.title": "Wilds & Wanderers"}
    table.update({f"chapter_group.{gid}.title": name for gid, name in group_names.items()})
    table.update(lang)
    table.update({f"task.{tid}.title": t for tid, t in TASK_TITLES.items()})
    table = {k: ftb_text(v) for k, v in table.items()}
    for loc in ("uk_ua", "en_us"):
        old = os.path.join(OUT, "lang", loc)
        if os.path.isdir(old):
            shutil.rmtree(old)
        open(os.path.join(OUT, "lang", loc + ".snbt"), "w", encoding="utf-8").write(snbt(table) + "\n")

    os.makedirs(CORE_DATA, exist_ok=True)
    json.dump({"pins": PINS}, open(os.path.join(CORE_DATA, "onboarding.json"), "w", encoding="utf-8"), indent=1)
    json.dump(TUTORIAL, open(os.path.join(CORE_DATA, "tutorial.json"), "w", encoding="utf-8"), indent=1, sort_keys=True)
    json.dump(GATES, open(os.path.join(CORE_DATA, "gates.json"), "w", encoding="utf-8"), indent=1, sort_keys=True)
    total = sum(len(c["quests"]) for c in chapters)
    print(f"chapters: {len(chapters)}, quests: {total}, errors: {len(ERRORS)}")
    for e in ERRORS:
        print("  ERROR", e)
    sys.exit(1 if ERRORS else 0)


if __name__ == "__main__":
    main()
