"""Quest chapter «Виживання» (12 quests): Їжа, пори року, скрині й скарби. Read by gen_quests.py."""
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
        {
            "key": "iron_armor", "title": "Залізна броня", "icon": "minecraft:iron_chestplate", "deps": ["knife"],
            "desc": [
                "Перші ночі легше пережити в броні. Повний комплект заліза — добрий старт; цифра поруч зі смужкою броні показує, скільки її у вас насправді."
            ],
            "tasks": ["item:minecraft:iron_helmet", "item:minecraft:iron_chestplate", "item:minecraft:iron_leggings", "item:minecraft:iron_boots"], "rewards": ["item:minecraft:iron_ingot*6", "xp:60"], "skill": 50,
        },
        {
            "key": "shield", "title": "Щит", "icon": "minecraft:shield", "deps": ["iron_armor"],
            "desc": [
                "Щит у другій руці (ПКМ) блокує удари й стріли спереду."
            ],
            "tasks": ["item:minecraft:shield"], "rewards": ["xp:30"], "skill": 20,
        },
        {
            "key": "bow", "title": "Лук і стріли", "icon": "minecraft:bow", "deps": ["iron_armor"],
            "desc": [
                "Багато ворогів краще бити здалеку: криперів, павуків, відьом. Тримайте лук і запас стріл."
            ],
            "tasks": ["item:minecraft:bow", "item:minecraft:arrow*32"], "rewards": ["item:minecraft:arrow*32", "xp:30"], "skill": 20,
        },
        {
            "key": "farm", "title": "Свій хліб", "icon": "minecraft:wheat", "deps": ["pot"],
            "desc": [
                "Пшениця росте майже весь рік, але зима її зупиняє. Зберіть урожай і посійте знову: стиглий урожай ще й дає досвід дерева навичок."
            ],
            "tasks": ["item:minecraft:wheat*32"], "rewards": ["item:minecraft:bone_meal*16", "xp:40"], "skill": 30,
        },
        {
            "key": "anvil", "title": "Ковадло", "icon": "minecraft:anvil", "deps": ["iron_armor"],
            "desc": [
                "Ковадло лагодить речі, поєднує зачарування й перейменовує. Знадобиться й для першого самоцвітного пилу."
            ],
            "tasks": ["item:minecraft:anvil"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "enchanting", "title": "Стіл зачарувань", "icon": "minecraft:enchanting_table", "deps": ["anvil"],
            "desc": [
                "Стіл зачарувань із полицями навколо дає перші чари. Згодом полиці з Незеру, моря й Енду піднімуть його силу (розділ «Зачарування»)."
            ],
            "tasks": ["item:minecraft:enchanting_table"], "rewards": ["item:minecraft:lapis_lazuli*16", "xp:60"], "skill": 40,
        },
    ],
}
