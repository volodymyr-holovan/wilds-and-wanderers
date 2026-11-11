"""Quest chapter «Зачарування» (12 quests): Рідкісні речі, самоцвіти, бібліотека чар. Read by gen_quests.py."""
CHAPTER = {
    "key": "apotheosis",
    "title": "&eЗачарування",
    "subtitle": "Рідкісні речі, самоцвіти, бібліотека чар",
    "group": "Магія",
    "icon": "apotheosis:gem",
    "quests": [
        {
            "key": "affix", "title": "Рідкісні речі", "icon": "minecraft:diamond_sword", "size": 1.5,
            "desc": [
                "Apotheosis додає речам &eрідкісність&r (звичайна, незвичайна, рідкісна, епічна, міфічна) з випадковими властивостями-афіксами.",
                "Такі речі падають з мобів (особливо з &cбосів-чемпіонів&r зі світлою аурою) і трапляються в скринях.",
                "",
                "Наведіть на предмет — у підказці видно рідкість і всі бонуси."
            ],
            "tasks": ["check"], "rewards": ["xp:30"], "skill": 30,
        },
        {
            "key": "gem", "title": "Самоцвіти", "icon": "apotheosis:gem", "deps": ["affix"],
            "desc": [
                "&aСамоцвіти&r випадають з мобів. Їх вставляють у &eгнізда&r спорядження на ковальському столі. Гнізда додає &aсигіл інкрустації&r."
            ],
            "tasks": ["item:apotheosis:gem", "item:apotheosis:sigil_of_socketing"], "rewards": ["xp:60"], "skill": 50,
        },
        {
            "key": "salvage", "title": "Розбирання і перековка", "icon": "apotheosis:salvaging_table", "deps": ["affix"],
            "desc": [
                "&aСтіл перероблення&r розбирає непотрібні рідкісні речі на матеріали, а &aстіл перекування&r за ці матеріали перекидає афікси на кращі."
            ],
            "tasks": ["item:apotheosis:salvaging_table", "item:apotheosis:reforging_table"], "rewards": ["xp:80"], "skill": 50,
        },
        {
            "key": "shelves", "title": "Сильніші зачарування", "icon": "apothic_enchanting:hellshelf", "deps": ["affix"],
            "desc": [
                "Звичайні книжкові полиці дають мало. Полиці &aпекла&r (Незер), &aморя&r і &aЕнду&r піднімають силу столу зачарувань, а деякі — рівні чар понад звичний максимум.",
                "Ставте різні полиці навколо столу і дивіться статистику в його інтерфейсі."
            ],
            "tasks": ["item:apothic_enchanting:hellshelf*4", "item:apothic_enchanting:seashelf*4"], "rewards": ["xp:100"], "skill": 60,
        },
        {
            "key": "library", "title": "Бібліотека чар", "icon": "apothic_enchanting:library", "deps": ["shelves"],
            "desc": [
                "&aБібліотека&r зберігає всі ваші зачаровані книги як «очки» і видає книгу будь-якого рівня, який ви вже вносили. Більше не треба скринь з книгами."
            ],
            "tasks": ["item:apothic_enchanting:library"], "rewards": ["xp:150"], "skill": 80,
        },
        {
            "key": "first_dust", "title": "Перший самоцвітний пил", "icon": "apotheosis:gem_dust", "deps": ["gem"],
            "desc": [
                "Самоцвітний пил потрібен для столів огранювання, розбирання й перекування, але перший пил ні з чого не скрафтиш.",
                "Викиньте простий самоцвіт на землю (&aQ&r) і &eскиньте на нього ковадло&r: поставте ковадло на блок просто над ним. Самоцвіт розсиплеться на пил."
            ],
            "tasks": ["item:apotheosis:gem_dust*3"], "rewards": ["xp:60"], "skill": 40,
        },
        {
            "key": "cutting", "title": "Огранювання", "icon": "apotheosis:gem_cutting_table", "deps": ["first_dust"],
            "desc": [
                "На &aстолі огранювання&r самоцвіт стає чистішим: тріснутий, щербатий, з вадою, звичайний, бездоганний, досконалий. Для цього потрібні пил і матеріали рідкості з розібраних речей."
            ],
            "tasks": ["item:apotheosis:gem_cutting_table"], "rewards": ["item:apotheosis:gem_dust*8", "xp:80"], "skill": 50,
        },
        {
            "key": "augmenting", "title": "Посилення", "icon": "apotheosis:augmenting_table", "deps": ["cutting"],
            "desc": [
                "&aСтіл посилення&r перекидає або підсилює одну властивість речі, не чіпаючи решти. Дорого, зате точно."
            ],
            "tasks": ["item:apotheosis:augmenting_table"], "rewards": ["loot:wilds:reward/magic", "xp:120"], "skill": 60,
        },
        {
            "key": "tier_frontier", "title": "Світовий рівень: Кордон", "icon": "minecraft:iron_chestplate", "deps": ["affix"], "size": 1.25,
            "desc": [
                "Світовий рівень — ваша особиста складність світу: що вищий, то кращі речі й самоцвіти. Вікно рівнів — &aCtrl+T&r.",
                "Щоб відкрити &eКордон&r, вдягніть одночасно шолом, нагрудник, поножі, черевики й візьміть зброю — усе з властивостями. Потім активуйте рівень у вікні."
            ],
            "tasks": ["adv:apotheosis:progression/frontier"], "rewards": ["loot:wilds:reward/explorer", "xp:100"], "skill": 60,
        },
        {
            "key": "tier_ascent", "title": "Світовий рівень: Сходження", "icon": "minecraft:diamond_chestplate", "deps": ["tier_frontier"],
            "desc": [
                "Повний комплект &aнезвичайних&r (зелених) речей або кращих — і переможіть &cзагарбника&r. Загарбники з'являються, поки активний Кордон."
            ],
            "tasks": ["adv:apotheosis:progression/ascent"], "rewards": ["loot:wilds:reward/boss", "xp:200"], "skill": 100,
        },
        {
            "key": "tier_summit", "title": "Світовий рівень: Вершина", "icon": "minecraft:wither_skeleton_skull", "deps": ["tier_ascent"],
            "desc": [
                "Повний комплект &9рідкісних&r (синіх) речей — і переможіть &cВізера&r."
            ],
            "tasks": ["adv:apotheosis:progression/summit"], "rewards": ["loot:wilds:reward/boss", "xp:300"], "skill": 150,
        },
        {
            "key": "tier_pinnacle", "title": "Світовий рівень: Пік", "icon": "minecraft:dragon_head", "deps": ["tier_summit"], "size": 1.5, "shape": "gear",
            "desc": [
                "Повний комплект &5епічних&r (фіолетових) речей — і переможіть &cдракона Енду&r. Найвищий рівень: найсильніші вороги й найкраща здобич."
            ],
            "tasks": ["adv:apotheosis:progression/pinnacle"], "rewards": ["loot:wilds:reward/boss", "loot:wilds:reward/magic", "xp:500"], "skill": 250,
        },
    ],
}
