"""Write the pack's Questborne data into kubejs/data/questborne/questborne/ (a datapack over the mod's own):

  * every quest of the mod, with its dialogue in Ukrainian (pack/tools/i18n/questborne/<npc>.json, keyed by quest
    name; a quest without a translation keeps the English text),
  * the pack's own quests (CUSTOM below): NPCs send players to the bosses, structures and crafts of the other mods,
  * the five shops with Ukrainian item names and mod goods added, and the three reward crates with mod loot.
  python gen_questborne.py   (checks every id against docs/registries; rerun after editing a translation)
"""
import glob, json, os, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "pack", "overrides", "kubejs", "data", "questborne", "questborne")
I18N = os.path.join(HERE, "i18n", "questborne")
REG = os.path.join(ROOT, "docs", "registries")
JAR = glob.glob(os.path.join(ROOT, "client", "mods", "questborne-*.jar"))[0]

reg = lambda n: set(open(os.path.join(REG, n), encoding="utf-8").read().split())
ITEMS, ENTITIES, STRUCTS = reg("item.txt"), reg("entity_type.txt"), reg("worldgen_structure.txt")
DIMS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "twilightforest:twilight_forest", "aether:the_aether"}
ERRORS = []


def need(pool, value, where):
    if value not in pool:
        ERRORS.append(f"{where}: unknown id {value}")


def q(npc, name, rarity, rep, objective, rewards, dialogue, cooldown="5d", repeatable=True, requires=None):
    d = {"id": f"wilds:{npc}/{name}", "npc_type": npc, "rarity": rarity, "reputation_required": rep,
         "repeatable": repeatable, "cooldown": cooldown, "objectives": [objective], "rewards": rewards,
         "dialogue": dict(zip(("description", "accept", "decline", "in_progress", "complete"), dialogue))}
    if requires:
        d["requires"] = requires
    return d


def kill(e, a, b=None):
    return {"type": "kill", "target": e, "count": {"min": a, "max": b or a}}


def obtain(i, a, b=None, consume=True):
    return {"type": "obtain", "item": i, "count": {"min": a, "max": b or a}, "consume": consume}


def craft(i, a, b=None):
    return {"type": "craft", "item": i, "count": {"min": a, "max": b or a}, "consume": True}


def visit(s):
    return {"type": "visit_structure", "structure": s, "count": 1}


def dim(d):
    return {"type": "visit_dimension", "target": d, "count": 1}


def rw(qp, rep, xp, *items):
    r = {"quest_points": qp, "reputation": rep, "bonus_xp": xp}
    if items:
        r["items"] = [{"item": i.split("*")[0], "count": int(i.split("*")[1]) if "*" in i else 1} for i in items]
    return r


# The pack's own quests. Their text tells where to go and how to find it, like the FTB quest book does.
CUSTOM = [
    # --- hunter: monsters and bosses of the other mods ---
    q("hunter", "zombie_kobolds", "common", 0, kill("kobolds:kobold_zombie", 4, 8), rw(14, 4, 45),
      ("Під лісами риються кобольди, а біля їхніх нір бродять мертві кобольди. Мерці лізуть на поверхню вночі. Заспокой кількох — шукай біля лігвищ у лісах і тайзі.",
       "Мертвим кобольдам — спокій.", "Не сьогодні.", "Зомбі-кобольди невеликі, але кусючі. Ходи там, де ліс густий.",
       "Тихіше стало біля нір. Дякую, мисливцю.")),
    q("hunter", "orc_raid", "uncommon", 200, kill("orc_hut:orc", 2, 3), rw(30, 8, 90, "minecraft:iron_ingot*6"),
      ("Орки звели хатини на рівнинах і грабують мандрівників. Вони сильні, але повільні: стріляй здалеку й відходь. Хатину знайде Компас дослідника — «orc_hut».",
       "Орки пошкодують.", "Орки мені не по зубах.", "Орк б'є як молот. Не стій поруч.",
       "Дороги знову безпечні. Ось твоя плата.")),
    q("hunter", "goblin_warband", "uncommon", 200, kill("goblins_tyranny:knight_goblin", 3, 5), rw(28, 7, 85, "goblins_tyranny:coin*12"),
      ("Гобліни-лицарі з таборів на рівнинах і в саванах нападають на подорожніх. Розжени їхній загін. Табір покаже Компас дослідника — «goblinscamp».",
       "Залізо проти гоблінського заліза.", "Хай гобліни поки гуляють.", "Тримай щит: гобліни б'ють гуртом.",
       "Ось їхні монети — гобліни з ними не розлучаються добровільно.")),
    q("hunter", "barbarians", "uncommon", 200, kill("minecolonies:barbarian", 5, 8), rw(30, 8, 90, "minecraft:arrow*32"),
      ("Варвари стоять таборами в лісах і ходять у набіги на колонії. Пройдися по їхньому табору, поки вони не прийшли до нас. Компас дослідника — «barbarian_camp».",
       "Варвари побачать світанок не всі.", "Іншим разом.", "Їхні лучники ховаються за наметами.",
       "Вогнища варварів згасли. Добра робота.")),
    q("hunter", "raptors", "rare", 600, kill("alexscaves:vallumraptor", 3, 5), rw(55, 12, 220, "alexscaves:dinosaur_nugget*8"),
      ("Глибоко під землею, у Первісних печерах, полюють зграї валлумрапторів. Вони вже нападали на шахтарів. Печеру знайде мапа печерного біома від картографа.",
       "Полювання на мисливців.", "Динозаври — то вже занадто.", "Раптори нападають зграєю. Бийся спиною до стіни.",
       "Ти справді бився з динозаврами? Тримай — заслужив.")),
    q("hunter", "naga", "rare", 600, kill("twilightforest:naga", 1), rw(70, 15, 300, "twilightforest:naga_scale*4"),
      ("У Сутінковому лісі, у дворі з моховитого каміння, кружляє велетенська змія — Наґа. Поки вона жива, ліс не пустить далі. Портал: ставок 2×2 з квітами довкола і алмаз у воду.",
       "Змія чи я — побачимо.", "Сутінковий ліс мене лякає.", "Наґа відкривається після ривка. Бий тоді.",
       "Луска наґи! Тепер шлях до вежі ліча відкритий.")),
    q("hunter", "hydra", "epic", 1000, kill("twilightforest:hydra", 1), rw(120, 22, 600, "twilightforest:fiery_blood*2"),
      ("У Вогняному болоті Сутінкового лісу живе триголова гідра. Її кров горить, і зброярі заплатять за неї будь-що. Бери зілля вогнестійкості — там озера лави.",
       "Три голови — три удари.", "Це вже не для мене.", "Голови відростають, поки тіло ціле. Бий у відкриту пащу.",
       "Вогняна кров... вона ще гаряча. Ти легенда, мисливцю.")),
    q("hunter", "ignis", "epic", 2000, kill("cataclysm:ignis", 1), rw(160, 30, 900, "cataclysm:ignitium_ingot*2"),
      ("У Незері, на Палаючій арені, чекає Іґніс — лицар у вогні. Скрафти Полум'яне око з ока Енду, кинь його — і воно покаже дорогу.",
       "Вогонь проти сталі.", "Я ще не готовий.", "Три фази, і кожна лютіша. Заходь збоку.",
       "Ігнітій! Таке бачили лише в старих легендах.")),
    # --- explorer: structures and worlds of the other mods ---
    q("explorer", "goblin_camp", "common", 0, visit("goblins_tyranny:goblinscamp"), rw(20, 6, 60),
      ("Кажуть, на рівнинах з'явилися табори гоблінів: хатини з колод, тотеми, таверна. Подивися, скільки їх там. Компас дослідника — «goblinscamp». Гобліни не люблять чужих — хіба що в гоблінських вухах.",
       "Подивлюся здалеку.", "Гобліни? Ні, дякую.", "Шукай на рівнинах, у саванах і на луках.",
       "Цілий табір! Треба попередити мандрівників.")),
    q("explorer", "kobold_den", "common", 0, visit("kobolds:kobold_den"), rw(20, 6, 60),
      ("Кобольди — дрібні ковалі з-під землі. Їхні лігвища в лісах і тайзі. Знайди одне — їхнє залізо дуже цінують. Компас дослідника — «kobold_den».",
       "Пошукаю нори.", "Не зараз.", "Лігвище під землею, вхід — у лісі.",
       "Ти знайшов кобольдів! Їхні кирки легкі, як пір'я.")),
    q("explorer", "dragon_roost", "uncommon", 200, visit("iceandfire:fire_dragon_roost"), rw(40, 10, 150),
      ("Над рівнинами бачили дим і обвуглену землю — там гніздо вогняного дракона. Знайди його, але не буди господаря. Компас дослідника — «fire_dragon_roost».",
       "Тихо, як миша.", "Дракони? Ні.", "Купи золота й кістки — ознака гнізда.",
       "Ти бачив дракона і повернувся! Розкажеш усім у таверні.")),
    q("explorer", "pirate_ship", "uncommon", 200, visit("dungeons_arise:undead_pirate_ship"), rw(40, 10, 150, "smallships:cannon_ball*8"),
      ("Моряки бачать у тумані корабель з мертвим екіпажем. Знайди його. Знадобиться свій корабель — його будують на верфі біля води. Компас дослідника — «undead_pirate_ship».",
       "Підніміть вітрила!", "Я боюся води.", "Корабель-привид дрейфує у відкритому океані.",
       "Значить, це правда... Тримай ядра — стануть у пригоді.")),
    q("explorer", "underwater_city", "rare", 600, visit("hopo:underwater/underwater_city"), rw(60, 14, 250, "hybrid_aquatic:glowstick*4"),
      ("На дні глибокого океану лежить місто, яке затонуло давно. Потрібні зілля підводного дихання або водолазний костюм. Компас дослідника — «underwater/underwater_city».",
       "Пірнаю.", "Глибина — не моє.", "Місто на самому дні, у глибокому океані.",
       "Що ж там було колись... Дякую, що повернувся живим.")),
    q("explorer", "twilight", "uncommon", 200, dim("twilightforest:twilight_forest"), rw(30, 12, 150), (
        "Є ліс, де ніколи не сходить сонце. Щоб туди потрапити: ставок 2×2, трава й квіти довкола, і алмаз у воду. Сходи туди й повернися з розповіддю.",
        "Відкрию портал.", "Мені й тут добре.", "Квіти мають стояти на кожному блоці трави навколо ставка.",
        "Вічні сутінки... Ти тепер справжній мандрівник."), "0h", False),
    q("explorer", "aether", "rare", 600, dim("aether:the_aether"), rw(40, 14, 200, "aether:cold_parachute*2"), (
        "Над хмарами плавають острови. Рамка зі світлокаменю і вода всередині — і ти там. Візьми парашути: падати звідти дуже довго.",
        "До небес!", "Я боюся висоти.", "Рамка як у порталу в Незер, тільки зі світлокаменю.",
        "Небесні острови! Ось тобі парашути на наступний раз."), "0h", False),
    q("explorer", "abandoned_colony", "uncommon", 200, visit("minecolonies:medieval_oak_colony"), rw(35, 9, 120, "minecolonies:ancienttome"),
      ("Десь у лісах стоїть покинута колонія — будинки, майстерні, але жодної душі. Подивися, що сталося. Компас дослідника — «medieval_oak_colony».",
       "Розгадаю загадку.", "Не люблю порожніх міст.", "Шукай у дубових лісах.",
       "Хто знає, куди всі поділися... Ця книга лежала в ратуші — візьми.")),
    # --- gatherer: materials of the other mods ---
    q("gatherer", "pebbles", "common", 0, obtain("gew2:pebble", 16, 32), rw(8, 3, 30),
      ("Камінці валяються всюди під ногами. Збери їх для доріжок у саду — і руки не зайняті, і кайло не треба.",
       "Нагнуся кілька разів.", "Камінці? Серйозно?", "Просто збирай з землі руками.", "Гарні камінці. Дякую!")),
    q("gatherer", "amber", "uncommon", 200, obtain("alexscaves:amber", 6, 10), rw(30, 8, 90),
      ("Бурштин із Первісних печер зберігає давнє життя. Мені треба кілька шматків. Печеру знайде мапа печерного біома від картографа.",
       "Принесу бурштин.", "Надто глибоко.", "Бурштин росте у вапняку Первісних печер.", "Він теплий на дотик... Дивовижно.")),
    q("gatherer", "pearls", "uncommon", 200, obtain("hybrid_aquatic:pearl", 2, 4), rw(32, 8, 100),
      ("Велетенські молюски на дні ховають перли. Принеси кілька — для намиста.",
       "Пірну за перлами.", "Вода холодна.", "Молюски лежать на дні теплих морів.", "Які гарні! Дякую.")),
    q("gatherer", "zanite", "rare", 600, obtain("aether:zanite_gemstone", 8, 12), rw(50, 12, 200),
      ("Заніт — небесний самоцвіт з островів Етеру. Інструменти з нього міцніють, що більше ними працюєш. Принеси кілька.",
       "Полечу за ним.", "Небо не для мене.", "Руда в святокамені, сірому камені островів.", "Синій, як небо. Дякую!")),
    q("gatherer", "silver", "common", 0, obtain("iceandfire:silver_ingot", 4, 8), rw(16, 4, 50),
      ("Срібло — найкраще проти нежиті. Переплав руду на злитки і принеси.", "Буде срібло.", "Іншим разом.",
       "Срібна руда — у звичайному камені, глибше під землею.", "Чисте срібло. Мерці затремтять.")),
    # --- crafter: crafts of the tech and camping mods ---
    q("crafter", "cogwheels", "common", 0, craft("create:cogwheel", 8, 16), rw(14, 4, 45),
      ("Мені треба шестерні для млина. Андезитовий сплав і дошки — на верстаку виходять шестерні Create.",
       "Зроблю шестерні.", "Не сьогодні.", "Андезитовий сплав — андезит і залізні самородки.", "Тепер млин закрутиться!")),
    q("crafter", "steel_casing", "uncommon", 200, craft("mekanism:steel_casing", 2, 3), rw(32, 8, 100),
      ("Для машин Mekanism потрібен сталевий корпус. Сталь, скло й осмій — і корпус готовий.",
       "Зберу корпуси.", "Надто складно.", "Сталь роблять у металургійному інфузорі з заліза й вугілля.", "Міцні корпуси. Добра робота.")),
    q("crafter", "yurt", "uncommon", 200, craft("nomadictents:yurt_section", 3), rw(28, 7, 85, "nomadictents:mallet"),
      ("Мандрівники просять юрту. Секція: 4 полотна і 2 палиці. Полотно — дві вовни одна над одною.",
       "Пошию.", "Не вмію шити.", "Три секції дадуть крихітну юрту.", "Чудово! Ось молоток — без нього юрту не поставиш.")),
    q("crafter", "cannonballs", "common", 0, craft("smallships:cannon_ball", 8, 16), rw(14, 4, 45),
      ("Корабельникам бракує ядер. Залізні самородки і порох — на верстаку.", "Наллю ядер.", "Іншим разом.",
       "Чотири самородки навколо пороху.", "Тепер пірати двічі подумають.")),
    q("crafter", "tents", "common", 0, craft("simplytents:tent", 1, 2), rw(14, 4, 45),
      ("Мандрівникам потрібні намети. Шкіра й палиці — найпростіший намет, що згортається разом з усім усередині.",
       "Зроблю.", "Не зараз.", "Шкіра зверху, палиці знизу.", "Тепер вони ночуватимуть під дахом.")),
    # --- merchant: goods of the other mods ---
    q("merchant", "goblin_gold", "common", 0, obtain("goblins_tyranny:coin", 16, 32), rw(16, 4, 50, "minecraft:emerald*4"),
      ("Гоблінське золото тече дорогами. Я міняю його на смарагди. Принеси монети — з гоблінів чи з їхніх скринь.",
       "Принесу монети.", "Гобліни мені не друзі.", "Монети падають з гоблінів і лежать у скринях таборів.", "Добре золото. Ось смарагди.")),
    q("merchant", "sea_food", "uncommon", 200, obtain("oceansdelight:fugu_roll", 4, 6), rw(28, 7, 85),
      ("Гості з узбережжя хочуть роли з фугу. Обережно з рибою — отрута!", "Приготую.", "Не вмію готувати рибу.",
       "Рецепт покаже JEI (клавіша R на ролі).", "Смачно й ніхто не отруївся. Чудово!")),
    q("merchant", "brass", "uncommon", 200, obtain("create:brass_ingot", 8, 16), rw(30, 8, 90),
      ("Латунь Create — у великому попиті. Мідь і цинк у механічному змішувачі з нагрівом.", "Буде латунь.", "Надто складно.",
       "Змішувач над казаном, під казаном — горілка з вогнем.", "Блищить! Покупці будуть раді.")),
]

# extra goods for the shops: (npc, item, count, cost, Ukrainian name, reputation)
SHOP_EXTRA = [
    ("explorer", "explorerscompass:explorerscompass", 1, 60, "Компас дослідника", 200),
    ("explorer", "naturescompass:naturescompass", 1, 45, "Компас природи", 200),
    ("explorer", "waystones:return_scroll", 2, 20, "Сувій повернення", 0),
    ("explorer", "simplytents:tent", 1, 25, "Намет", 0),
    ("explorer", "minecolonies:supplycampdeployer", 1, 120, "Табір постачання (колонія)", 600),
    ("gatherer", "gew2:pebble", 16, 4, "Камінці", 0),
    ("gatherer", "farmersdelight:rice_panicle", 4, 8, "Рис", 0),
    ("gatherer", "fishofthieves:coconut", 4, 8, "Кокоси", 0),
    ("crafter", "create:andesite_alloy", 16, 15, "Андезитовий сплав", 0),
    ("crafter", "create:brass_ingot", 4, 25, "Латунний злиток", 200),
    ("crafter", "mekanism:basic_control_circuit", 2, 40, "Базова керувальна схема", 600),
    ("crafter", "nomadictents:mallet", 1, 30, "Наметовий молоток", 200),
    ("hunter", "naturalist:capture_net", 1, 15, "Сачок для звірів", 0),
    ("hunter", "iceandfire:silver_sword", 1, 60, "Срібний меч", 200),
    ("hunter", "smallships:cannon_ball", 16, 20, "Гарматні ядра", 200),
    ("merchant", "goblins_tyranny:coin", 8, 15, "Гоблінські монети", 0),
    ("merchant", "smallships:dockyard", 1, 80, "Верф", 600),
    ("merchant", "sophisticatedbackpacks:backpack", 1, 90, "Рюкзак", 600),
]
# extra crate loot: tier -> (item, min, max)
CRATE_EXTRA = {
    "bronze": [("farmersdelight:beef_stew", 1, 2), ("gew2:pebble", 8, 16), ("goblins_tyranny:coin", 3, 8), ("comforts:rope_and_nail", 1, 2)],
    "silver": [("create:brass_ingot", 2, 4), ("mekanism:ingot_osmium", 2, 5), ("iceandfire:silver_ingot", 2, 4),
               ("ars_nouveau:source_gem", 2, 4), ("irons_spellbooks:arcane_essence", 2, 4), ("waystones:warp_dust", 2, 6)],
    "gold": [("create:precision_mechanism", 1, 1), ("mekanism:alloy_reinforced", 1, 2), ("irons_spellbooks:rare_ink", 1, 1),
             ("apotheosis:sigil_of_socketing", 1, 1), ("minecolonies:ancienttome", 1, 1), ("hybrid_aquatic:black_pearl", 1, 1)],
}


def load_i18n():
    tr = {}
    for f in glob.glob(os.path.join(I18N, "*.json")):
        npc = os.path.splitext(os.path.basename(f))[0]
        for name, d in json.load(open(f, encoding="utf-8")).items():
            tr[f"{npc}/{name}"] = d
    return tr


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(data, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)


def main():
    tr = load_i18n()
    z = zipfile.ZipFile(JAR)
    translated = total = 0
    for n in z.namelist():
        if "/questborne/quests/" in n and n.endswith(".json"):
            d = json.loads(z.read(n))
            key = d["id"].split(":", 1)[1]
            total += 1
            if key in tr:
                missing = set(d["dialogue"]) - set(tr[key])
                if missing:
                    ERRORS.append(f"{key}: translation lacks {sorted(missing)}")
                d["dialogue"] = {k: tr[key].get(k, v) for k, v in d["dialogue"].items()}
                translated += 1
                write(os.path.join(OUT, "quests", key + ".json"), d)
    for d in CUSTOM:
        where = d["id"]
        o = d["objectives"][0]
        if o["type"] == "kill":
            need(ENTITIES, o["target"], where)
        elif o["type"] in ("obtain", "craft"):
            need(ITEMS, o["item"], where)
        elif o["type"] == "visit_structure":
            need(STRUCTS, o["structure"], where)
        elif o["type"] == "visit_dimension":
            need(DIMS, o["target"], where)
        for i in d["rewards"].get("items", []):
            need(ITEMS, i["item"], where)
        write(os.path.join(OUT, "quests", d["npc_type"], "wilds_" + d["id"].split("/")[1] + ".json"), d)
    names = json.load(open(os.path.join(I18N, "shop_names.json"), encoding="utf-8")) if os.path.exists(os.path.join(I18N, "shop_names.json")) else {}
    for n in z.namelist():
        if "/shop_pools/" in n and n.endswith(".json"):
            d = json.loads(z.read(n))
            for it in d["items"]:
                it["display_name"] = names.get(it["display_name"], it["display_name"])
            for npc, item, count, cost, name, rep in SHOP_EXTRA:
                if npc == d["npc_type"]:
                    need(ITEMS, item, "shop " + npc)
                    e = {"item": item, "count": count, "cost": cost, "display_name": name}
                    if rep:
                        e["reputation_required"] = rep
                    d["items"].append(e)
            write(os.path.join(OUT, "shop_pools", os.path.basename(n)), d)
        if "/crate_loot/" in n and n.endswith(".json"):
            d = json.loads(z.read(n))
            tier = os.path.splitext(os.path.basename(n))[0]
            for item, a, b in CRATE_EXTRA.get(tier, []):
                need(ITEMS, item, "crate " + tier)
                d["entries"].append({"item": item, "min": a, "max": b})
            write(os.path.join(OUT, "crate_loot", os.path.basename(n)), d)
    print(f"quests: {translated}/{total} translated, {len(CUSTOM)} own; shops and crates written; errors: {len(ERRORS)}")
    for e in ERRORS:
        print("  ERROR", e)
    sys.exit(1 if ERRORS else 0)


main()
