---
mod: aeronautics
for: aeronautics:propeller_bearing, aeronautics:gyroscopic_propeller_bearing, aeronautics:smart_propeller, aeronautics:wooden_propeller, aeronautics:andesite_propeller, aeronautics:adjustable_burner, aeronautics:steam_vent, aeronautics:white_envelope, aeronautics:white_envelope_encased_shaft, aeronautics:levitite, aeronautics:pearlescent_levitite, aeronautics:levitite_blend_bucket, aeronautics:end_stone_powder, aeronautics:aviators_goggles, aeronautics:mounted_potato_cannon
title: Перший дирижабль (Aeronautics)
summary: Аеростат, пальник, пропелери й левітит: що потрібно, щоб приладина піднялася й полетіла.
keywords: дирижабль, повітряна куля, аеростат, оболонка, пальник аеростата, паровий отвір, пропелер, вальниця пропелера, левітит, політ, airship, balloon, envelope, hot air burner, propeller bearing, levitite, aeronautics
category: tech
---
# Перший дирижабль
Літальний апарат — це [[wilds_wiki:guides/aeronautics/simulated_contraptions|фізична приладина]], якій дали **підйомну силу** й **тягу**. Збирають його фізичним збирачем, як і все в Simulated.

# Підйом: аеростат
- Збудуйте з [[aeronautics:white_envelope]] (вовна й палиці) замкнену оболонку — вона тримає гаряче повітря. Пофарбувати її можна барвником (ПКМ).
- Повітря не має куди витікати: оболонка мусить бути **герметичною**. Вал, що проходить крізь неї, обкладіть оболонкою — вийде облицьований оболонкою вал.
- Блоки всередині кулі забирають її об'єм.
- Під оболонку поставте [[aeronautics:adjustable_burner]]. Із сигналом редстоуну він поступово наповнює кулю над собою. На панелі задають найбільший потік, а сила сигналу масштабує його — так регулюють висоту.
- Кілька пальників можуть гріти одну кулю. Підйомна сила обмежена розміром кулі.
- Замість пальника можна взяти [[aeronautics:steam_vent]]: він наповнює кулю парою і, як паровий двигун Create, потребує тепла, води й котла.
- Що вище, то рідше повітря: підйом слабшає, і апарат зависає там, де підйом дорівнює вазі. Точний тиск показує [[simulated:altitude_sensor]].

# Рух: пропелери
- [[aeronautics:propeller_bearing]] чіпляється до блока перед собою. Будь-яка конструкція з **щонайменше двома** вітрилами чи схожими блоками на ній — це пропелер; що більше вітрил, то сильніша тяга.
- Обертання дає тягу; зміна напрямку обертання (або гайковий ключ) розвертає тягу назад.
- [[aeronautics:wooden_propeller]] і [[aeronautics:andesite_propeller]] — готові малі пропелери. [[aeronautics:smart_propeller]] сам повертається лопатями догори.
- Для гелікоптера беріть [[aeronautics:gyroscopic_propeller_bearing]]: він тримає гвинт рівно. Сигнал редстоуну скидає його нахил.
- Тягу й потік повітря видно в [[create:goggles]] або [[aeronautics:aviators_goggles]].

# Левітит
- Достатньо [[aeronautics:levitite]] на приладині тримає її в повітрі, але **підняти** її сам не може — потрібна ще якась сила. На малій швидкості левітит гальмує рух, на великій — менше.
- Як зробити: дробильні колеса мелють камінь Енду на [[aeronautics:end_stone_powder]]; у підігрітому змішувачі 4 порошку, 2 цинкові самородки й вода дають левітитову суміш. Вилита суміш від тепла кристалізується в левітит і розповзається по всій рідині. Деякі блоки форми при цьому руйнуються, тож нею можна відливати фігури.
>! Кристалізований левітит назад у суміш не зібрати.

> Усе про кожен блок — в анімованих інструкціях: наведіть мишу на блок і утримуйте W.
