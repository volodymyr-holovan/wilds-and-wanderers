"""Generates i18n/batches/b18_npc_storage.json: Easy NPC (entity names from vanilla uk_ua) and Sophisticated Storage."""
import json, os, re

HERE = os.path.dirname(__file__)
I18N = os.path.join(HERE, "..", "..", "i18n")
load = lambda p: json.load(open(os.path.join(I18N, p), encoding="utf-8"))
VAN = load("ref/minecraft.json")
NPC_SRC, SS_SRC = load("src/easy_npc.json"), load("src/sophisticatedstorage.json")
OUT = os.path.join(I18N, "batches", "b18_npc_storage.json")

# ---------- Easy NPC ----------
EXTRA_ENT = {"cobblemon_npc": "Cobblemon", "doppler": "Двійник", "easy_model_entities_npc": "NPC Easy Model", "fairy": "Фея",
             "humanoid": "Гуманоїд", "humanoid_slim": "Гуманоїд (тонкий)", "orc": "Орк", "orc_warrior": "Орк-воїн",
             "skeleton_bogged": VAN.get("entity.minecraft.bogged", "Болотяник")}


def ent(name):
    if name in EXTRA_ENT:
        return EXTRA_ENT[name]
    return VAN.get("entity.minecraft." + name, name)


npc = {}
for k in NPC_SRC:
    m = re.match(r"entity\.easy_npc\.epic_fight_([a-z_]+)_raw$", k)
    if m:
        npc[k] = "[EF] " + ent(m.group(1)); continue
    m = re.match(r"entity\.easy_npc\.([a-z_]+)$", k)
    if m:
        npc[k] = ent(m.group(1))
NPC_TIPS = {
    "allay": "Веселий маленький помічник: танцює під музику й збирає предмети. Напрочуд надійний як для когось без кишень.",
    "cat": "Незалежний, осудливий і дивно заспокійливий. Ігнорує команди, якщо не в настрої.",
    "cave_spider": "Менший за павука, але вдвічі зухваліший. Ідеальний для тісних місць і напружених розмов.",
    "chicken": "Багато кудкудакає, мало думає. Для скромних NPC з простими завданнями.",
    "cobblemon_npc": "NPC на основі Cobblemon, що може набути вигляду будь-якого виду Cobblemon. Квест має бути в кожного!",
    "creeper": "Напрочуд балакучий як для того, хто зазвичай просто вибухає. Поводьтеся обережно.",
    "doppler": "NPC-перевертень, що може набути будь-якої моделі. Це птах? Зомбі? Це те, що вам потрібно.",
    "drowned": "Піднявся з глибин і має що сказати. Водонепроникний і напрочуд красномовний.",
    "easy_model_entities_npc": "NPC, що може набути вигляду будь-якої моделі Easy Model Entities. Для повністю власних NPC.",
    "enderman": "Високий, темний і загадковий. Уникає погляду в очі за будь-яку ціну — якщо його не дратувати.",
    "evoker": "Заклинач із твердими поглядами й звичкою викликати щось без запрошення.",
    "fairy": "Крихітна чарівна істота з великою енергією. Може виконати бажання. А може й ні.",
    "fox": "Хитра, спритна й зовсім ненадійна з ягодами. Чудовий матеріал для NPC.",
    "ghast": "Великий, ширяючий і глибоко емоційний. Не питайте, як він сюди потрапив.",
    "horse": "Шляхетний. Швидкий. Трохи осудливий. Гідно понесе ваші квести.",
    "humanoid": "Схожий на людину. Ходить як людина. Говорить як людина. Майже людина.",
    "humanoid_slim": "Як гуманоїд, але з кращою поставою й тоншим силуетом.",
    "husk": "Загартований пустелею зомбі, що забув пити воду. Досі має що сказати.",
    "illusioner": "Створює копії, гне реальність і все одно приходить на роботу. Вражає.",
    "iron_golem": "Створений для захисту, перепрофільований для розмов. Напрочуд лагідний, якщо познайомитися ближче.",
    "orc": "Міцний, гучний і напрочуд вправний у переговорах. Переважно про силу.",
    "orc_warrior": "Загартований у боях і гордий. Має багато думок про поняття честі.",
    "pig": "Кругла, рожева й з характером. Класичний вибір для скромних ролей.",
    "piglin": "Любить золото. Торгуватиме за золото. Не чіпай золото.",
    "piglin_brute": "Ваше золото його не цікавить — лише щоб ви пішли. Негайно.",
    "pillager": "Професійний розбійник з арбалетом і характером. Тепер бере квести.",
    "skeleton": "Кістлявий, з луком і дивно філософський уночі.",
    "bogged": "Скелет, просочений болотяною водою. Повільніший, м'якший і трохи смердючий.",
    "skeleton_horse": "Кінь, що пережив смерть і скаче далі. Моторошно, але надійно.",
    "slime": "Зелений, м'який і дивно оптимістичний. Відскакує від усього — буквально.",
    "spider": "Вісім очей, вісім ніг, одна мета. Напрочуд корисний, коли не на стелі.",
    "stray": "Крижаний скелет із тундри. Холодний зовні, холодний усередині, холодна розмова.",
    "vex": "Маленький, злий і проходить крізь стіни. Якось знаходить час поговорити.",
    "villager": "Опора будь-якого села NPC. Торгується завзято, скаржиться ще завзятіше.",
    "vindicator": "Сокирник із суворим моральним кодексом — переважно про сокири.",
    "witch": "Варить зілля, часом регоче й завжди має саме те, що потрібно, — за ціну.",
    "wither_skeleton": "Викуваний у Незері, говорить загадками. Носить меч і багато невирішених почуттів.",
    "wolf": "Вірний, лютий і найкращий компаньйон NPC, якого не купиш за гроші.",
    "zombie": "Човгає, стогне й іноді має сказати щось важливе. Переважно човгає.",
    "zombie_horse": "Нежить, смердючий і все одно швидший за ходьбу. Найкращий друг зомбі.",
    "zombie_villager": "Селянин переживає важкі часи. Терпінням його можна вилікувати — або просто дати квест.",
    "zombified_piglin": "Застряг між двома світами й цим незадоволений. Не дратуйте родину.",
}
for name, t in NPC_TIPS.items():
    npc["tooltip.easy_npc.spawn_egg." + name] = t
SPAWNER = {"boss_spawner": "босів", "default_spawner": "звичайний", "group_spawner": "груповий", "single_spawner": "одиночний", "world_spawner": "світовий"}
npc.update({
    "block.easy_npc.boss_spawner": "Породжувач босів EasyNPC", "block.easy_npc.default_spawner": "Звичайний породжувач EasyNPC",
    "block.easy_npc.group_spawner": "Груповий породжувач EasyNPC", "block.easy_npc.single_spawner": "Одиночний породжувач EasyNPC",
    "block.easy_npc.world_spawner": "Світовий породжувач EasyNPC",
    "tooltip.easy_npc.spawner.boss_spawner": "Особливий породжувач, що створює NPC-боса.",
    "tooltip.easy_npc.spawner.default_spawner": "Породжувач, що створює звичайного NPC.",
    "tooltip.easy_npc.spawner.group_spawner": "Породжувач, що створює групу NPC.",
    "tooltip.easy_npc.spawner.single_spawner": "Породжувач, що створює одного NPC.",
    "tooltip.easy_npc.spawner.world_spawner": "Груповий породжувач, що рахує населення свого шаблону по всьому світу (усіх вимірах).",
    "item.easy_npc.custom_data_test": "Предмет перевірки власних даних", "item.easy_npc.easy_npc_preset": "Шаблон NPC",
    "item.easy_npc.easy_npc_preset_empty": "Порожній шаблон NPC", "item.easy_npc.epic_fight_spawn_egg": "Яйце виклику Epic Fight: %s",
    "item.easy_npc.move_easy_npc": "Переміщення EasyNPC", "item.easy_npc.npc_coin": "Монета NPC", "item.easy_npc.npc_key": "Ключ NPC",
    "item.easy_npc.npc_red_token": "Червоний жетон NPC", "item.easy_npc.spawn_egg": "Яйце виклику NPC: %s",
    "itemGroup.easy_npc.config_items": "EasyNPC — предмети налаштування", "itemGroup.easy_npc.epic_fight_spawn_eggs": "EasyNPC — яйця Epic Fight",
    "itemGroup.easy_npc.integration_spawn_eggs": "EasyNPC — інтеграції", "itemGroup.easy_npc.spawn_eggs": "EasyNPC — яйця виклику",
    "message.easynpc.access.no_permission": "Вам не можна змінювати цього NPC.",
    "message.easynpc.access.npc_not_found": "Цього NPC не знайдено або його вже немає.",
    "text.easy_npc.armourers_workshop.no_support": "Модель скіна %1$s для %2$s ще офіційно не підтримується Armourer's Workshop! Використовуйте рекомендовані моделі, наприклад гуманоїда.",
    "text.easy_npc.cancel": "Скасувати", "text.easy_npc.copy": "Копіювати", "text.easy_npc.copy_uuid": "Копіювати UUID", "text.easy_npc.debug": "Налагодження",
    "text.easy_npc.delete": "Видалити", "text.easy_npc.execute_as_player": "Виконати від імені гравця",
    "text.easy_npc.faction.internal_dispute.0": "Вартові %s схрещують руки. Розбирайтеся самі.",
    "text.easy_npc.faction.internal_dispute.1": "%s не втручається. Ніхто не хоче заповнювати ці папери.",
    "text.easy_npc.faction.internal_dispute.2": "Вартові %s раптом дуже зацікавилися стелею.",
    "text.easy_npc.faction.internal_dispute.3": "%s оголосив це приватною справою й повернувся до роботи.",
    "text.easy_npc.invalid_remote_image": "Віддалене зображення недійсне або недоступне!\nПеревірте адресу й спробуйте ще раз.",
    "text.easy_npc.item.custom_data_test": "ПКМ — перевірити власні дані предмета.",
    "text.easy_npc.item.easy_npc_preset": "► ПКМ по вільному місцю — створити NPC.",
    "text.easy_npc.item.easy_npc_preset_empty": "► ПКМ по NPC — зберегти його налаштування як новий шаблон.",
    "text.easy_npc.item.easy_npc_preset.custom_name": "Ім'я: %s", "text.easy_npc.item.easy_npc_preset.entity_type": "Тип істоти: %s",
    "text.easy_npc.item.move_easy_npc": "Виберіть EasyNPC правою кнопкою миші й перемістіть його лівою кнопкою в потрібне місце.",
    "text.easy_npc.item.npc_coin": "Валюта NPC для квестів, перевірок, торгівлі й власних нагород.",
    "text.easy_npc.item.npc_key": "Ключ NPC для квестів, перевірок, контролю доступу й власних замків.",
    "text.easy_npc.item.npc_red_token": "Червоний жетон NPC для квестів, перевірок, торгівлі й власних нагород.",
    "text.easy_npc.dialog.button.duplicate_label": "Мітку кнопки %s уже використовує інша кнопка цього діалогу.",
    "text.easy_npc.dialog.button.locked": "Ви ще не відповідаєте вимогам цього варіанта.", "text.easy_npc.locked": "Заблоковано",
    "text.easy_npc.not_implemented": "Ще не реалізовано!",
    "text.easy_npc.preset.batch.cooldown": "Зачекайте ще %1$s с до наступного пакетного імпорту.",
    "text.easy_npc.preset.batch.entry": "- %1$s", "text.easy_npc.preset.batch.more": "- І ще шаблонів: %1$s.",
    "text.easy_npc.preset.batch.no_matches": "Жоден шаблон не відповідає взірцю \"%1$s\".",
    "text.easy_npc.preset.batch.pattern_invalid": "Взірець \"%1$s\" неправильний. Перед першою \"*\" має бути щонайменше три символи.",
    "text.easy_npc.preset.batch.preview": "Відповідних шаблонів: %1$s. Нових NPC: %2$s, наявних, які буде перезаписано: %3$s. Додайте \"confirm\", щоб виконати.",
    "text.easy_npc.preset.batch.preview_new": "Відповідних шаблонів: %1$s, буде створено нових NPC: %2$s. Додайте \"confirm\", щоб виконати.",
    "text.easy_npc.preset.batch.result": "Пакетний імпорт завершено: створено %1$s, оновлено %2$s, замінено %3$s, з помилкою %4$s.",
    "text.easy_npc.preset.batch.too_many": "Відповідних шаблонів: %1$s, але за один імпорт дозволено лише %2$s.",
    "text.easy_npc.preset.export_failed": "Не вдалося експортувати шаблон у \"%1$s\".",
    "text.easy_npc.preset.exported": "Експортовано \"%1$s\" разом з ID NPC %2$s і його розташуванням.",
    "text.easy_npc.preset.exported_hint": "«Імпорт» відновлює цього NPC, «Створити новий» і предмет шаблону створюють копію.",
    "text.easy_npc.preset.exported_without_identity": "Експортовано \"%1$s\" без ID NPC — імпорт завжди створюватиме нового NPC.",
    "text.easy_npc.preset.import_failed": "Не вдалося імпортувати шаблон \"%1$s\".",
    "text.easy_npc.preset.imported": "Імпортовано \"%1$s\" як NPC %2$s у %3$s.",
    "text.easy_npc.preset.imported_replaced": "Імпортовано \"%1$s\" із заміною наявного NPC %2$s іншого типу.",
    "text.easy_npc.preset.imported_updated": "Імпортовано \"%1$s\" у наявного NPC %2$s.",
    "text.easy_npc.preset.restore_denied": "Вам не можна відновлювати NPC з його збереженим ID і розташуванням.",
    "text.easy_npc.preset.sanitize.action_permission_clamped": "Рівні дозволів дій знижено.",
    "text.easy_npc.preset.sanitize.attribute_removed": "Частину атрибутів прибрано.", "text.easy_npc.preset.sanitize.block_action_removed": "Дії з блоками прибрано.",
    "text.easy_npc.preset.sanitize.command_action_removed": "Дії-команди прибрано.", "text.easy_npc.preset.sanitize.command_permission_clamped": "Рівні дозволів команд знижено.",
    "text.easy_npc.preset.sanitize.custom_action_removed": "Власні дії прибрано.", "text.easy_npc.preset.sanitize.entry": "- %1$s",
    "text.easy_npc.preset.sanitize.header": "Шаблон імпортовано, але деякі можливості обмежено правилами сервера.",
    "text.easy_npc.preset.sanitize.message_action_removed": "Дії-повідомлення прибрано.", "text.easy_npc.preset.sanitize.more": "- І ще змін: %1$s.",
    "text.easy_npc.preset.sanitize.movement_removed": "Дані руху прибрано.", "text.easy_npc.preset.sanitize.objective_removed": "Цілі прибрано.",
    "text.easy_npc.preset.sanitize.owner_removed": "Дані власника прибрано.", "text.easy_npc.preset.sanitize.owner_rewritten": "Дані власника оновлено.",
    "text.easy_npc.preset.sanitize.position_removed": "Дані розташування прибрано.", "text.easy_npc.preset.sanitize.scoreboard_action_removed": "Дії з таблицею рахунку прибрано.",
    "text.easy_npc.preset.sanitize.trading_action_removed": "Торгові дії прибрано.", "text.easy_npc.preset.sanitize.trading_removed": "Торгівлю прибрано.",
    "text.easy_npc.preset.spawn_denied": "Вам не можна створювати NPC із шаблонів.",
    "text.easy_npc.preset.spawn_rate_limited": "Досягнуто межі в %1$s NPC за хвилину, зачекайте трохи.",
    "text.easy_npc.preset.export_rate_limited": "Досягнуто межі в %1$s шаблонів за хвилину, зачекайте трохи.",
    "text.easy_npc.respawn": "Відродити", "text.easy_npc.save": "Зберегти", "text.easy_npc.trading": "Торгівля",
    "text.easy_npc.trading.busy": "%1$s зараз торгується з %2$s.\n Зачекайте, доки торгівля завершиться.",
    "text.easy_npc.type": "Тип", "text.easy_npc.value": "Значення",
})

# ---------- Sophisticated Storage ----------
MAT = {"wood": ("Дерев'яна", "Дерев'яна", "Дерев'яна"), "copper": ("Мідна", "Мідна", "Мідна"), "iron": ("Залізна", "Залізна", "Залізна"),
       "gold": ("Золота", "Золота", "Золота"), "diamond": ("Алмазна", "Алмазна", "Алмазна"), "netherite": ("Незеритова", "Незеритова", "Незеритова")}
ROMAN = {"1": "I", "2": "II", "3": "III", "4": "IV"}
UPG = {"stackUpgrade": "Збільшувач стосу", "compactingUpgrade": "Ущільнювач", "advancedCompactingUpgrade": "Просунутий ущільнювач",
       "feedingUpgrade": "Годувальник", "advancedFeedingUpgrade": "Просунутий годувальник", "filterUpgrade": "Фільтрувальник",
       "advancedFilterUpgrade": "Просунутий фільтрувальник", "magnetUpgrade": "Притягувач", "advancedMagnetUpgrade": "Просунутий притягувач",
       "pickupUpgrade": "Збирач", "advancedPickupUpgrade": "Просунутий збирач", "voidUpgrade": "Знищувач", "advancedVoidUpgrade": "Просунутий знищувач",
       "smeltingUpgrade": "Піч", "smokingUpgrade": "Коптильня", "blastingUpgrade": "Домна", "autoSmeltingUpgrade": "Авто-піч",
       "autoSmokingUpgrade": "Авто-коптильня", "autoBlastingUpgrade": "Авто-домна", "pumpUpgrade": "Помпа", "xpPumpUpgrade": "Помпа досвіду",
       "compressionUpgrade": "Стискувач", "hopperUpgrade": "Лійка", "advancedHopperUpgrade": "Просунута лійка",
       "advancedJukeboxUpgrade": "Просунутий програвач", "alchemyUpgrade": "Алхімік", "advancedAlchemyUpgrade": "Просунутий алхімік"}
ss = {}
for k in SS_SRC:
    m = re.match(r"sophisticatedstorage\.configuration\.(wood|copper|iron|gold|diamond|netherite)(Barrel|Chest|ShulkerBox)$", k)
    if m:
        mat, kind = m.groups()
        ss[k] = {"Barrel": f"{MAT[mat][0]} діжка", "Chest": f"{MAT[mat][0][:-1]}а скриня", "ShulkerBox": f"{MAT[mat][0][:-1]}а шалкерова коробка"}[kind]
        continue
    m = re.match(r"sophisticatedstorage\.configuration\.limited(Copper|Iron|Gold|Diamond|Netherite)?Barrel([1-4])$", k)
    if m:
        mat = (m.group(1) or "wood").lower()
        ss[k] = "Обмежена діжка " + ROMAN[m.group(2)] if mat == "wood" else f"Обмежена {MAT[mat][0].lower()} діжка {ROMAN[m.group(2)]}"
        continue
    m = re.match(r"sophisticatedstorage\.configuration\.([A-Za-z]+Upgrade)$", k)
    if m and m.group(1) in UPG:
        ss[k] = UPG[m.group(1)]; continue
    m = re.match(r"item\.sophisticatedstorage\.stack_upgrade_tier_([0-9_plus]+)_to_tier_([0-9_plus]+)_conversion$", k)
    if m:
        a, b = (x.replace("_plus", "+") for x in m.groups())
        ss[k] = f"Перетворення збільшувача стосу: рівень {a} → {b}"
ss.update({
    "item.sophisticatedstorage.packing_tape.tooltip.disabled": "Вимкнено — налаштування пакують усі сховища під час зламу",
    "item.sophisticatedstorage.super_packing_tape": "Суперпакувальна стрічка", "item.sophisticatedstorage.super_packing_tape.tooltip": "Залишилось використань: %s",
    "item.sophisticatedstorage.super_packing_tape.tooltip.disabled": "Вимкнено — налаштування пакують усі сховища під час зламу",
    "item.sophisticatedstorage.paintbrush.overlay.missing_items": "Бракує предметів: ", "item.sophisticatedstorage.paintbrush": "Пензель",
    "item.sophisticatedstorage.paintbrush.tooltip": "Оздоблює блоки сховищ у світі\nОздоблення задається на столі оздоблення",
    "item.sophisticatedstorage.paintbrush.tooltip.materials": "Матеріали:", "item.sophisticatedstorage.paintbrush.tooltip.material": " - %s: %s",
    "item.sophisticatedstorage.paintbrush.tooltip.main_color": "Основний колір: %s", "item.sophisticatedstorage.paintbrush.tooltip.accent_color": "Колір акценту: %s",
    "item.sophisticatedstorage.storage_connector": "%1$s%2$sЗ'єднувач сховищ", "block.sophisticatedstorage.storage_connector": "%1$s%2$sЗ'єднувач сховищ",
    "block.sophisticatedstorage.storage_connector.tooltip": "Заповнює проміжки між блоками сховищ у багатоблоковому сховищі.",
    "item.sophisticatedstorage.storage_io": "Вхід/вихід сховища", "block.sophisticatedstorage.storage_io": "Вхід/вихід сховища",
    "block.sophisticatedstorage.storage_io.tooltip": "У багатоблоковому сховищі дає подавати й забирати предмети трубами",
    "item.sophisticatedstorage.storage_input": "Вхід сховища", "block.sophisticatedstorage.storage_input": "Вхід сховища",
    "block.sophisticatedstorage.storage_input.tooltip": "У багатоблоковому сховищі дає подавати предмети трубами\nОптимізовано, щоб мало навантажувати сервер",
    "item.sophisticatedstorage.storage_output": "Вихід сховища", "block.sophisticatedstorage.storage_output": "Вихід сховища",
    "block.sophisticatedstorage.storage_output.tooltip": "У багатоблоковому сховищі дає забирати предмети трубами",
    "block.sophisticatedstorage.decoration_table": "Стіл оздоблення", "item.sophisticatedstorage.advanced_jukebox_upgrade": "Просунутий програвач",
    "item.sophisticatedstorage.advanced_jukebox_upgrade.tooltip": "Переносний програвач на більше платівок\nІ більше режимів відтворення",
    "item.sophisticatedstorage.stack_upgrade_tier_5": "Збільшувач стосу 5",
    "item.sophisticatedstorage.stack_upgrade_tier_5.tooltip": "Збільшує кількість стосів у слоті в 32 рази",
    "item.sophisticatedstorage.stack_upgrade_omega_tier": "Збільшувач стосу Омега",
    "item.sophisticatedstorage.stack_upgrade_omega_tier.tooltip": "Збільшує кількість стосів у слоті до максимуму (×33 554 431)",
    "item.sophisticatedstorage.stack_downgrade_tier_1": "Зменшувач стосу 1", "item.sophisticatedstorage.stack_downgrade_tier_1.tooltip": "Зменшує кількість стосів у слоті у 8 разів",
    "item.sophisticatedstorage.stack_downgrade_tier_2": "Зменшувач стосу 2", "item.sophisticatedstorage.stack_downgrade_tier_2.tooltip": "Зменшує кількість стосів у слоті в 16 разів",
    "item.sophisticatedstorage.stack_downgrade_tier_3": "Зменшувач стосу 3", "item.sophisticatedstorage.stack_downgrade_tier_3.tooltip": "Зменшує кількість стосів у слоті в 32 рази",
    "item.sophisticatedstorage.infinity_upgrade": "Нескінченність (адмін)",
    "item.sophisticatedstorage.infinity_upgrade.tooltip": "Робить усі предмети у сховищі нескінченними\nЛише адміністратори можуть задавати нескінченні предмети й ламати сховище",
    "item.sophisticatedstorage.survival_infinity_upgrade": "Нескінченність для виживання",
    "item.sophisticatedstorage.survival_infinity_upgrade.tooltip": "Робить усі предмети у сховищі нескінченними\nУсі гравці можуть задавати нескінченні предмети й ламати сховище",
    "item.sophisticatedstorage.alchemy_upgrade": "Алхімік",
    "item.sophisticatedstorage.alchemy_upgrade.tooltip": "Автоматично використовує зілля, їжу з ефектами чи відра молока",
    "item.sophisticatedstorage.advanced_alchemy_upgrade": "Просунутий алхімік",
    "item.sophisticatedstorage.advanced_alchemy_upgrade.tooltip": "Автоматично використовує зілля, їжу з ефектами чи відра молока\nМає додаткові фільтри ефектів",
    "item.sophisticatedstorage.sawmill.sawmill_upgrade": "Лісопилка", "item.sophisticatedstorage.sawmill.sawmill_upgrade.tooltip": "Пилка на вкладці покращення",
    "gui.sophisticatedstorage.error.add.compression_exists": "В іншому слоті вже є стискувач",
    "gui.sophisticatedstorage.error.add.compacting_exists": "В іншому слоті вже є ущільнювач",
    "gui.sophisticatedstorage.linked_storage.primary": "Головне зв'язане сховище", "gui.sophisticatedstorage.linked_storage.secondary": "Зв'язане сховище",
    "gui.sophisticatedstorage.upgrades.buttons.refill_crafting_grid": "Поповнювати сітку крафту",
    "gui.sophisticatedstorage.upgrades.buttons.do_not_refill_crafting_grid": "Не поповнювати сітку крафту",
    "gui.sophisticatedstorage.upgrades.buttons.refill_input": "Поповнювати вхід", "gui.sophisticatedstorage.upgrades.buttons.do_not_refill_input": "Не поповнювати вхід",
    "gui.sophisticatedstorage.barrel_part.top_inner_trim": "Внутрішній верхній акцент", "gui.sophisticatedstorage.barrel_part.top_trim": "Верхній акцент",
    "gui.sophisticatedstorage.barrel_part.side_trim": "Боковий акцент", "gui.sophisticatedstorage.barrel_part.bottom_trim": "Нижній акцент",
    "gui.sophisticatedstorage.barrel_part.top": "Верх основний", "gui.sophisticatedstorage.barrel_part.side": "Бік основний",
    "gui.sophisticatedstorage.barrel_part.bottom": "Низ основний", "gui.sophisticatedstorage.barrel_part.all": "Усі частини",
    "gui.sophisticatedstorage.barrel_part.all_trim": "Усі акценти", "gui.sophisticatedstorage.barrel_part.all_but_trim": "Усе основне",
    "gui.sophisticatedstorage.barrel_part.top_all": "Увесь верх", "gui.sophisticatedstorage.barrel_part.side_all": "Усі боки",
    "gui.sophisticatedstorage.barrel_part.bottom_all": "Увесь низ", "gui.sophisticatedstorage.material": "Матеріал",
    "gui.sophisticatedstorage.tint.accent": "Відтінок акценту", "gui.sophisticatedstorage.tint.main": "Основний відтінок",
    "gui.sophisticatedstorage.buttons.decoration_inheritance_on": "Повторно використовувати матеріал",
    "gui.sophisticatedstorage.buttons.decoration_inheritance_off": "Не використовувати матеріал повторно",
    "gui.sophisticatedstorage.buttons.pick_color": "Клацніть, щоб вибрати колір",
    "gui.sophisticatedstorage.status.infinity_upgrade_only_admin_break": "Сховище з нескінченністю можуть ламати лише адміністратори",
    "gui.sophisticatedstorage.status.tier_upgrade_main_linked_storage_only": "Покращити рівень можна лише головному зв'язаному сховищу",
    "gui.sophisticatedstorage.status.packing_tape_disabled": "Налаштування пакують усі сховища під час зламу без стрічки",
    "gui.sophisticatedstorage.status.ender_linker.packed_storage": "Запаковане сховище не можна зв'язати",
    "gui.sophisticatedstorage.status.packing_tape_linked_storage": "Зв'язане сховище не можна запакувати",
    "gui.sophisticatedstorage.tooltip.parts_stored": "Збережені частини",
    "sophisticatedstorage.configuration.client": "Клієнт", "sophisticatedstorage.configuration.common": "Спільне", "sophisticatedstorage.configuration.server": "Сервер",
    "sophisticatedstorage.configuration.dropPacked": "Завжди випадати запакованим",
    "sophisticatedstorage.configuration.showHigherTierGenericWoodVariants": "Показувати вищі рівні загальних дерев'яних варіантів",
    "sophisticatedstorage.configuration.showHigherTierTintedVariants": "Показувати відтінені варіанти рівнів",
    "sophisticatedstorage.configuration.showSingleWoodVariantOnly": "Показувати один варіант дерева",
    "sophisticatedstorage.configuration.shulkerBox": "Шалкерова коробка",
    "sophisticatedstorage.configuration.shulkerBoxDisallowedItems": "Обмеження предметів шалкерових коробок",
    "sophisticatedstorage.configuration.inventorySlotCount": "Слоти інвентаря", "sophisticatedstorage.configuration.upgradeSlotCount": "Слоти покращень",
    "sophisticatedstorage.configuration.baseSlotLimitMultiplier": "Множник базової місткості слота",
    "sophisticatedstorage.configuration.disallowedItems": "Заборонені предмети", "sophisticatedstorage.configuration.containerItemsDisallowed": "Заборонити предмети-контейнери",
    "sophisticatedstorage.configuration.nonStackableItems": "Нестосовні предмети", "sophisticatedstorage.configuration.filterSlots": "Слоти фільтра",
    "sophisticatedstorage.configuration.slotsInRow": "Слотів у ряду", "sophisticatedstorage.configuration.inputFilterSlots": "Слоти фільтра входу",
    "sophisticatedstorage.configuration.inputFilterSlotsInRow": "Слотів входу в ряду", "sophisticatedstorage.configuration.outputFilterSlots": "Слоти фільтра виходу",
    "sophisticatedstorage.configuration.outputFilterSlotsInRow": "Слотів виходу в ряду", "sophisticatedstorage.configuration.transferSpeedTicks": "Інтервал перенесення",
    "sophisticatedstorage.configuration.maxTransferStackSize": "Найбільший стос перенесення", "sophisticatedstorage.configuration.maxNumberOfSlots": "Найбільше слотів стискувача",
    "sophisticatedstorage.configuration.additionalDecompressibleItems": "Додаткові предмети для розстискання",
    "sophisticatedstorage.configuration.magnetRange": "Радіус притягувача", "sophisticatedstorage.configuration.voidAlwaysEnabled": "Дозволити пряме знищення",
    "sophisticatedstorage.configuration.smeltingSpeedMultiplier": "Швидкість приготування", "sophisticatedstorage.configuration.fuelEfficiencyMultiplier": "Ефективність палива",
    "sophisticatedstorage.configuration.fuelFilterSlots": "Слоти фільтра палива", "sophisticatedstorage.configuration.fuelFilterSlotsInRow": "Слотів палива в ряду",
    "sophisticatedstorage.configuration.maxInputOutput": "Межа перенесення", "sophisticatedstorage.configuration.stackMultiplierRatio": "Масштаб збільшувача стосу",
    "sophisticatedstorage.configuration.capacityPerSlotRow": "Місткість резервуара на ряд",
    "sophisticatedstorage.configuration.autoFillDrainContainerCooldown": "Затримка наповнення/спорожнення контейнерів",
    "sophisticatedstorage.configuration.mendingOn": "Увімкнути лагодження", "sophisticatedstorage.configuration.maxXpPointsPerMending": "Найбільше досвіду за лагодження",
    "sophisticatedstorage.configuration.numberOfSlots": "Слоти платівок", "sophisticatedstorage.configuration.tooManyItemEntityDrops": "Найбільше випалих предметів",
    "sophisticatedstorage.configuration.limitedBarrelCountDyeingEnabled": "Фарбування обмежених діжок", "sophisticatedstorage.configuration.controllerRange": "Радіус контролера",
    "sophisticatedstorage.configuration.maxUpgradesPerStorage": "Межі покращень",
})
miss_npc = [k for k in NPC_SRC if k not in npc]
miss_ss = [k for k in SS_SRC if k not in ss]
json.dump({"easy_npc": {k: v for k, v in npc.items() if k in NPC_SRC}, "sophisticatedstorage": {k: v for k, v in ss.items() if k in SS_SRC}},
          open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("npc", len(npc), "missing", miss_npc, "| storage", len(ss), "missing", miss_ss)
