# Graph Report - Minecraft Server 1.21.1  (2026-11-21)

## Corpus Check
- 329 files · ~318,246 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 9 file(s) not represented in the graph (top: .patch 4, (none) 2, .manifest 1)

## Summary
- 3558 nodes · 9263 edges · 192 communities (86 shown, 106 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 262 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2fb75a16`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .level
- Skill abilities and perks
- WildsDump.java
- Translation pipeline (i18n)
- Page
- Installer main window
- Wiki screen and history
- Wiki page builder
- Installer downloads and paths
- os
- Quest book generator
- Installer engine and upscalers
- Skill tree rewards
- Wiki data index
- paste.py
- TraderSkills
- 8. Покажчик файлів
- Wiki JEI integration
- Wiki data generators
- FinderField.java
- ThrownWeapon
- Seed search and client launch tools
- gen_skilltree.py
- Copper golem sorter
- MusicDirector
- Server night shutdown
- Wanderer NPC and dialogue
- NPC quest data
- gen_skilltree.py (2)
- org.spongepowered.asm.mixin.Mixin
- servers.dat writer
- Quests and FTB bridge
- Attributes panel and recipe book
- RecipeView
- sys
- Installer downloads and paths (2)
- Structure guard mixins
- Recipe book panel
- PROJECT_MAP.md
- Net
- Golem sorting by theme
- Wiki search
- WildsDump
- Finder field
- Accessories and FTB mixins
- Golem dock block
- Launcher profiles and model
- Installer app, log, self-update
- Journal notes
- Effects panel
- Data fixers and dependency checks
- Equipment screen
- WildsCore
- Pack sync and storage
- Graphics presets
- Wiki JEI plugin
- CaveBiomes
- Enchantment loot filter
- MobLevels.java
- make_models.py
- gen_skilltree.py (3)
- Mod resolution (Modrinth/CurseForge)
- Uninstaller
- Dialogue camera
- Wilds & Wanderers work log (ПРОГРЕС)
- gen_mob_balance.py
- Equipment layout
- Kind
- org.spongepowered.asm.mixin.gen.Accessor
- .has
- org.spongepowered.asm.mixin.injection.Inject
- MobLevels
- Bridges between mods (addons + KubeJS recipes)
- Armor shift and anvil mixins
- ColonyTest.java
- Sort statuses
- Mixin plugin
- SortTitle
- Wiki JEI calls
- Reactive music config
- Wilds Core mod
- Installer main window (2)
- net.minecraft.client.Minecraft
- Installer theme
- NPC world
- Skill XP and NPC world
- Інструкції для ШІ-асистентів
- net.minecraft.world.entity.Mob
- Keyboard layout
- RecipeBookKnowsMixin.java
- Finder server
- Мапа проєкту Wilds & Wanderers
- Join test
- net.minecraft.client.Minecraft (2)
- Installer app, log, self-update (2)
- .apply
- Recent
- Armor number HUD
- Golem registration
- net.minecraft.resources.ResourceLocation
- Dragons Plus translation
- net.minecraft.commands.CommandSourceStack
- MobCommand
- Currencies
- Item frame click
- Spawn script
- PathJobMixin.java
- DialogueScreen
- .WildsCoreClient
- paste.py
- 4. Власні моди
- Installer project
- gen_data.py
- World dump script
- Mod selection notes
- Mod selection rationale
- Journal notes (2)
- ColonyNavigateMixin.java
- LogNoise.java
- math
- Рівні мобів, нові істоти, лут і боси — версія 2
- LeafSaplingMapMixin.java
- .init
- gen_fantasy_gui.py
- struct
- UiTour
- Kind
- BuilderSpeedMixin.java
- OpBreakMixin.java
- TASKS.md
- gen_quest_shapes.py
- Дизайн-документ: завершення Кінцевого замку
- Технічна архітектура
- ReactiveMusicBridge
- compatibility.md
- Аналіз механік TF, на які спирається завершення
- Тестування
- .onPlace
- KillCredit
- Невикористаний і недороблений вміст TF 4.8.3345
- FirstRunDefaults
- MusicPack
- Дослідження: історія Twilight Forest і задум фіналу
- CmdTest
- Журнал змін завершення Кінцевого замку
- Аналіз прогресу й баланс
- PersonalQuests
- .mayUse
- uitour.sh
- README.md
- restart_with_jar.sh

## God Nodes (most connected - your core abstractions)
1. `WikiData` - 72 edges
2. `WikiScreen` - 67 edges
3. `Entry` - 58 edges
4. `WildsCore` - 57 edges
5. `PageBuilder` - 49 edges
6. `Page` - 45 edges
7. `8. Покажчик файлів` - 41 edges
8. `Paths` - 40 edges
9. `Dialogue` - 40 edges
10. `ПРОГРЕС.md - work log` - 39 edges

## Surprising Connections (you probably didn't know these)
- `Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер)` --references--> `WildsMixinPlugin`  [INFERRED]
  PROJECT_MAP.md → mods-src/wilds_core/src/main/java/ua/wilds/core/mixin/WildsMixinPlugin.java
- `Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер)` --references--> `NpcQuest`  [INFERRED]
  PROJECT_MAP.md → mods-src/wilds_core/src/main/java/ua/wilds/core/npc/NpcQuest.java
- `main()` --indirect_call--> `f()`  [INFERRED]
  pack/tools/i18n/gen_b55_create_addons.py → mods-src/twilightforest-completion/tools/make_models.py
- `load()` --indirect_call--> `f()`  [INFERRED]
  pack/server/night_shutdown.py → mods-src/twilightforest-completion/tools/make_models.py
- `save()` --indirect_call--> `f()`  [INFERRED]
  pack/server/night_shutdown.py → mods-src/twilightforest-completion/tools/make_models.py

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Server upload to Crafty** — agents_crafty_deploy_procedure, pack_tools_bundle_changes, pack_tools_crafty_helpers, прогрес_crafty_server [EXTRACTED 1.00]
- **Post-change upkeep (map, graph, log)** — agents_post_change_checklist, pack_tools_gen_project_map, pack_tools_install_hooks, project_map_graphify_knowledge_graph, прогрес [EXTRACTED 1.00]
- **Player release pipeline** — project_map_pack_overrides, pack_tools_release_client, project_map_github_release, readme_installer_exe [EXTRACTED 1.00]

## Communities (192 total, 106 thin omitted)

### Community 0 - ".level"
Cohesion: 0.06
Nodes (4): CraftSkills, ExplorerSkills, HunterSkills, SeaSkills

### Community 3 - "Translation pipeline (i18n)"
Cohesion: 0.04
Nodes (7): main(), load_i18n(), main(), need(), write(), main(), main()

### Community 4 - "Page"
Cohesion: 0.06
Nodes (15): Button, Buttons, Cards, Chips, Element, Elements, Facts, Header (+7 more)

### Community 5 - "Installer main window"
Cohesion: 0.06
Nodes (29): Bar, FolderButton, FolderText, MemoryHint, MemorySlider, MemoryText, News, NewsCard (+21 more)

### Community 6 - "Wiki screen and history"
Cohesion: 0.08
Nodes (3): History, Row, WikiScreen

### Community 10 - "Installer downloads and paths"
Cohesion: 0.10
Nodes (16): Manifest, VersionId, NeoForge, Paths, CacheDir, DataDir, GameDir, Isolated (+8 more)

### Community 11 - "os"
Cohesion: 0.06
Nodes (29): build(), install(), jdk21(), main(), prepare(), run(), classpath(), lib_path() (+21 more)

### Community 12 - "Quest book generator"
Cohesion: 0.06
Nodes (26): auto_layout(), d(), check(), compose_desc(), curve(), decorations(), ftb_text(), guide_lines() (+18 more)

### Community 14 - "Skill tree rewards"
Cohesion: 0.09
Nodes (6): Ability, AbilityReward, PerkReward, SkillRewards, StatReward, StatSpec

### Community 15 - "Wiki data index"
Cohesion: 0.06
Nodes (6): QuestData, Article, Category, Section, WikiData, RichText

### Community 16 - "paste.py"
Cohesion: 0.16
Nodes (7): cloth(), paper(), periodic_noise(), color(), render(), Schematic, _state()

### Community 17 - "TraderSkills"
Cohesion: 0.14
Nodes (3): DefendHirer, FollowHirer, TraderSkills

### Community 18 - "8. Покажчик файлів"
Cohesion: 0.05
Nodes (41): 8. Покажчик файлів, Data files by folder, `launcher/src/WildsLauncher/`, `launcher/src/WildsLauncher/Core/`, `launcher/tools/`, `mods-src/wilds_core/`, `mods-src/wilds_core/src/main/java/ua/wilds/core/`, `mods-src/wilds_core/src/main/java/ua/wilds/core/client/` (+33 more)

### Community 19 - "Wiki JEI integration"
Cohesion: 0.11
Nodes (6): ItemStackDurabilityMixin, Jei, Role, RECIPES, STATION, USES

### Community 20 - "Wiki data generators"
Cohesion: 0.11
Nodes (25): add(), ae2_guide(), bestiary(), bestiary_add(), citadel(), citadel_paras(), desc_keys(), link_key() (+17 more)

### Community 23 - "Seed search and client launch tools"
Cohesion: 0.11
Nodes (22): main(), pools(), main(), probe(), Rcon, server_running(), set_seed(), start() (+14 more)

### Community 24 - "gen_skilltree.py"
Cohesion: 0.05
Nodes (20): Server registry dumps (docs/registries), ok(), flat(), walk(), md(), dump_registries.sh script, define(), icon() (+12 more)

### Community 25 - "Copper golem sorter"
Cohesion: 0.20
Nodes (3): CopperSorter, Job, Survey

### Community 27 - "Server night shutdown"
Cohesion: 0.11
Nodes (20): f(), crafty_containers(), Handler, host(), load(), log(), main(), minecraft_pids() (+12 more)

### Community 28 - "Wanderer NPC and dialogue"
Cohesion: 0.08
Nodes (3): WandererRenderer, StayHome, Wanderer

### Community 29 - "NPC quest data"
Cohesion: 0.21
Nodes (4): Dialogue, Option, Session, NpcQuest

### Community 30 - "gen_skilltree.py (2)"
Cohesion: 0.22
Nodes (10): blend_cross(), centre_angle(), cloth(), cross(), dim(), main(), name_label(), polar_cell() (+2 more)

### Community 31 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.12
Nodes (7): BiomeClimateMixin, ApothTooltipMixin, FtbTeamsSelfMixin, PathStartMixin, TownHallMapMixin, CopperGolemStatueMixin, Parties via FTB Teams + PartyPeace

### Community 32 - "servers.dat writer"
Cohesion: 0.16
Nodes (6): ServerEntry, BitConverterBE, Compound, List, Nbt, ServersDat

### Community 33 - "Quests and FTB bridge"
Cohesion: 0.11
Nodes (3): FtbBridge, QuestFanfare, Quests

### Community 36 - "sys"
Cohesion: 0.07
Nodes (14): corals(), generated(), main(), build(), english(), extract(), load_lang(), merge() (+6 more)

### Community 37 - "Installer downloads and paths (2)"
Cohesion: 0.09
Nodes (13): Engine, ArchivedMods, Config, Manifest, Offline, Paths, Presets, Settings (+5 more)

### Community 38 - "Structure guard mixins"
Cohesion: 0.22
Nodes (3): StructureGuardCheckMixin, StructureGuardMixin, StructureGuard

### Community 40 - "PROJECT_MAP.md"
Cohesion: 0.10
Nodes (20): Post-change checklist (map, graph, progress log), describe(), first_sentence(), folders(), index(), main(), tracked(), main() (+12 more)

### Community 41 - "Net"
Cohesion: 0.11
Nodes (13): SkillInput, TutorialTracker, Act, DialogueChoice, DialoguePage, FinderHits, FinderQuery, Net (+5 more)

### Community 42 - "Golem sorting by theme"
Cohesion: 0.20
Nodes (4): Box, Index, Likeness, Theme

### Community 43 - "Wiki search"
Cohesion: 0.15
Nodes (5): Doc, Hit, Searcher, SettingsSearch, Text

### Community 45 - "Finder field"
Cohesion: 0.13
Nodes (3): FinderField, Layout, Lines

### Community 46 - "Accessories and FTB mixins"
Cohesion: 0.11
Nodes (6): AccessoriesResultSlotMixin, DhClientWorldMixin, WorkerToolLevelMixin, FtbQuestsTeamMixin, RsoSearchSessionMixin, Personal quests in parties

### Community 48 - "Launcher profiles and model"
Cohesion: 0.17
Nodes (5): CurrentPreset, Json, DistantHorizonsSettings, Preset, PresetSet

### Community 49 - "Installer app, log, self-update"
Cohesion: 0.11
Nodes (8): Application, App, Args, Paths, Source, Log, FilePath, ToConsole

### Community 50 - "Journal notes"
Cohesion: 0.11
Nodes (11): Crafty deploy procedure, Lithostitched wrapper and Tectonic continentalness cap, ПРОГРЕС.md - work log, Chunky pregeneration (continueOnRestart), Crafty server 'Wilds and Wanderers 1.21.1', Extra inventory rows as separate panel (ExtraRows/RowsPanel), Boss kill credit for all attackers, TrueNAS night shutdown app (+3 more)

### Community 51 - "Effects panel"
Cohesion: 0.18
Nodes (6): EffectsPanel, Mode, NONE, ROW, SMALL, WIDE

### Community 52 - "Data fixers and dependency checks"
Cohesion: 0.15
Nodes (11): scan(), fix(), is_air(), known(), main(), old_name(), scan(), nbt_spawn() (+3 more)

### Community 55 - "WildsCore"
Cohesion: 0.05
Nodes (5): WildsCoreClient, LithostitchedSourceMixin, WildsCore, WildsWiki, Wilds Core mod

### Community 56 - "Pack sync and storage"
Cohesion: 0.13
Nodes (11): ConsoleProgress, FileEntry, HashMismatchException, PackSync, Status, SyncPlan, DownloadBytes, Empty (+3 more)

### Community 57 - "Graphics presets"
Cohesion: 0.16
Nodes (5): GraphicsPresets, TomlFile, LauncherProfiles, Files, Settings

### Community 60 - "Enchantment loot filter"
Cohesion: 0.26
Nodes (3): EnchantRandomlyMixin, EnchantWithLevelsMixin, Enchants

### Community 62 - "make_models.py"
Cohesion: 0.06
Nodes (39): anvil(), box(), Canvas, cape(), cloth(), cloth_trim(), deco_body(), deco_cape() (+31 more)

### Community 63 - "gen_skilltree.py (3)"
Cohesion: 0.15
Nodes (4): Q(), S(), Nether and End update (Incendium, Enderscape...), Spawn 'Перехрестя' and greeter Мірра

### Community 64 - "Mod resolution (Modrinth/CurseForge)"
Cohesion: 0.14
Nodes (8): Ukrainian glossary (pack/i18n/glossary.md), fetch(), get(), merge(), pick_mr(), targets(), get(), Typical tasks: what to change

### Community 67 - "Wilds & Wanderers work log (ПРОГРЕС)"
Cohesion: 0.12
Nodes (3): Own mods built with javac via build.py (no Gradle), Telegram patch log style, Private claims disabled (OPAC claims.enabled=false)

### Community 68 - "gen_mob_balance.py"
Cohesion: 0.12
Nodes (20): after_armor(), armor_add(), boss_hp(), defence(), defence_level(), dmg_mult(), document(), ehp() (+12 more)

### Community 70 - "Kind"
Cohesion: 0.33
Nodes (6): Kind, BULLET, MUTED, NORMAL, TIP, WARN

### Community 72 - ".has"
Cohesion: 0.07
Nodes (4): CombatSkills, DiplomatSkills, IronsMana, MagicSkills

### Community 73 - "org.spongepowered.asm.mixin.injection.Inject"
Cohesion: 0.12
Nodes (6): PinnedQuestsHideMixin, RMSongPickerMixin, ColonyNavigateMixin, WorkerErrorPauseMixin, FtbQuestsPartyMixin, VillagerPricesMixin

### Community 75 - "Bridges between mods (addons + KubeJS recipes)"
Cohesion: 0.13
Nodes (10): Applied Energistics 2, Almost Unified, Ars 'n' Spells (shared mana), Create, Create Aeronautics + Sable physics, Ice and Fire: Community Edition, KubeJS, Mekanism (+2 more)

### Community 78 - "Sort statuses"
Cohesion: 0.18
Nodes (6): SortStatus, IGNORE, MISC, NORMAL, SOURCE, SOURCE_LIKE

### Community 82 - "Reactive music config"
Cohesion: 0.14
Nodes (13): author, credits, description, entries, events, forceChance, forceStartMusicOnValid, forceStopMusicOnChanged (+5 more)

### Community 83 - "Wilds Core mod"
Cohesion: 0.17
Nodes (12): README.md - Wilds & Wanderers, Adaptive music, Graphics presets (Minimal..Maximal, Complementary shaders), In-game wiki (key O), docs/mods.json mod list, Minecraft 1.21.1 / NeoForge 21.1.264, One inventory for everything (Accessories equipment screen), Quest book (28 chapters, 540+ quests) (+4 more)

### Community 84 - "Installer main window (2)"
Cohesion: 0.22
Nodes (3): MemoryGb, GameStarter, MemoryStatusEx

### Community 86 - "Installer theme"
Cohesion: 0.24
Nodes (10): b, dot, Indeterminate, PART_Indicator, PART_Track, ResourceDictionary, TemplateRoot, Border (+2 more)

### Community 89 - "Інструкції для ШІ-асистентів"
Cohesion: 0.20
Nodes (10): 1. Спілкування, 2. Як думати, 3. Робочий процес, 4. Що потребує дозволу власника, 5. Сервер (Crafty) і заливка, 6. Репозиторій, 7. Правила гри й дизайну збірки, 8. Технічні пастки проєкту (+2 more)

### Community 90 - "net.minecraft.world.entity.Mob"
Cohesion: 0.18
Nodes (3): MobAbilities, Web, Info

### Community 96 - "Мапа проєкту Wilds & Wanderers"
Cohesion: 0.22
Nodes (9): 1. Що це за проєкт, 2. Як усе пов'язано, 3. Теки репозиторію, 5. Конвеєри: що з чого генерується, 6. Типові задачі: що змінювати, 7. Граф знань (graphify), Зміст, Мапа проєкту Wilds & Wanderers (+1 more)

### Community 97 - "Join test"
Cohesion: 0.36
Nodes (5): alive(), rcon(), join_test.sh script, restart_test.sh script, Local tests and self-tests (-Dwilds_core.*, -Dwilds_wiki.selftest)

### Community 99 - "Installer app, log, self-update (2)"
Cohesion: 0.38
Nodes (3): SelfUpdate, Current, Exe

### Community 104 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.15
Nodes (7): Effect, Family, LevelAbility, MobData, Variant, SkillLevel, Photos

### Community 105 - "Dragons Plus translation"
Cohesion: 0.33
Nodes (3): gen(), gen_dye(), parse()

### Community 106 - "net.minecraft.commands.CommandSourceStack"
Cohesion: 0.23
Nodes (3): ColonyStats, ColonyTest, Walk

### Community 107 - "MobCommand"
Cohesion: 0.28
Nodes (3): KL, MobCommand, Pending

### Community 110 - "Spawn script"
Cohesion: 0.47
Nodes (3): inSafeZone(), safeDamage(), spawnDistanceSq()

### Community 113 - ".WildsCoreClient"
Cohesion: 0.11
Nodes (4): AllRecipes, ConfigPatches, PlaceTest, ClientAbilities

### Community 114 - "paste.py"
Cohesion: 0.19
Nodes (8): allowed(), flatten(), lib_path(), load(), main(), fix_state(), main(), rotate_state()

### Community 115 - "4. Власні моди"
Cohesion: 0.50
Nodes (4): 4. Власні моди, Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер), Wilds Dump (`mods-src/wilds_dump/`, лише для тесту), Wilds Wiki (`mods-src/wilds_wiki/`, id `wilds_wiki`, лише клієнт)

### Community 117 - "gen_data.py"
Cohesion: 0.16
Nodes (10): classify(), entity_of(), entry_items(), read_data(), table_items(), table_items_raw(), tag_items(), is_block() (+2 more)

### Community 159 - "math"
Cohesion: 0.19
Nodes (6): background(), icon(), bolt(), clamp(), main(), scepter()

### Community 160 - "Рівні мобів, нові істоти, лут і боси — версія 2"
Cohesion: 0.14
Nodes (13): Лут, Нові істоти (75), Перевірка 1: ванільний зомбі (20 здоров'я, 2 броні), Перевірка 2: нові види — близько 4 ударів, коли відкриваються, Перевірка 3: скільки ударів моба витримує гравець, Перевірка 4: боси без зілля, яблук і заклять, Припущення (можна змінити в генераторі), Прогресія: хто з'являється замість ванільних (+5 more)

### Community 163 - "gen_fantasy_gui.py"
Cohesion: 0.32
Nodes (9): button(), frame(), hotbar(), main(), offhand(), selection(), slider_handle(), text_field() (+1 more)

### Community 164 - "struct"
Cohesion: 0.29
Nodes (7): packet(), recv(), recv_exact(), run(), servers_dat(), tag_byte(), tag_string()

### Community 166 - "Kind"
Cohesion: 0.18
Nodes (9): Kind, BIOME, BLOCK, EFFECT, ENCHANTMENT, ENTITY, ITEM, STRUCTURE (+1 more)

### Community 170 - "gen_quest_shapes.py"
Cohesion: 0.38
Nodes (7): gear(), main(), make(), mask_of(), mask_of_line(), polygon(), studs()

### Community 171 - "Дизайн-документ: завершення Кінцевого замку"
Cohesion: 0.22
Nodes (9): 1. Мета, 2. Шлях гравця, 3. Куб знищення (рішення власника), 4. Моби замку, 5. Передвісник горна, 6. Рунний володар, 7. Вигляд, 8. Що свідомо не робимо (+1 more)

### Community 172 - "Технічна архітектура"
Cohesion: 0.22
Nodes (9): 1. Чому зміни в самому TF, а не міксин чи аддон, 2. Як збирається, 3. Що змінено в TF (класи), 4. Знайдена помилка самого TF, 5. Старі замки (оновлення на льоту), 6. Відновлення попередньої версії (відкат), 7. Виправлення, внесені після тестів (у патчі 0003), 8. Підказки біля альтанки (патч 0004, `w2`) (+1 more)

### Community 174 - "compatibility.md"
Cohesion: 0.25
Nodes (5): Сумісність зі збіркою, Висновки, Фанатські продовження, Джерела, Чого немає в джерелах

### Community 176 - "Аналіз механік TF, на які спирається завершення"
Cohesion: 0.29
Nodes (7): Аналіз механік TF, на які спирається завершення, Боси TF: спільний каркас (`BaseTFBoss`), Керована поява мобів у спорудах (`ControlledSpawningConfig`), Ланцюжок прогресу (`Restrictions`, досягнення `progress_*`), Силові поля, двері замку, руни, Шкода й чесний бій у TF, Що з цього взято для нових механік

### Community 177 - "Тестування"
Cohesion: 0.29
Nodes (7): Механіки, Моделі й анімації, Не перевірено, Підказки біля альтанки (`w2`, sha256 `17fc3df8…`, 21.11.2026), Регресія на остаточній збірці (`w1`, sha256 `51439e02…`), Сценарії із завдання, Тестування

### Community 180 - "Невикористаний і недороблений вміст TF 4.8.3345"
Cohesion: 0.33
Nodes (6): Блоки, Невикористаний і недороблений вміст TF 4.8.3345, Предмети, Ресурси, створені заново, Споруди й дані, Сутності

### Community 183 - "Дослідження: історія Twilight Forest і задум фіналу"
Cohesion: 0.40
Nodes (5): 1. Коротка історія, 2. Що саме лишилось недоробленим (перевірено в коді S1), 3. Реконструкція задуму (що можна довести і що ні), 4. Обмеження стилю TF, яких дотримуємося, Дослідження: історія Twilight Forest і задум фіналу

### Community 185 - "Журнал змін завершення Кінцевого замку"
Cohesion: 0.50
Nodes (4): w1 — 17.11.2026 (випущено 18.11 у збірці 2026.11.18.1), w2 — 21.11.2026 (встановлено в client/mods, server/mods, testserver/mods; не випущено), Журнал змін завершення Кінцевого замку, Як відкотити

### Community 186 - "Аналіз прогресу й баланс"
Cohesion: 0.50
Nodes (4): Аналіз прогресу й баланс, Де стоїть фінал у збірці, Нагороди, Чим перевірено

## Knowledge Gaps
- **87 isolated node(s):** `SOURCE`, `VersionId`, `DownloadBytes`, `Empty`, `CacheDir` (+82 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 959 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **106 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Wilds Core mod` connect `WildsCore` to `Skill abilities and perks`, `Item finder and place self-test`, `PROJECT_MAP.md`, `List`, `os`, `Golem dock block`, `Mixin plugin`, `KillCredit.java`, `Likeness.java`, `MobLevels.java`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **What connects `SOURCE`, `VersionId`, `DownloadBytes` to the rest of the system?**
  _87 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.level` be split into smaller, more focused modules?**
  _Cohesion score 0.06462585034013606 - nodes in this community are weakly interconnected._
- **Why does `Wilds Wiki mod` connect `gen_skilltree.py` to `WildsDump.java`, `Item finder and place self-test`, `PROJECT_MAP.md`, `List`, `os`, `Likeness.java`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **Should `Skill abilities and perks` be split into smaller, more focused modules?**
  _Cohesion score 0.04911180773249739 - nodes in this community are weakly interconnected._
- **Why does `WildsCore` connect `WildsCore` to `Quests and FTB bridge`, `WildsDump.java`, `Skill abilities and perks`, `Equipment layout`, `Item finder and place self-test`, `List`, `ColonyTest.java`, `KillCredit.java`, `Golem dock block`, `MobLevels.java`, `Likeness.java`, `Wanderer NPC and dialogue`, `RecipeBookKnowsMixin.java`, `LogNoise.java`?**
  _High betweenness centrality (0.040) - this node is a cross-community bridge._
- **Should `WildsDump.java` be split into smaller, more focused modules?**
  _Cohesion score 0.09024390243902439 - nodes in this community are weakly interconnected._