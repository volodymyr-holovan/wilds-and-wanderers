"""Batch 43: the small namespaces left after the new mods (When Dungeons Arise, Guard Villagers, Hybrid Aquatic, FallingTree,
Upgrade Aquatic, CTOV, Kobolds, Chef's Delight, Moog's structures, BlockUI and a few single strings)."""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
I18N = os.path.join(HERE, "..", "..", "i18n")
OUT = os.path.join(I18N, "batches", "b43_small.json")
B = {}

# --- When Dungeons Arise: explorer maps are "Мапа дослідника: <place>", achievements keep the song-title jokes in spirit
DA = {"monastery": "Монастир", "illager_campsite": "Табір ілладжерів", "illager_castle": "Замок ілладжерів", "illager_hall": "Зала ілладжерів",
      "illager_fort": "Форт ілладжерів", "abandoned_temple": "Покинутий храм", "lighthouse": "Маяк", "mushroom_mines": "Грибні шахти",
      "mushroom_village": "Грибне село", "coliseum": "Колізей", "fishing_hut": "Рибальська хатина", "small_prairie_house": "Хатинка в преріях",
      "wishing_well": "Колодязь бажань", "merchant_campsite": "Табір торговців", "infested_temple": "Заражений храм",
      "heavenly_rider": "Небесний вершник", "mining_system": "Гірнича система", "heavenly_conqueror": "Небесний завойовник",
      "scorched_mines": "Випалені шахти", "undead_pirate_ship": "Піратський корабель нежиті", "foundry": "Ливарня", "small_blimp": "Малий дирижабль",
      "bandit_village": "Бандитське селище", "typhon": "Тифон", "ceryneian_hind": "Керинейська лань", "heavenly_challenger": "Небесний суперник",
      "illager_corsair": "Корсар ілладжерів", "illager_galley": "Галера ілладжерів", "mushroom_house": "Грибний будинок",
      "aviary": "Пташник", "bandit_towers": "Бандитські вежі", "giant_mushroom": "Велетенський гриб", "illager_windmill": "Вітряк ілладжерів",
      "plague_asylum": "Чумний притулок", "shiraz_palace": "Палац Шираз", "thornborn_towers": "Терносхідні вежі", "keep_kayra": "Твердиня Кайра",
      "greenwood_pub": "Корчма «Зелений гай»", "bathhouse": "Лазня", "mechanical_nest": "Механічне гніздо", "kisegi_sanctuary": "Святилище Кісеґі",
      "mining_complex": "Гірничий комплекс"}
da = {f"filled_map.dungeons_arise:{k}": f"Мапа дослідника: {v}" for k, v in DA.items() if k not in ("aviary", "bandit_towers", "giant_mushroom",
      "illager_windmill", "plague_asylum", "shiraz_palace", "thornborn_towers", "keep_kayra", "greenwood_pub", "bathhouse", "mechanical_nest",
      "kisegi_sanctuary", "mining_complex")}
ADV = {"wda_root": ("When Dungeons Arise", "Підземелля постають у новому світі"),
       "find_abandoned_temple": ("Бракує слів", None), "find_aviary": ("Три пташки", None), "find_bandit_towers": ("Що сталося у Вегасі...", None),
       "find_bandit_village": ("Іржаве коріння", None), "find_ceryneian_hind": ("Хто вбив цю тварину?", None),
       "find_coliseum": ("Пам'ятай мене віками", None), "find_fishing_hut": ("Рибалка в темряві", None), "find_foundry": ("Залізна діва", None),
       "find_giant_mushroom": ("Блюдце таємниць", None), "find_heavenly_challenger": ("Імперський марш", None),
       "find_heavenly_conqueror": ("Примарні вершники в небі", None), "find_heavenly_rider": ("Політ валькірій", None),
       "find_illager_campsite": ("Люди дивні", None), "find_illager_fort": ("Дикі історії", None), "find_illager_windmill": ("Уздовж сторожової вежі", None),
       "find_infested_temple": ("Страшні чудовиська", None), "find_lighthouse": ("Маяк", None), "find_merchant_campsite": ("Танцюймо", None),
       "find_mining_system": ("Камінь вільний", None), "find_monastery": ("Тихе місце", None), "find_mushroom_house": ("За садовою стіною", None),
       "find_mushroom_mines": ("Тягнімо разом", None), "find_mushroom_village": ("Солодких снів", None), "find_plague_asylum": ("Кругообіг кохання", None),
       "find_scorched_mines": ("Катабасис", None), "find_shiraz_palace": ("Пісочна людина", None), "find_small_blimp": ("Під небесами", None),
       "find_typhon": ("Щелепи", None), "find_undead_pirate_ship": ("Дим над водою", None), "find_wishing_well": ("Стрибай", None),
       "find_keep_kayra": ("Далекий мрійник", None), "find_greenwood_pub": ("Клуб самотніх сердець", None), "find_bathhouse": ("Прокляття", None),
       "find_mechanical_nest": ("Ласкаво просимо до машини", None), "find_kisegi_sanctuary": ("Співчуття дияволу", None),
       "find_mining_complex": ("Нагодуй машину", None), "find_thornborn_towers": ("Зброя і троянди", None)}
for k, (title, desc) in ADV.items():
    da[f"advancement.dungeons_arise.{k}.title"] = title
    place = DA.get(k[5:]) if k.startswith("find_") else None
    da[f"advancement.dungeons_arise.{k}.desc"] = desc or (f"Знайти: {place}" if place else "")
da["advancement.dungeons_arise.find_illager_corsair_or_illager_galley.title"] = "Ти — пірат"
da["advancement.dungeons_arise.find_illager_corsair_or_illager_galley.desc"] = "Знайти корсара або галеру ілладжерів"
da["advancement.dungeons_arise.find_infested_temple_map.title"] = "У дорозі"
da["advancement.dungeons_arise.find_infested_temple_map.desc"] = "Знайти мапу дослідника до зараженого храму"
da["advancement.dungeons_arise.find_lighthouse.desc"] = "Дістатися до маяка"
da["advancement.dungeons_arise.find_thornborn_towers.desc"] = "Побувати в Терносхідних вежах"
da.update({
    "enchantment.dungeons_arise.purification": "Очищення",
    "enchantment.dungeons_arise.purification.desc": "Дає шанс відкинути отруєння й висихання, вивільняючи сплеск енергії, що ранить ворогів поруч.",
    "enchantment.dungeons_arise.voltaic_shot": "Вольтів постріл",
    "enchantment.dungeons_arise.voltaic_shot.desc": "Заряджає стріли електрикою: вони летять прямо і вибухають.",
    "enchantment.dungeons_arise.discharge": "Розряд",
    "enchantment.dungeons_arise.discharge.desc": "Удари по ворогах розганяють вас, але велика швидкість розряджається бурхливим вибухом, що ранить і ціль, і вас.",
    "enchantment.dungeons_arise.lolths_curse": "Прокляття Ллос",
    "enchantment.dungeons_arise.lolths_curse.desc": "Дає шанс відновити здоров'я під час атаки, але з тіла ворога можуть вилізти павуки. Шкода членистоногим відбивається на власника.",
    "enchantment.dungeons_arise.ensnaring": "Пастка",
    "enchantment.dungeons_arise.ensnaring.desc": "Дає шанс сильно сповільнити ворога й викликає ікла, що атакують усіх монстрів навколо.",
})
B["dungeons_arise"] = da

GV = "guardvillagers.configuration."
B["guardvillagers"] = {
    GV + "raids and illagers": "Набіги й ілладжери", "guardvillagers.config.cleric": "Клірики лікують гравців і селян?",
    GV + "Have baby villagers have big heads like in bedrock?": "Великі голови в дітей-селян, як у Bedrock?",
    GV + "Guard follow range": "Дальність, на якій охоронці йдуть за гравцем", GV + "villager stuff": "Селяни",
    "guardvillagers.config.range": "Дальність, на якій охоронці кидаються на істот, що нападають на селян.",
    "guardvillagers.config.RaidAnimals": "Нападники атакують худобу під час набігу?",
    "guardvillagers.config.IllagersRunFromPolarBears": "Ілладжери тікають від білих ведмедів",
    "guardvillagers.config.armorvillager": "Зброярі й ковалі лагодять спорядження охоронців, коли воно зношене більш ніж наполовину?",
    "guardvillagers.config.steveModel": "Модель Стіва для охоронців",
    "guardvillagers.config.hotvPatrolPoint": "Охоронці патрулюють вказані місця лише для Героя села",
    "guardvillagers.config.hotv": "Селян можна перетворити на охоронців лише гравцю з ефектом «Герой села»",
    "guardvillagers.config.blacksmith": "Ковалі лагодять залізних големів зі здоров'ям нижче 80",
    GV + "Chance to drop equipment": "Шанс випустити спорядження",
    "guardvillagers.config.GuardFormation": "Охоронці зі щитами тримаються разом у бою",
    GV + "Allow guards to naturally patrol villages? This feature can cause lag if a lot of guards are spawned": "Охоронці самі патрулюють села? Може гальмувати, якщо охоронців багато",
    "guardvillagers.config.guardArrows": "Стріли охоронців можуть влучати в селяноподібних істот?",
    "guardvillagers.config.AttackAllMobs": "Охоронці атакують усіх істот",
    "guardvillagers.config.VillagersRunFromPolarBears": "Селяни тікають від білих ведмедів?",
    GV + "Allow guards to teleport if following the player": "Охоронці телепортуються, коли йдуть за гравцем",
    GV + "Have guards randomly spawn with biome variants?": "Охоронці мають різний вигляд залежно від біому?",
    "guardvillagers.config.FriendlyFire": "Охоронці намагаються не стріляти у своїх?",
    "guardvillagers.config.amountofHealthRegenerated": "Скільки сердець відновлюється",
    GV + "How low of a reputation of a player should have to be instantly aggroed upon by guards and golems?": "За якої низької репутації охоронці й големи одразу нападають на гравця?",
    GV + "guard stuff": "Охоронці", "guardvillagers.config.WitchesVillager": "Відьми атакують селян",
    "guardvillagers.config.GuardsOpenDoors": "Охоронці відчиняють двері", GV + "mob ai in general": "Поведінка істот загалом",
    GV + "Guard speed": "Швидкість охоронців", GV + "Mob Whitelist": "Істоти, яких охоронці атакують",
    GV + "Have guards only follow the player if they have hero of the village?": "Охоронці йдуть лише за Героєм села?",
    GV + "Guard health": "Здоров'я охоронців",
    GV + "Minimum reputation requirement for guards to give you access to their inventories": "Найменша репутація, щоб охоронці дали доступ до свого інвентаря",
    GV + "All mobs attack guards": "Усі істоти атакують охоронців", GV + "Mob Blacklist": "Чорний список істот",
    "guardvillagers.config.GuardRaiseShield": "Охоронці постійно тримають щит піднятим?",
    GV + "Allow Iron Golems to float on water?": "Залізні големи тримаються на воді?", GV + "golem stuff": "Големи",
    "guardvillagers.config.hotvArmor": "Давати речі охоронцям можна лише з ефектом «Герой села»?",
    "guardvillagers.config.multifollow": "Клік по дзвону кличе охоронців іти за гравцем?",
    GV + "Structure pieces that spawn guards": "Частини споруд, де з'являються охоронці",
    GV + "How many guards should spawn in a village?": "Скільки охоронців з'являється в селі?",
    GV + "Allow guards to convert to zombie villagers upon being killed by zombies?": "Охоронці, вбиті зомбі, стають зомбі-селянами?",
    GV + "Guard crossbow attack radius": "Радіус стрільби охоронців з арбалета",
    "stat.guardvillagers.guards_made": "Охоронців створено", "subtitles.entity.guard.yes": "Охоронець погоджується",
    "subtitles.entity.guard.no": "Охоронець заперечує", GV + "Chance for guards to lose durability": "Шанс зношення спорядження охоронців",
    GV + "Profession Whitelist for guard conversion": "Професії, які можна перетворити на охоронців",
    GV + "How many times a villager can heal a guard's equipment in one day": "Скільки разів на день селянин лагодить спорядження охоронця",
    GV + "How many times a cleric can heal a guard in one day": "Скільки разів на день клірик лікує охоронця",
    GV + "Profession Whitelist for guard weaponry repair ai": "Професії, що лагодять зброю охоронців",
    GV + "How many times a smith villager can heal a golem in one day": "Скільки разів на день коваль лагодить голема",
    GV + "Profession Whitelist for healing ai for clerics": "Професії, що лікують, як клірики",
    GV + "Profession Whitelist for golem repair ai": "Професії, що лагодять големів",
    GV + "Display guard health in icons": "Показувати здоров'я охоронців значками",
    GV + "Mobs that guards actively protect when they get hurt": "Істоти, яких охоронці захищають, коли тих ранять",
    GV + "Angle of how ranged guards determine if a friendly mob is infront of them before firing": "Кут, у якому стрілець-охоронець перевіряє, чи немає своїх перед пострілом",
    GV + "Depth value for guards fighting underwater mobs": "Глибина для бою охоронців з підводними істотами",
    GV + "Allow guards to sink temporarily to fight mobs that are under water?": "Охоронці тимчасово занурюються, щоб битися з підводними істотами?",
    GV + "Mobs that guards actively protect when they get targeted": "Істоти, яких охоронці захищають, коли на них націлюються",
    GV + "Allow guards to patrol around villager workstations like golems?": "Охоронці патрулюють біля робочих місць селян, як големи?",
}

ha = {"advancements.hybrid_aquatic.creature_net.description": "Зробити сачок для істот, щоб ловити й переносити морських мешканців",
      "advancements.hybrid_aquatic.creature_net.title": "Майже відро",
      "biome.hybrid_aquatic.cold_trench": "Холодна западина", "biome.hybrid_aquatic.coral_reef": "Кораловий риф",
      "biome.hybrid_aquatic.deep_coral_reef": "Глибокий кораловий риф", "biome.hybrid_aquatic.deep_warm_ocean": "Глибокий теплий океан",
      "biome.hybrid_aquatic.frozen_trench": "Замерзла западина", "biome.hybrid_aquatic.lukewarm_trench": "Тепла западина",
      "biome.hybrid_aquatic.red_meadow": "Червона лука", "biome.hybrid_aquatic.seagrass_bed": "Луки морської трави",
      "biome.hybrid_aquatic.sulfuric_caves": "Сірчані печери", "biome.hybrid_aquatic.tide_pools": "Припливні калюжі",
      "biome.hybrid_aquatic.trench": "Западина", "biome.hybrid_aquatic.tropical_deep_coral_reef": "Тропічний глибокий кораловий риф",
      "biome.hybrid_aquatic.tropical_river": "Тропічна річка", "biome.hybrid_aquatic.warm_trench": "Гаряча западина",
      "block.hybrid_aquatic.tiger_shark_plushie.description": "Можна носити як капелюх!",
      "item.hybrid_aquatic.african_butterflyfish": "Африканська риба-метелик",
      "subtitles.hybrid_aquatic.entity.otter.ambient": "Видра пищить", "subtitles.hybrid_aquatic.entity.otter.die": "Видра гине",
      "subtitles.hybrid_aquatic.entity.otter.hurt": "Видра отримує шкоду"}
PAINT = {"abyssal": "Безодня", "admiral_skellington": "Адмірал Скелінгтон", "admiral_steve": "Адмірал Стів", "calm_waters": "Тихі води",
         "chasm": "Прірва", "diorama": "Діорама", "gone_fishing": "Пішов рибалити", "jona_saxisa": "Jona Saxisa",
         "lighthouse_after_dusk": "Маяк після заходу", "majestic_behemoth": "Величний велетень", "pansexual_pride_flag": "Пансексуальний прапор",
         "red_devil": "Червоний диявол", "seashell_seashelf": "Полиця з мушлями", "selgio": "Selgio", "shallows": "Мілина",
         "steve_and_the_sea": "Стів і море", "the_fast_regiment": "Швидкий полк"}
src_ha = json.load(open(os.path.join(I18N, "src", "hybrid_aquatic.json"), encoding="utf-8"))
for k, v in PAINT.items():
    ha[f"painting.hybrid_aquatic.{k}.title"] = v
for k in src_ha:
    if k.endswith(".author"):
        ha[k] = src_ha[k]
B["hybrid_aquatic"] = ha

FT = "text.autoconfig.fallingtree."
CH = "Швидка рубка"
B["fallingtree"] = {
    FT + "category.enchantment": "Зачарування", FT + "option.sneakMode": "Режим присідання",
    FT + "option.sneakMode.@Tooltip": "Що відбувається, коли ви присідаєте.\n\nSNEAK_DISABLE: присідання вимикає мод\nSNEAK_ENABLE: присідання вмикає мод\nIGNORE: мод працює завжди",
    FT + "option.lootInCreative": "Здобич у творчому режимі",
    FT + "option.lootInCreative.@Tooltip": "Якщо вимкнено, у творчому режимі мод не лишає здобичі.",
    FT + "option.trees.minSize": "Найменший розмір",
    FT + "option.trees.minSize.@Tooltip": "Найменший розмір дерева. Якщо колод менше, мод пропускає дерево.",
    FT + "option.trees.maxLeafDistanceFromLog": "Найбільша відстань листя від колоди",
    FT + "option.trees.maxLeafDistanceFromLog.@Tooltip": "Найбільша відстань листка від дерева. Впливає лише на пошук дерева.",
    FT + "option.trees.includePersistentLeavesInRequiredCount": "Рахувати постійне листя",
    FT + "option.trees.includePersistentLeavesInRequiredCount.@Tooltip": "Якщо увімкнено, постійне листя (поставлене гравцем чи обстрижене) рахується в «найменшу кількість листя». Інакше — ні.",
    FT + "option.trees.breakMangroveRoots": "Ламати мангрове коріння",
    FT + "option.trees.breakMangroveRoots.@Tooltip": "Якщо увімкнено, мангрове коріння ламається разом зі стовбуром.",
    FT + "option.trees.spawnItemsAtBreakPoint": "Колоди там, де рубали",
    FT + "option.trees.spawnItemsAtBreakPoint.@Tooltip": "Якщо увімкнено, усі колоди з'являються там, де зламано колоду, а не на своїх місцях.\n\nINFO: лише в режимі INSTANTANEOUS.",
    FT + "option.trees.trunkLootPercentage": "Відсоток колод стовбура",
    FT + "option.trees.trunkLootPercentage.@Tooltip": "Яка частка колод стовбура випадає предметами.\n\nINFO: не діє в режимі SHIFT_DOWN.",
    FT + "option.tools.durabilityMode": "Режим міцності",
    FT + "option.tools.durabilityMode.@Tooltip": "Що робити, коли інструмент от-от зламається.\n\nABORT: рубку скасовано, інструмент цілий, нічого не зламано.\nSAVE: зламано найбільше блоків, інструменту лишається 1 міцності. Якщо вже 1 — зламається.\nNORMAL: зламано скільки вийде, інструмент ламається.\nBYPASS: зламано все дерево незалежно від міцності. Інструмент ламається.",
    FT + "option.tools.forceToolUsage": "Обов'язковий інструмент",
    FT + "option.tools.forceToolUsage.@Tooltip": "Якщо увімкнено, колоди можна ламати лише правильним інструментом.",
    FT + "category.player": "Гравець", FT + "option.player.allowedTags": "Потрібні мітки гравця",
    FT + "option.player.allowedTags.@Tooltip": "Мітки, одну з яких гравець має мати, щоб мод працював.\nЯкщо список порожній, обмежень немає",
    FT + "option.enchantment.registerEnchant": "Додати зачарування «Швидка рубка»",
    FT + "option.enchantment.registerEnchant.@Tooltip": "Якщо увімкнено, з'являється зачарування швидкої рубки.\n\nINFO: тоді мод потрібен і клієнтам.\nINFO: потрібен перезапуск.",
    FT + "option.enchantment.registerSpecificEnchant": "Додати особливі зачарування рубки",
    FT + "option.enchantment.registerSpecificEnchant.@Tooltip": "Якщо увімкнено, з'являється кілька зачарувань рубки, кожне зі своїм режимом.\n\nINFO: тоді мод потрібен і клієнтам.\nINFO: потрібен перезапуск.",
    FT + "option.enchantment.hideEnchant": "Сховати зачарування",
    FT + "option.enchantment.hideEnchant.@Tooltip": "Якщо увімкнено, зачарування не трапляється й не продається.",
    FT + "option.enchantment.requireEnchantment": "Потрібне зачарування",
    FT + "option.enchantment.requireEnchantment.@Tooltip": "Якщо увімкнено, мод працює лише з зачарованим інструментом.",
    "chat.fallingtree.break_tree_too_small": "Дерево замале — мод його не рубатиме",
    "chat.fallingtree.break_tree_too_big": "Дерево завелике, одним ударом не зрубати (найбільше: %d)",
    "chat.fallingtree.force_tool_usage": "Потрібен правильний інструмент, а ви взяли не той.",
    "chat.fallingtree.veinminer_incompatibility": "FallingTree: є VeinMiner, можливі конфлікти. Радимо виконати команду: /veinminer groups remove Wood",
    "command.fallingtree.toggle.enabled": "FallingTree увімкнено", "command.fallingtree.toggle.disabled": "FallingTree вимкнено",
    "enchantment.fallingtree.chopper": CH,
    "enchantment.fallingtree.chopper.desc": "Інструмент рубає дерево цілком одним ударом у звичайному режимі",
    "enchantment.fallingtree.chopper_instantaneous": CH + " (миттєва)",
    "enchantment.fallingtree.chopper_instantaneous.desc": "Інструмент рубає дерево цілком одним ударом, миттєво",
    "enchantment.fallingtree.chopper_fall_block": CH + " (падіння блоків)",
    "enchantment.fallingtree.chopper_fall_block.desc": "Інструмент рубає дерево цілком, і воно падає блоками",
    "enchantment.fallingtree.chopper_fall_all_block": CH + " (падіння всіх блоків)",
    "enchantment.fallingtree.chopper_fall_all_block.desc": "Інструмент рубає дерево цілком, і всі блоки падають",
    "enchantment.fallingtree.chopper_fall_item": CH + " (падіння предметів)",
    "enchantment.fallingtree.chopper_fall_item.desc": "Інструмент рубає дерево цілком, і воно падає предметами",
    "enchantment.fallingtree.chopper_shift_down": CH + " (зсув униз)",
    "enchantment.fallingtree.chopper_shift_down.desc": "Інструмент рубає дерево цілком, і воно поступово зсувається вниз",
}

ua = {"advancements.upgrade_aquatic.adventure.kill_great_thrasher.description": "Убити великого молотильника",
      "advancements.upgrade_aquatic.adventure.kill_great_thrasher.title": "Знадобиться човен більший",
      "advancements.upgrade_aquatic.adventure.sleep_underwater.description": "Поспати на спальнику під водою",
      "advancements.upgrade_aquatic.adventure.sleep_underwater.title": "Спати з рибами",
      "advancements.upgrade_aquatic.adventure.tooth_fairy.description": "Зібрати зуби молотильника — давнього жаху холодних глибин",
      "advancements.upgrade_aquatic.adventure.tooth_fairy.title": "Зубна фея",
      "advancements.upgrade_aquatic.adventure.unethical_dentistry.description": "Поставити будь-який блок із зубів молотильника",
      "advancements.upgrade_aquatic.adventure.unethical_dentistry.title": "Неетична стоматологія",
      "advancements.upgrade_aquatic.husbandry.collect_all_pike.description": "Зібрати у відро всі різновиди щуки",
      "advancements.upgrade_aquatic.husbandry.collect_all_pike.title": "Щучий світовий тур",
      "advancements.upgrade_aquatic.husbandry.eat_pickerelweed.description": "Закоптити й з'їсти варену понтедерію, щоб відновити повітря",
      "advancements.upgrade_aquatic.husbandry.eat_pickerelweed.title": "Майже законно",
      "advancements.upgrade_aquatic.nether.sleepless_slave.description": "Створити спалаха, наславши безсоння на фантома",
      "advancements.upgrade_aquatic.nether.sleepless_slave.title": "Безсонний слуга",
      "advancements.upgrade_aquatic.nether.vibe_check.description": "Зварити з крилатки зілля відштовхування і обернути його на зілля настрою",
      "advancements.upgrade_aquatic.nether.vibe_check.title": "Перевірка настрою",
      "jukebox_song.upgrade_aquatic.atlantis": "hatsondogs - Atlantis"}
PIKE = {"amur": "Амурська щука", "black_southern": "Чорна південна щука", "brown_northern": "Бура північна щука",
        "chain_pickerel": "Ланцюгова щука", "ebony_southern": "Ебенова південна щука", "golden_southern": "Золота південна щука",
        "grass_pickerel": "Трав'яна щука", "jade_northern": "Нефритова північна щука", "lemon_southern": "Лимонна південна щука",
        "mahogany_northern": "Червонодеревна північна щука", "muskellunge": "Маскінонг", "mustard_southern": "Гірчична південна щука",
        "obsidian": "Обсидіанова щука", "olive_northern": "Оливкова північна щука", "redfin_pickerel": "Червоноплавцева щука",
        "spectral": "Примарна щука", "spotted_brown_northern": "Плямиста бура північна щука", "spotted_jade_northern": "Плямиста нефритова північна щука",
        "spotted_mahogany_northern": "Плямиста червонодеревна північна щука", "spotted_olive_northern": "Плямиста оливкова північна щука",
        "supercharged": "Заряджена щука"}
for k, v in PIKE.items():
    ua[f"pike_variant.upgrade_aquatic.{k}"] = v
B["upgrade_aquatic"] = ua

CT = {"village_christmas": "Різдвяне село", "village_desert_fortified": "Укріплене пустельне село", "village_jungle_tree": "Село на деревах у джунглях",
      "village_mesa_fortified": "Укріплене село бедлендів", "village_mountain_alpine": "Альпійське село", "village_plains_fortified": "Укріплене село рівнин",
      "village_savanna_na": "Село корінних народів савани", "village_swamp_fortified": "Укріплене болотне село", "village_taiga_fortified": "Укріплене село тайги"}
SIZE = {"large": "велике", "medium": "середнє", "small": "мале"}
B["ctov"] = {f"structure.ctov.{s}.{k}": f"{v} ({n})" for k, v in CT.items() for s, n in SIZE.items()}

COLORS = {"black": "Чорний", "blue": "Синій", "brown": "Бурий", "cyan": "Бірюзовий", "gray": "Сірий", "green": "Зелений", "light_blue": "Блакитний",
          "light_gray": "Світло-сірий", "lime": "Лаймовий", "magenta": "Пурпуровий", "orange": "Помаранчевий", "pink": "Рожевий",
          "purple": "Фіолетовий", "red": "Червоний", "yellow": "Жовтий", "white": "Білий"}
kb = {f"block.kobolds.banner.kobold.{c}": f"{v} кобольд" for c, v in COLORS.items()}
kb.update({"item.kobolds.kobold_iron_spear": "Спис з кобольдського заліза",
           "item.minecraft.tipped_arrow.effect.amethyst": "Аметистова стріла зцілення",
           "item.minecraft.potion.effect.amethyst": "Аметистове зілля зцілення",
           "item.minecraft.splash_potion.effect.amethyst": "Аметистове вибухове зілля зцілення",
           "item.minecraft.lingering_potion.effect.amethyst": "Аметистове тривке зілля зцілення"})
B["kobolds"] = kb

cd = {"entity.minecraft.villager.chefsdelight.chef": "Шеф-кухар", "entity.minecraft.villager.chefsdelight.cook": "Кухар",
      "chefsdelight.configuration.title": "Будиночки в селах",
      "chefsdelight.configuration.section.chefsdelight.common.toml": "Будиночки в селах",
      "chefsdelight.configuration.section.chefsdelight.common.toml.title": "Будиночки в селах"}
for biome, name in {"Plains": "рівнини", "Desert": "пустеля", "Taiga": "тайга", "Savanna": "савана", "Snowy": "сніги"}.items():
    cd[f"chefsdelight.configuration.cookHouse{biome}"] = f"Будинок кухаря: {name}"
    cd[f"chefsdelight.configuration.chefHouse{biome}"] = f"Будинок шеф-кухаря: {name}"
B["chefsdelight"] = cd

B["moogs_structures"] = {
    "moogs_structures.config.title": "Moog's Structures",
    "moogs_structures.config.reload_notice": "Зміни діють після перезавантаження світу (вийдіть у меню й зайдіть знову або /reload) і лише для нових чанків.",
    "moogs_structures.config.category.replace_vanilla": "Заміна звичайних споруд", "moogs_structures.config.category.structures": "Споруди",
    "moogs_structures.config.universal_frequency": "Загальна частота",
    "moogs_structures.config.universal_frequency.tooltip": "Наскільки часті всі споруди Moog. 2x — приблизно вдвічі більше, 0.5x — удвічі менше.",
    "moogs_structures.config.all_of_mod": "Усі: %s",
    "moogs_structures.config.mod_frequency.tooltip": "Наскільки часті всі споруди %s. 2x — приблизно вдвічі більше, 0.5x — удвічі менше.",
    "moogs_structures.config.preview": "Перегляд", "moogs_structures.config.preview.unavailable": "Немає перегляду: мод не вказав посилання (mod_slug).",
    "moogs_structures.config.enabled": "Увімкнено", "moogs_structures.config.disabled": "Вимкнено",
    "moogs_structures.cloth_required.title": "Moog's Structure Lib",
    "moogs_structures.cloth_required.message": "Екран налаштувань Moog's Structure Lib потребує Cloth Config API. Встановіть його, щоб налаштовувати в грі.",
}
B["blockui"] = {"blockui.container_gui.client_side_only": "Вміст контейнера видно лише в одиночній грі (без LAN).",
                "blockui.container_gui.empty": "Контейнер порожній.", "blockui.tooltip.item_additional_info": "Затисніть %s для подробиць",
                "blockui.tooltip.properties": "Властивості блока:", "blockui.config.default.boolean": "[Типово: %s]",
                "blockui.config.default.string": "[Типово: %s]", "blockui.config.default.enum": "[Типово: %s, значення: %s]",
                "blockui.config.default.number": "[Типово: %s, мін.: %s, макс.: %s]"}
B["mos"] = {"modmenu.nameTranslation.mos": "MOS - Moog's Ocean Structures",
            "modmenu.summaryTranslation.mos": "Понад 100 океанських споруд у звичайному стилі: руїни Атлантиди, давні корабельні уламки, морські храми й плоти зі здобиччю.",
            "modmenu.descriptionTranslation.mos": "Moog's Ocean Structures оживлює океани: руїни Атлантиди, давні уламки кораблів, морські храми й плоти в кожному водному біомі. Усе збудовано зі звичайних блоків і істот, тож мод пасує до будь-якої збірки й зберігає звичний вигляд гри.",
            "mos.releases": "GitHub Releases", "mos.kofi": "Ko-fi", "mos.curseforge": "CurseForge", "mos.modrinth": "Modrinth"}
B["mvs"] = {"modmenu.nameTranslation.mvs": "Moog's Voyager Structures",
            "modmenu.summaryTranslation.mvs": "Понад 160 споруд зі звичайних блоків і істот: здобич, вороги й селяни на шляху!",
            "modmenu.descriptionTranslation.mvs": "Moog's Voyager Structures додає світові живих і цікавих споруд. Вони зроблені зі звичайних блоків і істот, тож органічно вписуються в гру: здобич, вороги й привітні селяни. Пасує до будь-якої збірки й зберігає класичний вигляд Minecraft.",
            "mvs.releases": "GitHub Releases", "mvs.kofi": "Ko-fi", "mvs.curseforge": "CurseForge", "mvs.modrinth": "Modrinth"}
B["easy_villagers"] = {"block.easy_villagers.inventory_viewer": "Перегляд інвентаря", "gui.easy_villagers.inventory_viewer": "Перегляд інвентаря",
                       "gui.easy_villagers.villager_inventory": "Інвентар селянина", "gui.easy_villagers.villager_equipment": "Спорядження селянина"}
B["hapi"] = {"item.hapi.creature_net": "Сачок для істот", "item.hapi.creature_net.description": "Усередині: %s",
             "item.hapi.creature_net.function": "Ловить і переносить водних істот",
             "item.hapi.creature_net.properties": "Випущені істоти стають мирними й не зникають"}
B["oceansdelight"] = {"item.oceansdelight.cooked_stuffed_squid": "Смажений фарширований кальмар",
                      "item.oceansdelight.kelp_encrusted_cod": "Тріска в ламінарії", "item.oceansdelight.stuffed_squid": "Фарширований кальмар"}
B["orc_hut"] = {"subtitles.org-ughhh": "Орк гарчить", "entity.orc_hut.orc": "Орк", "item.orc_hut.orc_spawn_egg": "Яйце виклику орка"}
B["multipiston"] = {"block.multipiston.multipistonblock": "Мультипоршень", "com.ldtteam.multipiston.equalpos": "Напрям входу й виходу не може збігатися!"}
B["blueprint"] = {"entity.blueprint.falling_block": "Падаючий блок"}
B["spectrelib"] = {"spectrelib.networking.failed": "Помилка мережі SpectreLib: %s"}
B["villagertradingplus"] = {"_comment_short_labels": "Для вузької колонки керування чи каталогу (малий масштаб інтерфейсу)."}
B["ftbquests"] = {"ftbquests.file.defaults.reward_team_by_type": "Командна нагорода за типом"}

for ns, d in B.items():
    src = json.load(open(os.path.join(I18N, "src", ns + ".json"), encoding="utf-8"))
    ukp = os.path.join(I18N, "uk", ns + ".json")
    done = json.load(open(ukp, encoding="utf-8")) if os.path.exists(ukp) else {}
    left = [k for k in src if k not in done and k not in d]
    if left:
        raise SystemExit(f"{ns}: untranslated {left[:5]}")
    B[ns] = {k: v for k, v in d.items() if k in src}
json.dump(B, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("written", sum(len(v) for v in B.values()))
