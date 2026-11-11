---
mod: create_enchantment_industry
for: create_enchantment_industry:blaze_enchanter, create_enchantment_industry:blaze_forger, create_enchantment_industry:enchanting_template, create_enchantment_industry:super_enchanting_template, create_enchantment_industry:super_experience_block, create_enchantment_industry:super_experience_nugget, create_enchantment_industry:experience_cake, create_enchantment_industry:experience_cake_base, create_enchantment_industry:experience_cake_slice, create_dragons_plus:blaze_upgrade_smithing_template
title: Пломеневий зачарувальник і кузня
summary: Зачарування й ковадло на рідкому досвіді: шаблони, рівень зачарування, суперзачарування.
keywords: пломеневий зачарувальник, пломенева кузня, шаблон зачарування, суперзачарування, торт зачарування, ковальський шаблон, blaze enchanter, blaze forger, enchanting template, super enchanting
category: tech
---
# Як їх зробити
Обидві машини — це [[create:blaze_burner]], покращений на ковальському столі:
- [[create:blaze_burner]] + [[minecraft:enchanting_table]] + [[create_dragons_plus:blaze_upgrade_smithing_template]] → [[create_enchantment_industry:blaze_enchanter]].
- [[create:blaze_burner]] + ковадло + той самий шаблон → [[create_enchantment_industry:blaze_forger]].
- Перший **ковальський шаблон** шукайте в скринях бастіонів і незерських фортець. Далі його копіюють у верстаті: шаблон, пекельний камінь і 7 стрижнів іфрита дають 2 шаблони.

# Пломеневий зачарувальник
Працює як стіл зачарувань, але на **рідкому досвіді** замість ваших рівнів.
- Подайте в нього рідкий досвід трубою.
- На панелі задайте **рівень зачарування** (у звичайного столу межа — 30). Вищий рівень — не завжди краще: деякі зачарування на високому рівні не випадають.
- Панель — це ще й слот-фільтр. Покладіть туди предмет-зразок (наприклад, алмазний нагрудник), і зачарувальник накладатиме на [[create_enchantment_industry:enchanting_template]] лише ті чари, що підходять цьому предмету.
- Предмети подає й забирає [[create:mechanical_arm]]; вона ж може підкидати паливо з досвідом.

# Пломенева кузня
Працює як ковадло, але **без ціни ремонту**, яка росте з кожним разом.
- Об'єднує чари однакових предметів і шаблонів.
- **Накладає шаблон зачарування** на предмет.
- Знімає чари з речі, книги чи шаблону на порожній шаблон.

# Суперзачарування
- Нагодуйте зачарувальник чи кузню [[create_enchantment_industry:experience_cake]] — пломінь закипить. Цей «другий шлунок» трубою не наповнити, лише тортом.
- У цьому режимі межа рівнів зачарування значно вища, з'являються скарбові чари, а кузня може поєднати навіть несумісні чари й перевищити звичайні межі рівнів.
- Але працюють вони тоді лише з [[create_enchantment_industry:super_enchanting_template]]: його пресують з [[create_enchantment_industry:super_experience_block]].
- Блок супердосвіду виходить, коли блискавка вдаряє в [[create:experience_block]]. Звичайна гроза робить це не завжди, а блискавка від суперзачарування — завжди.
>! Суперзачарування кличе блискавки. Поставте поруч громовідвід. Якщо накрити машину блоком, блискавок не буде, зате на предмет може лягти прокляття (у кузні — з'явиться ціна ремонту).

> Торт зачарування: основу (яйце, цукор, лазурит) пресують у чаші, а потім дозатор доливає 1000 мБ рідкого досвіду.
