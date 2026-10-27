"""Generates i18n/batches/b14_iceandfire.json: banner patterns (colour x motif grid) plus the remaining Ice and Fire strings."""
import json, os, re

HERE = os.path.dirname(__file__)
SRC = json.load(open(os.path.join(HERE, "..", "..", "i18n", "src", "iceandfire.json"), encoding="utf-8"))
OUT = os.path.join(HERE, "..", "..", "i18n", "batches", "b14_iceandfire.json")

COLORS = {"black": "Чорний", "blue": "Синій", "brown": "Коричневий", "cyan": "Бірюзовий", "gray": "Сірий", "green": "Зелений",
          "light_blue": "Блакитний", "light_gray": "Світло-сірий", "lime": "Лаймовий", "magenta": "Пурпуровий",
          "orange": "Помаранчевий", "pink": "Рожевий", "purple": "Фіолетовий", "red": "Червоний", "white": "Білий", "yellow": "Жовтий"}
MOTIFS = {"amphithere": ("амфіптера", "f"), "bird": ("стімфалійський птах", "m"), "dread": ("знак жаху", "m"),
          "eye": ("око циклопа", "n"), "fae": ("фея", "f"), "feather": ("перо", "n"), "fire": ("вогняний дракон", "m"),
          "fire_head": ("голова вогняного дракона", "f"), "gorgon": ("голова горгони", "f"), "hippocampus": ("гіпокамп", "m"),
          "hippogryph_head": ("голова гіпогрифа", "f"), "ice": ("крижаний дракон", "m"), "ice_head": ("голова крижаного дракона", "f"),
          "lightning": ("громовий дракон", "m"), "lightning_head": ("голова громового дракона", "f"), "mermaid": ("сирена", "f"),
          "sea_serpent": ("левіафан", "m"), "troll": ("голова троля", "f"), "weezer": ("Weezer", "m")}


def agree(adj, g):
    if g == "m":
        return adj
    soft = adj.endswith("ій")
    return adj[:-2] + {"f": "я" if soft else "а", "n": "є" if soft else "е"}[g]


out = {}
for k in SRC:
    m = re.match(r"item\.iceandfire\.banner_pattern_([a-z_]+)\.desc\.([a-z_]+)$", k)
    if m and m.group(1) in MOTIFS and m.group(2) in COLORS:
        noun, g = MOTIFS[m.group(1)]
        out[k] = f"{agree(COLORS[m.group(2)], g)} {noun}"

MOB = {"amphithere": "амфіптера", "bird": "стімфалійський птах", "cockatrice": "кокатріс", "cyclops": "циклоп", "deathworm": "черв смерті",
       "dragon": "дракон", "ghost": "примара", "gorgon": "горгона", "hippocampus": "гіпокамп", "hippogryphs": "гіпогрифи", "hydra": "гідра",
       "lich": "ліч", "pixie": "піксі", "seaSerpent": "левіафан", "siren": "сирена", "troll": "троль"}
GENERIC = {"attackDamage": "Шкода атаки", "maxHealth": "Найбільше здоров'я", "spawn": "Може з'являтися", "spawnWeight": "Вага появи",
           "spawnChance": "Шанс появи", "griefing": "Руйнування блоків", "targetSearchLength": "Дальність пошуку цілі",
           "flightSpeed": "Швидкість польоту", "tameTime": "Час приручення", "comment": "Не використовується"}
SPECIFIC = {
    "bestiary_gui": "Бестіарій", "block.iceandfire.deepslate_silver_ore": "Глибосланцева срібна руда",
    "block.iceandfire.dreadwood_leaves": "Листя дерева жаху", "block.iceandfire.dreadwood_sapling": "Паросток дерева жаху",
    "block.iceandfire.raw_silver_block": "Блок необробленого срібла",
    "config.iceandfire.amphithere.spawnWeight": "Вага появи", "config.iceandfire.amphithere.villagerSearchLength": "Дальність пошуку селян",
    "config.iceandfire.armors.dragonSteelBaseArmor": "Базовий захист", "config.iceandfire.armors.dragonSteelBaseDamage": "Базова шкода",
    "config.iceandfire.armors.dragonSteelBaseDurability": "Базова міцність",
    "config.iceandfire.armors.dragonsteelArmorEnchantability": "Зачаровуваність броні з драконесталі",
    "config.iceandfire.armors.dragonsteelArmorKnockbackResistance": "Опір відкиданню броні з драконесталі",
    "config.iceandfire.armors.dragonsteelArmorToughness": "Міцність броні з драконесталі",
    "config.iceandfire.armors.dragonsteelBootsArmor": "Захист чобіт з драконесталі", "config.iceandfire.armors.dragonsteelBootsDurability": "Міцність чобіт з драконесталі",
    "config.iceandfire.armors.dragonsteelChestplateArmor": "Захист нагрудника з драконесталі", "config.iceandfire.armors.dragonsteelChestplateDurability": "Міцність нагрудника з драконесталі",
    "config.iceandfire.armors.dragonsteelHelmetArmor": "Захист шолома з драконесталі", "config.iceandfire.armors.dragonsteelHelmetDurability": "Міцність шолома з драконесталі",
    "config.iceandfire.armors.dragonsteelLeggingsArmor": "Захист наголінників з драконесталі", "config.iceandfire.armors.dragonsteelLeggingsDurability": "Міцність наголінників з драконесталі",
    "config.iceandfire.bird.attackAnimals": "Атакує тварин", "config.iceandfire.bird.dataTagDrops": "Здобич за тегами даних",
    "config.iceandfire.bird.featherAttackDamage": "Шкода пір'ям", "config.iceandfire.bird.featherDropChance": "Шанс випадіння пір'я",
    "config.iceandfire.bird.flightHeight": "Висота польоту", "config.iceandfire.bird.flockLength": "Розмір зграї",
    "config.iceandfire.category.armors": "Броня", "config.iceandfire.category.misc": "Різне", "config.iceandfire.category.tools": "Інструменти",
    "config.iceandfire.category.worldgen": "Генерація світу", "config.iceandfire.client": "Клієнт",
    "config.iceandfire.cockatrice.chickenSearchLength": "Дальність пошуку курей", "config.iceandfire.cockatrice.chickensLayRottenEggs": "Кури несуть тухлі яйця",
    "config.iceandfire.cockatrice.eggChance": "Шанс появи з кинутого яйця", "config.iceandfire.customMainMenu": "Власне головне меню Ice and Fire",
    "config.iceandfire.cyclops.biteDamage": "Шкода укусу", "config.iceandfire.cyclops.sheepSearchLength": "Дальність пошуку овець",
    "config.iceandfire.cyclops.spawnCaveChance": "Шанс печери циклопа", "config.iceandfire.cyclops.spawnWanderingChance": "Шанс мандрівного циклопа",
    "config.iceandfire.deathworm.attackMonsters": "Атакує чудовиськ",
    "config.iceandfire.dragon.animalsFear": "Тварини бояться", "config.iceandfire.dragon.attackDamageFire": "Шкода вогнем",
    "config.iceandfire.dragon.attackDamageIce": "Шкода кригою", "config.iceandfire.dragon.attackDamageLightning": "Шкода блискавкою",
    "config.iceandfire.dragon.blockBreakingDropChance": "Шанс здобичі з розбитих блоків", "config.iceandfire.dragon.breakBlockCooldown": "Затримка руйнування блоків",
    "config.iceandfire.dragon.brushTimesMul": "Множник чищення щіткою", "config.iceandfire.dragon.canDespawn": "Може зникати",
    "config.iceandfire.dragon.canHealFromBiting": "Лікується від укусів", "config.iceandfire.dragon.chunkLoadSummonCrystal": "Кристал виклику завантажує чанки",
    "config.iceandfire.dragon.digWhenStuck": "Прокопується, коли застряг", "config.iceandfire.dragon.dragonFlightSpeedMod": "Множник швидкості польоту дракона",
    "config.iceandfire.dragon.eggBornTime": "Час вилуплення з яйця", "config.iceandfire.dragon.enableBrushDragonScales": "Збирати луску щіткою",
    "config.iceandfire.dragon.explosiveBreath": "Вибухове дихання", "config.iceandfire.dragon.flapNoiseDistance": "Дальність звуку крил",
    "config.iceandfire.dragon.fluteDistance": "Дальність дії флейти", "config.iceandfire.dragon.generate.denGoldAmount": "Кількість золота в лігві",
    "config.iceandfire.dragon.generate.oreRatio": "Частка руди", "config.iceandfire.dragon.generate.skeletonChance": "Шанс скелета",
    "config.iceandfire.dragon.generate.skeletons": "Генерувати скелети", "config.iceandfire.dragon.goldSearchLength": "Дальність пошуку золота",
    "config.iceandfire.dragon.hungerTickRate": "Швидкість голоду (тіки)", "config.iceandfire.dragon.loot.blood": "Здобич: кров",
    "config.iceandfire.dragon.loot.heart": "Здобич: серце", "config.iceandfire.dragon.loot.skull": "Здобич: череп",
    "config.iceandfire.dragon.maxBreathTimeMul": "Множник тривалості дихання", "config.iceandfire.dragon.maxBrushScalesDropPerTime": "Найбільше луски за одне чищення",
    "config.iceandfire.dragon.maxFlight": "Найбільша висота польоту", "config.iceandfire.dragon.maxPathingNodes": "Найбільше вузлів шляху",
    "config.iceandfire.dragon.maxTamedDragonAge": "Найбільший вік прирученого дракона (дні)", "config.iceandfire.dragon.movedWronglyFix": "Виправлення «рухався неправильно»",
    "config.iceandfire.dragon.neutralToPlayer": "Типово нейтральний до гравця", "config.iceandfire.dragon.sleep": "Може спати",
    "config.iceandfire.dragon.tamedGriefing": "Приручені руйнують блоки", "config.iceandfire.dragon.villagersFear": "Селяни бояться",
    "config.iceandfire.dragon.wanderFromHomeDistance": "Дальність блукання від дому", "config.iceandfire.dragonAuto3rdPerson": "Автоматичний вигляд від третьої особи",
    "config.iceandfire.ghost.alwaysSpawnFromChest": "Завжди з'являється з одиночної скрині", "config.iceandfire.ghost.fromPlayerDeaths": "З'являється після смерті гравців",
    "config.iceandfire.ghost.generateGraveyards": "Генерувати цвинтарі",
    "config.iceandfire.hippocampus.spawnChance": "Шанс появи гіпокампа", "config.iceandfire.hippocampus.swimSpeedMod": "Множник швидкості плавання гіпокампа",
    "config.iceandfire.hippogryphs.fightSpeedMod": "Множник швидкості польоту",
    "config.iceandfire.misc.allowAttributeOverriding": "Дозволити перевизначення атрибутів", "config.iceandfire.misc.dreadQueenMaxHealth": "Найбільше здоров'я Королеви жаху",
    "config.iceandfire.misc.enableDragonSeeker": "Увімкнути шукач драконів", "config.iceandfire.pixie.size": "Розмір", "config.iceandfire.pixie.stealItems": "Краде предмети",
    "config.iceandfire.seaSerpent.attackDamage": "Сила атаки", "config.iceandfire.seaSerpent.baseHealth": "Базове здоров'я",
    "config.iceandfire.siren.maxSingTime": "Найдовший спів", "config.iceandfire.siren.shader": "Шейдер сирени", "config.iceandfire.siren.timeBetweenSongs": "Час між піснями",
    "config.iceandfire.title": "§km§bIce §fand §6Fire§f: налаштування§km",
    "config.iceandfire.tools.dragonBloodFireDuration": "Тривалість вогню від драконячої крові", "config.iceandfire.tools.dragonBloodFrozenDuration": "Тривалість заморожування від драконячої крові",
    "config.iceandfire.tools.dragonFireAbility": "Здібності вогняного дракона", "config.iceandfire.tools.dragonIceAbility": "Здібності крижаного дракона",
    "config.iceandfire.tools.dragonLightningAbility": "Здібності громового дракона", "config.iceandfire.tools.dragonLightningDamageReduction": "Зменшення шкоди від блискавки",
    "config.iceandfire.tools.dragonLightningMaxSearchCount": "Найбільше цілей блискавки", "config.iceandfire.tools.dragonLightningSearchRange": "Дальність пошуку цілей блискавки",
    "config.iceandfire.tools.dragonsteelFireDuration": "Тривалість вогню від драконесталі", "config.iceandfire.tools.dragonsteelFrozenDuration": "Тривалість заморожування від драконесталі",
    "config.iceandfire.tools.phantasmalBladeAbility": "Здібність примарного клинка", "config.iceandfire.troll.dropWeapon": "Випускає зброю",
    "config.iceandfire.worldgen.dangerousDistanceLimit": "Межа відстані небезпечних споруд", "config.iceandfire.worldgen.dangerousSeparationLimit": "Межа віддаленості небезпечних споруд",
    "config.iceandfire.worldgen.generateCyclopsCaveChance": "Шанс печери циклопа", "config.iceandfire.worldgen.generateFireDragonCaveChance": "Шанс печери вогняного дракона",
    "config.iceandfire.worldgen.generateFireDragonRoostChance": "Шанс гнізда вогняного дракона", "config.iceandfire.worldgen.generateGorgonTempleChance": "Шанс храму горгони",
    "config.iceandfire.worldgen.generateGraveYardChance": "Шанс цвинтаря", "config.iceandfire.worldgen.generateHydraCaveChance": "Шанс печери гідри",
    "config.iceandfire.worldgen.generateIceDragonCaveChance": "Шанс печери крижаного дракона", "config.iceandfire.worldgen.generateIceDragonRoostChance": "Шанс гнізда крижаного дракона",
    "config.iceandfire.worldgen.generateLightningDragonCaveChance": "Шанс печери громового дракона", "config.iceandfire.worldgen.generateLightningDragonRoostChance": "Шанс гнізда громового дракона",
    "config.iceandfire.worldgen.generateMausoleumChance": "Шанс мавзолею", "config.iceandfire.worldgen.generateMyemexHiveDesertChance": "Шанс вулика мірмексів у пустелі",
    "config.iceandfire.worldgen.generateMyemexHiveJungleChance": "Шанс вулика мірмексів у джунглях", "config.iceandfire.worldgen.generatePixieVillageChance": "Шанс села піксі",
    "config.iceandfire.worldgen.generateSirenIslandChance": "Шанс острова сирен", "config.iceandfire.worldgen.villagerHouseWeight": "Вага будинку селянина",
    "config.jade.plugin_iceandfire.dragon": "Дані дракона", "config.jade.plugin_iceandfire.dragon_egg_block": "Дані блока драконячого яйця",
    "config.jade.plugin_iceandfire.dragon_egg_entity": "Дані драконячого яйця", "config.jade.plugin_iceandfire.multipart": "Багаточастинні істоти",
    "dragon_egg.hatchTime": "До вилуплення: %s с", "effect.iceandfire.frozen": "Заморожений", "effect.iceandfire.siren_charm": "Чари сирени",
    "emi.category.iceandfire.fire_forge": "Вогняна драконяча кузня", "emi.category.iceandfire.ice_forge": "Крижана драконяча кузня",
    "emi.category.iceandfire.lightning_forge": "Громова драконяча кузня", "entity.iceandfire.amphithere_arrow": "Стріла амфіптери",
    "entity.iceandfire.lightning_dragon_charge": "Заряд громового дракона", "entity.minecraft.villager.iceandfire.scribe": "Писар",
    "iceandfire.ponder.dragon_forge.header": "Дракон живить драконячу кузню",
    "iceandfire.ponder.dragon_forge.text_1": "Це ядро кузні: усі предмети кладуться сюди",
    "iceandfire.ponder.dragon_forge.text_2": "Дракон має бачити цей блок, щоб живити кузню",
    "iceandfire.ponder.dragon_forge.text_3": "Кузню живить лише дракон відповідного виду",
    "iceandfire.ponder.dragon_forge.text_4": "Кузню відкривають клацанням по блоках у центрі кожної грані",
    "iceandfire.ponder.tag.dragon_forge": "Драконяча кузня", "iceandfire.ponder.tag.dragon_forge.description": "Кузні для виплавки драконесталі",
    "item.iceandfire.cooked_rice_with_fire_dragon_meat": "Смажений рис з м'ясом вогняного дракона",
    "item.iceandfire.cooked_rice_with_ice_dragon_meat": "Смажений рис з м'ясом крижаного дракона",
    "item.iceandfire.cooked_rice_with_lightning_dragon_meat": "Смажений рис з м'ясом громового дракона",
    "item.iceandfire.dragon_seeker": "Шукач драконів", "item.iceandfire.dragon_seeker.credit": "Перенесено з мода Ice and Fire: Dragonseeker від Syrikal",
    "item.iceandfire.dragon_seeker.found": "Дракона знайдено", "item.iceandfire.dragon_seeker.found_location": "Найближчий дракон: ",
    "item.iceandfire.dragon_seeker.not_found": "Дракона не знайдено", "item.iceandfire.dragon_seeker.tooltip": "Показує найближчого дракона в радіусі 150 блоків",
    "item.iceandfire.dragonarmor_dragon_steel_fire": "Драконячі обладунки з вогняної драконесталі",
    "item.iceandfire.dragonarmor_dragon_steel_ice": "Драконячі обладунки з крижаної драконесталі",
    "item.iceandfire.dragonarmor_dragon_steel_lightning": "Драконячі обладунки з громової драконесталі",
    "item.iceandfire.dragonarmor_netherite": "Незеритові драконячі обладунки",
    "item.iceandfire.epic_dragon_seeker": "Епічний шукач драконів", "item.iceandfire.epic_dragon_seeker.tooltip": "Показує найближчого живого дракона в радіусі 200 блоків",
    "item.iceandfire.ghost_cream": "Примарні вершки", "item.iceandfire.godly_dragon_seeker": "Божественний шукач драконів",
    "item.iceandfire.godly_dragon_seeker.tooltip": "Показує розташування найближчого дикого живого дракона в радіусі 500 блоків (лише творчий режим)",
    "item.iceandfire.legendary_dragon_seeker": "Легендарний шукач драконів",
    "item.iceandfire.legendary_dragon_seeker.tooltip": "Показує найближчого дикого живого дракона в радіусі 300 блоків",
    "item.iceandfire.netherite_hippogryph_armor": "Незеритові обладунки для гіпогрифа", "item.iceandfire.pixie_dust_milky_tea": "Чай з молоком і пилком піксі",
    "item.iceandfire.raw_silver": "Необроблене срібло", "item.iceandfire.tooltip.require.delight": "Для крафту потрібен Farmer's Delight",
    "itemGroup.iceandfire.armors": "Ice and Fire | Броня", "itemGroup.iceandfire.tools_weapons": "Ice and Fire | Інструменти й зброя",
    "jei.iceandfire.dragon_forge_fire": "Вогняна драконяча кузня", "jei.iceandfire.dragon_forge_ice": "Крижана драконяча кузня",
    "jei.iceandfire.dragon_forge_lightning": "Громова драконяча кузня",
    "resourcePack.iceandfire.legacy.description": "Текстури до IAF-CE 2.0", "resourcePack.iceandfire.legacy.name": "Старі текстури Ice and Fire",
    "screen.iceandfire.client.title": "Ice and Fire: налаштування клієнта", "screen.iceandfire.common.title": "Ice and Fire: спільні налаштування",
    "text.iceandfire.not_enable": "Спершу ввімкніть цю можливість у налаштуваннях!", "attribute.name.generic.dragon_forge_speed": "Швидкість драконячої кузні",
}
for k in SRC:
    if k in out:
        continue
    if k in SPECIFIC:
        out[k] = SPECIFIC[k]; continue
    m = re.match(r"config\.iceandfire\.category\.([A-Za-z]+)$", k)
    if m and m.group(1) in MOB:
        out[k] = MOB[m.group(1)][0].upper() + MOB[m.group(1)][1:]; continue
    last = k.rsplit(".", 1)[-1]
    if k.startswith("config.iceandfire.") and last in GENERIC:
        out[k] = GENERIC[last]
missing = [k for k in SRC if k not in out]
json.dump({"iceandfire": out}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(out), "translated;", len(missing), "missing:", missing)
