"""Речі на місцях: backpack upgrades, Sophisticated Storage, and the pack's own help with things: the item finder, the
copper golem sorter and its chest statuses, the extra inventory rows."""
CHAPTER = {
    "key": "storage",
    "title": "&bРечі на місцях",
    "subtitle": "Рюкзаки, сховища, пошук і сортування",
    "group": "Поселення",
    "icon": "sophisticatedstorage:barrel",
    "quests": [
        {
            "key": "barrel", "title": "Розумна бочка", "icon": "sophisticatedstorage:barrel", "size": 1.5, "shape": "hexagon",
            "subtitle": "Сховище, яке росте разом з вами",
            "desc": [
                "Бочки й скрині Sophisticated Storage вміщають стільки ж, скільки звичайні, але їх можна &eпокращувати&r, не перекладаючи речей.",
                "Зламана, вона не розсипає вміст: забирає його з собою (якщо так налаштовано)."
            ],
            "tasks": ["item:sophisticatedstorage:barrel"], "rewards": ["xp:30"], "skill": 30,
        },
        {
            "key": "tier", "title": "Більше сховище", "icon": "sophisticatedstorage:basic_tier_upgrade", "deps": ["barrel"],
            "desc": [
                "&aПокращення рівня&r перетворює звичайну бочку чи скриню на мідну, залізну, золоту й далі. Клацніть ним по сховищу — речі лишаються всередині, а місця більшає."
            ],
            "tasks": ["item:sophisticatedstorage:basic_tier_upgrade"], "rewards": ["xp:40"], "skill": 40,
        },
        {
            "key": "stack", "title": "Більші стоси", "icon": "sophisticatedstorage:stack_upgrade_tier_1", "deps": ["tier"],
            "desc": [
                "Покращення стосу вставляється в слот покращень сховища: у кожну комірку влазить удвічі більше. Ідеально для каменю, землі й руди."
            ],
            "tasks": ["item:sophisticatedstorage:stack_upgrade_tier_1"], "rewards": ["xp:50"], "skill": 40,
        },
        {
            "key": "compacting", "title": "Стискання", "icon": "sophisticatedstorage:compacting_upgrade", "deps": ["stack"],
            "desc": [
                "&aПокращення стискання&r саме перетворює дев'ять злитків на блок, дев'ять самородків на злиток — сховище тримає менше комірок зайнятими."
            ],
            "tasks": ["item:sophisticatedstorage:compacting_upgrade"], "rewards": ["xp:60"], "skill": 40,
        },
        {
            "key": "controller", "title": "Одне вікно на всі скрині", "icon": "sophisticatedstorage:controller", "deps": ["tier"], "size": 1.25,
            "desc": [
                "&aКонтролер&r з'єднує сховища, що торкаються його або одне одного (чи з'єднані конекторами). Відкрийте контролер — і бачите вміст усіх разом, кладете й берете з одного вікна."
            ],
            "tasks": ["item:sophisticatedstorage:controller"], "rewards": ["loot:wilds:reward/tech", "xp:100"], "skill": 60,
        },
        {
            "key": "pack_pickup", "title": "Рюкзак сам збирає", "icon": "sophisticatedbackpacks:pickup_upgrade", "deps": ["barrel"],
            "desc": [
                "Покращення рюкзака вставляються в його нижні слоти. &aПідбирання&r кладе все підібране одразу в рюкзак, а з фільтром — лише потрібне."
            ],
            "tasks": ["item:sophisticatedbackpacks:pickup_upgrade"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "pack_magnet", "title": "Магніт", "icon": "sophisticatedbackpacks:magnet_upgrade", "deps": ["pack_pickup"],
            "desc": [
                "&aМагніт&r притягує предмети й досвід з кількох блоків. Разом із підбиранням рюкзак сам збирає здобич після бою."
            ],
            "tasks": ["item:sophisticatedbackpacks:magnet_upgrade"], "rewards": ["xp:60"], "skill": 40,
        },
        {
            "key": "pack_feeding", "title": "Годування в дорозі", "icon": "sophisticatedbackpacks:feeding_upgrade", "deps": ["pack_pickup"],
            "desc": [
                "&aГодування&r саме дає вам їжу з рюкзака, коли ви зголодніли. Покладіть у рюкзак страви з Кухні — і про голод можна забути."
            ],
            "tasks": ["item:sophisticatedbackpacks:feeding_upgrade"], "rewards": ["xp:50"], "skill": 30,
        },
        {
            "key": "pack_iron", "title": "Залізний рюкзак", "icon": "sophisticatedbackpacks:iron_backpack", "deps": ["pack_pickup"],
            "desc": [
                "Рюкзак покращують на верстаті: мідний, залізний, золотий, діамантовий. Кожен має більше комірок і слотів покращень, а речі всередині лишаються."
            ],
            "tasks": ["item:sophisticatedbackpacks:iron_backpack"], "rewards": ["xp:80"], "skill": 50,
        },
        {
            "key": "finder", "title": "Де мої діаманти?", "icon": "minecraft:spyglass", "deps": ["barrel"],
            "desc": [
                "Ліворуч від інвентаря (&aE&r) є поле &eЗнайти в скринях…&r. Напишіть назву предмета будь-якою мовою — і скрині, бочки та рюкзаки поруч, де він лежить, засвітяться крізь стіни.",
                "Руду й поставлені блоки пошук не бачить: лише те, що лежить у сховищах і на землі."
            ],
            "tasks": ["check"], "rewards": ["xp:20"], "skill": 20,
        },
        {
            "key": "golem", "title": "Мідний помічник", "icon": "minecraft:copper_block", "deps": ["controller"], "size": 1.25,
            "subtitle": "Голем, що сам розкладає речі",
            "desc": [
                "Поставте &aвирізаний гарбуз просто на мідний блок&r. Блок стане мідною скринею, на ній з'явиться голем.",
                "Кладіть у цю скриню речі — голем рознесе їх у скрині поруч, де вже лежить таке саме."
            ],
            "tasks": ["check"], "rewards": ["item:minecraft:honeycomb*4", "xp:60"], "skill": 40,
        },
        {
            "key": "golem_status", "title": "Джерело, різне, не чіпати", "icon": "minecraft:chest", "deps": ["golem"],
            "desc": [
                "Кожна скриня біля голема має статус. Присядьте й клацніть по скрині &aпорожньою рукою&r, щоб перемкнути: звичайна → джерело (точно) → джерело (за схожістю) → різне → не чіпати.",
                "Статус видно в заголовку відкритої скрині. Соти на големові не дають йому зіржавіти."
            ],
            "tasks": ["check"], "rewards": ["xp:40"], "skill": 30,
        },
    ],
}
