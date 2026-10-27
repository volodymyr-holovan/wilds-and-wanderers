CHAPTER = {
    "key": "wildlife",
    "title": "&2Дика природа",
    "subtitle": "Звірі, птахи, комахи і дрібниці під ногами",
    "group": "Поселення",
    "icon": "naturalist:capture_net",
    "quests": [
        {
            "key": "ground", "title": "Що лежить під ногами", "icon": "gew2:pebble", "size": 1.5, "shape": "hexagon",
            "desc": [
                "На землі лежать &aкамінці&r і &aгілочки&r — збирайте їх руками, без інструмента.",
                " • 4 камінці → &aкругляк&r: перша кам'яна кирка без копання;",
                " • гілочки → &aпалиці&r;",
                " • жорна Create мелють камінці на пісок і трохи кременю."
            ],
            "tasks": ["item:gew2:pebble*8", "item:gew2:twigs*4"], "rewards": ["item:minecraft:stone_pickaxe", "xp:20"], "skill": 20,
        },
        {
            "key": "net", "title": "Сачок для звірів", "icon": "naturalist:capture_net", "deps": ["ground"],
            "desc": [
                "&aСачок&r ловить дрібних тварин — метеликів, світлячків, равликів, ящірок — і переносить їх живими. Так заселяють сад чи зоопарк біля бази."
            ],
            "guide": {"tip": "світлячки світяться вночі над болотами й луками; метелики запилюють квіти й саджанці."},
            "tasks": ["item:naturalist:capture_net"], "rewards": ["xp:30"], "skill": 30,
        },
        {
            "key": "hunt", "title": "Мисливська здобич", "icon": "naturalist:venison", "deps": ["ground"],
            "desc": [
                "Олені, кабани, качки й індики — нова їжа: &aоленина&r, &aдичина&r, &aстегенця&r. З більших звірів падають &aхутро&r і &aшкура&r.",
                "",
                "&dЗв'язок світу:&r хутро й шкури звірів, гоблінів і динозаврів &eпідходять замість шкіри&r у рецептах; хутро ще й перетворюється на вовну."
            ],
            "guide": {
                "where": "ліси, тайга, савани й болота — у кожному краї свої звірі.",
                "danger": 2,
                "tip": "ведмеді й алігатори нападають першими; не підходьте близько до ведмежат."
            },
            "tasks": ["item:naturalist:venison*4", "item:naturalist:fur*2"], "rewards": ["loot:wilds:reward/explorer", "xp:60"], "skill": 50,
        },
        {
            "key": "antler", "title": "Оленячі роги", "icon": "naturalist:antler", "deps": ["hunt"],
            "desc": [
                "Олені в лісах іноді скидають &aроги&r — підберіть їх із землі. Роги можна повісити як прикрасу, а жорна Create перемелюють їх на кістяне борошно для добрива."
            ],
            "tasks": ["item:naturalist:antler"], "rewards": ["item:minecraft:bone_meal*16", "xp:40"], "skill": 30,
        },
        {
            "key": "savanna", "title": "Звірі савани", "icon": "naturalist:ostrich_egg", "deps": ["hunt"],
            "desc": [
                "У савані живуть слони, жирафи, зебри, леви, носороги й страуси. Леви — хижаки, а носоріг таранить тих, хто підійде близько."
            ],
            "guide": {
                "where": "савани.",
                "danger": 3,
                "tip": "на зебру можна сісти верхи, як на коня."
            },
            "tasks": ["biome:minecraft:savanna", "kill:naturalist:lion"], "rewards": ["loot:wilds:reward/explorer", "xp:80"], "skill": 60,
        },
        {
            "key": "swamp", "title": "Мешканці боліт", "icon": "naturalist:alligator_egg", "deps": ["hunt"],
            "desc": [
                "У болотах — алігатори, бегемоти й черепахи, а в ставках — сом і окунь. Алігатори нападають з-під води."
            ],
            "guide": {"where": "болота й мангрові ліси.", "danger": 3},
            "tasks": ["item:naturalist:alligator_egg"], "rewards": ["xp:80"], "skill": 60,
        },
    ],
}
