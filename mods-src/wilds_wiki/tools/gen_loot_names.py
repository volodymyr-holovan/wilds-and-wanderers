"""Ukrainian names of the loot tables listed in generated/loot_tables.txt, written as lang keys wilds_wiki.loot.<ns>.<path>.

  python gen_loot_names.py

A name is "<structure>" or "<structure>: <room/chest>"; tables that mods inject into vanilla chests take the vanilla name.
Writes src/main/resources/assets/wilds_wiki/lang/uk_ua.loot.json, merged into uk_ua.json by build.py.
Unknown tables are printed: add them to STRUCT / ROLE below.
"""
import json, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki")

VANILLA = {
    "archaeology/desert_pyramid": "Пустельна піраміда (підозрілий пісок)", "archaeology/desert_well": "Пустельний колодязь (підозрілий пісок)",
    "archaeology/ocean_ruin_cold": "Холодні океанські руїни (підозрілий гравій)", "archaeology/ocean_ruin_warm": "Теплі океанські руїни (підозрілий пісок)",
    "archaeology/trail_ruins_common": "Руїни стежок (підозрілий гравій)", "archaeology/trail_ruins_rare": "Руїни стежок, рідкісна знахідка",
    "chests/abandoned_mineshaft": "Покинута шахта", "chests/ancient_city": "Стародавнє місто", "chests/ancient_city_ice_box": "Стародавнє місто: льодовня",
    "chests/bastion_bridge": "Залишки бастіону: міст", "chests/bastion_hoglin_stable": "Залишки бастіону: стайня гоглінів",
    "chests/bastion_other": "Залишки бастіону", "chests/bastion_treasure": "Залишки бастіону: скарбниця", "chests/buried_treasure": "Закопаний скарб",
    "chests/desert_pyramid": "Пустельна піраміда", "chests/end_city_treasure": "Місто Енду", "chests/igloo_chest": "Іглу",
    "chests/illager_mansion/ancient_city_raid_chest": "Лісовий маєток", "chests/illager_mansion/evoker_chest": "Лісовий маєток: кімната заклинача",
    "chests/illager_mansion/generic": "Лісовий маєток", "chests/illager_mansion/kitchen": "Лісовий маєток: кухня",
    "chests/illager_mansion/library_chest": "Лісовий маєток: бібліотека", "chests/illager_mansion/map_chest": "Лісовий маєток: картографічна",
    "chests/illager_mansion/pillager_chest": "Лісовий маєток: кімната розбійників", "chests/illager_mansion/ravager_chest": "Лісовий маєток: стійло спустошувача",
    "chests/illager_mansion/secret_room": "Лісовий маєток: таємна кімната", "chests/illager_mansion/smithing_room": "Лісовий маєток: кузня",
    "chests/illager_mansion/vindicator_chest": "Лісовий маєток: кімната поборника", "chests/illager_mansion/witch_chest": "Лісовий маєток: кімната відьми",
    "chests/illager_mansion/wool": "Лісовий маєток: склад вовни", "chests/jungle_temple": "Храм у джунглях",
    "chests/jungle_temple_dispenser": "Храм у джунглях: пастка", "chests/nether_bridge": "Фортеця Незеру",
    "chests/nether_fortress/fort_inside": "Фортеця Незеру", "chests/nether_fortress/fort_inside_generic": "Фортеця Незеру",
    "chests/pillager_outpost": "Аванпост розбійників", "chests/ruined_portal": "Зруйнований портал", "chests/shipwreck_map": "Корабельна аварія: карта",
    "chests/shipwreck_supply": "Корабельна аварія: припаси", "chests/shipwreck_treasure": "Корабельна аварія: скарб",
    "chests/simple_dungeon": "Підземелля", "chests/spawn_bonus_chest": "Бонусна скриня на початку гри", "chests/stronghold_corridor": "Твердиня: коридор",
    "chests/stronghold_crossing": "Твердиня: перехрестя", "chests/stronghold_library": "Твердиня: бібліотека",
    "chests/trial_chambers/corridor": "Камери випробувань: коридор", "chests/trial_chambers/entrance": "Камери випробувань: вхід",
    "chests/trial_chambers/intersection": "Камери випробувань: перехрестя", "chests/trial_chambers/intersection_barrel": "Камери випробувань: бочка",
    "chests/trial_chambers/reward": "Камери випробувань: сховище", "chests/trial_chambers/reward_common": "Камери випробувань: сховище",
    "chests/trial_chambers/reward_ominous": "Камери випробувань: зловісне сховище", "chests/trial_chambers/reward_ominous_common": "Камери випробувань: зловісне сховище",
    "chests/trial_chambers/reward_ominous_rare": "Камери випробувань: зловісне сховище, рідкісне",
    "chests/trial_chambers/reward_ominous_unique": "Камери випробувань: зловісне сховище, особливе", "chests/trial_chambers/reward_rare": "Камери випробувань: сховище, рідкісне",
    "chests/trial_chambers/reward_unique": "Камери випробувань: сховище, особливе", "chests/trial_chambers/supply": "Камери випробувань: припаси",
    "chests/underwater_ruin_big": "Великі океанські руїни", "chests/underwater_ruin_small": "Малі океанські руїни",
    "chests/village/village_armorer": "Село: бронник", "chests/village/village_butcher": "Село: м'ясник", "chests/village/village_cartographer": "Село: картограф",
    "chests/village/village_desert_house": "Пустельне село: будинок", "chests/village/village_fisher": "Село: рибалка", "chests/village/village_fletcher": "Село: лучник",
    "chests/village/village_jungle_house": "Село в джунглях: будинок", "chests/village/village_mason": "Село: муляр", "chests/village/village_plains_house": "Село на рівнині: будинок",
    "chests/village/village_savanna_house": "Село в савані: будинок", "chests/village/village_shepherd": "Село: вівчар", "chests/village/village_snowy_house": "Засніжене село: будинок",
    "chests/village/village_swamp_house": "Село на болоті: будинок", "chests/village/village_taiga_house": "Село в тайзі: будинок", "chests/village/village_tannery": "Село: шкіряник",
    "chests/village/village_temple": "Село: храм", "chests/village/village_toolsmith": "Село: зброяр інструментів", "chests/village/village_weaponsmith": "Село: зброяр",
    "chests/woodland_mansion": "Лісовий маєток", "gameplay/cat_morning_gift": "Ранковий подарунок кота", "gameplay/fishing": "Риболовля",
    "gameplay/fishing/fish": "Риболовля: риба", "gameplay/fishing/junk": "Риболовля: мотлох", "gameplay/fishing/treasure": "Риболовля: скарб",
    "gameplay/panda_sneeze": "Чхання панди", "gameplay/piglin_bartering": "Обмін з піґлінами", "gameplay/sniffer_digging": "Нюхач викопує",
    "shearing/bogged": "Стрижка болотяника",
}
GIFT = {"armorer": "бронника", "butcher": "м'ясника", "cartographer": "картографа", "cleric": "священника", "farmer": "фермера",
        "fisherman": "рибалки", "fletcher": "лучника", "leatherworker": "шкіряника", "librarian": "бібліотекаря", "mason": "муляра",
        "shepherd": "вівчаря", "toolsmith": "майстра інструментів", "weaponsmith": "зброяра", "beekeeper": "бджоляра", "fluix_researcher": "дослідника флюїксу"}

# structure keys (a path segment) of the mods
STRUCT = {
    # Aether
    "bronze_dungeon": "Бронзове підземелля (Етер)", "silver_dungeon": "Срібне підземелля (Етер)", "gold_dungeon": "Золоте підземелля (Етер)",
    # Alex's Caves
    "abyssal_ruins": "Руїни Безодні", "caveman_house": "Хатина печерної людини", "forlorn_ruins": "Руїни Покинутих западин",
    "gingerbread_town": "Пряникове містечко", "licowitch_tower": "Вежа лакричної відьми", "magnetic_ruins": "Руїни Магнетичних печер",
    "toxic_ruins": "Руїни Токсичних печер", "underground_cabin": "Підземна хатина", "witch_hut": "Хатина відьми",
    # YUNG's
    "skeleton_dungeon": "Підземелля скелетів", "small_dungeon": "Мале підземелля", "small_nether_dungeon": "Мале підземелля Незеру",
    "spider_dungeon": "Павуче підземелля", "zombie_dungeon": "Підземелля зомбі",
    # Cataclysm
    "cursed_pyramid": "Проклята піраміда", "abandoned": "Покинута кузня", "acropolis": "Затонулий акрополь", "amethyst_nest": "Аметистове гніздо",
    "desert": "Пустельна гробниця", "frosted_prison": "Крижана в'язниця",
    # Friends & Foes
    "iceologer_cabin": "Хатина крижаника", "illusioner_shack": "Халупа ілюзіоніста", "citadel_explorer_map": "Скриня з картою до цитаделі",
    # Ice and Fire
    "cyclops_cave": "Печера циклопа", "fire_dragon_female_cave": "Печера вогняної драконки", "fire_dragon_male_cave": "Печера вогняного дракона",
    "fire_dragon_roost": "Гніздо вогняного дракона", "graveyard": "Цвинтар", "hydra_cave": "Печера гідри",
    "ice_dragon_female_cave": "Печера крижаної драконки", "ice_dragon_male_cave": "Печера крижаного дракона", "ice_dragon_roost": "Гніздо крижаного дракона",
    "lightning_dragon_female_cave": "Печера громової драконки", "lightning_dragon_male_cave": "Печера громового дракона",
    "lightning_dragon_roost": "Гніздо громового дракона", "mausoleum_chest": "Мавзолей", "village_scribe": "Село: писар",
    # Iron's Spells
    "battleground": "Поле битви", "catacombs": "Катакомби", "citadel": "Цитадель", "evoker_fort": "Форт заклиначів", "ice_spider_den": "Лігво крижаних павуків",
    "impaled_icebreaker": "Пробитий криголам", "mangrove_hut": "Хатина в мангрових заростях", "mountain_tower": "Гірська вежа", "priest_house": "Будинок священника",
    "pyromancer_tower": "Вежа піромага", "sewer_loot": "Каналізація", "component_storage": "Склад магічних компонентів",
    "filler_storage_loot": "Склад", "generic_magic_treasure": "Магічний скарб", "bookshelf_loot": "Книжкова полиця", "magic_bookshelf_loot": "Магічна книжкова полиця",
    "wheat": "Склад зерна",
    # Mowzie's
    "monastery_chest": "Монастир", "umvuthana_grove_chest": "Гай умвутан",
    # Nova Structures
    "badland_miner_outpost": "Аванпост шахтарів у пустошах", "bunker_altar": "Бункер: вівтар", "conduit_ruin": "Руїни провідника",
    "creeping_crypt": "Повзучий склеп", "desert_ruins": "Пустельні руїни", "end_castle": "Замок Енду", "end_lighthouse": "Маяк Енду",
    "firewatch_tower": "Пожежна вежа", "food_supply": "Склад їжі", "hamlet": "Хутір", "illager_camp": "Табір ілагерів", "illager_hideout": "Схованка ілагерів",
    "jungle_ruins": "Руїни в джунглях", "lone_citadel": "Самотня цитадель", "mangrove_witchhud": "Хатина відьми в мангрових заростях",
    "mining_supply": "Шахтарські припаси", "nether_keep": "Замок Незеру", "nether_port": "Порт Незеру", "nether_skeleton_tower": "Вежа скелетів Незеру",
    "piglin_camp": "Табір піґлінів", "piglin_donjon": "Донжон піґлінів", "piglin_outstation": "Застава піґлінів", "pillager_outpost": "Аванпост розбійників",
    "ruin_loot_master": "Руїни", "shrine": "Святилище", "stray_fort": "Форт примар", "tavern_quest": "Таверна: доручення", "toxic_lair": "Токсичне лігво",
    "trident_trial_monument": "Монумент тризубця", "undead_crypts_grave": "Склеп нежиті: могила", "village_birch_house": "Березове село: будинок",
    "water_supply": "Запаси води", "witch_villa": "Вілла відьми",
    # misc
    "elytra": "Місто Енду: елітри", "underground": "Підземна скриня", "unstable_spawner": "Нестабільний породжувач",
    # Twilight Forest
    "basement": "Хатина в Сутінковому лісі: підвал", "casket_loot": "Скринька", "hut_junk": "Хатина в Сутінковому лісі", "just_bones": "Кістки",
    "suspicious_stew": "Підозріле рагу", "tower": "Вежа чарівника",
}
ROLE = {
    "disc": "платівка", "gummies": "желейки", "loot": "здобич", "neptune": "обладунок Нептуна", "reward": "нагорода", "trash": "мотлох",
    "treasure": "скарб", "tresure": "скарб", "cape_config": "плащ", "gravitite": "гравітит", "valkyrie": "обладунок валькірії", "secret": "таємна кімната",
    "common": "звичайна скриня", "middle": "середня зала", "loot_piles": "купи здобичі", "egg_room": "кімната з яйцями", "special": "особлива скриня",
    "tombstone": "надгробок", "necklace": "намисто", "armory_loot": "зброярня", "coffin_loot": "труна", "dead_king_vault": "скарбниця Мертвого короля",
    "hidden_trough_treasure": "схований скарб", "pot": "горщик", "wall_loot": "ніша в стіні", "citadel_bookshelf": "книжкова полиця",
    "citadel_tomes": "томи", "citadel_vault": "скарбниця", "rampart_chest": "вал", "rampart_supplies": "припаси на валу", "spawner_reward": "нагорода за породжувач",
    "guard_tower": "вартова вежа", "cask": "барило", "dungeon": "темниця", "tower": "вежа", "captain_quarters": "каюта капітана", "food_barrel": "бочка з їжею",
    "hidden_potion_storage": "схованка зілль", "potion_ingredient_storage": "склад інгредієнтів", "ice_barrel": "бочка з льодом", "burnt_chest": "обгоріла скриня",
    "fire_ale_cask": "барило вогняного елю", "fire_ale_trove": "запас вогняного елю", "old_cask": "старе барило",
    "pyromancer_basic_storage": "комора", "pyromancer_supplies": "припаси", "burial_loot": "поховання", "forge": "кузня", "towers": "вежі",
    "conduit_ruin_big": "велика", "conduit_ruin_main": "головна", "conduit_ruin_small": "мала", "crypt_grave": "могила", "crypt_hallway": "коридор",
    "vault_creeping": "сховище", "desert_ruin_grave": "могила", "desert_ruin_house": "будинок", "desert_ruin_lesser_treasure": "менший скарб",
    "desert_ruin_main_temple": "головний храм", "castle_bar": "бар", "castle_brewing": "зіллеварня", "greater_loot": "велика здобич",
    "lesser_loot": "мала здобич", "rocket_barrel": "бочка з ракетами", "supply": "припаси", "treasure_lighthouse": "скарб маяка",
    "vault_brigattine": "сховище бригантини", "vault_galleon": "сховище галеона", "vault_slope": "сховище на схилі", "supply_lighthouse": "припаси маяка",
    "vault_lighthouse": "сховище маяка", "hamlet": "будинок", "hamlet_tresure": "скарб", "brewing": "зіллеварня", "fishing": "рибальська",
    "heart_loot": "серце схованки", "lesser_tresure": "менший скарб", "library": "бібліотека", "meat": "м'ясна комора", "random": "різне",
    "raw_ores": "руда", "utility": "господарська", "vegitarian": "овочева комора", "weaponry": "зброярня", "jungle_ruins_house": "будинок",
    "jungle_ruins_main_temple": "головний храм", "jungle_ruins_main_temple_wild": "здичавілий храм", "c_banner": "прапори", "c_cold_barrel": "холодна бочка",
    "c_dispenser": "роздавач", "c_forge": "кузня", "c_forge_chest": "скриня кузні", "c_grave": "могила", "c_library": "бібліотека", "c_supply": "припаси",
    "c_vault": "сховище", "c_vault_boss": "сховище боса", "vault_keep": "сховище", "vault_keep_generic": "сховище", "nether_port_chest": "скриня",
    "nether_port_supplies": "припаси", "skeleton_tower_camp": "табір", "skeleton_tower_chest": "скриня", "skeleton_tower_supply": "припаси",
    "chest": "скриня", "donjon_generic": "загальна скриня", "vault_piglin_donjon": "сховище", "outstation_generic": "загальна скриня",
    "outstation_tower": "вежа", "outstation_treasure": "скарб", "vault_piglin_outstation": "сховище", "kitchen": "кухня", "combat_pot": "горщик",
    "combat_treasure_1": "бойовий скарб", "combat_treasure_2": "бойовий скарб", "combat_treasure_3": "бойовий скарб", "combat_treasure_4": "бойовий скарб",
    "combat_treasure_5": "бойовий скарб", "shrine_lesser_ominous": "зловісна скриня", "shrine_lesser_treasure": "менший скарб",
    "vault_shrine_ominous": "зловісне сховище", "archer": "лучники", "loot_generic": "загальна скриня", "generic": "загальна скриня",
    "toxic_boss_vault": "сховище боса", "toxic_ominous_vault": "зловісне сховище", "toxic_vault": "сховище", "ttm_boss_vault_heart": "сховище боса: серце",
    "ttm_boss_vault_trident": "сховище боса: тризубець", "ttm_common_vault": "сховище", "bedroom": "спальня", "dirty_clothes": "брудний одяг",
    "gardening": "садівництво", "general_storage": "комора", "lab": "лабораторія", "music_room": "музична кімната", "potion_brewing": "зіллеварня",
    "pumpkin_room": "гарбузова кімната", "slime_room": "кімната слизу", "armoury": "зброярня", "cmd_yung": "командирська", "crypt": "склеп",
    "grand_library": "велика бібліотека", "library_md": "бібліотека", "mess": "їдальня", "prison_lg": "в'язниця", "trap": "пастка",
    "abyssal_chasm": "Безодня", "candy_cavity": "Цукеркова порожнина", "forlorn_hollows": "Покинуті западини", "magnetic_caves": "Магнетичні печери",
    "primordial_caves": "Первісні печери", "toxic_caves": "Токсичні печери", "secret_room": "таємна кімната", "basic": "комора",
    "enchanting": "зачарування", "foyer": "передпокій", "grave_lower": "нижня гробниця", "grave_upper": "верхня гробниця", "jars": "банки",
    "room": "кімната", "abandoned_treasure": "скарб", "acropolis_treasure": "скарб", "amethyst_nest_chest": "скриня", "desert_treasure": "скарб",
    "frosted_prison_treasure": "скарб", "burial": "поховання", "piglin_camp": "табір піґлінів", "basement": "підвал", "potion": "зілля",
    "cmd_yung": "кімната командира", "treasure": "скарбниця",
}
MOD_TABLES = {
    "ae2:gameplay/hero_of_the_village/fluix_researcher_gifts": "Подарунок дослідника флюїксу (герой села)",
    "alexscaves:gameplay/deep_one_barter": "Обмін з глибинником", "alexscaves:gameplay/deep_one_knight_barter": "Обмін з лицарем-глибинником",
    "alexscaves:gameplay/deep_one_mage_barter": "Обмін з магом-глибинником", "alexscaves:gameplay/sea_pig_digestion": "Морська свиня перетравлює",
    "alexsmobs:gameplay/anteater_reward": "Нагорода мурахоїда", "alexsmobs:gameplay/platypus_reward": "Качкодзьоб знаходить",
    "alexsmobs:gameplay/platypus_supercharged_reward": "Качкодзьоб знаходить (заряджений)", "alexsmobs:gameplay/pupfish_reward": "Нагорода карликової рибки",
    "alexsmobs:gameplay/seal_reward": "Подарунок тюленя", "alexsmobs:gameplay/sugar_glider_reward": "Подарунок цукрової летяги",
    "alexsmobs:gameplay/trader_elephant_chest": "Слон торговця", "alexsmobs:gameplay/transmutation_table_common": "Стіл трансмутації",
    "alexsmobs:gameplay/transmutation_table_rare": "Стіл трансмутації, рідкісне", "alexsmobs:gameplay/transmutation_table_uncommon": "Стіл трансмутації, незвичайне",
    "aether:chests/ruined_portal": "Зруйнований портал (Етер)", "lootr:chests/elytra": "Місто Енду: елітри",
    "iceandfire:chest/village_scribe": "Село: писар", "irons_spellbooks:chests/evoker_fort": "Форт заклиначів",
    "irons_spellbooks:chests/mangrove_hut": "Хатина в мангрових заростях", "irons_spellbooks:chests/priest_house": "Будинок священника",
    "nova_structures:chests/illager_camp": "Табір ілагерів", "nova_structures:chests/firewatch_tower": "Пожежна вежа",
}
IRONS_EXTRA = {"ancient_city": "Стародавнє місто", "end_city": "Місто Енду", "generic": "Звичайні скрині", "good": "Цінні скрині",
               "library": "Бібліотеки", "nether": "Скрині Незеру", "treasure": "Скарбниці"}


# --- structures and tables of the mods added in the October 2026 update
STRUCT.update({
    # When Dungeons Arise
    "abandoned_temple": "Покинутий храм", "aviary": "Пташник", "bandit_towers": "Бандитські вежі", "bandit_village": "Бандитське селище",
    "bathhouse": "Лазня", "ceryneian_hind": "Керинейська лань", "fishing_hut": "Рибальська хатина", "foundry": "Ливарня",
    "greenwood_pub": "Корчма «Зелений гай»", "heavenly_challenger": "Небесний суперник", "heavenly_conqueror": "Небесний завойовник",
    "heavenly_rider": "Небесний вершник", "illager_campsite": "Табір ілладжерів", "illager_corsair": "Корсар ілладжерів",
    "illager_fort": "Форт ілладжерів", "illager_galley": "Галера ілладжерів", "illager_windmill": "Вітряк ілладжерів",
    "infested_temple": "Заражений храм", "jungle_tree_house": "Будинок на дереві", "keep_kayra": "Твердиня Кайра",
    "kisegi_sanctuary": "Святилище Кісеґі", "lighthouse": "Маяк", "mechanical_nest": "Механічне гніздо", "merchant_campsite": "Табір торговців",
    "mines": "Шахти", "mining_complex": "Гірничий комплекс", "mining_system": "Гірнича система", "monastery": "Монастир",
    "mushroom_house": "Грибний будинок", "mushroom_mines": "Грибні шахти", "mushroom_village": "Грибне село", "plague_asylum": "Чумний притулок",
    "scorched_mines": "Випалені шахти", "shiraz_palace": "Палац Шираз", "small_blimp": "Малий дирижабль", "thornborn_towers": "Терносхідні вежі",
    "typhon": "Тифон", "undead_pirate_ship": "Піратський корабель нежиті",
    # Seven Seas ships
    "corsair_corvette": "Корвет корсарів", "pirate_junk": "Піратська джонка", "small_yacht": "Мала яхта", "unicorn_galleon": "Галеон «Єдиноріг»",
    "victory_frigate": "Фрегат «Перемога»",
    # villages and camps
    "camp": "Табір мешканців (Questborne)", "seapost": "Морська пошта", "supplycamp": "Табір постачання", "supplyship": "Корабель постачання",
    "small_ruin": "Малі підводні руїни", "small_ruins": "Малі підводні руїни", "warm_small_ruins": "Теплі малі підводні руїни",
    "upper_side_chamber": "Океанський монумент: бічна зала",
})
ROLE.update({"barrels": "бочки", "normal": "скриня", "normal_lower": "нижня палуба", "normal_upper": "верхня палуба", "crafter": "майстер",
             "explorer": "дослідник", "gatherer": "збирач", "hunter": "мисливець", "merchant": "торговець", "combat": "зброя",
             "firework": "феєрверки", "big": "велика", "medium": "середня", "small": "мала", "cold": "холодні", "warm": "теплі",
             "barrel": "бочка", "barrels_hallways": "бочки в коридорах", "bridges": "мости", "cells": "камери", "chains": "ланцюги",
             "coal_storage": "вугільний склад", "elite": "добірна скриня", "enchants": "зачарування", "entrance": "вхід",
             "equipment": "спорядження", "gardens": "сади", "housing": "житло", "hub": "центральна зала", "lava_pit": "лавова яма",
             "map": "мапа", "ores": "руда", "passage_exterior": "зовнішній прохід", "potions": "зілля", "redstone_chamber": "редстоун-камера",
             "room_bookshelf": "кімната з полицями", "room_garden": "кімната-сад", "room_table": "кімната зі столом", "rooms": "кімнати",
             "storage": "склад", "tent": "намет", "tents": "намети", "theater": "театр", "tools": "знаряддя", "top": "вершина",
             "top_rooms": "верхні кімнати", "vault_ominous": "зловісне сховище"})
MOD_TABLES.update({
    "chefsdelight:chests/cooker": "Село: будинок кухаря",
    "goblins_tyranny:gameplay/mushroom_hat_loot": "Грибний капелюх гобліна",
    "kobolds:archaeology/gravel": "Лігвище кобольдів (підозрілий гравій)", "kobolds:chests/iron_loot": "Лігвище кобольдів: кузня",
    "kobolds:chests/kobold_loot": "Лігвище кобольдів", "kobolds:gameplay/enchanter_loot": "Обмін з кобольдом-чарівником",
    "kobolds:gameplay/engineer_loot": "Обмін з кобольдом-інженером", "kobolds:gameplay/stone_loot": "Обмін з кобольдом за камінь",
    "kobolds:gameplay/trader_loot": "Обмін з кобольдом-торговцем",
    "naturalist:gameplay/fishing/fish": "Риболовля: риба (Naturalist)",
    "hopo:chests/copper": "Підводні руїни: мідна скриня", "hopo:chests/coral": "Підводні руїни: коралова скриня",
    "hopo:chests/food": "Підводні руїни: їжа", "hopo:chests/materials": "Підводні руїни: матеріали", "hopo:chests/prismarine": "Підводні руїни: призмарин",
    "hybrid_aquatic:gameplay/beach_archaeology": "Пляж (підозрілий пісок)", "hybrid_aquatic:gameplay/placer_river_archaeology": "Річка (підозрілий гравій)",
    "hybrid_aquatic:gameplay/clawed_lobster": "Омар з клешнями", "hybrid_aquatic:gameplay/clawless_lobster": "Омар без клешнів",
    "hybrid_aquatic:gameplay/crab_pot_treasure": "Пастка для крабів", "hybrid_aquatic:gameplay/message_in_a_bottle": "Лист у пляшці",
    "hybrid_aquatic:gameplay/vent_drops": "Гідротермальне джерело", "hybrid_aquatic:gameplay/fishing/ha_crates": "Риболовля: ящики",
    "hybrid_aquatic:gameplay/fishing/ha_large_fish": "Риболовля: велика риба", "hybrid_aquatic:gameplay/fishing/ha_medium_fish": "Риболовля: середня риба",
    "hybrid_aquatic:gameplay/fishing/ha_small_fish": "Риболовля: дрібна риба", "hybrid_aquatic:gameplay/hybrid_crate_treasure": "Морський ящик",
})
for _w, _n in {"acacia": "акацієвий", "bamboo": "бамбуковий", "birch": "березовий", "cherry": "вишневий", "dark_oak": "темнодубовий",
               "jungle": "тропічний", "mangrove": "мангровий", "oak": "дубовий", "spruce": "ялиновий"}.items():
    MOD_TABLES[f"hybrid_aquatic:gameplay/{_w}_crate_treasure"] = f"Морський ящик ({_n})"
for _c, _n in {"brain": "мозковий", "bubble": "пухирчастий", "fire": "вогняний", "horn": "роговий", "lophelia": "лофелія",
               "thorn": "терновий", "tube": "трубчастий"}.items():
    MOD_TABLES[f"hybrid_aquatic:gameplay/decorator_{_c}"] = f"Краб-декоратор ({_n} корал)"
for _h, _n in {"butcherhouse": "м'ясник", "farmer_house": "фермер", "fishermanhouse": "рибалка", "fletcherhouse": "лучник", "forge": "кузня",
               "library": "бібліотека", "shepherd": "вівчар", "stable": "стайня", "temple": "храм", "toolsmith": "майстер інструментів"}.items():
    MOD_TABLES[f"ocean_lily_pad_village:chests/{_h}_loottable"] = f"Село на лататті: {_n}"
for _p, _n in {"alchemist": "алхімік", "horticulturist": "садівник", "miner": "шахтар", "occultist": "окультист", "oceanographer": "океанограф"}.items():
    MOD_TABLES[f"villagersplus:chests/village/village_{_p}"] = f"Село: {_n}"
for _b, _n in {"badlands": "бедлендів", "beach": "узбережжя", "dark_forest": "темного лісу", "jungle": "джунглів", "mountain": "гір",
               "mushroom": "грибних полів", "swamp": "боліт"}.items():
    MOD_TABLES[f"wares:chests/village/{_b}_warehouse"] = f"Склад села {_n}"
for _h, _n in {"badlands_house": "будинок у бедлендах", "bakery": "пекарня", "beach_house": "будинок на узбережжі", "dark_forest_house": "будинок у темному лісі",
               "farm": "ферма", "jungle_house": "будинок у джунглях", "library": "бібліотека", "mountain_house": "будинок у горах",
               "mushroom_house": "грибний будинок", "smith": "кузня", "swamp_house": "будинок на болоті"}.items():
    MOD_TABLES[f"ctov:chests/village/village_{_h}"] = f"Село: {_n}"
for _s in ("combat", "firework", "supply"):
    MOD_TABLES[f"fishofthieves:chests/seapost_barrel_{_s}"] = "Морська пошта: " + {"combat": "зброя", "firework": "феєрверки", "supply": "припаси"}[_s]

def vanilla_name(path):
    if path in VANILLA:
        return VANILLA[path]
    m = re.match(r"gameplay/hero_of_the_village/(\w+?)_gifts?$", path)
    if m and m[1] in GIFT:
        return f"Подарунок {GIFT[m[1]]} (герой села)"
    return None


def name(tid):
    ns, path = tid.split(":", 1)
    if tid in MOD_TABLES:
        return MOD_TABLES[tid]
    if ns == "minecraft":
        return vanilla_name(path)
    m = re.match(r"inject/(.+)$", path)  # Sophisticated Backpacks adds backpacks to vanilla chests
    if m and vanilla_name(m[1]):
        return vanilla_name(m[1])
    m = re.match(r"chests/fd_(.+)$", path)  # Farmer's Delight adds food to vanilla chests
    if m:
        v = vanilla_name("chests/" + m[1]) or vanilla_name("chests/village/" + m[1])
        if v:
            return v
    m = re.match(r"gameplay/hero_of_the_village/(\w+?)_gifts?$", path)
    if m and m[1] in GIFT:
        return f"Подарунок {GIFT[m[1]]} (герой села)"
    m = re.match(r"chests/additional_(\w+)_loot$", path)
    if m and m[1] in IRONS_EXTRA:
        return IRONS_EXTRA[m[1]]
    if ns == "betterstrongholds":  # YUNG's Better Strongholds: rooms of the stronghold
        role = ROLE.get(path.split("/")[-1])
        return f"Твердиня: {role}" if role else "Твердиня"
    if re.match(r"chests/dungeon_\d+$", path):
        return "Підземелля"
    m = re.match(r"chests/trial_chambers/(.+)$", path)
    if m and ns == "irons_spellbooks":
        return "Камери випробувань"
    parts = path.split("/")
    segs = [p for p in parts if p not in ("chests", "chest", "archaeology", "gameplay", "dungeon")]
    # the structure: the first known segment, or a known prefix of the file name
    struct, rest = None, []
    for i, s in enumerate(segs):
        if s in STRUCT:
            struct, rest = STRUCT[s], segs[i + 1:]
            break
        for k in sorted(STRUCT, key=len, reverse=True):
            if s.startswith(k + "_"):
                struct, rest = STRUCT[k], [s[len(k) + 1:]] + segs[i + 1:]
                break
        if struct:
            break
    if not struct:
        return None
    role = None
    if rest:
        last = rest[-1]
        role = ROLE.get(last)
        if role is None:
            for k in sorted(ROLE, key=len, reverse=True):
                if last.endswith(k):
                    role = ROLE[k]
                    break
        if role is None:
            return None
    if parts[0] == "archaeology":
        struct += " (археологія)"
    return f"{struct}: {role}" if role else struct


def main():
    tables = open(os.path.join(RES, "generated", "loot_tables.txt"), encoding="utf-8").read().split()
    out, missing = {}, []
    for t in tables:
        n = name(t)
        if n:
            out["wilds_wiki.loot." + t.replace(":", ".", 1).replace("/", ".")] = n
        else:
            missing.append(t)
    json.dump(out, open(os.path.join(RES, "lang", "uk_ua.loot.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"{len(out)} named, {len(missing)} missing")
    for m in missing:
        print("  ", m)


main()
