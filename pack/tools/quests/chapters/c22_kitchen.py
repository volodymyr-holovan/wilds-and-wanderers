"""Кухня: Farmer's Delight from the first wild cabbage to a feast on the table (the knife, the board and the pot are in
Виживання)."""
CHAPTER = {
    "key": "kitchen",
    "title": "&6Кухня",
    "subtitle": "Город, плита, святковий стіл",
    "group": "Поселення",
    "icon": "farmersdelight:cooking_pot",
    "quests": [
        {
            "key": "garden", "title": "Дикий город", "icon": "farmersdelight:cabbage", "size": 1.5, "shape": "hexagon",
            "subtitle": "Капуста, томати, цибуля",
            "desc": [
                "У природі ростуть &aдикі городні рослини&r: капуста біля моря, томати в посушливих місцях, цибуля на луках, картопля й морква в лісах.",
                "Зламайте дику рослину — отримаєте насіння й урожай. Посадіть на грядці біля дому, і город годуватиме вас усю гру."
            ],
            "tasks": ["item:farmersdelight:cabbage*4", "item:farmersdelight:tomato*4", "item:farmersdelight:onion*4"],
            "rewards": ["item:farmersdelight:rich_soil*4", "xp:30"], "skill": 30,
        },
        {
            "key": "rice", "title": "Рис росте у воді", "icon": "farmersdelight:rice", "deps": ["garden"],
            "desc": [
                "&aДикий рис&r шукайте на мілководді в болотах і біля річок.",
                "Садіть рис у воду глибиною в один блок, на землю під нею. Стиглий рис дає зерно й солому."
            ],
            "tasks": ["item:farmersdelight:rice*8"], "rewards": ["item:farmersdelight:rice_bale", "xp:30"], "skill": 30,
        },
        {
            "key": "compost", "title": "Багатий ґрунт", "icon": "farmersdelight:rich_soil", "deps": ["garden"],
            "desc": [
                "&aОрганічний компост&r складається з соломи, гнилих томатів, кісткового борошна й землі. Поставлений на землю, він згодом стає &aбагатим ґрунтом&r.",
                "На багатому ґрунті рослини ростуть швидше, а гриби на ньому розростаються в колонії."
            ],
            "tasks": ["item:farmersdelight:organic_compost*2"], "rewards": ["item:minecraft:bone_meal*16", "xp:40"], "skill": 30,
        },
        {
            "key": "sharp", "title": "Гострий ніж", "icon": "farmersdelight:iron_knife", "deps": ["garden"],
            "desc": [
                "Залізний ніж міцніший за кремінний і швидше ріже на дошці. Ножем зручно збирати й траву: з неї частіше падає солома.",
                "Ножі — це ще й зброя: швидкі, хоч і слабші за меч."
            ],
            "tasks": ["item:farmersdelight:iron_knife"], "rewards": ["xp:25"], "skill": 20,
        },
        {
            "key": "cuts", "title": "Нарізка", "icon": "farmersdelight:bacon", "deps": ["sharp"],
            "desc": [
                "Покладіть продукт на &aнарізну дошку&r і клацніть по ній ножем. Капуста дає листя, шматок свинини — бекон, курка — шматочки.",
                "Порізане м'ясо смажиться швидше й стає основою для страв."
            ],
            "tasks": ["item:farmersdelight:cabbage_leaf*4", "item:farmersdelight:bacon*2"], "rewards": ["item:farmersdelight:cooked_bacon*4", "xp:30"], "skill": 30,
        },
        {
            "key": "dough", "title": "Тісто й макарони", "icon": "farmersdelight:wheat_dough", "deps": ["cuts"],
            "desc": [
                "&aТісто&r — пшениця з водою (або з яйцем). З тіста на дошці виходять &aсирі макарони&r, а в печі — хліб."
            ],
            "tasks": ["item:farmersdelight:wheat_dough*4", "item:farmersdelight:raw_pasta*2"], "rewards": ["item:minecraft:egg*4", "xp:30"], "skill": 30,
        },
        {
            "key": "soup", "title": "Перший суп", "icon": "farmersdelight:vegetable_soup", "deps": ["cuts"],
            "desc": [
                "Каструля стоїть на плиті або над вогнем. Покладіть у неї до шести інгредієнтів і миску — за хвилину суп готовий.",
                "Супи й рагу дають ефект &aситості&r: голод не росте, і ви повільно загоюєтеся."
            ],
            "tasks": ["item:farmersdelight:vegetable_soup*2"], "rewards": ["item:minecraft:bowl*8", "xp:40"], "skill": 40,
        },
        {
            "key": "skillet", "title": "Сковорідка", "icon": "farmersdelight:skillet", "deps": ["cuts"],
            "desc": [
                "Сковорідку ставлять на плиту й смажать по одному продукту. А в руці вона — важка зброя.",
                "Спробуйте підсмажити яйце."
            ],
            "tasks": ["item:farmersdelight:skillet", "item:farmersdelight:fried_egg*2"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "pie", "title": "Пиріг", "icon": "farmersdelight:apple_pie", "deps": ["dough"],
            "desc": [
                "Спершу спечіть із тіста &aскоринку для пирога&r, а тоді додайте яблука, пшеницю й цукор. Поставте пиріг на стіл: від нього відрізають шматки, кожен ситніший за яблуко."
            ],
            "tasks": ["item:farmersdelight:apple_pie"], "rewards": ["item:minecraft:sugar*16", "xp:40"], "skill": 30,
        },
        {
            "key": "drinks", "title": "Гарячі напої", "icon": "farmersdelight:hot_cocoa", "deps": ["soup"],
            "desc": [
                "Какао з молоком і цукром вариться в каструлі й гріє в холодні вечори. Яблучний сидр і кавуновий сік — для літа."
            ],
            "tasks": ["item:farmersdelight:hot_cocoa", "item:farmersdelight:apple_cider"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "pets", "title": "Миска для улюбленця", "icon": "farmersdelight:dog_food", "deps": ["soup"],
            "desc": [
                "&aЇжа для собак&r лікує й посилює прирученого вовка, а &aкорм для коней&r — коня, віслюка чи лами. Нагодуйте їх перед мандрівкою."
            ],
            "tasks": ["item:farmersdelight:dog_food", "item:farmersdelight:horse_feed"], "rewards": ["item:minecraft:lead*2", "xp:40"], "skill": 30,
        },
        {
            "key": "feast", "title": "Святковий стіл", "icon": "farmersdelight:roast_chicken_block", "deps": ["pie", "drinks"], "size": 1.5, "shape": "gear",
            "subtitle": "Страва на всю компанію",
            "desc": [
                "Великі страви — запечену курку, глазуровану шинку, пастуший пиріг — ставлять на стіл. Клацніть по страві з мискою в руці, щоб взяти порцію.",
                "Одна страва нагодує всю вашу групу після походу."
            ],
            "tasks": ["item:farmersdelight:roast_chicken_block"], "rewards": ["loot:wilds:reward/explorer", "xp:120"], "skill": 80,
        },
    ],
}
