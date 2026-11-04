"""Село й торгівля: Bountiful's bounty boards, the new villager professions of Villagers Plus, trading well, and the hero
of the village."""
CHAPTER = {
    "key": "village",
    "title": "&2Село й торгівля",
    "subtitle": "Доручення, нові професії, вигідні обміни",
    "group": "Поселення",
    "icon": "bountiful:bountyboard",
    "quests": [
        {
            "key": "board", "title": "Дошка оголошень", "icon": "bountiful:bountyboard", "size": 1.5, "shape": "hexagon",
            "subtitle": "Робота для мандрівника",
            "desc": [
                "У селах стоять &aдошки оголошень&r. На них селяни вивішують доручення: принести речі, перемогти мобів. Візьміть доручення — у вас з'явиться аркуш з умовами й часом на виконання.",
                "Дошку можна скрафтити й поставити вдома."
            ],
            "tasks": ["adv:bountiful:bountiful/bounty_taken"], "rewards": ["item:minecraft:emerald*4", "xp:30"], "skill": 30,
        },
        {
            "key": "bounty_done", "title": "Чесна плата", "icon": "bountiful:bounty", "deps": ["board"],
            "desc": [
                "Виконайте умови й клацніть аркушем по дошці. Нагорода — смарагди й речі, а ще &aрепутація&r: з нею доручення стають цікавішими."
            ],
            "tasks": ["adv:bountiful:bountiful/bounty_complete"], "rewards": ["loot:wilds:reward/explorer", "xp:60"], "skill": 50,
        },
        {
            "key": "rush", "title": "Термінова доставка", "icon": "minecraft:clock", "deps": ["bounty_done"],
            "desc": [
                "Виконайте доручення задовго до кінця його терміну."
            ],
            "tasks": ["adv:bountiful:bountiful/rush_order"], "rewards": ["item:minecraft:emerald*8", "xp:80"], "skill": 50,
        },
        {
            "key": "alchemist", "title": "Алхімік", "icon": "villagersplus:alchemist_table", "deps": ["board"],
            "desc": [
                "Villagers Plus додає селам нові професії. Безробітний селянин біля &aалхімічного столу&r стає &aалхіміком&r: він торгує зіллями та їхніми інгредієнтами."
            ],
            "tasks": ["item:villagersplus:alchemist_table"], "rewards": ["item:minecraft:glass_bottle*8", "xp:40"], "skill": 30,
        },
        {
            "key": "horticulturist", "title": "Садівник", "icon": "villagersplus:oak_horticulturist_table", "deps": ["board"],
            "desc": [
                "&aКвіткова діжка&r робить селянина &aсадівником&r, що торгує рослинами."
            ],
            "tasks": ["item:villagersplus:oak_horticulturist_table"], "rewards": ["item:minecraft:bone_meal*16", "xp:40"], "skill": 30,
        },
        {
            "key": "oceanographer", "title": "Океанограф", "icon": "villagersplus:oceanographer_table", "deps": ["board"],
            "desc": [
                "&aАкваріум&r — робоче місце &aокеанографа&r: він торгує дарами моря."
            ],
            "tasks": ["item:villagersplus:oceanographer_table"], "rewards": ["item:minecraft:prismarine_shard*8", "xp:40"], "skill": 30,
        },
        {
            "key": "miner", "title": "Рудокоп", "icon": "villagersplus:ore_grinder", "deps": ["board"],
            "desc": [
                "&aДробарка руди&r дає селу &aрудокопа&r. Він торгує рудою й камінням — зручно, коли бракує заліза."
            ],
            "tasks": ["item:villagersplus:ore_grinder"], "rewards": ["item:minecraft:iron_ingot*8", "xp:40"], "skill": 30,
        },
        {
            "key": "occultist", "title": "Окультист", "icon": "villagersplus:occultist_table", "deps": ["alchemist"],
            "desc": [
                "&aЗачарована чаша&r приваблює &aокультиста&r: він торгує магічними речами."
            ],
            "tasks": ["item:villagersplus:occultist_table"], "rewards": ["item:minecraft:lapis_lazuli*16", "xp:50"], "skill": 40,
        },
        {
            "key": "trader_pocket", "title": "Селянин у кишені", "icon": "easy_villagers:villager", "deps": ["miner"],
            "desc": [
                "Easy Villagers дозволяє взяти селянина в руки: присядьте й клацніть по ньому. Так його легко перенести в торговий блок чи на свою базу."
            ],
            "tasks": ["item:easy_villagers:villager"], "rewards": ["xp:50"], "skill": 30,
        },
        {
            "key": "hero", "title": "Герой села", "icon": "minecraft:totem_of_undying", "deps": ["bounty_done", "occultist"], "size": 1.5, "shape": "gear",
            "subtitle": "Відбийте рейд",
            "desc": [
                "Отримавши ефект &cЗловісне знамення&r (переможіть капітана розбійників), зайдіть у село — почнеться рейд.",
                "Відбийте всі хвилі, і селяни назвуть вас &aгероєм&r: знижки на обміни й подарунки."
            ],
            "tasks": ["adv:minecraft:adventure/hero_of_the_village"], "rewards": ["loot:wilds:reward/boss", "item:minecraft:emerald*16", "xp:250"], "skill": 150,
        },
    ],
}
