# Graph Report - Minecraft Server 1.21.1  (2026-11-11)

## Corpus Check
- 280 files · ~258,358 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: (none) 2, .manifest 1, .toml 1)

## Summary
- 2961 nodes · 7727 edges · 167 communities (72 shown, 95 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 216 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `4f71589c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Skill abilities and perks
- Translation pipeline (i18n)
- Installer main window
- Page
- Installer downloads and paths
- Skill tree rewards
- Wiki screen and history
- Network packets and dialogue UI
- Wilds Core entry and registration
- MusicDirector
- Wiki page builder
- .level
- WildsDump
- Quest book generator
- TraderSkills
- Item finder and place self-test
- glob
- Installer engine and upscalers
- 8. Покажчик файлів
- Wilds Core mod
- Seed search and client launch tools
- Copper golem sorter
- Wanderer NPC and dialogue
- Quests and FTB bridge
- Launcher profiles and model
- Equipment screen
- Wiki JEI integration
- zipfile
- Wiki data generators
- servers.dat writer
- .font
- Wiki self-test
- Attributes panel and recipe book
- Installer app, log, self-update
- NPC quest data
- Golem sorting by theme
- Wiki search
- Cave biomes and climate mixin
- Pack sync and storage
- sys
- Accessories and FTB mixins
- Equipment layout
- Golem dock block
- Server night shutdown
- Wiki data index
- Recipe book panel
- Data fixers and dependency checks
- Structure guard mixins
- Wiki recipe views
- Effects panel
- Finder field
- Wiki JEI plugin
- Misc mixins
- Installer engine model
- net.minecraft.resources.ResourceLocation
- Element
- Uninstaller
- Dialogue camera
- Skill XP and NPC world
- gen_skilltree.py
- Sort statuses
- org.spongepowered.asm.mixin.Mixin
- Wiki loot and world data
- Release and deploy bundle
- Thrown weapon
- DialogueScreen
- Armor shift and anvil mixins
- Mod resolution (Modrinth/CurseForge)
- paste.py
- net.minecraft.client.Minecraft
- Personal quests
- Wiki rich text
- Reactive music config
- Mixin plugin
- Wiki JEI calls
- Enchantment loot filter
- LogNoise.java
- Graphics presets
- Installer theme
- net.minecraft.world.entity.player.Player
- SortTitle.java
- Keyboard layout
- Header
- Finder server
- CORS file server
- Mod selection notes
- Інструкції для ШІ-асистентів
- Armor number HUD
- Golem registration
- gen_project_map.py
- Dragons Plus translation
- Mod selection rationale
- NPC world
- launch_client.py
- Currencies
- Item frame click
- PageBuilder.java
- Spawn script
- Journal notes
- Join test
- README
- Installer project
- World dump script
- Journal
- Mod notes
- Registry dump script
- Restart test
- ПРОГРЕС.md
- Bridges between mods (addons + KubeJS recipes)
- net.minecraft.client.gui.GuiGraphics
- History
- Мапа проєкту Wilds & Wanderers
- Wilds & Wanderers work log (ПРОГРЕС)
- Mod selection document
- KillCredit
- Recent
- Shared resources (Almost Unified + KubeJS)
- .onPlace
- FirstRunDefaults
- RecipeBookKnowsMixin.java
- CmdTest
- QuestFanfare
- WorldgenAudit
- Buttons
- Traveler quest givers (72 NPCs, 100 errands)
- 4. Власні моди

## God Nodes (most connected - your core abstractions)
1. `Entry` - 71 edges
2. `WikiData` - 70 edges
3. `WikiScreen` - 67 edges
4. `WildsCore` - 50 edges
5. `PageBuilder` - 49 edges
6. `Page` - 45 edges
7. `8. Покажчик файлів` - 41 edges
8. `Dialogue` - 40 edges
9. `Paths` - 40 edges
10. `Wanderer` - 36 edges

## Surprising Connections (you probably didn't know these)
- `Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер)` --references--> `WildsMixinPlugin`  [INFERRED]
  PROJECT_MAP.md → mods-src/wilds_core/src/main/java/ua/wilds/core/mixin/WildsMixinPlugin.java
- `Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер)` --references--> `NpcQuest`  [INFERRED]
  PROJECT_MAP.md → mods-src/wilds_core/src/main/java/ua/wilds/core/npc/NpcQuest.java
- `One system instead of several` --semantically_similar_to--> `Shared resources (Almost Unified + KubeJS)`  [INFERRED] [semantically similar]
  README.md → docs/вибір-модів.md
- `Wilds & Wanderers work log (ПРОГРЕС)` --references--> `Mod selection document`  [EXTRACTED]
  ПРОГРЕС.md → docs/вибір-модів.md
- `One system instead of several` --conceptually_related_to--> `One mod per role principle`  [INFERRED]
  README.md → docs/вибір-модів.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Generators validated against server registries** — pack_tools_quests_gen_quests, pack_tools_npc_gen_npc, pack_tools_gen_skilltree, readme_registries [EXTRACTED 1.00]
- **Wilds Core mod feature set** — readme_wilds_core, readme_quest_givers, readme_skill_tree, readme_adaptive_music, readme_one_inventory_screen, прогрес_currencies, прогрес_copper_golem_sorter, прогрес_chest_search [EXTRACTED 1.00]
- **Worldgen fixes for Alex's Caves / Tectonic / structures** — прогрес_cave_biomes_fix, прогрес_tectonic_continentalness, прогрес_structure_guard, прогрес_wildsgen, прогрес_serene_seasons_crash [INFERRED 0.85]

## Communities (167 total, 95 thin omitted)

### Community 1 - "Translation pipeline (i18n)"
Cohesion: 0.04
Nodes (13): faq(), write(), main(), load_i18n(), main(), need(), write(), main() (+5 more)

### Community 2 - "Installer main window"
Cohesion: 0.05
Nodes (32): MemoryGb, GameStarter, MemoryStatusEx, Bar, FolderButton, FolderText, MemoryHint, MemorySlider (+24 more)

### Community 3 - "Page"
Cohesion: 0.19
Nodes (3): Cards, Chips, Page

### Community 4 - "Installer downloads and paths"
Cohesion: 0.07
Nodes (20): ConsoleProgress, Manifest, VersionId, Java, NeoForge, Net, AllowLocalFiles, Status (+12 more)

### Community 5 - "Skill tree rewards"
Cohesion: 0.09
Nodes (7): Ability, AbilityReward, PerkReward, SkillRewards, StatReward, StatSpec, Skills

### Community 7 - "Network packets and dialogue UI"
Cohesion: 0.08
Nodes (14): SkillInput, SortTitle, TutorialTracker, Act, DialogueChoice, DialoguePage, FinderHits, FinderQuery (+6 more)

### Community 8 - "Wilds Core entry and registration"
Cohesion: 0.03
Nodes (5): AllRecipes, PlaceTest, WildsCoreClient, LegacyRows, WildsCore

### Community 10 - "Wiki page builder"
Cohesion: 0.13
Nodes (3): Entry, Button, PageBuilder

### Community 11 - ".level"
Cohesion: 0.04
Nodes (6): CombatSkills, CraftSkills, DiplomatSkills, HunterSkills, MagicSkills, SeaSkills

### Community 14 - "Quest book generator"
Cohesion: 0.08
Nodes (23): auto_layout(), d(), check(), compose_desc(), ftb_text(), guide_lines(), hid(), item_stack() (+15 more)

### Community 15 - "TraderSkills"
Cohesion: 0.16
Nodes (3): DefendHirer, FollowHirer, TraderSkills

### Community 17 - "glob"
Cohesion: 0.10
Nodes (9): classpath(), lib_path(), main(), classpath(), lib_path(), main(), classpath(), lib_path() (+1 more)

### Community 18 - "Installer engine and upscalers"
Cohesion: 0.13
Nodes (3): Wilds.Launcher.Core, Wilds.Launcher, Upscaler

### Community 19 - "8. Покажчик файлів"
Cohesion: 0.05
Nodes (41): 8. Покажчик файлів, Data files by folder, `launcher/src/WildsLauncher/`, `launcher/src/WildsLauncher/Core/`, `launcher/tools/`, `mods-src/wilds_core/`, `mods-src/wilds_core/src/main/java/ua/wilds/core/`, `mods-src/wilds_core/src/main/java/ua/wilds/core/client/` (+33 more)

### Community 20 - "Wilds Core mod"
Cohesion: 0.24
Nodes (9): Adaptive music (calm/combat/boss), Skill tree (8 branches), Wilds Core mod, RecipeBookKnowsMixin + AllRecipes, 'Torba' skill branch & extra inventory rows (removed 06.11), FirstRunDefaults (uk_ua, quest book on J), KeyLayout (key rebinding defaults), SkillRewards respawn fix (+1 more)

### Community 21 - "Seed search and client launch tools"
Cohesion: 0.13
Nodes (20): main(), probe(), Rcon, server_running(), set_seed(), start(), stop(), evaluate() (+12 more)

### Community 23 - "Copper golem sorter"
Cohesion: 0.23
Nodes (3): Claims, CopperSorter, Job

### Community 24 - "Wanderer NPC and dialogue"
Cohesion: 0.08
Nodes (3): WandererRenderer, StayHome, Wanderer

### Community 25 - "Quests and FTB bridge"
Cohesion: 0.11
Nodes (3): FtbBridge, Quests, IronsMana

### Community 26 - "Launcher profiles and model"
Cohesion: 0.15
Nodes (3): Json, LauncherProfiles, Settings

### Community 28 - "Wiki JEI integration"
Cohesion: 0.14
Nodes (5): Jei, Role, RECIPES, STATION, USES

### Community 29 - "zipfile"
Cohesion: 0.11
Nodes (12): scan(), cyrillic(), effective(), build(), extract(), load_lang(), merge(), scan() (+4 more)

### Community 30 - "Wiki data generators"
Cohesion: 0.10
Nodes (22): ok(), add(), ae2_guide(), bestiary(), bestiary_add(), citadel(), citadel_paras(), desc_keys() (+14 more)

### Community 31 - "servers.dat writer"
Cohesion: 0.16
Nodes (6): ServerEntry, BitConverterBE, Compound, List, Nbt, ServersDat

### Community 32 - ".font"
Cohesion: 0.15
Nodes (8): Facts, Kind, BULLET, MUTED, NORMAL, TIP, WARN, Paragraph

### Community 35 - "Installer app, log, self-update"
Cohesion: 0.09
Nodes (11): Application, App, Args, Paths, Source, Log, FilePath, ToConsole (+3 more)

### Community 36 - "NPC quest data"
Cohesion: 0.24
Nodes (4): Dialogue, Option, Session, NpcQuest

### Community 37 - "Golem sorting by theme"
Cohesion: 0.20
Nodes (5): Box, Survey, Index, Likeness, Theme

### Community 38 - "Wiki search"
Cohesion: 0.15
Nodes (5): Doc, Hit, Searcher, SettingsSearch, Text

### Community 39 - "Cave biomes and climate mixin"
Cohesion: 0.15
Nodes (3): BiomeClimateMixin, LithostitchedSourceMixin, CaveBiomes

### Community 40 - "Pack sync and storage"
Cohesion: 0.12
Nodes (9): FileEntry, HashMismatchException, PackSync, SyncPlan, DownloadBytes, Empty, InstallState, Installed (+1 more)

### Community 41 - "sys"
Cohesion: 0.08
Nodes (11): main(), pools(), corals(), generated(), packet(), recv(), recv_exact(), run() (+3 more)

### Community 42 - "Accessories and FTB mixins"
Cohesion: 0.15
Nodes (4): AccessoriesArmorShiftMixin, AccessoriesResultSlotMixin, DhClientWorldMixin, FtbQuestsTeamMixin

### Community 45 - "Server night shutdown"
Cohesion: 0.19
Nodes (15): crafty_containers(), Handler, host(), load(), log(), main(), minecraft_pids(), next_run() (+7 more)

### Community 46 - "Wiki data index"
Cohesion: 0.11
Nodes (4): QuestData, Category, Section, WikiData

### Community 48 - "Data fixers and dependency checks"
Cohesion: 0.23
Nodes (9): fix(), is_air(), known(), main(), old_name(), nbt_spawn(), name(), payload() (+1 more)

### Community 49 - "Structure guard mixins"
Cohesion: 0.14
Nodes (4): StructureGuardCheckMixin, StructureGuardMixin, Entry, StructureGuard

### Community 52 - "Effects panel"
Cohesion: 0.13
Nodes (7): EffectsPanel, Mode, NONE, ROW, SMALL, WIDE, Lines

### Community 55 - "Misc mixins"
Cohesion: 0.14
Nodes (5): RMSongPickerMixin, FtbQuestsPartyMixin, OpBreakMixin, VillagerPricesMixin, OpBreak

### Community 56 - "Installer engine model"
Cohesion: 0.12
Nodes (13): Engine, ArchivedMods, Config, CurrentPreset, Manifest, Offline, Paths, Presets (+5 more)

### Community 57 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.10
Nodes (10): Kind, BIOME, BLOCK, EFFECT, ENCHANTMENT, ENTITY, ITEM, STRUCTURE (+2 more)

### Community 58 - "Element"
Cohesion: 0.10
Nodes (6): Element, Elements, Heading, Image, Model, Spacer

### Community 59 - "Uninstaller"
Cohesion: 0.20
Nodes (3): PackConfig, Uninstaller, UninstallPlan

### Community 62 - "gen_skilltree.py"
Cohesion: 0.06
Nodes (21): background(), icon(), blend_cross(), centre_angle(), cloth(), cross(), dim(), main() (+13 more)

### Community 63 - "Sort statuses"
Cohesion: 0.16
Nodes (6): SortStatus, IGNORE, MISC, NORMAL, SOURCE, SOURCE_LIKE

### Community 64 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.16
Nodes (5): ApothTooltipMixin, FtbTeamsSelfMixin, CopperGolemStatueMixin, ItemStackDurabilityMixin, RsoSearchSessionMixin

### Community 65 - "Wiki loot and world data"
Cohesion: 0.17
Nodes (9): classify(), entity_of(), entry_items(), read_data(), table_items(), table_items_raw(), tag_items(), is_block() (+1 more)

### Community 66 - "Release and deploy bundle"
Cohesion: 0.10
Nodes (12): build(), build_app(), fetch(), main(), next_version(), overrides_zip(), run(), sha1() (+4 more)

### Community 70 - "Mod resolution (Modrinth/CurseForge)"
Cohesion: 0.17
Nodes (6): fetch(), get(), merge(), pick_mr(), targets(), get()

### Community 71 - "paste.py"
Cohesion: 0.11
Nodes (11): cloth(), paper(), periodic_noise(), theme(), fix_state(), main(), rotate_state(), color() (+3 more)

### Community 75 - "Reactive music config"
Cohesion: 0.14
Nodes (13): author, credits, description, entries, events, forceChance, forceStartMusicOnValid, forceStopMusicOnChanged (+5 more)

### Community 78 - "Enchantment loot filter"
Cohesion: 0.25
Nodes (3): EnchantRandomlyMixin, EnchantWithLevelsMixin, Enchants

### Community 80 - "Graphics presets"
Cohesion: 0.35
Nodes (3): GraphicsPresets, TomlFile, Files

### Community 81 - "Installer theme"
Cohesion: 0.24
Nodes (10): b, dot, Indeterminate, PART_Indicator, PART_Track, ResourceDictionary, TemplateRoot, Border (+2 more)

### Community 89 - "Mod selection notes"
Cohesion: 0.33
Nodes (7): Alex's Caves Continued, Serene Seasons, CaveBiomes fix (Lithostitched wrapper vs Alex's Caves), Worldgen crash: Serene Seasons snow/ice + Alex's Caves lakes, StructureGuard (overlap rule), Tectonic continentalness cap (~0.56) & BiomeClimateMixin, /wildsgen worldgen audit command

### Community 90 - "Інструкції для ШІ-асистентів"
Cohesion: 0.20
Nodes (10): 1. Спілкування, 2. Як думати, 3. Робочий процес, 4. Що потребує дозволу власника, 5. Сервер (Crafty) і заливка, 6. Репозиторій, 7. Правила гри й дизайну збірки, 8. Технічні пастки проєкту (+2 more)

### Community 93 - "gen_project_map.py"
Cohesion: 0.33
Nodes (6): describe(), first_sentence(), folders(), index(), main(), tracked()

### Community 94 - "Dragons Plus translation"
Cohesion: 0.33
Nodes (3): gen(), gen_dye(), parse()

### Community 95 - "Mod selection rationale"
Cohesion: 0.33
Nodes (6): Chunky (pregeneration), Distant Horizons (client only), Crafty server deployment, DhClientWorldMixin (DH skips tent dimensions), TrueNAS night-shutdown app, Local test server (RCON, join_test.sh)

### Community 97 - "launch_client.py"
Cohesion: 0.42
Nodes (5): allowed(), flatten(), lib_path(), load(), main()

### Community 101 - "Spawn script"
Cohesion: 0.47
Nodes (3): inSafeZone(), safeDamage(), spawnDistanceSq()

### Community 102 - "Journal notes"
Cohesion: 0.40
Nodes (4): FTB Quests, KillCredit (shared boss quest credit), PartyPeace / FTB Teams groups, Personal quests (FtbQuestsTeamMixin, PersonalQuests)

### Community 103 - "Join test"
Cohesion: 0.83
Nodes (3): alive(), rcon(), join_test.sh script

### Community 104 - "README"
Cohesion: 0.50
Nodes (4): In-game wiki (key O), Wilds Wiki mod, Wiki engine rework (categories, mod pages, guides), wilds_dump dev mod (biome/structure dump)

### Community 108 - "Journal"
Cohesion: 0.67
Nodes (3): Chest search (Finder), Copper golem sorter (CopperSorter, Likeness, GolemDock), Friends & Foes copper golem statue server crash

### Community 148 - "Bridges between mods (addons + KubeJS recipes)"
Cohesion: 0.22
Nodes (8): Applied Energistics 2, Create, Create Aeronautics + Sable physics, Ice and Fire: Community Edition, KubeJS, Mekanism, Loot injection (wilds_loot.js, gen_loot_inject.py), LOOT TABLE ERROR book fix (world.Enchants)

### Community 151 - "Мапа проєкту Wilds & Wanderers"
Cohesion: 0.22
Nodes (9): 1. Що це за проєкт, 2. Як усе пов'язано, 3. Теки репозиторію, 5. Конвеєри: що з чого генерується, 6. Типові задачі: що змінювати, 7. Граф знань (graphify), Зміст, Мапа проєкту Wilds & Wanderers (+1 more)

### Community 152 - "Wilds & Wanderers work log (ПРОГРЕС)"
Cohesion: 0.29
Nodes (5): Graphics presets (Minimal/Medium/High/Maximum), WildsAndWanderers.exe installer/launcher, NeoForge 21.1.264 (Minecraft 1.21.1), Super Resolution (DLSS/FSR/XeSS) optional mod, Wilds & Wanderers work log (ПРОГРЕС)

### Community 153 - "Mod selection document"
Cohesion: 0.29
Nodes (6): Mod selection document, Nether & End expansion (Incendium, Eternal Nether, Enderscape, YUNG's...), build_neo.py (mod downloader), docs/mods.json (mod list with links and checksums), Wilds & Wanderers modpack, Server optimization (ServerCore, Incendium clock patch)

### Community 156 - "Shared resources (Almost Unified + KubeJS)"
Cohesion: 0.33
Nodes (3): Almost Unified, Ars 'n' Spells (shared mana), Currencies exchange (Currencies.java)

### Community 164 - "Traveler quest givers (72 NPCs, 100 errands)"
Cohesion: 0.40
Nodes (4): Traveler quest givers (72 NPCs, 100 errands), pack/removed.txt, Crossroads spawn (Elven World Spawn, paste.py), Spawn protection (wilds_spawn.js)

### Community 165 - "4. Власні моди"
Cohesion: 0.50
Nodes (4): 4. Власні моди, Wilds Core (`mods-src/wilds_core/`, id `wilds_core`, клієнт + сервер), Wilds Dump (`mods-src/wilds_dump/`, лише для тесту), Wilds Wiki (`mods-src/wilds_wiki/`, id `wilds_wiki`, лише клієнт)

## Knowledge Gaps
- **96 isolated node(s):** `SOURCE`, `net48`, `Microsoft.NET.Sdk`, `Border`, `CheckBox` (+91 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 784 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **95 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Entry` connect `Wiki page builder` to `Wiki self-test`, `Thrown weapon`, `PageBuilder.java`, `Page`, `Wiki search`, `Wiki screen and history`, `net.minecraft.client.Minecraft`, `Wilds Core entry and registration`, `Wiki rich text`, `Enchantment loot filter`, `Wiki data index`, `net.minecraft.client.gui.GuiGraphics`, `Header`, `net.minecraft.resources.ResourceLocation`, `Wiki JEI integration`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **What connects `SOURCE`, `net48`, `Microsoft.NET.Sdk` to the rest of the system?**
  _96 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Skill abilities and perks` be split into smaller, more focused modules?**
  _Cohesion score 0.0612859097127223 - nodes in this community are weakly interconnected._
- **Why does `Engine` connect `Installer engine model` to `Installer main window`, `Installer app, log, self-update`, `Installer downloads and paths`, `Pack sync and storage`, `Installer engine and upscalers`, `Launcher profiles and model`, `Uninstaller`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **Should `Translation pipeline (i18n)` be split into smaller, more focused modules?**
  _Cohesion score 0.04036036036036036 - nodes in this community are weakly interconnected._
- **Why does `main()` connect `launch_client.py` to `Skill abilities and perks`, `Translation pipeline (i18n)`, `Seed search and client launch tools`, `Wiki data generators`?**
  _High betweenness centrality (0.045) - this node is a cross-community bridge._
- **Should `Installer main window` be split into smaller, more focused modules?**
  _Cohesion score 0.051929824561403506 - nodes in this community are weakly interconnected._