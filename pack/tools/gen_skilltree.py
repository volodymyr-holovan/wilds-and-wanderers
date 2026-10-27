"""Generate the radial skill tree «Шлях мандрівника» for Pufferfish's Skills (with Wilds Core's reward types).

  python gen_skilltree.py <pack overrides dir>

Layout: the root in the centre, an inner ring of 16 general bonuses, and 8 explicit branches of 45 degrees each:
Воїн, Мисливець, Маг, Майстер, Мореплавець, Слідопит, Дипломат, Торговець. A branch has three spokes of 11 nodes;
the 6th node of a spoke is a notable (2.5x value, costs 2), the 11th is a big unique ability of Wilds Core; some
uniques have a mastery behind them (level 2), and every branch ends in a keystone at the rim. Neighbouring branches
are joined at their 4th tier, so builds can mix. About 320 nodes in all.

Rewards: "wilds_core:stat" (attribute with a fallback when the attribute's mod is missing), "wilds_core:perk" (the pack's
own numbers) and "wilds_core:ability" (big abilities, with a fallback ability when their mod is missing).
Experience: Pufferfish's sources for fights, ores, logs, crops, fishing, enchanting and food (below), plus Wilds Core's
own for travel, discoveries, breeding, taming, trading, sleep and advancements (SkillXp.java).
"""
import json, math, os, sys

OUT = os.path.join(sys.argv[1], "kubejs", "data", "wilds", "puffish_skills")
CAT = os.path.join(OUT, "categories", "path")

# ---------------------------------------------------------------- reward helpers

FALLBACK = {  # when a mod's attribute is missing, a vanilla one of similar worth
    "irons_spellbooks": ("generic.max_health", 1, "add_value"),
    "irons_lib": ("generic.attack_damage", 0.02, "add_multiplied_base"),
    "neoforge": ("generic.movement_speed", 0.01, "add_multiplied_base"),
}


def stat(attribute, value, op="add_value"):
    d = {"attribute": attribute, "value": value, "operation": op}
    ns = attribute.split(":")[0] if ":" in attribute else "minecraft"
    if ns in FALLBACK:
        a, v, o = FALLBACK[ns]
        d["fallback"] = {"attribute": a, "value": v, "operation": o}
    return {"type": "wilds_core:stat", "data": d}


def perk(name, value):
    return {"type": "wilds_core:perk", "data": {"perk": name, "value": value}}


def ability(name, fallback=None):
    d = {"ability": name}
    if fallback:
        d["fallback"] = fallback
    return {"type": "wilds_core:ability", "data": d}


def icon(item):
    return {"type": "item", "data": {"item": item}}


DEF = {}


def define(key, title, item, rewards, desc, frame="task", cost=1, **extra):
    d = {"title": title, "description": desc, "icon": icon(item), "frame": {"type": "advancement", "data": {"frame": frame}},
         "rewards": rewards}
    if cost != 1:
        d["cost"] = cost
    d.update(extra)
    DEF[key] = d
    return key


def minor(title, item, rewards, text):
    return (title, item, rewards, text)


def scaled(rewards, k):
    out = []
    for r in rewards:
        r = json.loads(json.dumps(r))
        if "value" in r["data"]:
            r["data"]["value"] = round(r["data"]["value"] * k, 4)
        if "fallback" in r["data"]:
            r["data"]["fallback"]["value"] = round(r["data"]["fallback"]["value"] * k, 4)
        out.append(r)
    return out


B = {}  # branch key -> dict(name, icon, spokes=[(spoke name, [minors], unique)], keystone)

# --- Воїн
B["war"] = dict(name="Воїн", icon="minecraft:iron_sword", spokes=[
    ("Сила", [minor("Важка рука", "minecraft:iron_sword", [stat("generic.attack_damage", 0.03, "add_multiplied_base")], "+3% шкоди в ближньому бою"),
              minor("Влучний удар", "minecraft:flint", [stat("irons_lib:crit_damage", 0.06)], "+6% критичної шкоди"),
              minor("Розмах", "minecraft:golden_sword", [stat("player.sweeping_damage_ratio", 0.06)], "+6% шкоди розмаху мечем")],
     "heavy_strike"),
    ("Захист", [minor("Міцне тіло", "minecraft:golden_apple", [stat("generic.max_health", 2)], "+1 серце"),
                minor("Загартування", "minecraft:iron_chestplate", [stat("generic.armor", 1)], "+1 броні"),
                minor("Непохитність", "minecraft:anvil", [stat("generic.knockback_resistance", 0.04)], "+4% опору відкиданню"),
                minor("Сталь", "minecraft:iron_block", [stat("generic.armor_toughness", 0.5)], "+0,5 міцності броні")],
     "last_stand"),
    ("Спритність", [minor("Швидкий клинок", "minecraft:feather", [stat("generic.attack_speed", 0.03, "add_multiplied_base")], "+3% швидкості атаки"),
                    minor("Ухильність", "minecraft:phantom_membrane", [stat("irons_lib:dodge_chance", 0.015)], "+1,5% шансу ухилитися"),
                    minor("Довгий випад", "minecraft:stick", [stat("player.entity_interaction_range", 0.2)], "+0,2 блока дальності удару"),
                    minor("Легкі ноги", "minecraft:sugar", [stat("generic.movement_speed", 0.015, "add_multiplied_base")], "+1,5% швидкості руху")],
     "weapon_throw"),
], keystone=("Берсерк", "minecraft:netherite_axe", [ability("retribution"), stat("generic.attack_damage", 0.10, "add_multiplied_base"), stat("generic.max_health", 4)],
             "Ключова навичка. Відплата: 10% отриманої шкоди повертається нападнику. +10% шкоди, +2 серця."))
# --- Мисливець
B["hun"] = dict(name="Мисливець", icon="minecraft:bow", spokes=[
    ("Лучник", [minor("Тятива", "minecraft:string", [stat("irons_lib:arrow_damage", 0.05)], "+5% шкоди стріл"),
                minor("Далекий постріл", "minecraft:spectral_arrow", [perk("far_shot", 0.06)], "Стріли, тризуби й снаряди заклинань летять на 6% швидше й далі"),
                minor("Пробиття", "minecraft:arrow", [stat("irons_lib:projectile_damage", 0.03)], "+3% шкоди всіх снарядів")],
     "volley"),
    ("Звіролов", [minor("Вірний звір", "minecraft:bone", [perk("tamer", 0.08)], "Ваші приручені звірі й коні: +8% здоров'я і шкоди"),
                  minor("Нюх мисливця", "minecraft:rabbit_foot", [stat("generic.luck", 0.25)], "+0,25 удачі (кращий лут і улов)"),
                  minor("Досвід слідопита", "minecraft:experience_bottle", [stat("irons_lib:experience_gained", 0.03)], "+3% досвіду з мобів і блоків")],
     "beast_master"),
    ("Хижак", [minor("Око хижака", "minecraft:spider_eye", [stat("irons_lib:crit_damage", 0.04)], "+4% критичної шкоди"),
               minor("Бронебій", "minecraft:iron_nugget", [stat("irons_lib:armor_pierce", 0.5)], "Ігнорує 0,5 броні ворога"),
               minor("Тихі кроки", "minecraft:leather_boots", [stat("generic.movement_speed", 0.015, "add_multiplied_base")], "+1,5% швидкості руху")],
     "hunters_mark"),
], keystone=("Господар зграї", "minecraft:lead", [ability("twin_births"), perk("tamer", 0.25), stat("irons_lib:arrow_damage", 0.10)],
             "Ключова навичка. Двійня: тварини, яких ви розводите, з шансом 25% народжують двох. Звірі на 25% сильніші, +10% шкоди стріл."))
# --- Маг
SCHOOLS = [("fire", "minecraft:fire_charge", "Вогонь"), ("ice", "minecraft:packed_ice", "Лід"), ("lightning", "minecraft:lightning_rod", "Блискавка"),
           ("holy", "minecraft:golden_apple", "Світло"), ("nature", "minecraft:oak_sapling", "Природа"), ("ender", "minecraft:ender_pearl", "Енд"),
           ("blood", "minecraft:redstone", "Кров"), ("evocation", "minecraft:totem_of_undying", "Закликання"), ("eldritch", "minecraft:sculk", "Невимовне")]


def school(s, it, n):
    return minor(f"Школа: {n}", it, [stat(f"irons_spellbooks:{s}_spell_power", 0.05, "add_multiplied_base")], f"+5% сили школи «{n}»")


B["mag"] = dict(name="Маг", icon="minecraft:enchanted_book", spokes=[
    ("Сила чар", [minor("Сила заклять", "minecraft:blaze_powder", [stat("irons_spellbooks:spell_power", 0.03, "add_multiplied_base")], "+3% сили заклинань")]
     + [school(*x) for x in SCHOOLS[:4]], "spell_reflect"),
    ("Мана", [minor("Резервуар мани", "minecraft:lapis_lazuli", [stat("irons_spellbooks:max_mana", 15)], "+15 мани"),
              minor("Потік мани", "minecraft:amethyst_shard", [stat("irons_spellbooks:mana_regen", 0.05, "add_multiplied_base")], "+5% відновлення мани")]
     + [school(*x) for x in SCHOOLS[4:7]], "mana_harvest"),
    ("Захист чар", [minor("Магічний щит", "minecraft:shield", [stat("irons_spellbooks:spell_resist", 0.03, "add_multiplied_base")], "+3% опору заклинанням"),
                    minor("Відвернення", "minecraft:prismarine_shard", [perk("magic_guard", 0.05)], "Магічна шкода менша на 5%"),
                    minor("Плинні жести", "minecraft:clock", [stat("irons_spellbooks:cast_time_reduction", 0.03, "add_multiplied_base")], "-3% часу творення заклять"),
                    minor("Швидкий розум", "minecraft:redstone_torch", [stat("irons_spellbooks:cooldown_reduction", 0.03, "add_multiplied_base")], "-3% перезаряджання заклять")]
     + [school(*x) for x in SCHOOLS[7:]], "mana_shield"),
], keystone=("Архімаг", "minecraft:nether_star", [ability("free_spell", "arcane_ward"), stat("irons_spellbooks:spell_power", 0.10, "add_multiplied_base"), stat("irons_spellbooks:max_mana", 60)],
             "Ключова навичка. Подих мани: раз на годину одне закляття не коштує мани (без Iron's Spells — Чарівний захист: -20% магічної шкоди). +10% сили заклять, +60 мани."))
# --- Майстер
B["cra"] = dict(name="Майстер", icon="minecraft:diamond_pickaxe", spokes=[
    ("Шахтар", [minor("Міцні руки", "minecraft:iron_pickaxe", [stat("player.block_break_speed", 0.04, "add_multiplied_base")], "+4% швидкості видобутку"),
                minor("Багата жила", "minecraft:raw_iron", [perk("extra_ore", 0.02)], "2% шанс, що руда дасть ще один шматок"),
                minor("Кам'яне серце", "minecraft:cobblestone", [stat("irons_lib:mining_speed", 0.03)], "+3% швидкості копання")],
     "vein_miner"),
    ("Ремісник", [minor("Дбайливість", "minecraft:anvil", [perk("tool_saver", 0.05)], "5% шанс, що інструмент не зноситься від удару"),
                  minor("Довгі руки", "minecraft:stick", [stat("player.block_interaction_range", 0.25)], "+0,25 блока дальності з блоками"),
                  minor("Учень майстра", "minecraft:book", [stat("irons_lib:experience_gained", 0.03)], "+3% досвіду")],
     "thrifty_crafter"),
    ("Рудознавець", [minor("Нюх на руду", "minecraft:emerald", [perk("extra_ore", 0.02)], "2% шанс додаткової руди"),
                     minor("Щаслива кирка", "minecraft:gold_ingot", [stat("generic.luck", 0.25)], "+0,25 удачі"),
                     minor("Водолаз-шахтар", "minecraft:turtle_helmet", [stat("player.submerged_mining_speed", 0.15, "add_multiplied_base")], "+15% видобутку під водою")],
     "prospector"),
], keystone=("Дотик горна", "minecraft:blast_furnace", [ability("smelters_touch"), perk("extra_ore", 0.06), perk("tool_saver", 0.10)],
             "Ключова навичка. Сира руда з шансом 20% одразу випадає виплавленою. +6% шансу додаткової руди, інструменти зношуються на 10% рідше."))
# --- Мореплавець
B["sea"] = dict(name="Мореплавець", icon="minecraft:trident", spokes=[
    ("Плавець", [minor("Плавці", "minecraft:tropical_fish", [stat("neoforge:swim_speed", 0.06, "add_multiplied_base")], "+6% швидкості плавання"),
                 minor("Глибокий вдих", "minecraft:glass_bottle", [stat("generic.oxygen_bonus", 0.5)], "Довше без повітря під водою"),
                 minor("Течія", "minecraft:kelp", [stat("generic.water_movement_efficiency", 0.05)], "Вода менше сповільнює")],
     "deep_sight"),
    ("Мисливець глибин", [minor("Шкіра акули", "minecraft:prismarine_shard", [perk("aqua_guard", 0.05)], "Морські істоти завдають на 5% менше шкоди"),
                          minor("Підводний шахтар", "minecraft:prismarine", [stat("player.submerged_mining_speed", 0.15, "add_multiplied_base")], "+15% видобутку під водою"),
                          minor("Гарпунер", "minecraft:trident", [perk("far_shot", 0.05)], "Тризуби й стріли летять на 5% далі")],
     "battle_breath"),
    ("Шукач скарбів", [minor("Морська удача", "minecraft:nautilus_shell", [stat("generic.luck", 0.3)], "+0,3 удачі (краща риболовля й скрині)"),
                       minor("Розповіді моряків", "minecraft:map", [stat("irons_lib:experience_gained", 0.03)], "+3% досвіду"),
                       minor("Солоний вітер", "minecraft:oak_boat", [stat("generic.oxygen_bonus", 0.5)], "Довше без повітря під водою")],
     "sea_gifts"),
], keystone=("Тінь глибин", "minecraft:heart_of_the_sea", [ability("deep_shadow"), ability("deep_shadow"), perk("aqua_guard", 0.15), stat("neoforge:swim_speed", 0.15, "add_multiplied_base")],
             "Ключова навичка. Морські істоти бачать вас у воді вдвічі гірше й швидше втрачають. -15% шкоди від них, +15% швидкості плавання."))
# --- Слідопит
B["exp"] = dict(name="Слідопит", icon="minecraft:compass", spokes=[
    ("Мандрівник", [minor("Легка хода", "minecraft:feather", [stat("generic.movement_speed", 0.015, "add_multiplied_base")], "+1,5% швидкості руху"),
                    minor("М'яке приземлення", "minecraft:leather_boots", [stat("generic.fall_damage_multiplier", -0.05), stat("generic.safe_fall_distance", 0.5)], "-5% шкоди від падіння, +0,5 блока безпечного падіння"),
                    minor("Сходинка", "minecraft:oak_stairs", [stat("generic.step_height", 0.1)], "Легше заходите на сходинки")],
     "pathfinder"),
    ("Виживальник", [minor("Вогнетривкість", "minecraft:magma_cream", [perk("fire_guard", 0.05), stat("generic.burning_time", -0.08)], "-5% шкоди від вогню, горите на 8% коротше"),
                     minor("Загоєння", "minecraft:glistering_melon_slice", [perk("regen", 0.25)], "Поки ви ситі, кожні 5 с загоюєтеся (¼ серця за кожен такий вузол)"),
                     minor("Ситний обід", "minecraft:cooked_beef", [perk("hearty", 0.10)], "+10% насичення від їжі"),
                     minor("Витривалість", "minecraft:apple", [stat("generic.max_health", 1)], "+½ серця")],
     "well_rested"),
    ("Вершник", [minor("Вірний кінь", "minecraft:saddle", [perk("mount", 0.06)], "Ваш скакун на 6% швидший"),
                 minor("Стрибун", "minecraft:rabbit_hide", [stat("generic.jump_strength", 0.03, "add_multiplied_base")], "+3% висоти стрибка"),
                 minor("Дорожній досвід", "minecraft:map", [stat("irons_lib:experience_gained", 0.02)], "+2% досвіду")],
     "rider"),
], keystone=("Чуття небезпеки", "minecraft:spyglass", [ability("danger_sense"), perk("fire_guard", 0.15), stat("generic.fall_damage_multiplier", -0.15)],
             "Ключова навичка. Над кожним монстром, що полює на вас, видно сердиті іскри — навіть крізь стіни. -15% шкоди від вогню й падіння."))
# --- Дипломат
B["dip"] = dict(name="Дипломат", icon="minecraft:writable_book", spokes=[
    ("Тінь", [minor("Нечутні кроки", "minecraft:white_wool", [stat("player.sneaking_speed", 0.05)], "Швидше крадетеся"),
              minor("Непомітність", "minecraft:black_dye", [stat("generic.movement_speed", 0.01, "add_multiplied_base")], "+1% швидкості руху"),
              minor("Спокій", "minecraft:honey_bottle", [stat("irons_lib:healing_received", 0.04)], "+4% отримуваного лікування")],
     "quiet_step"),
    ("Посередник", [minor("Переконливість", "minecraft:paper", [perk("persuasion", 0.05)], "+5% до шансу виторгувати плату чи переконати квестодавця"),
                    minor("Добре ім'я", "minecraft:name_tag", [stat("generic.luck", 0.2)], "+0,2 удачі"),
                    minor("Мудрість", "minecraft:book", [stat("irons_lib:experience_gained", 0.03)], "+3% досвіду")],
     "silver_tongue"),
    ("Друг народів", [minor("Відкрите серце", "minecraft:golden_apple", [stat("generic.max_health", 1)], "+½ серця"),
                      minor("Гостинність", "minecraft:bread", [perk("hearty", 0.06)], "+6% насичення від їжі"),
                      minor("Довіра", "minecraft:emerald", [perk("persuasion", 0.04)], "+4% до шансу виторгувати плату чи переконати квестодавця")],
     "goblin_friend"),
], keystone=("Миротворець", "minecraft:white_banner", [ability("peacemaker"), perk("persuasion", 0.15)],
             "Ключова навичка. Присядьте 4 рази поспіль — усі нейтральні істоти, що розлютилися на вас (вовки, големи, бджоли, ендермени, зомбі-пігліни...), заспокоюються. Раз на 10 хвилин."))
# --- Торговець
B["tra"] = dict(name="Торговець", icon="minecraft:emerald", spokes=[
    ("Купець", [minor("Торг", "minecraft:emerald", [perk("discount", 0.34)], "Кожні 3 такі вузли — мінус 1 смарагд з кожного обміну"),
                minor("Комерсант", "minecraft:gold_nugget", [stat("generic.luck", 0.2)], "+0,2 удачі"),
                minor("Досвід ринку", "minecraft:experience_bottle", [stat("irons_lib:experience_gained", 0.02)], "+2% досвіду")],
     "haggler"),
    ("Сусід", [minor("Привітність", "minecraft:poppy", [stat("irons_lib:healing_received", 0.03)], "+3% отримуваного лікування"),
               minor("Сільський хліб", "minecraft:bread", [perk("hearty", 0.06)], "+6% насичення від їжі"),
               minor("Урожай", "minecraft:wheat", [perk("harvest", 0.04)], "4% шанс, що стиглий урожай дасть ще одну одиницю")],
     "village_gifts"),
    ("Наймач", [minor("Обладунок найманця", "minecraft:chainmail_chestplate", [stat("generic.armor", 1)], "+1 броні"),
                minor("Звичка до бою", "minecraft:shield", [stat("generic.knockback_resistance", 0.03)], "+3% опору відкиданню"),
                minor("Тугий гаманець", "minecraft:bundle", [perk("discount", 0.25)], "Знижка при обміні (сумується)")],
     "mercenaries"),
], keystone=("Щедра угода", "minecraft:villager_spawn_egg", [ability("generous_deal"), perk("discount", 1), perk("harvest", 0.10)],
             "Ключова навичка. Раз на день будь-який обмін із селянином безкоштовний — сплачене повертається. Ще мінус 1 смарагд з кожного обміну, +10% урожаю."))

# ---------------------------------------------------------------- unique abilities: title, icon, description, fallback, has a mastery
U = {
    "heavy_strike": ("Нищівний удар", "minecraft:mace", "Затисніть клавішу зліва від 1 (`; її можна змінити в Керуванні), дивлячись на ворога: коли під прицілом наповниться смужка (не швидше, ніж перезаряджається зброя, мінімум 1 с), ви завдасте потрійної шкоди й виб'єте щит із рук.", None, True),
    "last_stand": ("Останній рубіж", "minecraft:totem_of_undying", "Раз на 20 хвилин смертельний удар залишає вам одне серце, а кілька секунд ви майже невразливі й відновлюєтеся.", None, False),
    "weapon_throw": ("Метальник", "minecraft:iron_axe", "Затисніть праву кнопку з мечем чи сокирою в руці й натисніть ліву: зброя полетить у ціль і завдасть подвійної шкоди, а тоді повернеться до вас.", None, True),
    "volley": ("Залп", "minecraft:crossbow", "Кожна п'ята стріла летить разом із двома сусідніми (на 2-му рівні — кожна четверта).", None, True),
    "beast_master": ("Володар звірів", "minecraft:bone", "Ваші приручені тварини й коні мають на 25% більше здоров'я і шкоди за кожен рівень.", None, True),
    "hunters_mark": ("Мітка мисливця", "minecraft:spectral_arrow", "Ворог, у якого влучив ваш постріл, світиться 10 секунд і отримує на 15% більше шкоди від усіх.", None, False),
    "spell_reflect": ("Дзеркало чар", "minecraft:glass", "Замахніться зброєю чи підніміть щит, коли у вас летить закляття, вогняна куля чи інший магічний снаряд, — він полетить назад у того, хто його випустив.", None, False),
    "mana_harvest": ("Жнива мани", "minecraft:lapis_block", "Кожне вбивство відновлює 10% мани за рівень. Без Iron's Spells — лікує ½ серця.", "soul_siphon", True),
    "mana_shield": ("Щит мани", "minecraft:amethyst_block", "15% отриманої шкоди сплачується маною, поки її вистачає. Без Iron's Spells — Чарівний захист: -20% магічної шкоди.", "arcane_ward", False),
    "vein_miner": ("Жила", "minecraft:diamond_ore", "Копайте руду присівши — разом із нею зламаються ще до 6 таких самих блоків руди, що торкаються (на 2-му рівні — до 10).", None, True),
    "thrifty_crafter": ("Ощадливий майстер", "minecraft:crafting_table", "Під час крафту з шансом 8% за рівень один інгредієнт повертається.", None, True),
    "prospector": ("Рудознатець", "minecraft:amethyst_shard", "Присядьте й клацніть ПКМ киркою по каменю — уся руда в радіусі 8 блоків (10 на 2-му рівні) заблищить для вас. Раз на хвилину.", None, True),
    "deep_sight": ("Очі глибин", "minecraft:sea_lantern", "Під водою ви бачите ясно, як удень.", None, False),
    "battle_breath": ("Подих бою", "minecraft:pufferfish", "Кожен ваш удар під водою повертає три бульбашки повітря (за рівень).", None, True),
    "sea_gifts": ("Дари моря", "minecraft:nautilus_shell", "Коли вперше відкриваєте скриню в затонулому кораблі, океанських руїнах чи іншій морській споруді, часто знаходите ще раковину наутилуса, смарагди, призмарин, а зрідка — серце моря.", None, True),
    "pathfinder": ("Дорожня хода", "minecraft:dirt_path", "На дорогах — стежках, гравії, бруківці, кам'яній цеглі — ви на 20% швидші за рівень.", None, True),
    "well_rested": ("Добрий відпочинок", "minecraft:red_bed", "Проспавши ніч у ліжку чи наметі, прокидаєтеся з регенерацією й поглинанням шкоди.", None, False),
    "rider": ("Вершник", "minecraft:saddle", "Ваш скакун на 15% швидший за рівень, і ні він, ні ви не отримуєте шкоди від падіння під час їзди.", None, True),
    "quiet_step": ("Тиха хода", "minecraft:sculk_sensor", "Монстри помічають вас на 20% пізніше.", None, True),
    "silver_tongue": ("Красномовство", "minecraft:writable_book", "У розмовах із мандрівниками-квестодавцями з'являється варіант «збрехати, що все вже зроблено» — лише в найцінніших дорученнях і з невеликим шансом (на провал замовник ображається й більше не дає вам доручень). Також +15% за рівень до шансу виторгувати кращу плату.", None, True),
    "goblin_friend": ("Друг гоблінів", "minecraft:gold_nugget", "Гобліни-вояки більше не нападають першими, а корчмар, купець і коваль гоблінів раз на день дарують вам гоблінські монети. Без мода гоблінів — Друг піглінів: пігліни спокійні й без золотої броні.", "piglin_friend", False),
    "haggler": ("Торгівля з розумом", "minecraft:emerald_block", "Кожен обмін із селянином на 1 смарагд дешевший за рівень.", None, True),
    "village_gifts": ("Улюбленець села", "minecraft:cake", "Селяни поруч з вами час від часу кидають вам подарунки свого ремесла.", None, True),
    "mercenaries": ("Найманці", "minecraft:iron_golem_spawn_egg", "Присядьте й клацніть по залізному голему чи вартовому села, тримаючи 8 смарагдів, — він іде за вами й б'ється за вас до наступного дня (на 2-му рівні — двоє водночас).", None, True),
}

# ---------------------------------------------------------------- layout
ORDER = ["war", "hun", "mag", "cra", "sea", "exp", "dip", "tra"]   # clockwise from the top
R_INNER, R0, STEP, SPREAD = 120, 210, 56, 13
TIERS, NOTABLE = 11, 6
skills, links = {}, []


def pos(angle, r):
    a = math.radians(angle)
    return round(math.cos(a) * r), round(-math.sin(a) * r)


define("root", "Мандрівник", "minecraft:compass", [stat("generic.max_health", 2)],
       "Початок шляху. +1 серце. Звідси вісім гілок: Воїн, Мисливець, Маг, Майстер, Мореплавець, Слідопит, Дипломат, Торговець.", "challenge")
skills["root"] = {"x": 0, "y": 0, "definition": "root", "root": True}

INNER = [("Витривалість", "minecraft:apple", [stat("generic.max_health", 2)], "+1 серце"),
         ("Легка хода", "minecraft:feather", [stat("generic.movement_speed", 0.02, "add_multiplied_base")], "+2% швидкості руху"),
         ("Щаслива зірка", "minecraft:rabbit_foot", [stat("generic.luck", 0.25)], "+0,25 удачі"),
         ("Цікавість", "minecraft:experience_bottle", [stat("irons_lib:experience_gained", 0.03)], "+3% досвіду"),
         ("Шкіра", "minecraft:leather_chestplate", [stat("generic.armor", 1)], "+1 броні"),
         ("Міцні руки", "minecraft:iron_pickaxe", [stat("player.block_break_speed", 0.03, "add_multiplied_base")], "+3% швидкості видобутку"),
         ("Іскра магії", "minecraft:lapis_lazuli", [stat("irons_spellbooks:max_mana", 10)], "+10 мани"),
         ("Загоєння", "minecraft:glistering_melon_slice", [perk("regen", 0.25)], "Поки ви ситі, повільно загоюєтеся")]
inner_ids = []
for i in range(16):
    t, it, rw, tx = INNER[i % 8]
    k = define(f"in_{i % 8}", t, it, rw, tx)
    sid = f"in_{i}"
    x, y = pos(90 - i * 22.5 - 11.25, R_INNER)
    skills[sid] = {"x": x, "y": y, "definition": k}
    inner_ids.append(sid)
for i in range(16):
    links.append([inner_ids[i], inner_ids[(i + 1) % 16]])

tier4 = {}
for bi, bk in enumerate(ORDER):
    br = B[bk]
    centre = 90 - bi * 45
    # the two inner nodes of this sector; the root touches one of them
    a_in, b_in = inner_ids[(2 * bi) % 16], inner_ids[(2 * bi + 1) % 16]
    links.append(["root", a_in if bi % 2 == 0 else b_in])
    ends = []
    for si, (sname, minors, uniq) in enumerate(br["spokes"]):
        angle = centre - (si - 1) * SPREAD
        prev = a_in if si == 0 else b_in if si == 2 else (a_in if bi % 2 else b_in)
        for t in range(1, TIERS + 1):
            sid = f"{bk}_{si}_{t}"
            x, y = pos(angle, R0 + (t - 1) * STEP)
            if t == TIERS:
                title, item, desc, fb, mastery = U[uniq]
                key = define(f"{bk}_u_{uniq}", title, item, [ability(uniq, fb)], f"Велика навичка гілки «{br['name']}». {desc}", "challenge", 3,
                             extra_description="Ціна: 3 очки.")
            elif t == NOTABLE:
                m = minors[0]
                key = define(f"{bk}_{si}_notable", f"{m[0]} II", m[1], scaled(m[2], 2.5),
                             f"Помітний вузол гілки «{br['name']}» ({sname}): у 2,5 раза більше. {m[3]}", "goal", 2,
                             extra_description="Ціна: 2 очки.")
            else:
                m = minors[(t - 1) % len(minors)]
                key = define(f"{bk}_{si}_m{(t - 1) % len(minors)}", m[0], m[1], m[2], f"{br['name']} · {sname}. {m[3]}")
            skills[sid] = {"x": x, "y": y, "definition": key}
            links.append([prev, sid])
            prev = sid
            if t == 4:
                tier4[(bi, si)] = sid
        ends.append(prev)
        if U[uniq][4]:  # mastery: the unique's second level, one step further out
            mid = f"{bk}_{si}_mastery"
            x, y = pos(angle, R0 + TIERS * STEP + 8)
            title, item, desc, fb, _ = U[uniq]
            key = define(f"{bk}_mastery_{uniq}", f"{title} II", item, [ability(uniq, fb)],
                         f"Майстерність: «{title}» сильніша (2-й рівень).", "challenge", 3, required_spent_points=40,
                         extra_description="Відкривається, коли у дереві витрачено щонайменше 40 очок. Ціна: 3 очки.")
            skills[mid] = {"x": x, "y": y, "definition": key}
            links.append([prev, mid])
    # the keystone at the rim, reachable from any of the branch's three uniques
    kt, kitem, krew, kdesc = br["keystone"]
    kid = f"{bk}_keystone"
    x, y = pos(centre, R0 + TIERS * STEP + 92)
    key = define(f"{bk}_keystone", kt, kitem, krew, kdesc, "challenge", 4, required_spent_points=70,
                 extra_description="Ключова навичка: з'єднана з усіма трьома великими навичками гілки, досить мати одну з них. "
                                   "Відкривається, коли у дереві витрачено щонайменше 70 очок. Ціна: 4 очки.")
    skills[kid] = {"x": x, "y": y, "definition": key}
    for e in ends:
        links.append([e, kid])

# neighbouring branches meet at their 4th tier
for bi in range(len(ORDER)):
    links.append([tier4[(bi, 2)], tier4[((bi + 1) % len(ORDER), 0)]])

category = {
    "unlocked_by_default": True,
    "title": "Шлях мандрівника",
    "description": "Вісім гілок розвитку. Досвід дають бої, видобуток, мандри, відкриття нових біомів і споруд, риболовля, торгівля, "
                   "розведення тварин, досягнення й квести. Великі навички на кінцях гілок змінюють гру; ключові — на краю.",
    "icon": icon("minecraft:compass"),
    "background": "textures/block/deepslate_tiles.png",
}


def tag_test(kind, tag):
    return {"type": "puffish_skills:test", "data": {kind: tag}}


experience = {
    # slower than before: level 10 needs 100, level 50 needs 340, from level 144 on 900 per level
    "experience_per_level": {"type": "expression", "data": {"expression": "min(40 + level * 6, 900)"}},
    "sources": [
        {"type": "puffish_skills:kill_entity", "data": {
            "variables": {"dropped_xp": {"operations": [{"type": "get_dropped_experience"}]},
                          "max_health": {"operations": [{"type": "get_killed_living_entity"}, {"type": "get_max_health"}]}},
            "experience": "dropped_xp * 0.6 + max_health / 12",
            "anti_farming": {"limit_per_chunk": 15, "reset_after_seconds": 300}}},
        {"type": "puffish_skills:mine_block", "data": {
            "variables": {
                "silk_touch": {"operations": [{"type": "get_tool_item_stack"},
                                              {"type": "puffish_skills:test", "data": {"nbt": "{Enchantments:[{id:\"minecraft:silk_touch\"}]}"}}]},
                "ore": {"operations": [{"type": "get_mined_block_state"}, tag_test("block", "#c:ores")]}},
            # logs (not ones a player placed) and ripe crops pay in Wilds Core (SkillXp), which can tell them apart
            "experience": [{"condition": "!silk_touch & ore", "expression": "3"}]}},
        {"type": "puffish_skills:fish_item", "data": {"experience": "4"}},
        {"type": "puffish_skills:enchant_item", "data": {
            "variables": {"levels": {"operations": [{"type": "get_levels"}]}},
            "experience": "levels"}},
        {"type": "puffish_skills:eat_food", "data": {"experience": "1"}},
    ],
}

os.makedirs(CAT, exist_ok=True)
json.dump({"version": 3, "categories": ["path"]}, open(os.path.join(OUT, "config.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
for name, data in [("category.json", category), ("definitions.json", DEF), ("skills.json", skills),
                   ("connections.json", {"normal": {"bidirectional": links}}), ("experience.json", experience)]:
    json.dump(data, open(os.path.join(CAT, name), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
uniques = sum(1 for k in DEF if "_u_" in k)
print(f"skills: {len(skills)}, definitions: {len(DEF)}, connections: {len(links)}, unique abilities: {uniques}, keystones: {len(ORDER)}")

# ---------------------------------------------------------------- the wiki page, from the same data, for players
WIKI = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "mods-src", "wilds_wiki", "src", "main", "resources",
                    "assets", "wilds_wiki", "wilds_wiki", "uk_ua", "skills.md")
page = ["---", "topic: skills", "title: Шлях мандрівника (навички)", "icon: minecraft:compass", "order: 15",
        "keywords: навички, навичка, вміння, дерево навичок, рівень, очки, досвід навичок, гілка, skills, skill tree, perks, "
        "воїн, мисливець, маг, майстер, мореплавець, слідопит, дипломат, торговець, нищівний удар, метання, красномовство", "---",
        "«Шлях мандрівника» — кругле дерево навичок. Відкривається клавішею **K**. "
        "У центрі — початок, навколо — кільце загальних бонусів, а від нього розходяться **вісім гілок**: "
        + ", ".join(B[k]["name"] for k in ORDER) + ". Можна йти однією гілкою до кінця або брати потроху з сусідніх — "
        "сусідні гілки з'єднані на четвертому кроці.",
        "", "# Як отримати очки",
        "Кожен новий рівень дерева — одне очко. Досвід дерева дають майже всі справи в грі, потроху за кожну:",
        "- перемоги над істотами (сильніші — більше; на одному місці фармити не вийде — досвід там швидко вичерпується) і боси;",
        "- руда, видобута без «Шовкового дотику», природні дерева (не ті, що ви самі поставили) і **стиглий** урожай;",
        "- мандри пішки, верхи, човном і в повітрі — якщо ви справді кудись ідете, а не кружляєте на місці;",
        "- нові біоми й нові види споруд, перше відкриття кожної скрині зі здобиччю, перший крафт кожного предмета;",
        "- риболовля, зачарування, їжа, розведення й приручення тварин, торгівля, ніч у ліжку, досягнення;",
        "- квести з книги квестів (J) і доручення мандрівників — найщедріше.",
        "Перші рівні приходять швидко, далі — повільніше.",
        "", "# Ціна вузлів",
        "- Звичайний вузол коштує **1 очко**, помітний (більший, «II») — **2**, велика навичка на кінці гілки — **3**.",
        "- Вузол можна взяти, лише якщо поруч уже є взятий: ідіть від центру назовні.",
        "- **Майстерність** (друга ступінь великої навички) коштує 3 очки й відкривається, коли у дереві загалом витрачено 40 очок.",
        "- **Ключова навичка** на самому краю гілки коштує 4 очки й відкривається після 70 витрачених очок; досить мати одну з трьох великих навичок гілки.",
        "Якщо вузол не береться — подивіться в його підказку: там написано, скільки він коштує і чого бракує.",
        ""]
for k in ORDER:
    br = B[k]
    page.append(f"# {br['name']}")
    for sname, minors, uniq in br["spokes"]:
        title, item, desc, fb, mastery = U[uniq]
        small = "; ".join(m[3][0].lower() + m[3][1:] if m[3][:1].isupper() else m[3] for m in minors)
        page.append(f"**{sname}.** Дрібні вузли: {small}. Шостий вузол — те саме, але в 2,5 раза сильніше.")
        page.append(f"- Велика навичка **«{title}»**: {desc}" + (" Має другу ступінь (майстерність)." if mastery else ""))
    kt, _, _, kdesc = br["keystone"]
    page.append(f"- Ключова навичка **«{kt}»**: {kdesc.replace('Ключова навичка. ', '')}")
    page.append("")
page += ["# Керування активними навичками",
         "- **Нищівний удар** — затиснути клавішу зліва від 1 (`), дивлячись на ворога. Клавішу можна змінити: Налаштування → Керування.",
         "- **Метальник** — затиснути праву кнопку миші з мечем чи сокирою й натиснути ліву.",
         "- **Миротворець** — присісти 4 рази поспіль.",
         "- **Найманці** — присісти й клацнути ПКМ по голему чи вартовому, тримаючи 8 смарагдів.",
         "- **Жила** — копати руду присівши. **Рудознатець** — присісти й клацнути ПКМ киркою по каменю.",
         "Решта навичок працює сама.",
         "", "> Якщо мода, на якому тримається навичка, немає, вона замінюється схожою (так написано в її описі) — порожніх вузлів не буває."]
open(WIKI, "w", encoding="utf-8").write("\n".join(page) + "\n")
print("wiki page ->", os.path.normpath(WIKI))
