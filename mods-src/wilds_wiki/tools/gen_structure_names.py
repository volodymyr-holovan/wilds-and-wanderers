"""Names of every structure of the pack for the wiki: lang/uk_ua.structures.json and lang/en_us.structures.json
(build.py merges them into the lang files), keys wilds_wiki.structure.<ns>.<path with dots>.

Unique structures have hand-written names; the many variants (villages, outposts, taverns, wells of every biome and wood)
are built from a noun and a qualifier. Prints the ids it could only spell out word by word, so they get a real name.

  python gen_structure_names.py
"""
import json, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
LANG = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "lang")
IDS = open(os.path.join(ROOT, "docs", "registries", "worldgen_structure.txt"), encoding="utf-8").read().split()

NAMES = {
    # vanilla
    "minecraft:ancient_city": "Стародавнє місто", "minecraft:bastion_remnant": "Залишки бастіону", "minecraft:buried_treasure": "Закопаний скарб",
    "minecraft:desert_pyramid": "Пустельна піраміда", "minecraft:end_city": "Місто Енду", "minecraft:fortress": "Незерська фортеця",
    "minecraft:igloo": "Іглу", "minecraft:jungle_pyramid": "Храм у джунглях", "minecraft:mansion": "Лісовий особняк",
    "minecraft:mineshaft": "Покинута шахта", "minecraft:mineshaft_mesa": "Покинута шахта (бедленд)", "minecraft:monument": "Океанська пам'ятка",
    "minecraft:nether_fossil": "Незерська скам'янілість", "minecraft:ocean_ruin_cold": "Океанські руїни (холодні)",
    "minecraft:ocean_ruin_warm": "Океанські руїни (теплі)", "minecraft:pillager_outpost": "Застава розбійників",
    "minecraft:ruined_portal": "Зруйнований портал", "minecraft:ruined_portal_desert": "Зруйнований портал (пустеля)",
    "minecraft:ruined_portal_jungle": "Зруйнований портал (джунглі)", "minecraft:ruined_portal_mountain": "Зруйнований портал (гори)",
    "minecraft:ruined_portal_nether": "Зруйнований портал (Незер)", "minecraft:ruined_portal_ocean": "Зруйнований портал (океан)",
    "minecraft:ruined_portal_swamp": "Зруйнований портал (болото)", "minecraft:shipwreck": "Уламки корабля",
    "minecraft:shipwreck_beached": "Уламки корабля на березі", "minecraft:stronghold": "Фортеця Енду", "minecraft:swamp_hut": "Хатинка відьми",
    "minecraft:trail_ruins": "Руїни стежок", "minecraft:trial_chambers": "Випробувальні зали", "minecraft:village_desert": "Село (пустеля)",
    "minecraft:village_plains": "Село (рівнини)", "minecraft:village_savanna": "Село (савана)", "minecraft:village_snowy": "Село (засніжене)",
    "minecraft:village_taiga": "Село (тайга)",
    # mods with a handful of structures
    # the Nether and the End update (28.10)
    "incendium:abandoned_tower": "Покинута вежа", "incendium:forbidden_castle": "Заборонений замок",
    "incendium:infernal_altar": "Пекельний вівтар", "incendium:nether_reactor": "Ядерний реактор Незеру",
    "incendium:piglin_village": "Село піглінів", "incendium:pipeline": "Трубопровід", "incendium:quartz_kitchen": "Кварцова кухня",
    "incendium:ruined_lab": "Покинута лабораторія", "incendium:sanctum": "Санктум",
    "eternalnether:catacomb": "Катакомби візера", "eternalnether:citadel": "Химерна цитадель", "eternalnether:piglin_manor": "Маєток піглінів",
    "enderscape:end_city": "Місто Енду (Enderscape)", "enderscape:mirestone_ruins": "Трясовинні руїни",
    "enderscape:center_gateway": "Шлюз Енду", "enderscape:large_center_gateway": "Великий шлюз Енду",
    "betterfortresses:fortress": "Незерська фортеця", "netherdepthsupgrade:nether_fortress_piece": "Уламок незерської фортеці",
    "ae2:meteorite": "Метеорит", "aether:bronze_dungeon": "Бронзове підземелля", "aether:gold_dungeon": "Золоте підземелля",
    "aether:large_aercloud": "Велика аерохмара", "aether:silver_dungeon": "Срібне підземелля",
    "alexscaves:abyssal_ruins": "Безодні руїни", "alexscaves:acid_pit": "Кислотна яма", "alexscaves:cake_cave": "Тортова печера",
    "alexscaves:dino_bowl": "Діночаша", "alexscaves:donut_arch": "Пончикова арка", "alexscaves:ferrocave": "Феропечера",
    "alexscaves:forlorn_bridge": "Покинутий міст", "alexscaves:forlorn_canyon": "Покинутий каньйон", "alexscaves:gingerbread_town": "Пряникове містечко",
    "alexscaves:licowitch_tower": "Вежа лакричної відьми", "alexscaves:ocean_trench": "Океанська западина", "alexscaves:soda_bottle": "Пляшка газованки",
    "alexscaves:underground_cabin": "Підземна хатинка", "alexscaves:volcano": "Вулкан",
    "apotheosis:tower_leaf": "Вежа учня (листяна)", "apotheosis:tower_main": "Вежа учня", "apotheosis:tower_sand": "Вежа учня (піщана)",
    "apotheosis:tower_spruce": "Вежа учня (ялинова)",
    "ars_nouveau:guardian_wilden_den": "Лігво дикого вартового", "ars_nouveau:hunter_wilden_den": "Лігво дикого мисливця",
    "ars_nouveau:stalker_wilden_den": "Лігво дикого переслідувача",
    "betterdungeons:skeleton_dungeon": "Підземелля скелетів", "betterdungeons:small_dungeon": "Мале підземелля",
    "betterdungeons:small_nether_dungeon": "Мале незерське підземелля", "betterdungeons:spider_dungeon": "Павуче підземелля",
    "betterdungeons:zombie_dungeon": "Підземелля зомбі",
    "betteroceanmonuments:ocean_monument": "Океанська пам'ятка", "betterstrongholds:stronghold": "Фортеця Енду",
    "cataclysm:abandoned_spire": "Покинутий шпиль", "cataclysm:abandoned_temple": "Покинутий храм", "cataclysm:abandoned_village": "Покинуте село",
    "cataclysm:acropolis": "Акрополь", "cataclysm:amethyst_nest": "Аметистове гніздо", "cataclysm:ancient_factory": "Стародавня фабрика",
    "cataclysm:burning_arena": "Палаюча арена", "cataclysm:cursed_pyramid": "Проклята піраміда",
    "cataclysm:desert_occupied_village": "Захоплене пустельне село", "cataclysm:desert_site": "Пустельний табір", "cataclysm:desert_temple": "Пустельний храм",
    "cataclysm:frosted_prison": "Морозна в'язниця", "cataclysm:ruined_citadel": "Зруйнована цитадель", "cataclysm:soul_black_smith": "Кузня душ",
    "cataclysm:sunken_city": "Затонуле місто",
    "dungeons_arise:abandoned_temple": "Покинутий храм", "dungeons_arise:aviary": "Пташник", "dungeons_arise:bandit_towers": "Бандитські вежі",
    "dungeons_arise:bandit_village": "Бандитське селище", "dungeons_arise:bathhouse": "Лазня", "dungeons_arise:ceryneian_hind": "Керинейська лань",
    "dungeons_arise:coliseum": "Колізей", "dungeons_arise:fishing_hut": "Рибальська хижа", "dungeons_arise:foundry": "Ливарня",
    "dungeons_arise:giant_mushroom": "Велетенський гриб", "dungeons_arise:greenwood_pub": "Корчма «Зелений ліс»",
    "dungeons_arise:heavenly_challenger": "Небесний суперник", "dungeons_arise:heavenly_conqueror": "Небесний завойовник",
    "dungeons_arise:heavenly_rider": "Небесний вершник", "dungeons_arise:illager_campsite": "Табір іледжерів",
    "dungeons_arise:illager_corsair": "Корсар іледжерів", "dungeons_arise:illager_fort": "Форт іледжерів", "dungeons_arise:illager_galley": "Галера іледжерів",
    "dungeons_arise:illager_windmill": "Млин іледжерів", "dungeons_arise:infested_temple": "Заражений храм",
    "dungeons_arise:jungle_tree_house": "Будиночок на дереві в джунглях", "dungeons_arise:keep_kayra": "Твердиня Кайра",
    "dungeons_arise:kisegi_sanctuary": "Святилище Кісеґі", "dungeons_arise:lighthouse": "Маяк", "dungeons_arise:mechanical_nest": "Механічне гніздо",
    "dungeons_arise:merchant_campsite": "Табір торговців", "dungeons_arise:mining_complex": "Гірничий комплекс",
    "dungeons_arise:mining_system": "Шахтна система", "dungeons_arise:monastery": "Монастир", "dungeons_arise:mushroom_house": "Грибний будинок",
    "dungeons_arise:mushroom_mines": "Грибні копальні", "dungeons_arise:mushroom_village": "Грибне селище",
    "dungeons_arise:plague_asylum": "Чумний притулок", "dungeons_arise:scorched_mines": "Випалені копальні",
    "dungeons_arise:shiraz_palace": "Палац Шираз", "dungeons_arise:small_blimp": "Малий дирижабль",
    "dungeons_arise:thornborn_towers": "Вежі Терновиків", "dungeons_arise:typhon": "Тифон", "dungeons_arise:undead_pirate_ship": "Корабель піратів-мерців",
    "dungeons_arise:wishing_well": "Колодязь бажань",
    "dungeons_arise_seven_seas:corsair_corvette": "Корвет корсарів", "dungeons_arise_seven_seas:pirate_junk": "Піратська джонка",
    "dungeons_arise_seven_seas:small_yacht": "Мала яхта", "dungeons_arise_seven_seas:unicorn_galleon": "Галеон «Єдиноріг»",
    "dungeons_arise_seven_seas:victory_frigate": "Фрегат «Перемога»",
    "fishofthieves:guardian_fruit_tree": "Фруктове дерево-вартовий", "fishofthieves:seapost": "Морська застава",
    "friendsandfoes:citadel": "Цитадель ілюзіоніста", "friendsandfoes:iceologer_cabin": "Хатинка крижаного чаклуна",
    "friendsandfoes:illusioner_shack": "Халупа ілюзіоніста", "friendsandfoes:illusioner_training_grounds": "Полігон ілюзіоністів",
    "goblins_tyranny:goblins_caravan": "Караван гоблінів", "goblins_tyranny:goblins_spot": "Стоянка гоблінів", "goblins_tyranny:goblinscamp": "Табір гоблінів",
    "hopo:underwater/underwater_city": "Підводне місто", "hopo:underwater/underwater_fossils": "Підводні скам'янілості",
    "hopo:underwater/underwater_small_ruins": "Малі підводні руїни", "hopo:underwater/underwater_warm_small_ruins": "Малі підводні руїни (теплі)",
    "hybrid_aquatic:whale_fall": "Кістяк кита на дні",
    "iceandfire:cyclops_cave": "Печера циклопа", "iceandfire:fire_dragon_cave": "Печера вогняного дракона",
    "iceandfire:fire_dragon_roost": "Гніздо вогняного дракона", "iceandfire:gorgon_temple": "Храм горгони", "iceandfire:graveyard": "Цвинтар драконів",
    "iceandfire:hydra_cave": "Печера гідри", "iceandfire:ice_dragon_cave": "Печера крижаного дракона", "iceandfire:ice_dragon_roost": "Гніздо крижаного дракона",
    "iceandfire:lightning_dragon_cave": "Печера громового дракона", "iceandfire:lightning_dragon_roost": "Гніздо громового дракона",
    "iceandfire:mausoleum": "Мавзолей", "iceandfire:pixie_village": "Село піксі", "iceandfire:siren_island": "Острів сирен",
    "irons_spellbooks:ancient_battleground": "Стародавнє поле битви", "irons_spellbooks:catacombs": "Катакомби", "irons_spellbooks:citadel": "Цитадель",
    "irons_spellbooks:evoker_fort": "Форт заклиначів", "irons_spellbooks:ice_spider_den": "Лігво крижаного павука",
    "irons_spellbooks:impaled_icebreaker": "Наштрикнутий криголам", "irons_spellbooks:mangrove_hut": "Мангрова хатина",
    "irons_spellbooks:mountain_tower": "Гірська вежа", "irons_spellbooks:pyromancer_tower": "Вежа піроманта",
    "joshie:village_ocean": "Морське село", "kobolds:kobold_den": "Лігво кобольдів", "orc_hut:orc_hut": "Хижа орків",
    "minecolonies:amazon_camp": "Табір амазонок", "minecolonies:asian_colony": "Покинута колонія (азійська)",
    "minecolonies:barbarian_camp": "Табір варварів", "minecolonies:caledonia_colony": "Покинута колонія (каледонська)",
    "minecolonies:colonial_colony": "Покинута колонія (колоніальна)", "minecolonies:desert_camp": "Пустельний табір варварів",
    "minecolonies:desertoasis_colony": "Покинута колонія (оаза)", "minecolonies:incan_colony": "Покинута колонія (інкська)",
    "minecolonies:lostmesacity_colony": "Покинута колонія (місто в бедленді)", "minecolonies:medieval_birch_colony": "Покинута колонія (середньовічна, береза)",
    "minecolonies:medieval_dark_oak_colony": "Покинута колонія (середньовічна, темний дуб)", "minecolonies:medieval_oak_colony": "Покинута колонія (середньовічна, дуб)",
    "minecolonies:medieval_spruce_colony": "Покинута колонія (середньовічна, ялина)", "minecolonies:ship": "Піратський корабель",
    "minecolonies:shire_colony": "Покинута колонія (Шир)", "minecolonies:warpednetherlands_colony": "Покинута колонія (викривлений Незер)",
    "mos:atlantis_tribune": "Трибуна Атлантиди", "mos:buried_treasure": "Закопаний скарб", "mos:conduit_arch": "Арка провідника",
    "mos:dragon_skeleton": "Скелет дракона", "mos:hauling_ship_1": "Вантажне судно", "mos:long_ship": "Драккар",
    "mos:magma_block_ravine": "Магмова ущелина", "mos:nether_portal_archway": "Арка незерського порталу", "mos:ocean_decor": "Підводний декор",
    "mos:prismarine_dog_statue": "Призмаринова статуя пса", "mos:rare_travel_ship": "Рідкісне мандрівне судно", "mos:ruined_archway": "Зруйнована арка",
    "mos:ruined_castle": "Зруйнований замок", "mos:ruined_church": "Зруйнована церква", "mos:ruined_house": "Зруйнований будинок",
    "mos:ruins_atlantis": "Руїни Атлантиди", "mos:sea_dragon_skeleton": "Скелет морського дракона", "mos:ship_small": "Мале судно",
    "mos:ship_wreck_dark_oak": "Уламки корабля (темний дуб)", "mos:ship_wreck_oak": "Уламки корабля (дуб)", "mos:ship_wreck_spruce": "Уламки корабля (ялина)",
    "mos:small_ocean_temple": "Малий океанський храм", "mos:sunken_ship": "Затонулий корабель", "mos:underwater_garden": "Підводний сад",
    "mos:underwater_shrine": "Підводне святилище", "mos:wanderer_raft": "Пліт мандрівника",
    "mowziesmobs:frostmaw_spawn": "Лігво Морозної Пащі", "mowziesmobs:monastery": "Монастир Сонця", "mowziesmobs:umvuthana_grove": "Гай умвутан",
    "mowziesmobs:wrought_chamber": "Кована зала",
    "questborne:camp_crafter": "Табір майстрів", "questborne:camp_explorer": "Табір дослідників", "questborne:camp_gatherer": "Табір збирачів",
    "questborne:camp_hunter": "Табір мисливців", "questborne:camp_merchant": "Табір торговців",
    "terralith:desert_outpost": "Пустельна застава", "terralith:fortified_desert_village": "Укріплене пустельне село",
    "terralith:fortified_village": "Укріплене село", "terralith:glacial_hut": "Льодовикова хатина", "terralith:igloo": "Іглу",
    "terralith:mage_complex": "Комплекс магів", "terralith:mage_tower": "Вежа мага", "terralith:mage_tower_autumn": "Вежа мага (осінь)",
    "terralith:mage_tower_spring": "Вежа мага (весна)", "terralith:mage_tower_summer": "Вежа мага (літо)", "terralith:mage_tower_winter": "Вежа мага (зима)",
    "terralith:spire": "Шпиль", "terralith:underground/frosted_dungeon": "Морозне підземелля", "terralith:underground/giant_bee_hive": "Велетенський вулик",
    "terralith:underground/mining_outpost": "Шахтарська застава", "terralith:underground/oak_cabin": "Підземна дубова хатина",
    "terralith:underground/old_refinery": "Старий завод", "terralith:underground/sunken_tower": "Затоплена вежа",
    "terralith:underground/witch_hut": "Підземна хатинка відьми", "terralith:underground_cabin": "Підземна хатина",
    "terralith:valley_lodge": "Долинна садиба", "terralith:witch_hut": "Хатинка відьми",
    "twilightforest:aurora_palace": "Палац Аврори", "twilightforest:camp": "Табір", "twilightforest:dark_tower": "Темна вежа",
    "twilightforest:fallen_trunk": "Повалений стовбур", "twilightforest:final_castle": "Останній замок", "twilightforest:giant_house": "Будинок велетнів",
    "twilightforest:hedge_maze": "Живоплотовий лабіринт", "twilightforest:hollow_tree": "Дупласте дерево", "twilightforest:hydra_lair": "Лігво гідри",
    "twilightforest:knight_stronghold": "Лицарська фортеця", "twilightforest:labyrinth": "Лабіринт", "twilightforest:large_hollow_hill": "Великий порожнистий пагорб",
    "twilightforest:lich_tower": "Вежа ліча", "twilightforest:medium_hollow_hill": "Середній порожнистий пагорб", "twilightforest:mushroom_tower": "Грибна вежа",
    "twilightforest:naga_courtyard": "Двір наги", "twilightforest:quest_grove": "Гай випробування", "twilightforest:small_hollow_hill": "Малий порожнистий пагорб",
    "twilightforest:swamp_hollow_tree": "Болотне дупласте дерево", "twilightforest:troll_cave": "Печера тролів", "twilightforest:yeti_cave": "Печера єті",
    "towns_and_towers:mimic_desert": "Пустельний мімік", "towns_and_towers:wreckage_ocean": "Морські уламки",
    "towns_and_towers:exclusives/village_wandering_trader_camp": "Табір мандрівного торговця",
    "towns_and_towers:exclusives/village_piglin": "Піглінське селище",
    # nova structures with a name of their own
    "nova_structures:badlands_miner_outpost": "Шахтарська застава в бедленді", "nova_structures:bunker": "Бункер",
    "nova_structures:conduit_ruin": "Руїни провідника", "nova_structures:creeping_crypt": "Повзучий склеп", "nova_structures:deepslate_camp": "Глибосланцевий табір",
    "nova_structures:desert_ruins": "Пустельні руїни", "nova_structures:end_castle": "Замок Енду", "nova_structures:end_lighthouse": "Маяк Енду",
    "nova_structures:end_ship": "Корабель Енду", "nova_structures:hamlet": "Хутір", "nova_structures:illager_camp": "Табір іледжерів",
    "nova_structures:illager_hideout": "Схованка іледжерів", "nova_structures:illager_manor": "Маєток іледжерів", "nova_structures:jungle_ruins": "Руїни в джунглях",
    "nova_structures:lone_citadel": "Самотня цитадель", "nova_structures:mangrove_witch_hut": "Мангрова хатинка відьми", "nova_structures:nether_keep": "Незерська твердиня",
    "nova_structures:nether_port": "Незерський порт", "nova_structures:piglin_camp": "Табір піглінів", "nova_structures:piglin_donjon": "Донжон піглінів",
    "nova_structures:piglin_outstation": "Застава піглінів", "nova_structures:ruin_town": "Зруйноване містечко", "nova_structures:shrine_tower": "Вежа-святилище",
    "nova_structures:stray_fort": "Форт зблуканих", "nova_structures:toxic_lair": "Отруйне лігво", "nova_structures:trident_trial_monument": "Пам'ятник випробування тризуба",
    "nova_structures:undead_crypt": "Склеп мерців", "nova_structures:underground_house": "Підземний будинок", "nova_structures:wild_ruin": "Дикі руїни",
    "nova_structures:witch_villa": "Вілла відьми",
    "nova_structures:remnant_bee_keeper": "Руїни пасіки", "nova_structures:remnant_big_remnant": "Великі руїни", "nova_structures:remnant_big_remnant_2": "Великі руїни 2",
    "nova_structures:remnant_big_remnant_3": "Великі руїни 3", "nova_structures:remnant_birch_graveyard": "Березовий цвинтар",
    "nova_structures:remnant_bridge_remnant": "Руїни мосту", "nova_structures:remnant_bunny_base": "Кролячий прихисток",
    "nova_structures:remnant_classic_village": "Руїни села", "nova_structures:remnant_desert_remnant": "Пустельні руїни",
    "nova_structures:remnant_forest_smith": "Лісова кузня", "nova_structures:remnant_frog_ranch": "Жаб'яче ранчо", "nova_structures:remnant_graveyard": "Цвинтар",
    "nova_structures:remnant_medium_remnant": "Середні руїни", "nova_structures:remnant_medium_remnant_2": "Середні руїни 2",
    "nova_structures:remnant_miner_hut": "Хижа шахтаря", "nova_structures:remnant_mud_brick_constructor": "Будівничий з глиняної цегли",
    "nova_structures:remnant_ominous_shop": "Зловісна крамниця", "nova_structures:remnant_ruin_farmer": "Руїни ферми", "nova_structures:remnant_ruin_smith": "Руїни кузні",
    "nova_structures:remnant_sawmill": "Лісопилка", "nova_structures:remnant_school_remnant": "Руїни школи", "nova_structures:remnant_taiga_castle": "Замок у тайзі",
    "nova_structures:remnant_woodland_hud": "Лісова хатина", "nova_structures:remnant_zombie_horse_ranch": "Ранчо коней-зомбі",
    # more villages and structures (mvs): the odd ones
    "mvs:azelea_house": "Азалієвий будинок", "mvs:bee_dome": "Бджолиний купол", "mvs:beach_bar": "Пляжний бар", "mvs:bench": "Лавка",
    "mvs:boulder": "Валун", "mvs:campsite": "Стоянка", "mvs:cart": "Візок", "mvs:cartographer_tower": "Вежа картографа", "mvs:castle_ruins": "Руїни замку",
    "mvs:cathedral": "Собор", "mvs:crimson_enchanting_table": "Багряний стіл зачарувань", "mvs:crystal": "Кристал", "mvs:duck": "Качка",
    "mvs:fire_camp": "Вогнище", "mvs:floating_islands": "Летючі острови", "mvs:flower_hole": "Квіткова яма", "mvs:fox_hut": "Лисяча хатка",
    "mvs:gallows": "Шибениця", "mvs:haystack": "Копиця сіна", "mvs:horse_campsite": "Кінська стоянка", "mvs:horse_pen": "Загін для коней",
    "mvs:house": "Будинок", "mvs:lamp_chest": "Ліхтар зі скринею", "mvs:large_cart_1": "Великий візок", "mvs:large_cart_2": "Великий візок 2",
    "mvs:large_floating_island": "Великий летючий острів", "mvs:large_mushroom": "Великий гриб", "mvs:large_warped_tower": "Велика викривлена вежа",
    "mvs:lecturn_garden": "Сад із пюпітром", "mvs:lil_house": "Хатинка", "mvs:log_ruin": "Колодні руїни", "mvs:medium_bamboo_cart": "Бамбуковий візок",
    "mvs:medium_igloo_1": "Середнє іглу", "mvs:medium_igloo_2": "Середнє іглу 2", "mvs:mine_with_campsite": "Шахта зі стоянкою",
    "mvs:mud_brick_house_1": "Будинок з глиняної цегли", "mvs:mushroom_pond": "Грибний ставок", "mvs:mushroom_statue": "Грибна статуя",
    "mvs:mushroom_well": "Грибний колодязь", "mvs:nether_devil": "Незерський чорт", "mvs:nether_well": "Незерський колодязь", "mvs:ocean_tower": "Морська вежа",
    "mvs:out_house": "Вбиральня", "mvs:paths": "Стежки", "mvs:pile": "Купа", "mvs:prismarine_house_1": "Призмариновий будинок",
    "mvs:prismarine_house_2": "Призмариновий будинок 2", "mvs:railway": "Залізниця", "mvs:rare_well": "Рідкісний колодязь", "mvs:red_tower": "Червона вежа",
    "mvs:rocky_well": "Кам'яний колодязь", "mvs:ruined_beacon": "Зруйнований маяк-промінь", "mvs:shed": "Повітка", "mvs:small_igloo": "Мале іглу",
    "mvs:small_oak_pond": "Малий дубовий ставок", "mvs:small_pillager_tower": "Мала вежа розбійників", "mvs:small_ruin": "Малі руїни",
    "mvs:small_ship": "Мале судно", "mvs:small_swamp_house": "Малий болотний будинок", "mvs:small_tower_well": "Колодязь-вежа",
    "mvs:small_well": "Малий колодязь", "mvs:snowy_dog_hut": "Засніжена будка", "mvs:snowy_fossil": "Засніжена скам'янілість",
    "mvs:statue_ruins": "Руїни статуї", "mvs:stone_fountain": "Кам'яний фонтан", "mvs:stone_pillars": "Кам'яні стовпи", "mvs:stone_rock": "Кам'яна брила",
    "mvs:sunzi_gate": "Ворота Сунь-цзи", "mvs:tall_house": "Високий будинок", "mvs:tree_monument": "Пам'ятник-дерево", "mvs:villager_statue": "Статуя жителя",
    "mvs:warped_house": "Викривлений будинок", "mvs:well": "Колодязь", "mvs:wheat_grain_bin": "Зерносховище", "mvs:windmill": "Вітряк",
    "mvs:wooden_wheat_farm": "Дерев'яна пшенична ферма", "mvs:barn": "Стодола", "mvs:blue_stall": "Синя ятка", "mvs:orange_stall": "Помаранчева ятка",
    "mvs:pink_stall": "Рожева ятка", "mvs:red_stall": "Червона ятка", "mvs:deepslate_house": "Глибосланцевий будинок", "mvs:desert_house": "Пустельний будинок",
    "mvs:desert_pump": "Пустельна помпа", "mvs:diorite_and_deepslate_house": "Будинок з діориту й глибосланцю", "mvs:diorite_tower": "Діоритова вежа",
    "mvs:jungle_tower": "Вежа в джунглях", "mvs:jungle_palm_tree": "Пальма в джунглях", "mvs:big_oak_tree": "Великий дуб",
    "mvs:acacia_well": "Акацієвий колодязь", "mvs:birch_well": "Березовий колодязь", "mvs:dark_oak_well": "Колодязь з темного дуба",
    "mvs:desert_well": "Пустельний колодязь", "mvs:jungle_well": "Колодязь у джунглях", "mvs:oak_well": "Дубовий колодязь", "mvs:snowy_well": "Засніжений колодязь",
    "mvs:spruce_well": "Ялиновий колодязь", "mvs:small_copper_well": "Малий мідний колодязь",
    "ocean_lily_pad_village:butcherhouse": "Будинок м'ясника", "ocean_lily_pad_village:farm": "Ферма", "ocean_lily_pad_village:farmerhouse": "Будинок фермера",
    "ocean_lily_pad_village:fishermanhouse": "Будинок рибалки", "ocean_lily_pad_village:fletcherhouse": "Будинок лучника",
    "ocean_lily_pad_village:forge": "Кузня", "ocean_lily_pad_village:hay": "Сіновал", "ocean_lily_pad_village:house_1": "Будинок",
    "ocean_lily_pad_village:library": "Бібліотека", "ocean_lily_pad_village:market": "Ринок", "ocean_lily_pad_village:mason": "Будинок муляра",
    "ocean_lily_pad_village:shepherd": "Будинок вівчаря", "ocean_lily_pad_village:stable": "Стайня", "ocean_lily_pad_village:temple": "Храм",
    "ocean_lily_pad_village:toolsmith": "Будинок інструментальника", "ocean_lily_pad_village:watchtower": "Сторожова вежа",
}
SUFFIX = {"ocean_lily_pad_village": " на лататті"}

PLACE = {  # biome or style qualifier -> how it reads in parentheses
    "beach": "пляж", "christmas": "різдвяне", "dark_forest": "темний ліс", "desert": "пустеля", "desert_oasis": "оаза", "jungle": "джунглі",
    "jungle_tree": "на деревах джунглів", "mesa": "бедленд", "mesa_fortified": "укріплене, бедленд", "mountain": "гори", "mountain_alpine": "альпійське",
    "mushroom": "грибне", "plains": "рівнини", "plains_fortified": "укріплене, рівнини", "savanna": "савана", "savanna_na": "савана, Північна Америка",
    "snowy_igloo": "іглу", "swamp": "болото", "swamp_fortified": "укріплене, болото", "taiga": "тайга", "taiga_fortified": "укріплене, тайга",
    "underground": "підземне", "badlands": "бедленд", "dark_forest_": "темний ліс", "snowy": "засніжене", "birch_forest": "березовий ліс",
    "flower_forest": "квітковий ліс", "forest": "ліс", "grove": "гай", "meadow": "лука", "mushroom_fields": "грибні поля", "ocean": "океан",
    "old_growth_taiga": "стара тайга", "savanna_plateau": "плато савани", "snowy_beach": "засніжений пляж", "snowy_plains": "засніжені рівнини",
    "snowy_slopes": "засніжені схили", "snowy_taiga": "засніжена тайга", "sparse_jungle": "рідкі джунглі", "sunflower_plains": "соняшникові рівнини",
    "wooded_badlands": "лісистий бедленд", "classic": "класичне", "iberian": "іберійське", "mediterranean": "середземноморське", "nilotic": "нільське",
    "oriental": "східне", "rustic": "сільське", "swedish": "шведське", "tudor": "тюдорівське",
    "acacia": "акація", "birch": "береза", "cherry": "вишня", "dark_oak": "темний дуб", "mangrove": "мангри", "oak": "дуб", "spruce": "ялина",
    "savana": "савана", "crimson": "багряний ліс", "soul": "долина душ", "warped": "викривлений ліс", "waste": "пустки", "bamboo": "бамбук",
    "campfire": "вогнище", "copper": "мідь", "dripstone": "крапельники", "ice": "крига", "lush": "пишні печери", "overgrown": "заросла",
    "red_desert": "червона пустеля", "spruce_snowy": "засніжена тайга",
}
SIZE = {"small": "мале", "medium": "середнє", "large": "велике"}
NOUN = [  # (id pattern, noun) — the qualifier is what is left of the id
    (r"village_(.+)", "Село"), (r"pillager_outpost_(.+)", "Застава розбійників"), (r"tavern_(.+)", "Таверна"), (r"well_(.+)", "Колодязь"),
    (r"firewatch_tower_(.+)", "Пожежна вежа"), (r"nether_skeleton_tower_(.+)", "Вежа незерських скелетів"), (r"skeleton_camp_(.+)", "Табір скелетів"),
    (r"shrine_combat_tier_(\d)", "Бойове святилище"), (r"mineshaft_(.+)", "Покинута шахта"), (r"rubble_(.+)", "Уламки"),
    (r"(.+)_log_pile", "Купа колод"), (r"dead_tree_(.+)", "Сухе дерево"), (r"(.+)_tree", "Дерево"), (r"small_(.+)_lantern", "Ліхтар"),
    (r"medium_(.+)_lantern", "Великий ліхтар"),
]
EN_WORDS = {"na": "N.A."}


def place(q):
    if q.isdigit():
        return f"рівень {q}"
    return PLACE.get(q) or PLACE.get(q.rstrip("_")) or None


def uk(sid):
    if sid in NAMES:
        return NAMES[sid], True
    ns, path = sid.split(":", 1)
    parts = path.split("/")
    size = SIZE.get(parts[0]) if len(parts) > 1 else None
    leaf = parts[-1]
    for pat, noun in NOUN:
        m = re.fullmatch(pat, leaf)
        if m:
            q = place(m[1])
            if q:
                extra = ", ".join(x for x in (q, size) if x)
                return f"{noun} ({extra}){SUFFIX.get(ns, '')}", True
    return " ".join(path.replace("/", " ").split("_")).capitalize(), False


def en(sid):
    ns, path = sid.split(":", 1)
    words = re.split(r"[_/]", path)
    return " ".join(EN_WORDS.get(w, w.capitalize()) for w in words if w not in ("exclusives", "underground") or len(words) == 1)


ukn, enn, rough = {}, {}, []
for sid in IDS:
    key = "wilds_wiki.structure." + sid.replace(":", ".").replace("/", ".")
    name, ok = uk(sid)
    ukn[key] = name
    enn[key] = en(sid)
    if not ok:
        rough.append(sid)
json.dump(ukn, open(os.path.join(LANG, "uk_ua.structures.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
json.dump(enn, open(os.path.join(LANG, "en_us.structures.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(IDS), "structures named,", len(rough), "spelled out word by word:")
for r in rough:
    print(" ", r, "->", ukn["wilds_wiki.structure." + r.replace(":", ".").replace("/", ".")])
