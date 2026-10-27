CHAPTER = {
    "key": "peoples",
    "title": "&2Народи",
    "subtitle": "Дикі народи світу: табори, торгівля, війна",
    "group": "Пригоди",
    "icon": "goblins_tyranny:coin",
    "quests": [
        {
            "key": "camp", "title": "Табір гоблінів", "icon": "goblins_tyranny:hut_logs", "size": 1.5, "shape": "hexagon",
            "desc": [
                "Гобліни живуть цілими таборами: хатини з колод, тотеми, таверна. Є ватажок, шамани, чемпіони, лицарі й мисливці — а ще мирні &aторговці&r, &aбармен&r, &aковаль&r і &aінженер&r. Гобліни ворожі до чужинців, але не до своїх.",
                "",
                "&dЗв'язок світу:&r золоті &aгоблінські монети&r беруть сільські клірики й картографи, а мандрівний торговець продає за них керувальні схеми Mekanism."
            ],
            "guide": {
                "where": "рівнини, засніжені рівнини, савани й луки; менші стоянки й каравани — на дорогах.",
                "danger": 3,
                "gear": "залізна броня, щит, лук",
                "find": "Компас дослідника → «goblinscamp»; каравани — «goblins_caravan».",
                "tip": "спершу виманюйте воїнів по одному: разом із шаманом вони дуже небезпечні."
            },
            "tasks": ["struct:goblins_tyranny:goblinscamp"], "rewards": ["loot:wilds:reward/fantasy", "xp:80"], "skill": 80,
        },
        {
            "key": "disguise", "title": "Свій серед гоблінів", "icon": "goblins_tyranny:fake_goblin_ears_helmet", "deps": ["camp"],
            "desc": [
                "Зробіть &aфальшиві гоблінські вуха&r — у цьому «шоломі» гобліни приймають вас за свого. Тоді можна спокійно торгувати:",
                " • &aторговець&r бере &cчервоні гриби&r — за них шкури, мішок бомб, інженерний брухт;",
                " • &aбармен&r бере &6коричневі гриби&r — за них гоблінський, вогняний і «смертельний» напої;",
                " • &aгоблін-мандрівник&r міняє золоті монети.",
                "&eМіряйте обережно:&r вогняний напій підпалює, а «смертельний» — отруює."
            ],
            "guide": {"tip": "принесіть торговцю миску грибного супу — він це оцінить."},
            "tasks": ["item:goblins_tyranny:fake_goblin_ears_helmet"], "rewards": ["item:minecraft:red_mushroom*16", "item:minecraft:brown_mushroom*16", "xp:80"], "skill": 60,
        },
        {
            "key": "leader", "title": "Ватажок гоблінів", "icon": "goblins_tyranny:goblin_upgraded_mace", "deps": ["camp"],
            "desc": [
                "Ватажок — найсильніший гоблін табору, з булавою й охороною. Шаман лікує його кулями зцілення — бийте шамана першим.",
                "Нагорода: &aгоблінські злитки&r, зброя і багато монет."
            ],
            "guide": {
                "where": "центр великого табору гоблінів.",
                "danger": 4,
                "gear": "алмазна броня, щит, лук",
                "tip": "&aпастки-бочки&r в таборі вибухають — не бийтеся біля них."
            },
            "tasks": ["kill:goblins_tyranny:leader_goblin", "kill:goblins_tyranny:shaman_goblin"], "rewards": ["loot:wilds:reward/fantasy", "loot:wilds:reward/boss", "xp:250"], "skill": 200,
        },
        {
            "key": "prototype", "title": "Прототип інженера", "icon": "goblins_tyranny:goblins_prototype_chestplate", "deps": ["leader"],
            "desc": [
                "Гоблінські інженери майструють &aпрототип броні&r. Здобудьте його і вдоскональте на &aінженерному верстаті&r інженерним брухтом — так отримаєте посилену броню."
            ],
            "guide": {"tip": "&aглітерон&r — блискучий звірок гоблінів — теж носить броню; її кують на тому ж верстаті."},
            "tasks": ["item:goblins_tyranny:goblins_prototype_chestplate"], "rewards": ["loot:wilds:reward/tech", "xp:150"], "skill": 100,
        },
        {
            "key": "kobolds", "title": "Лігвище кобольдів", "icon": "kobolds:kobold_iron_pickaxe", "deps": ["camp"],
            "desc": [
                "&aКобольди&r — дрібний народ ковалів. Вони ховаються в норах під лісами й мають власне &aкобольдське залізо&r: легкі й швидкі інструменти. Поряд бродять &aзомбі-кобольди&r.",
                "&aКобольдський шаблон&r на ковальському столі перековує ваші залізні інструменти на кобольдські."
            ],
            "guide": {
                "where": "ліси й тайга, лігвище — під землею.",
                "danger": 2,
                "find": "Компас дослідника → «kobold_den».",
                "tip": "кобольдське зачарування &aСтаратель&r (Prospector) іноді вибиває з ворогів смарагди."
            },
            "tasks": ["struct:kobolds:kobold_den"], "rewards": ["item:kobolds:kobold_template", "loot:wilds:reward/fantasy", "xp:100"], "skill": 80,
        },
        {
            "key": "orcs", "title": "Хатина орків", "icon": "minecraft:iron_axe", "deps": ["camp"],
            "desc": [
                "Орки ставлять самотні хатини на рівнинах і в лісах. Вони сильні й живучі, але повільні. Нагорода — скриня орка з усім, що він награбував."
            ],
            "guide": {
                "where": "рівнини й ліси.",
                "danger": 3,
                "find": "Компас дослідника → «orc_hut».",
                "tip": "стріляйте з лука і відходьте — орк не встигає наздогнати."
            },
            "tasks": ["kill:orc_hut:orc*3"], "rewards": ["loot:wilds:reward/fantasy", "xp:120"], "skill": 100,
        },
        {
            "key": "illagers", "title": "Твердиня ілладжерів", "icon": "minecraft:totem_of_undying", "deps": ["leader"],
            "desc": [
                "Ілладжери не сидять у самих лише форпостах: є фортеці, вітряки, табори, галери й навіть школа &aілюзіоністів&r і хижа &aкрижаного чаклуна&r."
            ],
            "guide": {
                "where": "темні ліси, гори й узбережжя; крижаний чаклун — у снігах.",
                "danger": 4,
                "gear": "щит від сокир, тотем безсмертя, лук",
                "find": "Компас дослідника → «illager_fort», «illager_windmill», «illusioner_training_grounds», «iceologer_cabin».",
                "tip": "&aохоронці сіл&r (Guard Villagers) допомагають відбивати набіги — заведіть кількох."
            },
            "tasks": ["struct:dungeons_arise:illager_fort"], "rewards": ["loot:wilds:reward/boss", "xp:250"], "skill": 200,
        },
        {
            "key": "bandits", "title": "Бандитське селище", "icon": "minecraft:crossbow", "deps": ["illagers"],
            "desc": [
                "Розбійники захопили ціле селище й вежі на пагорбах. Тут багато лучників — і багато награбованого."
            ],
            "guide": {
                "where": "рівнини й савани.",
                "danger": 4,
                "find": "Компас дослідника → «bandit_village», «bandit_towers».",
                "tip": "заходьте вночі, з тіні: лучники бачать далеко вдень."
            },
            "tasks": ["struct:dungeons_arise:bandit_village"], "rewards": ["loot:wilds:reward/fantasy", "loot:wilds:reward/explorer", "xp:200"], "skill": 150,
        },
        {
            "key": "raiders", "title": "Табори нападників", "icon": "minecolonies:scepterguard", "deps": ["bandits"],
            "desc": [
                "Варвари, амазонки, мумії пустелі й пірати стоять таборами — саме вони нападають на колонії. Розбийте табір, поки він не прийшов до вас.",
                "&dЗв'язок світу:&r у главі &aКолонії&r ці народи приходять у набіги на ваше місто."
            ],
            "guide": {
                "where": "варвари — ліси й рівнини; амазонки — джунглі; мумії — пустеля.",
                "danger": 4,
                "find": "Компас дослідника → «barbarian_camp», «amazon_camp», «desert_camp».",
                "tip": "вожді носять найкращу броню — і падають з неї."
            },
            "tasks": ["struct:minecolonies:barbarian_camp", "kill:minecolonies:chiefbarbarian"], "rewards": ["loot:wilds:reward/boss", "xp:250"], "skill": 200,
        },
    ],
}
