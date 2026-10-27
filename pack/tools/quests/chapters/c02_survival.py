CHAPTER = {
    "key": "survival",
    "title": "&aВиживання",
    "subtitle": "Їжа, пори року, скрині й скарби",
    "group": "Початок",
    "icon": "farmersdelight:cooking_pot",
    "quests": [
        {
            "key": "knife", "title": "Кремнієвий ніж", "icon": "farmersdelight:flint_knife",
            "desc": [
                "Farmer's Delight перетворює їжу на цілу кухню. Почніть із &aкремнієвого ножа&r: ним можна різати траву (дає волокно і насіння), а на нарізній дошці — м'ясо, рибу й овочі.",
                "",
                "&6Порада:&r ніж — теж зброя, хоч і слабка."
            ],
            "tasks": ["item:farmersdelight:flint_knife"], "rewards": ["item:minecraft:bread*4", "xp:15"],
        },
        {
            "key": "board", "title": "Нарізна дошка", "icon": "farmersdelight:cutting_board", "deps": ["knife"],
            "desc": [
                "Поставте &aнарізну дошку&r, покладіть на неї продукт (правою кнопкою) і клацніть ножем.",
                "Так з однієї курки виходить кілька шматків м'яса, а з капусти — листя для салатів."
            ],
            "tasks": ["item:farmersdelight:cutting_board"], "rewards": ["xp:20"],
        },
        {
            "key": "pot", "title": "Каструля і плита", "icon": "farmersdelight:cooking_pot", "deps": ["board"],
            "desc": [
                "&aКаструля&r на &aкухонній плиті&r (або над вогнищем) готує супи, рагу й пироги. Страви з неї дають ефект &eСитість&r — голод майже не росте кілька хвилин.",
                "",
                "Інгредієнти: &aкапуста, томати, цибуля, рис&r — їх насіння знаходять у дикій природі (кущі біля води й у полях)."
            ],
            "tasks": ["item:farmersdelight:cooking_pot", "item:farmersdelight:stove"], "rewards": ["item:farmersdelight:rice*4", "xp:40"], "skill": 40,
        },
        {
            "key": "seasons", "title": "Пори року", "icon": "sereneseasons:calendar", "deps": ["knife"],
            "desc": [
                "У світі змінюються &eпори року&r (Serene Seasons): весна, літо, осінь, зима. Взимку частина культур не росте, а листя змінює колір.",
                "",
                "&aКалендар&r показує поточну пору й день. Наведіть у JEI на насіння — у підказці видно, в які сезони воно росте.",
                "&6Порада:&r теплиці (культури під дахом зі скла) ростуть цілий рік."
            ],
            "tasks": ["item:sereneseasons:calendar"], "rewards": ["xp:30"], "skill": 30,
        },
        {
            "key": "loot", "title": "Особистий лут", "icon": "lootr:lootr_chest", "deps": ["knife"],
            "desc": [
                "Скрині в спорудах світу — &eособисті&r (Lootr): кожен гравець знаходить у них свій лут. Не треба поспішати, щоб друзі не забрали все першими.",
                "",
                "Позначка на скрині показує, чи ви вже її відкривали."
            ],
            "tasks": ["check"], "rewards": ["xp:15"],
        },
        {
            "key": "storage", "title": "Розумні скрині", "icon": "sophisticatedstorage:chest", "deps": ["loot"],
            "desc": [
                "Sophisticated Storage додає скрині й бочки, які можна &eпокращувати&r (більше місця) і ставити в них модулі: сортування, фільтри, переплавку, компресію.",
                "",
                "Рецепти — у JEI. Покращення ставиться правою кнопкою по скрині, вміст не губиться."
            ],
            "tasks": ["item:sophisticatedstorage:chest"], "rewards": ["xp:30"],
        },
    ],
}
