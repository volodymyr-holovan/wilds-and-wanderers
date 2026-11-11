---
mod: createaddition
for: createaddition:rolling_mill, createaddition:tesla_coil, createaddition:copper_wire, createaddition:iron_wire, createaddition:gold_wire, createaddition:electrum_wire, createaddition:copper_rod, createaddition:iron_rod, createaddition:gold_rod, createaddition:electrum_rod, createaddition:brass_rod, createaddition:electrum_ingot, createaddition:electrum_sheet, createaddition:electrum_nugget, createaddition:straw, createaddition:seed_oil_bucket, createaddition:bioethanol_bucket, createaddition:biomass, createaddition:biomass_pellet
title: Прокатний стан, котушка Тесли й рідке паливо
summary: Дріт і стрижні з прокатного стану, електрум із котушки Тесли, пальник пломеня на рідкому паливі.
keywords: прокатний стан, дріт, стрижень, котушка тесли, заряджання, електрум, соломинка, олія, біоетанол, біомаса, rolling mill, tesla coil, charging, electrum, straw, bioethanol, biomass
category: tech
---
# Прокатний стан
- [[createaddition:rolling_mill]] крутиться від обертання. Злиток дає 2 стрижні, пластина — 2 дроти.
- Вручну: киньте злитки чи пластини зверху на стан, а готове заберіть ПКМ. Для автоматизації — стрічка й дві лійки (на вхід і на вихід).
!recipe createaddition:rolling_mill

# Котушка Тесли
- [[createaddition:tesla_coil]] живиться енергією FE і **заряджає предмети під собою**: будь-які речі з батареєю, а ще перетворює золото на **електрум** (злиток, пластину, дріт, стрижень, самородок) і [[ae2:certus_quartz_crystal]] на заряджений.
- З сигналом редстоуну вона б'є струмом усіх істот поруч і накладає ефект «Шокований» (сильно сповільнює). Обережно біля баз.

# Рідке паливо для пальника
- Клацніть [[createaddition:straw]] по пальнику пломеня — тепер він приймає рідке паливо з відра або по трубах.
- Лава гріє пальник, а **біоетанол** розпалює його до перегріву.
- Ланцюжок: насіння в чаші під пресом → [[createaddition:seed_oil_bucket|олія]]; рослини, листя чи саджанці з олією в підігрітому змішувачі → [[createaddition:biomass]]; цукор, [[create:cinder_flour]] і біомаса в змішувачі → [[createaddition:bioethanol_bucket|біоетанол]].
- З біомаси під пресом виходять [[createaddition:biomass_pellet]] — тверде паливо.
