CHAPTER = {
    "key": "links",
    "title": "&9Зв'язки світу",
    "subtitle": "Як моди цієї збірки доповнюють один одного",
    "group": "Початок",
    "icon": "minecraft:chain",
    "quests": [
        {
            "key": "intro", "title": "Один світ, а не сто модів", "icon": "minecraft:chain", "size": 1.5,
            "desc": [
                "У цій збірці моди не живуть окремо. Однакові ресурси — це &eодин і той самий предмет&r, а здобич одного мода корисна в машинах і ритуалах іншого.",
                "",
                "У JEI шукайте рецепти з префіксом &awilds&r — це зв'язки, додані збіркою."
            ],
            "tasks": ["check"], "rewards": ["xp:20"], "skill": 20,
        },
        {
            "key": "ores", "title": "Спільні руди", "icon": "mekanism:ingot_uranium", "deps": ["intro"],
            "desc": [
                "Уран Mekanism і уран Alex's Caves, сірка, мідні самородки, пил алмазу й обсидіану — у збірці це &eодні предмети&r. Руди й лут автоматично стають «головною» версією, а рецепти всіх модів її приймають."
            ],
            "tasks": ["check"], "rewards": ["xp:20"],
        },
        {
            "key": "bones", "title": "Стародавні кістки", "icon": "alexscaves:heavy_bone", "deps": ["intro"],
            "desc": [
                "&aКістки дракона&r (Ice and Fire) і &aтяжкі кістки динозаврів&r (Alex's Caves) — взаємозамінні в рецептах обох модів.",
                "Жорна Create і камера збагачення Mekanism перемелюють їх на 9–12 кісткового борошна."
            ],
            "tasks": ["item:alexscaves:heavy_bone"], "rewards": ["item:minecraft:bone_meal*16"], "skill": 40,
        },
        {
            "key": "hides", "title": "Шкури звірів", "icon": "alexscaves:tough_hide", "deps": ["intro"],
            "desc": [
                "Жорстка шкіра динозаврів і шкури тролів годяться &eзамість шкіри&r — на ранці, намети, книги. Прес Create робить з однієї жорсткої шкіри три звичайні."
            ],
            "tasks": ["item:alexscaves:tough_hide"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "blood", "title": "Вогняна кров", "icon": "twilightforest:fiery_blood", "deps": ["intro"],
            "desc": [
                "Кров гідри із Сутінкового лісу і &aкров вогняного дракона&r — один реагент. Вогняні злитки Сутінкового лісу можна зробити з драконячої крові."
            ],
            "tasks": ["check"], "rewards": ["xp:30"],
        },
        {
            "key": "magic", "title": "Одна магія", "icon": "ars_nouveau:source_gem", "deps": ["intro"],
            "desc": [
                "Ars Nouveau й Iron's Spells мають &eспільну ману&r. Камера насичення Ars робить з аметисту магічну есенцію Iron's, а з есенції — джерельний самоцвіт.",
                "Серце дракона чи гідри замінює вісім рун у сфері покращення мани."
            ],
            "tasks": ["check"], "rewards": ["xp:30"],
        },
        {
            "key": "machines", "title": "Машини і звірі", "icon": "create:crushing_wheel", "deps": ["intro"],
            "desc": [
                "Дробильні колеса Create розмелюють &eдраконячу луску&r на золото й досвід. Mekanism збагачує &eбурштин&r до слизу. Crafts & Additions переводить обертання Create в енергію Mekanism і AE2, а Applied Mekanistics зберігає хімікати Mekanism у мережі AE2."
            ],
            "tasks": ["check"], "rewards": ["xp:30"],
        },
    ],
}
