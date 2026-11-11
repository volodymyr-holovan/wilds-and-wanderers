---
mod: createaddition
for: createaddition:alternator, createaddition:electric_motor, createaddition:connector, createaddition:large_connector, createaddition:small_light_connector, createaddition:copper_spool, createaddition:gold_spool, createaddition:electrum_spool, createaddition:spool, createaddition:festive_spool, createaddition:modular_accumulator, createaddition:redstone_relay, createaddition:portable_energy_interface, createaddition:capacitor
title: Електрика Create: генератор, двигун, дроти
summary: Як перетворити обертання Create на енергію FE і назад, протягнути дроти й зберегти енергію в акумуляторі.
keywords: електрика, енергія, FE, генератор змінного струму, альтернатор, електродвигун, з'єднувач, конектор, дріт, котушка, акумулятор, alternator, electric motor, connector, wire, spool, accumulator, crafts and additions
category: tech
---
# Електрика Create (Crafts & Additions)
Мод з'єднує обертання Create з **енергією FE** — тією, якою живляться машини Mekanism, мережа AE2 та інші моди.

# Обертання → енергія
- [[createaddition:alternator]] бере обертання з вала й дає енергію. Йому треба **щонайменше 32 об/хв**; що швидше обертання, то більше енергії — на 256 об/хв це близько 360 FE за такт.
- Він теж навантажує мережу Create, тож перевірте запас потужності [[create:goggles]].
!recipe createaddition:alternator

# Енергія → обертання
- [[createaddition:electric_motor]] робить навпаки: з енергії дає обертання до 256 об/хв.
- Швидкість задають, прокручуючи колесо миші над задньою панеллю двигуна. Що вища швидкість, то більше він їсть (на 256 об/хв — близько 480 FE за такт, найменше — 8).

# Дроти
- Поставте [[createaddition:connector]] на машину чи генератор. Візьміть котушку з дротом і клацніть ПКМ по двох з'єднувачах — між ними протягнеться дріт, а котушка не витрачається.
- **Малий з'єднувач**: до 4 дротів, до 16 блоків завдовжки, до 1000 FE за такт. Підходить будь-яка котушка: [[createaddition:copper_spool]], [[createaddition:gold_spool]] чи [[createaddition:electrum_spool]].
- **Великий з'єднувач** ([[createaddition:large_connector]]): до 6 дротів, до 32 блоків, до 5000 FE за такт. Потрібна котушка **золотого або електрумного** дроту — мідний сюди не чіпляється.
- Клацніть по з'єднувачу [[create:wrench]], щоб змінити режим: «Віддавати», «Забирати» або «Нічого».
- Зняти дріт: клацніть ПКМ [[createaddition:spool]] по двох з'єднаних з'єднувачах — дріт повернеться на котушку.
- Дріт роблять у [[createaddition:rolling_mill]] з пластин; з порожньої котушки й 4 дротів виходить повна котушка.

# Як підключити Mekanism та інші моди
- З'єднувач передає енергію будь-якому блоку, що працює з FE. Поставте його прямо на машину Mekanism, акумулятор чи енергокуб — і протягніть дріт до генератора.
- Можна й без дротів: генератор віддає енергію сусіднім блокам, тож кабель Mekanism, прикладений до нього, теж працює. Докладно про кабелі — квест [[wilds_wiki:quest_guides/mekanism_energy|«Перша енергія»]].

# Акумулятор
- [[createaddition:modular_accumulator]] — багатоблочне сховище енергії: ставте блоки поруч, до 3×3 у ширину й до 5 у висоту. Кожен блок вміщує 2 000 000 FE.
- На нього ставлять з'єднувачі: один на вхід, другий на вихід (режими — ключем). Блокам, що стоять зверху й знизу, акумулятор сам віддає енергію.
- [[createaddition:redstone_relay]] пропускає енергію з входу на вихід лише тоді, коли на нього подано сигнал редстоуну.
- [[createaddition:portable_energy_interface]] заряджає акумулятори на рухомих приладинах: поставте два інтерфейси — на приладині й на місці — з проміжком 1–2 блоки, і коли вони зустрінуться, енергія перейде.

> Наведіть мишу на будь-який блок мода й утримуйте W: гра покаже анімовану інструкцію.
