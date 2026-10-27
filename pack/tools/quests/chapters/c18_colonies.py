CHAPTER = {
    "key": "colonies",
    "title": "&6Села й колонії",
    "subtitle": "Селяни, охорона, власне місто з робітниками",
    "group": "Поселення",
    "icon": "minecolonies:blockhuttownhall",
    "quests": [
        {
            "key": "village", "title": "Перше село", "icon": "minecraft:bell", "size": 1.5, "shape": "hexagon",
            "desc": [
                "Села тут різні: великі й малі, з мурами, у пустелях, джунглях, горах і на воді. Кожен селянин має ім'я, а нові професії — &aалхімік&r, &aокультист&r, &aсадівник&r, &aокеанограф&r і &aшахтар&r — торгують товарами модів.",
                "",
                "&dЗв'язок світу:&r звичайні селяни теж продають речі модів: ключ Create, конфігуратор Mekanism, срібну зброю, компаси, кораблі й намети."
            ],
            "guide": {
                "where": "рівнини, савани, тайга, пустелі, болота, гори.",
                "danger": 1,
                "find": "Компас дослідника → «village_plains» або будь-яке село CTOV / Towns and Towers.",
                "tip": "біля дзвону завжди є ліжка — гарне місце для першої ночі."
            },
            "tasks": ["struct:#minecraft:village"], "rewards": ["item:minecraft:emerald*8", "xp:40"], "skill": 40,
        },
        {
            "key": "guards", "title": "Охорона села", "icon": "minecraft:iron_sword", "deps": ["village"],
            "desc": [
                "Будь-якого безробітного селянина можна зробити &aохоронцем&r: присядьте й клацніть по ньому правою кнопкою з мечем чи арбалетом у руці. Охоронець носить зброю й броню, яку ви йому дасте, і б'ється з монстрами й ілладжерами.",
                "Після перемоги над набігом (статус «Герой села») охоронці ходять за вами; дзвін скликає їх."
            ],
            "guide": {"tip": "дайте охоронцям щити — вони ними справді закриваються."},
            "tasks": ["kill:minecraft:pillager*5"], "rewards": ["item:minecraft:iron_sword", "item:minecraft:shield", "xp:80"], "skill": 60,
        },
        {
            "key": "easy", "title": "Селянин у кишені", "icon": "easy_villagers:villager", "deps": ["village"],
            "desc": [
                "Easy Villagers дозволяє підняти селянина: присядьте й клацніть по ньому порожньою рукою. Селянина в кишені можна посадити в блок:",
                " • &aторговець&r — торгівля в одному блоці; &aавтоторговець&r — сам міняє з воронки;",
                " • &aфермер&r — вирощує культури; &aрозплідник&r — народжує нових селян;",
                " • &aперетворювач&r — лікує зомбі-селян; &aзалізна ферма&r — дає залізо."
            ],
            "guide": {"tip": "так селян легко перевезти в колонію чи свою базу без доріг і човнів."},
            "tasks": ["item:easy_villagers:trader"], "rewards": ["loot:wilds:reward/explorer", "xp:80"], "skill": 60,
        },
        {
            "key": "colony", "title": "Своя колонія", "icon": "minecolonies:supplycampdeployer", "deps": ["village"], "size": 1.5,
            "desc": [
                "&eMineColonies&r — це місто з найнятими робітниками: лісоруби рубають і садять ліс, шахтарі копають шахти, фермери орють поля, будівельник зводить будинки за кресленнями.",
                "",
                "&eЯк почати:&r поставте &aТабір постачання&r (на суші) чи &aКорабель постачання&r (на воді). У його скрині — &aРатуша&r і &aІнструмент будівництва&r. Поставте Ратушу — і до вас прийдуть перші четверо поселенців.",
                "",
                "Табір постачання продає мандрівний торговець, а також він трапляється у торбах мандрівника за завдання."
            ],
            "guide": {
                "where": "щонайменше за 300 блоків від спавну; на одного гравця — одна колонія.",
                "tip": "оберіть рівне місце біля лісу й води: будівлі великі, а лісорубу й рибалці потрібен простір."
            },
            "tasks": ["item:minecolonies:blockhuttownhall"], "rewards": ["item:minecolonies:supplycampdeployer", "xp:100"], "skill": 100,
        },
        {
            "key": "builder", "title": "Майстерня будівельника", "icon": "minecolonies:blockhutbuilder", "deps": ["colony"],
            "desc": [
                "Без будівельника місто не росте. Поставте блок &aМайстерні будівельника&r, а &aІнструментом будівництва&r (золотий скіпетр Structurize) оберіть креслення будинку і місце.",
                "Будівельник сам принесе список потрібних матеріалів — покладіть їх у його скриню, і він зведе будинок блок за блоком."
            ],
            "guide": {"tip": "у &aЗаписнику&r видно всі прохання поселенців: що кому бракує."},
            "tasks": ["item:minecolonies:blockhutbuilder", "item:structurize:sceptergold"], "rewards": ["item:minecraft:oak_log*64", "item:minecraft:cobblestone*64", "xp:80"], "skill": 60,
        },
        {
            "key": "homes", "title": "Оселі й їдальня", "icon": "minecolonies:blockhutcitizen", "deps": ["builder"],
            "desc": [
                "Поселенцям потрібні &aОселі&r (у кожній — ліжка) і їжа. &aЇдальня&r з кухарем годує всіх; голодні поселенці працюють повільно і хворіють.",
                "&dЗв'язок світу:&r їжа Farmer's Delight та інших модів кухні добре підходить поселенцям."
            ],
            "tasks": ["item:minecolonies:blockhutcitizen", "item:minecolonies:blockhutcook"], "rewards": ["item:farmersdelight:beef_stew*8", "xp:80"], "skill": 60,
        },
        {
            "key": "forester", "title": "Найнятий лісоруб", "icon": "minecolonies:blockhutlumberjack", "deps": ["builder"],
            "desc": [
                "&aМайстерня лісоруба&r: лісоруб сам рубає дерева й одразу садить нові саджанці. &aІнструментом лісника&r позначте ділянку лісу, де йому працювати.",
                "Лісоруб уміє рубати й дерева модів, а великі дерева рубає цілком."
            ],
            "guide": {"tip": "дайте йому сокиру краще — він працюватиме швидше; дерева з лісорубні можна везти на лісопильню Create."},
            "tasks": ["item:minecolonies:blockhutlumberjack"], "rewards": ["item:minecraft:iron_axe", "xp:100"], "skill": 80,
        },
        {
            "key": "miner", "title": "Найнятий шахтар", "icon": "minecolonies:blockhutminer", "deps": ["builder"],
            "desc": [
                "&aШахта&r: шахтар копає вертикальний ствол і рівні з тунелями, приносить камінь і руди — зокрема руди Mekanism, Create і Ice and Fire. Що глибша шахта (вищий рівень будівлі), то багатша здобич.",
                "Йому потрібні кирка, лопата, дошки й смолоскипи — колонія сама попросить."
            ],
            "guide": {"tip": "шахту ставлять над твердим каменем, а не над печерою чи водою."},
            "tasks": ["item:minecolonies:blockhutminer"], "rewards": ["item:minecraft:iron_pickaxe", "item:minecraft:torch*64", "xp:100"], "skill": 80,
        },
        {
            "key": "farmer", "title": "Ферма й рибалка", "icon": "minecolonies:blockhutfarmer", "deps": ["homes"],
            "desc": [
                "&aФерма&r з полями: фермер сам сіє, поливає і збирає. &aРибальська хатина&r дає рибу; разом із їдальнею колонія годує себе сама."
            ],
            "tasks": ["item:minecolonies:blockhutfarmer", "item:minecolonies:blockhutfisherman"], "rewards": ["item:farmersdelight:cabbage_seeds*16", "item:farmersdelight:tomato_seeds*16", "xp:80"], "skill": 60,
        },
        {
            "key": "logistics", "title": "Склад і кур'єр", "icon": "minecolonies:blockhutwarehouse", "deps": ["farmer", "forester", "miner"],
            "desc": [
                "Коли будівель багато, ресурси треба розвозити. &aСклад&r збирає все добро колонії, а &aкур'єри&r носять його туди, де бракує."
            ],
            "guide": {"tip": "у склад можна скидати й власні ресурси: будівельник візьме звідти сам."},
            "tasks": ["item:minecolonies:blockhutwarehouse", "item:minecolonies:blockhutdeliveryman"], "rewards": ["loot:wilds:reward/tech", "xp:120"], "skill": 100,
        },
        {
            "key": "defence", "title": "Варта колонії", "icon": "minecolonies:blockhutguardtower", "deps": ["logistics"],
            "desc": [
                "Колонію раз на кілька тижнів атакують &cнабіги&r: варвари, пірати, амазонки, мумії, нормани. &aСторожові вежі&r і &aКазарми&r дають вартових — лицарів, лучників і друїдів.",
                "&aСкіпетр вартового&r показує вартовому, де стояти чи кого охороняти."
            ],
            "guide": {
                "danger": 4,
                "tip": "мури й ворота з кресленнями Сторожки затримують нападників, поки варта стріляє."
            },
            "tasks": ["item:minecolonies:blockhutguardtower", "kill:minecolonies:barbarian*5"], "rewards": ["loot:wilds:reward/boss", "xp:200"], "skill": 150,
        },
        {
            "key": "university", "title": "Університет", "icon": "minecolonies:blockhutuniversity", "deps": ["defence"],
            "desc": [
                "&aУніверситет&r відкриває дослідження: кращі інструменти поселенців, швидшу роботу, нові будівлі (кузня, плавильня, алхімік, лікарня).",
                "&aСтародавні книги&r з підземель потрібні для деяких досліджень."
            ],
            "tasks": ["item:minecolonies:blockhutuniversity"], "rewards": ["item:minecolonies:ancienttome", "loot:wilds:reward/magic", "xp:250"], "skill": 200,
        },
        {
            "key": "ruins", "title": "Покинута колонія", "icon": "minecolonies:scepterpermission", "deps": ["colony"],
            "desc": [
                "Світом розкидані покинуті колонії різних стилів: середньовічні, азійські, інкські, каледонські, колоніальні. У них — скрині й готові ідеї для вашого міста."
            ],
            "guide": {
                "where": "різні біоми — кожен стиль у своєму краї.",
                "danger": 2,
                "find": "Компас дослідника → «medieval_oak_colony», «asian_colony», «incan_colony» та інші.",
                "tip": "стиль будівель своєї колонії можна змінити в Ратуші."
            },
            "tasks": ["struct:minecolonies:medieval_oak_colony"], "rewards": ["loot:wilds:reward/explorer", "xp:120"], "skill": 80,
        },
        {
            "key": "falling", "title": "Рубати дерево цілком", "icon": "minecraft:diamond_axe", "deps": ["village"],
            "desc": [
                "Вам самим теж не треба лізти на кожне дерево: зрубайте &eнижню колоду сокирою&r — і все дерево впаде. Сокира зношується за кожну колоду.",
                "",
                " • &aприсядьте&r, щоб зрубати лише одну колоду;",
                " • зрубані будинки з колод без листя не падають — дерево має мати листя;",
                " • дерева понад 200 колод рубайте по частинах."
            ],
            "guide": {"tip": "з &aзачаруванням «Непорушність»&r сокира проживе довше; найкращі сокири — з драконячої сталі й кобольдського заліза."},
            "tasks": ["item:minecraft:oak_log*64"], "rewards": ["item:minecraft:iron_axe", "xp:40"], "skill": 30,
        },
    ],
}
