---
mod: sophisticatedbackpacks
title: Покращення ранця
for: sophisticatedbackpacks:upgrade_base, sophisticatedbackpacks:pickup_upgrade, sophisticatedbackpacks:advanced_pickup_upgrade, sophisticatedbackpacks:magnet_upgrade, sophisticatedbackpacks:advanced_magnet_upgrade, sophisticatedbackpacks:filter_upgrade, sophisticatedbackpacks:advanced_filter_upgrade, sophisticatedbackpacks:feeding_upgrade, sophisticatedbackpacks:advanced_feeding_upgrade, sophisticatedbackpacks:compacting_upgrade, sophisticatedbackpacks:advanced_compacting_upgrade, sophisticatedbackpacks:void_upgrade, sophisticatedbackpacks:advanced_void_upgrade, sophisticatedbackpacks:restock_upgrade, sophisticatedbackpacks:advanced_restock_upgrade, sophisticatedbackpacks:deposit_upgrade, sophisticatedbackpacks:advanced_deposit_upgrade, sophisticatedbackpacks:refill_upgrade, sophisticatedbackpacks:advanced_refill_upgrade, sophisticatedbackpacks:inception_upgrade, sophisticatedbackpacks:everlasting_upgrade, sophisticatedbackpacks:smelting_upgrade, sophisticatedbackpacks:auto_smelting_upgrade, sophisticatedbackpacks:smoking_upgrade, sophisticatedbackpacks:auto_smoking_upgrade, sophisticatedbackpacks:blasting_upgrade, sophisticatedbackpacks:auto_blasting_upgrade, sophisticatedbackpacks:crafting_upgrade, sophisticatedbackpacks:stonecutter_upgrade, sophisticatedbackpacks:jukebox_upgrade, sophisticatedbackpacks:advanced_jukebox_upgrade, sophisticatedbackpacks:stack_upgrade_starter_tier, sophisticatedbackpacks:stack_upgrade_tier_1, sophisticatedbackpacks:stack_upgrade_tier_2, sophisticatedbackpacks:stack_upgrade_tier_3, sophisticatedbackpacks:stack_upgrade_tier_4, sophisticatedbackpacks:stack_upgrade_omega_tier, sophisticatedbackpacks:stack_downgrade_tier_1, sophisticatedbackpacks:stack_downgrade_tier_2, sophisticatedbackpacks:stack_downgrade_tier_3, sophisticatedbackpacks:battery_upgrade, sophisticatedbackpacks:tank_upgrade, sophisticatedbackpacks:pump_upgrade, sophisticatedbackpacks:advanced_pump_upgrade, sophisticatedbackpacks:xp_pump_upgrade, sophisticatedbackpacks:tool_swapper_upgrade, sophisticatedbackpacks:advanced_tool_swapper_upgrade, sophisticatedbackpacks:anvil_upgrade, sophisticatedbackpacks:smithing_upgrade, sophisticatedbackpacks:alchemy_upgrade, sophisticatedbackpacks:advanced_alchemy_upgrade, sophisticatedbackpacks:mob_catcher_upgrade, sophisticatedbackpacks:advanced_mob_catcher_upgrade
category: tech
keywords: покращення ранця, апгрейд ранця, магніт, авто-збір, ущільнення, поглинання, годування, стос, вічність, швидкі руки, backpack upgrade, magnet upgrade, pickup upgrade, compacting, void upgrade, feeding upgrade, stack upgrade, everlasting
summary: Що робить кожне покращення ранця і з чого почати.
---
Покращення кладуть у слоти зліва від вікна ранця. Більшість має власну вкладку з налаштуваннями (фільтр: що брати, а що ні). Усі вони починаються з [[sophisticatedbackpacks:upgrade_base]]. **Просунуте** покращення робиться з простого і має більше комірок фільтра; налаштування при цьому не губляться.

!recipe sophisticatedbackpacks:upgrade_base

# Збирати й сортувати
- [[sophisticatedbackpacks:pickup_upgrade]] — підібрані предмети одразу летять у ранець, а не в інвентар.
- [[sophisticatedbackpacks:magnet_upgrade]] — притягує предмети навколо: простий за 3 блоки, просунутий — за 5.
- [[sophisticatedbackpacks:compacting_upgrade]] — пресує речі в блоки (9 злитків → блок). Простий уміє лише рецепти 2×2, просунутий — і 3×3.
- [[sophisticatedbackpacks:void_upgrade]] — знищує предмети з фільтра (наприклад, зайвий кругляк і землю).
- [[sophisticatedbackpacks:filter_upgrade]] — вирішує, що лійки й труби можуть класти в ранець і забирати з нього.

# Допомагати в дорозі
- [[sophisticatedbackpacks:feeding_upgrade]] — сам годує вас їжею з ранця, коли ви голодні.
- [[sophisticatedbackpacks:refill_upgrade]] — підкладає в інвентар нові предмети з ранця замість витрачених (смолоскипи, блоки). Просунуте дозволяє обрати слот і брати блоки з ранця середньою кнопкою миші.
- [[sophisticatedbackpacks:tool_swapper_upgrade]] — коли б'єте блок чи істоту, саме бере з ранця потрібний інструмент.
- [[sophisticatedbackpacks:alchemy_upgrade]] — сам п'є зілля, їсть їжу з ефектами або відро молока.
- [[sophisticatedbackpacks:everlasting_upgrade]] — ранець не горить, не зникає і не падає в порожнечу. Корисно, якщо ви часто помираєте в Енді чи лаві.

# Скриня поруч
Shift + ПКМ ранцем у руці по скрині:
- [[sophisticatedbackpacks:deposit_upgrade]] — **викладає** речі з ранця в скриню;
- [[sophisticatedbackpacks:restock_upgrade]] — **поповнює** ранець зі скрині.

# Майстерні у вкладці
Окрема вкладка з робочим місцем просто в ранці: [[sophisticatedbackpacks:crafting_upgrade]] (верстак), [[sophisticatedbackpacks:stonecutter_upgrade]], [[sophisticatedbackpacks:smithing_upgrade]], [[sophisticatedbackpacks:anvil_upgrade]], піч, коптильня й плавильня. Варіанти «Авто» самі беруть сировину з ранця й кладуть туди готове. Піч чи коптильня може бути лише одна на ранець.

# Більше місця
- **Покращення «Стос»** збільшує, скільки предметів влазить у кожну комірку: [[sophisticatedbackpacks:stack_upgrade_starter_tier]] — у 1,5 раза, рівні 1–4 — у 2, 4, 8 і 16 разів. Кожен наступний рівень робиться з попереднього й блоків заліза, золота, діаманту, незериту. У ранець влазить не більше трьох таких покращень.
- **Погіршення «Стос»** навпаки зменшує місце в комірці — для особливих сортувальних схем.
- [[sophisticatedbackpacks:inception_upgrade]] дозволяє класти ранці в ранець.

# Рідини, досвід, енергія
[[sophisticatedbackpacks:tank_upgrade]] замінює частину комірок баком для рідини, [[sophisticatedbackpacks:battery_upgrade]] — накопичувачем енергії. Помпа переливає рідину між баком і сусідніми блоками, а [[sophisticatedbackpacks:xp_pump_upgrade]] зберігає ваш досвід у баку й повертає його.

# Інше
- [[sophisticatedbackpacks:jukebox_upgrade]] — програвач платівок у ранці (один на ранець).
- [[sophisticatedbackpacks:mob_catcher_upgrade]] — Shift + ПКМ ловить мирну тварину в ранець; просунутий ловить і ворожих.

> Клавіші для покращень (увімкнути/вимкнути покращення в слоті, запустити викладання) є в налаштуваннях керування, розділ Sophisticated Backpacks.
