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
    ],
}
