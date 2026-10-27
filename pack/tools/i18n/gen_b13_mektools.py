"""Generates i18n/batches/b13_mektools.json: Mekanism: Tools config strings are one template per material."""
import json, os, re

HERE = os.path.dirname(__file__)
SRC = json.load(open(os.path.join(HERE, "..", "..", "i18n", "src", "mekanismtools.json"), encoding="utf-8"))
OUT = os.path.join(HERE, "..", "..", "i18n", "batches", "b13_mektools.json")

# genitive: "made of ..."
GEN = {"bronze": "бронзи", "lapis_lazuli": "лазуриту", "osmium": "осмію", "refined_glowstone": "очищеного світлокаменю",
       "refined_obsidian": "очищеного обсидіану", "steel": "сталі", "wood": "дерева", "stone": "каменю", "iron": "заліза",
       "gold": "золота", "diamond": "алмазу", "netherite": "незериту"}
MOB = ("Шанс множиться на модифікатор складності чанка.")

SPAWN = {  # gear_spawn_chance.<material>.<suffix>
    "chance.boots": ("Шанс чобіт", "Шанс, що моб з'явиться в чоботях з {g}."),
    "chance.chestplate": ("Шанс нагрудника", "Шанс, що моб з'явиться в нагруднику з {g}."),
    "chance.helmet": ("Шанс шолома", "Шанс, що моб з'явиться в шоломі з {g}."),
    "chance.leggings": ("Шанс наголінників", "Шанс, що моб з'явиться в наголінниках з {g}."),
    "chance.enchantment.armor": ("Шанс зачарованої броні", "Шанс, що броня з {g} на мобі буде зачарована. " + MOB),
    "chance.enchantment.weapon": ("Шанс зачарованої зброї", "Шанс, що зброя з {g} у моба буде зачарована. " + MOB + " Потрібно ввімкнути canSpawnWeapon."),
    "chance.multiple_piece": ("Шанс кількох частин", "Шанс, що після кожної частини броні з {g} моб більше не отримає частин. Порядок: чоботи, наголінники, нагрудник, шолом."),
    "chance.multiple_piece.hard": ("Шанс кількох частин (важко)", "Те саме на важкій складності: шанс, що після кожної частини броні з {g} моб більше не отримає частин. Порядок: чоботи, наголінники, нагрудник, шолом."),
    "spawn_weapon": ("Зі зброєю", "Якщо ввімкнено, зомбі можуть з'являтися зі зброєю з {g}."),
    "sword_weight": ("Вага меча", "Шанс, що моб отримає меч з {g}, а не лопату. Потрібно ввімкнути canSpawnWeapon."),
    "top_level": ("Спорядження з {g}", None),
    "top_level.button": ("Змінити шанси появи", None),
    "top_level.tooltip": ("Шанси появи частин спорядження з {g}. Примітка: ці значення застосовуються після загальної перевірки mobArmorSpawnRate (або відповідного шансу зброї) і після рівного розподілу між матеріалами.", None),
}
MATS = {  # materials.<material>.<suffix>
    "armor.boots": ("Захист чобіт", "Захист чобіт з {g}."), "armor.chestplate": ("Захист нагрудника", "Захист нагрудників з {g}."),
    "armor.helmet": ("Захист шолома", "Захист шоломів з {g}."), "armor.leggings": ("Захист наголінників", "Захист наголінників з {g}."),
    "attack_speed": ("Швидкість атаки", "Швидкість атаки паксельних інструментів з {g}."),
    "attack_speed.axe": ("Швидкість атаки сокири", "Швидкість атаки сокир з {g}."),
    "attack_speed.hoe": ("Швидкість атаки мотики", "Швидкість атаки мотик з {g}."),
    "attack_speed.paxel": ("Швидкість атаки пакселя", "Швидкість атаки пакселів з {g}."),
    "attack_speed.pickaxe": ("Швидкість атаки кайла", "Швидкість атаки кайл з {g}."),
    "attack_speed.shovel": ("Швидкість атаки лопати", "Швидкість атаки лопат з {g}."),
    "attack_speed.sword": ("Швидкість атаки меча", "Швидкість атаки мечів з {g}."),
    "axe_damage": ("Шкода сокири", "Модифікатор шкоди сокир з {g}."),
    "damage": ("Базова шкода", "Базова шкода предметів з {g}."),
    "damage.hoe": ("Шкода мотики", "Модифікатор шкоди мотик з {g}. Має бути не менше від'ємної базової шкоди."),
    "damage.paxel": ("Шкода пакселя", "Модифікатор шкоди пакселів з {g}. Має бути не менше від'ємної базової шкоди."),
    "damage.pickaxe": ("Шкода кайла", "Модифікатор шкоди кайл з {g}. Має бути не менше від'ємної базової шкоди."),
    "damage.shovel": ("Шкода лопати", "Модифікатор шкоди лопат з {g}. Має бути не менше від'ємної базової шкоди."),
    "damage.sword": ("Шкода меча", "Модифікатор шкоди мечів з {g}. Має бути не більше базової шкоди."),
    "durability": ("Міцність", "Найбільша міцність пакселів з {g}."),
    "durability.boots": ("Міцність чобіт", "Найбільша міцність чобіт з {g}."),
    "durability.chestplate": ("Міцність нагрудника", "Найбільша міцність нагрудників з {g}."),
    "durability.helmet": ("Міцність шолома", "Найбільша міцність шоломів з {g}."),
    "durability.leggings": ("Міцність наголінників", "Найбільша міцність наголінників з {g}."),
    "durability.paxel": ("Міцність пакселя", "Найбільша міцність пакселів з {g}."),
    "durability.shield": ("Міцність щита", "Найбільша міцність щитів з {g}."),
    "durability.tool": ("Міцність інструментів", "Найбільша міцність інструментів з {g}."),
    "efficiency": ("Ефективність", "Ефективність інструментів з {g}."),
    "efficiency.paxel": ("Ефективність пакселя", "Ефективність пакселів з {g}."),
    "enchantability": ("Зачаровуваність", "Природна зачаровуваність предметів з {g}."),
    "enchantability.paxel": ("Зачаровуваність пакселя", "Природна зачаровуваність пакселів з {g}."),
    "knockback_resistance": ("Опір відкиданню", "Базовий опір відкиданню броні з {g}."),
    "toughness": ("Міцність броні", "Базова міцність броні з {g}."),
    "top_level": ("Спорядження з {g}", None), "top_level.button": ("Змінити: {g}", None),
    "top_level.tooltip": ("Налаштування матеріалу: предмети з {g}", None),
}
FIXED = {
    "configuration.mekanismtools.client.durability_tooltips": "Підказки міцності",
    "configuration.mekanismtools.client.durability_tooltips.tooltip": "Показувати міцність спорядження з Mekanism: Tools у підказках.",
    "configuration.mekanismtools.server.gear_spawn_chance": "Шанс спорядження на мобах",
    "configuration.mekanismtools.server.gear_spawn_chance.button": "Змінити шанси спорядження",
    "configuration.mekanismtools.server.gear_spawn_chance.tooltip": "Налаштування шансу появи спорядження Mekanism: Tools на мобах",
    "configuration.mekanismtools.server.gear_spawn_chance.armor": "Шанс броні",
    "configuration.mekanismtools.server.gear_spawn_chance.armor.tooltip": "Шанс, що на мобах з'явиться броня Mekanism: Tools. Множиться на модифікатор складності чанка. У звичайній грі для броні 0.15, тут типово 0.1, щоб моби рідше мали змішану броню.",
    "configuration.mekanismtools.server.gear_spawn_chance.weapon": "Шанс зброї",
    "configuration.mekanismtools.server.gear_spawn_chance.weapon.tooltip": "Шанс, що зомбі з'явиться зі зброєю Mekanism: Tools у руці.",
    "configuration.mekanismtools.server.gear_spawn_chance.weapon.hard": "Шанс зброї (важко)",
    "configuration.mekanismtools.server.gear_spawn_chance.weapon.hard.tooltip": "Шанс, що зомбі з'явиться зі зброєю Mekanism: Tools у руці на важкій складності.",
    "configuration.mekanismtools.startup.materials": "Налаштування матеріалів",
    "configuration.mekanismtools.startup.materials.button": "Змінити налаштування матеріалів",
    "configuration.mekanismtools.startup.materials.tooltip": "Налаштування матеріалів Mekanism: Tools. Не синхронізуються автоматично між клієнтом і сервером, тож обов'язково тримайте однакові значення на обох.",
    "fml.menu.mods.info.description.mekanismtools": "Модуль інструментів для Mekanism",
    "mekanismtools.configuration.section.mekanism.tools.client.toml": "Клієнт",
    "mekanismtools.configuration.section.mekanism.tools.client.toml.title": "Mekanism: Tools — клієнт",
    "mekanismtools.configuration.section.mekanism.tools.materials.startup.toml": "Матеріали",
    "mekanismtools.configuration.section.mekanism.tools.materials.startup.toml.title": "Mekanism: Tools — матеріали",
    "mekanismtools.configuration.section.mekanism.tools.toml": "Загальне",
    "mekanismtools.configuration.section.mekanism.tools.toml.title": "Mekanism: Tools — загальне",
    "mekanismtools.configuration.title": "Налаштування Mekanism: Tools",
    "tag.item.c.tools.paxel": "Пакселі",
}
out, missing = {}, []
for k in SRC:
    if k in FIXED:
        out[k] = FIXED[k]; continue
    m = re.match(r"configuration\.mekanismtools\.(server\.gear_spawn_chance|startup\.materials)\.([a-z_]+)\.(.+)$", k)
    if not m or m.group(2) not in GEN:
        missing.append(k); continue
    section, mat, rest = m.groups()
    table = SPAWN if section.startswith("server") else MATS
    tooltip = rest.endswith(".tooltip") and rest[:-8] in table and table[rest[:-8]][1]
    key = rest[:-8] if tooltip else rest
    if key not in table:
        missing.append(k); continue
    text = table[key][1] if tooltip else table[key][0]
    out[k] = text.format(g=GEN[mat])
json.dump({"mekanismtools": out}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(out), "translated;", len(missing), "missing:", missing[:20])
