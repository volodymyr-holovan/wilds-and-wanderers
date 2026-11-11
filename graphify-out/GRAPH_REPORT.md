# Graph Report - Minecraft Server 1.21.1  (2026-11-11)

## Corpus Check
- 281 files · ~268,229 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: (none) 2, .manifest 1, .toml 1)

## Summary
- 3018 nodes · 7888 edges · 151 communities (66 shown, 85 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 219 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `bc63b0db`
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
- List
- Installer downloads and paths
- glob
- Quest book generator
- Installer engine and upscalers
- Skill tree rewards
- Wiki data index
- paste.py
- TraderSkills
- 8. Покажчик файлів
- Wiki JEI integration
- Wiki data generators
- Wilds Core entry and registration
- Thrown weapon
- Seed search and client launch tools
- release_client.py
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
- net.minecraft.resources.ResourceLocation
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
- WildsCore.java
- Pack sync and storage
- Graphics presets
- Wiki JEI plugin
- Cave biomes and climate mixin
- Enchantment loot filter
- Wiki rich text
- gen_skilltree.py (3)
- Mod resolution (Modrinth/CurseForge)
- Uninstaller
- Dialogue camera
- Wilds & Wanderers work log (ПРОГРЕС)
- .perk
- Equipment layout
- Kind
- org.spongepowered.asm.mixin.gen.Accessor
- MagicSkills
- Installer engine model
- Bridges between mods (addons + KubeJS recipes)
- Armor shift and anvil mixins
- gen_project_map.py
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
- HunterSkills
- Keyboard layout
- RecipeBookKnowsMixin.java
- Finder server
- Мапа проєкту Wilds & Wanderers
- Join test
- net.minecraft.client.Minecraft (2)
- Installer app, log, self-update (2)
- Article
- net.minecraft.network.chat.Component
- Armor number HUD
- Golem registration
- Dragons Plus translation
- Currencies
- Item frame click
- Spawn script
- 4. Власні моди
- Installer project
- World dump script
- Mod selection notes
- Mod selection rationale
- Journal notes (2)

## God Nodes (most connected - your core abstractions)
1. `WikiData` - 72 edges
2. `WikiScreen` - 67 edges
3. `Entry` - 58 edges
4. `WildsCore` - 50 edges
5. `PageBuilder` - 49 edges
6. `Page` - 45 edges
7. `8. Покажчик файлів` - 41 edges
8. `Paths` - 40 edges
9. `Dialogue` - 40 edges
10. `ПРОГРЕС.md - work log` - 39 edges

## Surprising Connections (you probably didn't know these)
- `Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер)` --references--> `NpcQuest`  [INFERRED]
  PROJECT_MAP.md → mods-src/wilds_core/src/main/java/ua/wilds/core/npc/NpcQuest.java
- `Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер)` --references--> `WildsMixinPlugin`  [INFERRED]
  PROJECT_MAP.md → mods-src/wilds_core/src/main/java/ua/wilds/core/mixin/WildsMixinPlugin.java
- `blend_cross()` --indirect_call--> `S()`  [INFERRED]
  pack/tools/gen_skilltree_bg.py → pack/tools/npc/common.py
- `Do not hand-edit generated files` --references--> `Generation pipelines (what generates what)`  [EXTRACTED]
  AGENTS.md → PROJECT_MAP.md
- `Questborne and Easy NPC removed (one NPC system)` --rationale_for--> `One system instead of several`  [INFERRED]
  ПРОГРЕС.md → AGENTS.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Server upload to Crafty** — agents_crafty_deploy_procedure, pack_tools_bundle_changes, pack_tools_crafty_helpers, прогрес_crafty_server [EXTRACTED 1.00]
- **Post-change upkeep (map, graph, log)** — agents_post_change_checklist, pack_tools_gen_project_map, pack_tools_install_hooks, project_map_graphify_knowledge_graph, прогрес [EXTRACTED 1.00]
- **Player release pipeline** — project_map_pack_overrides, pack_tools_release_client, project_map_github_release, readme_installer_exe [EXTRACTED 1.00]

## Communities (151 total, 85 thin omitted)

### Community 0 - ".level"
Cohesion: 0.06
Nodes (3): CraftSkills, DiplomatSkills, SeaSkills

### Community 3 - "Translation pipeline (i18n)"
Cohesion: 0.04
Nodes (11): faq(), write(), main(), load_i18n(), main(), need(), write(), main() (+3 more)

### Community 4 - "Page"
Cohesion: 0.09
Nodes (5): Chips, Header, Paragraph, Page, Theme

### Community 5 - "Installer main window"
Cohesion: 0.06
Nodes (29): Bar, FolderButton, FolderText, MemoryHint, MemorySlider, MemoryText, News, NewsCard (+21 more)

### Community 6 - "Wiki screen and history"
Cohesion: 0.09
Nodes (3): History, Row, WikiScreen

### Community 10 - "Installer downloads and paths"
Cohesion: 0.08
Nodes (20): FileEntry, Manifest, VersionId, NeoForge, SyncPlan, DownloadBytes, Empty, Paths (+12 more)

### Community 11 - "glob"
Cohesion: 0.08
Nodes (15): Server registry dumps (docs/registries), classpath(), lib_path(), main(), classpath(), lib_path(), main(), classpath() (+7 more)

### Community 12 - "Quest book generator"
Cohesion: 0.09
Nodes (20): auto_layout(), d(), check(), compose_desc(), ftb_text(), guide_lines(), hid(), item_stack() (+12 more)

### Community 14 - "Skill tree rewards"
Cohesion: 0.05
Nodes (9): LogNoise, PartyPeace, Ability, AbilityReward, PerkReward, SkillRewards, StatReward, StatSpec (+1 more)

### Community 15 - "Wiki data index"
Cohesion: 0.08
Nodes (4): QuestData, Category, Section, WikiData

### Community 16 - "paste.py"
Cohesion: 0.08
Nodes (16): cloth(), paper(), periodic_noise(), theme(), allowed(), flatten(), lib_path(), load() (+8 more)

### Community 17 - "TraderSkills"
Cohesion: 0.11
Nodes (4): StayHome, DefendHirer, FollowHirer, TraderSkills

### Community 18 - "8. Покажчик файлів"
Cohesion: 0.05
Nodes (41): 8. Покажчик файлів, Data files by folder, `launcher/src/WildsLauncher/`, `launcher/src/WildsLauncher/Core/`, `launcher/tools/`, `mods-src/wilds_core/`, `mods-src/wilds_core/src/main/java/ua/wilds/core/`, `mods-src/wilds_core/src/main/java/ua/wilds/core/client/` (+33 more)

### Community 19 - "Wiki JEI integration"
Cohesion: 0.21
Nodes (5): Jei, Role, RECIPES, STATION, USES

### Community 20 - "Wiki data generators"
Cohesion: 0.09
Nodes (28): ok(), add(), ae2_guide(), bestiary(), bestiary_add(), citadel(), citadel_paras(), desc_keys() (+20 more)

### Community 21 - "Wilds Core entry and registration"
Cohesion: 0.07
Nodes (3): WildsWiki, Copper golem sorter, Item finder in chests

### Community 23 - "Seed search and client launch tools"
Cohesion: 0.13
Nodes (20): main(), probe(), Rcon, server_running(), set_seed(), start(), stop(), evaluate() (+12 more)

### Community 24 - "release_client.py"
Cohesion: 0.11
Nodes (17): define(), icon(), minor(), scaled(), school(), stat(), build(), build_app() (+9 more)

### Community 25 - "Copper golem sorter"
Cohesion: 0.18
Nodes (4): Claims, Copper, CopperSorter, Job

### Community 26 - "MusicDirector"
Cohesion: 0.05
Nodes (8): AllRecipes, CmdTest, FirstRunDefaults, MusicDirector, ReactiveMusicBridge, PlaceTest, TutorialTracker, KillCredit

### Community 27 - "Server night shutdown"
Cohesion: 0.12
Nodes (16): crafty_containers(), Handler, host(), load(), log(), main(), minecraft_pids(), next_run() (+8 more)

### Community 29 - "NPC quest data"
Cohesion: 0.20
Nodes (4): Dialogue, Option, Session, NpcQuest

### Community 30 - "gen_skilltree.py (2)"
Cohesion: 0.10
Nodes (15): background(), icon(), is_block(), tfidf(), blend_cross(), centre_angle(), cloth(), cross() (+7 more)

### Community 31 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.12
Nodes (8): ApothTooltipMixin, FtbTeamsSelfMixin, RMSongPickerMixin, CopperGolemStatueMixin, FtbQuestsPartyMixin, VillagerPricesMixin, RsoSearchSessionMixin, Parties via FTB Teams + PartyPeace

### Community 32 - "servers.dat writer"
Cohesion: 0.16
Nodes (6): ServerEntry, BitConverterBE, Compound, List, Nbt, ServersDat

### Community 33 - "Quests and FTB bridge"
Cohesion: 0.09
Nodes (4): FtbBridge, PersonalQuests, QuestFanfare, Quests

### Community 35 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.07
Nodes (12): Kind, BIOME, BLOCK, EFFECT, ENCHANTMENT, ENTITY, ITEM, STRUCTURE (+4 more)

### Community 36 - "sys"
Cohesion: 0.05
Nodes (22): main(), pools(), corals(), generated(), effective(), main(), build(), english() (+14 more)

### Community 37 - "Installer downloads and paths (2)"
Cohesion: 0.15
Nodes (3): Java, Net, AllowLocalFiles

### Community 38 - "Structure guard mixins"
Cohesion: 0.17
Nodes (3): StructureGuardCheckMixin, StructureGuardMixin, StructureGuard

### Community 40 - "PROJECT_MAP.md"
Cohesion: 0.14
Nodes (15): Ukrainian glossary (pack/i18n/glossary.md), Post-change checklist (map, graph, progress log), Generated file index (section 8), graphify knowledge graph (graphify-out/), Translation pipeline (pack/i18n -> i18n.py), KubeJS scripts and data, Folders outside the repository (testserver, deploy, _tools), pack/overrides-initial (first-install only settings) (+7 more)

### Community 41 - "Net"
Cohesion: 0.07
Nodes (14): DialogueScreen, SkillInput, Act, DialogueChoice, DialoguePage, FinderHits, FinderQuery, Net (+6 more)

### Community 42 - "Golem sorting by theme"
Cohesion: 0.20
Nodes (5): Box, Survey, Index, Likeness, Theme

### Community 43 - "Wiki search"
Cohesion: 0.15
Nodes (5): Doc, Hit, Searcher, SettingsSearch, Text

### Community 44 - "WildsDump"
Cohesion: 0.11
Nodes (3): MusicPack, WorldgenAudit, WildsDump

### Community 45 - "Finder field"
Cohesion: 0.12
Nodes (3): FinderField, Layout, Lines

### Community 46 - "Accessories and FTB mixins"
Cohesion: 0.11
Nodes (5): AccessoriesResultSlotMixin, DhClientWorldMixin, FtbQuestsTeamMixin, OpBreakMixin, OpBreak

### Community 49 - "Installer app, log, self-update"
Cohesion: 0.11
Nodes (8): Application, App, Args, Paths, Source, Log, FilePath, ToConsole

### Community 50 - "Journal notes"
Cohesion: 0.11
Nodes (12): Crafty deploy procedure, Lithostitched wrapper and Tectonic continentalness cap, ПРОГРЕС.md - work log, Chunky pregeneration (continueOnRestart), Private claims disabled (OPAC claims.enabled=false), Crafty server 'Wilds and Wanderers 1.21.1', Boss kill credit for all attackers, TrueNAS night shutdown app (+4 more)

### Community 51 - "Effects panel"
Cohesion: 0.18
Nodes (6): EffectsPanel, Mode, NONE, ROW, SMALL, WIDE

### Community 52 - "Data fixers and dependency checks"
Cohesion: 0.13
Nodes (12): scan(), fix(), is_air(), known(), main(), old_name(), cyrillic(), scan() (+4 more)

### Community 55 - "WildsCore.java"
Cohesion: 0.07
Nodes (3): WildsCoreClient, LegacyRows, WildsCore

### Community 56 - "Pack sync and storage"
Cohesion: 0.17
Nodes (7): ConsoleProgress, HashMismatchException, PackSync, Status, InstallState, Installed, Known

### Community 57 - "Graphics presets"
Cohesion: 0.17
Nodes (5): GraphicsPresets, TomlFile, LauncherProfiles, Files, Settings

### Community 59 - "Cave biomes and climate mixin"
Cohesion: 0.15
Nodes (3): BiomeClimateMixin, LithostitchedSourceMixin, CaveBiomes

### Community 60 - "Enchantment loot filter"
Cohesion: 0.20
Nodes (4): EnchantRandomlyMixin, EnchantWithLevelsMixin, ItemStackDurabilityMixin, Enchants

### Community 63 - "gen_skilltree.py (3)"
Cohesion: 0.15
Nodes (4): Q(), S(), Nether and End update (Incendium, Enderscape...), Spawn 'Перехрестя' and greeter Мірра

### Community 64 - "Mod resolution (Modrinth/CurseForge)"
Cohesion: 0.17
Nodes (6): fetch(), get(), merge(), pick_mr(), targets(), get()

### Community 67 - "Wilds & Wanderers work log (ПРОГРЕС)"
Cohesion: 0.11
Nodes (3): Own mods built with javac via build.py (no Gradle), Telegram patch log style, Extra inventory rows as separate panel (ExtraRows/RowsPanel)

### Community 70 - "Kind"
Cohesion: 0.25
Nodes (6): Kind, BULLET, MUTED, NORMAL, TIP, WARN

### Community 74 - "Installer engine model"
Cohesion: 0.15
Nodes (13): Engine, ArchivedMods, Config, CurrentPreset, Manifest, Offline, Paths, Presets (+5 more)

### Community 75 - "Bridges between mods (addons + KubeJS recipes)"
Cohesion: 0.13
Nodes (10): Applied Energistics 2, Almost Unified, Ars 'n' Spells (shared mana), Create, Create Aeronautics + Sable physics, Ice and Fire: Community Edition, KubeJS, Mekanism (+2 more)

### Community 77 - "gen_project_map.py"
Cohesion: 0.33
Nodes (6): describe(), first_sentence(), folders(), index(), main(), tracked()

### Community 78 - "Sort statuses"
Cohesion: 0.16
Nodes (6): SortStatus, IGNORE, MISC, NORMAL, SOURCE, SOURCE_LIKE

### Community 82 - "Reactive music config"
Cohesion: 0.14
Nodes (13): author, credits, description, entries, events, forceChance, forceStartMusicOnValid, forceStopMusicOnChanged (+5 more)

### Community 83 - "Wilds Core mod"
Cohesion: 0.13
Nodes (15): Launcher (C# / WPF, launcher/), README.md - Wilds & Wanderers, Adaptive music, Graphics presets (Minimal..Maximal, Complementary shaders), In-game wiki (key O), WildsAndWanderers.exe installer, docs/mods.json mod list, Minecraft 1.21.1 / NeoForge 21.1.264 (+7 more)

### Community 84 - "Installer main window (2)"
Cohesion: 0.22
Nodes (3): MemoryGb, GameStarter, MemoryStatusEx

### Community 86 - "Installer theme"
Cohesion: 0.24
Nodes (10): b, dot, Indeterminate, PART_Indicator, PART_Track, ResourceDictionary, TemplateRoot, Border (+2 more)

### Community 89 - "Інструкції для ШІ-асистентів"
Cohesion: 0.20
Nodes (10): 1. Спілкування, 2. Як думати, 3. Робочий процес, 4. Що потребує дозволу власника, 5. Сервер (Crafty) і заливка, 6. Репозиторій, 7. Правила гри й дизайну збірки, 8. Технічні пастки проєкту (+2 more)

### Community 96 - "Мапа проєкту Wilds & Wanderers"
Cohesion: 0.22
Nodes (9): 1. Що це за проєкт, 2. Як усе пов'язано, 3. Теки репозиторію, 5. Конвеєри: що з чого генерується, 6. Типові задачі: що змінювати, 7. Граф знань (graphify), Зміст, Мапа проєкту Wilds & Wanderers (+1 more)

### Community 97 - "Join test"
Cohesion: 0.36
Nodes (5): alive(), rcon(), join_test.sh script, restart_test.sh script, Local tests and self-tests (-Dwilds_core.*, -Dwilds_wiki.selftest)

### Community 99 - "Installer app, log, self-update (2)"
Cohesion: 0.32
Nodes (4): PackConfig, SelfUpdate, Current, Exe

### Community 101 - "net.minecraft.network.chat.Component"
Cohesion: 0.07
Nodes (11): Recent, Button, Buttons, Cards, Element, Elements, Facts, Heading (+3 more)

### Community 105 - "Dragons Plus translation"
Cohesion: 0.33
Nodes (3): gen(), gen_dye(), parse()

### Community 110 - "Spawn script"
Cohesion: 0.47
Nodes (3): inSafeZone(), safeDamage(), spawnDistanceSq()

### Community 115 - "4. Власні моди"
Cohesion: 0.50
Nodes (4): 4. Власні моди, Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер), Wilds Dump (`mods-src/wilds_dump/`, лише для тесту), Wilds Wiki (`mods-src/wilds_wiki/`, id `wilds_wiki`, лише клієнт)

## Knowledge Gaps
- **87 isolated node(s):** `RECIPES`, `USES`, `STATION`, `SOURCE`, `VersionId` (+82 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 796 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **85 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Wilds Core mod` connect `List` to `Skill abilities and perks`, `WildsDump.java`, `Item finder and place self-test`, `PROJECT_MAP.md`, `glob`, `Golem dock block`, `WildsCore.java`, `Cave biomes and climate mixin`, `NpcWorld.java`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **What connects `RECIPES`, `USES`, `STATION` to the rest of the system?**
  _87 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.level` be split into smaller, more focused modules?**
  _Cohesion score 0.05878084179970972 - nodes in this community are weakly interconnected._
- **Why does `Wilds Wiki mod` connect `glob` to `Skill abilities and perks`, `Item finder and place self-test`, `PROJECT_MAP.md`, `List`, `WikiData.java`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **Should `Skill abilities and perks` be split into smaller, more focused modules?**
  _Cohesion score 0.05030864197530864 - nodes in this community are weakly interconnected._
- **Why does `WildsCore` connect `WildsCore.java` to `Quests and FTB bridge`, `WildsDump.java`, `Skill abilities and perks`, `Equipment layout`, `net.minecraft.network.chat.Component`, `Item finder and place self-test`, `List`, `Skill tree rewards`, `Golem dock block`, `NpcWorld.java`, `Wilds Core entry and registration`, `Thrown weapon`, `WikiData.java`, `Cave biomes and climate mixin`, `Wanderer NPC and dialogue`, `RecipeBookKnowsMixin.java`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Should `WildsDump.java` be split into smaller, more focused modules?**
  _Cohesion score 0.0660377358490566 - nodes in this community are weakly interconnected._