"""Mob levels, new kinds of mobs and their loot: one source for the game data and the design document.

  python pack/tools/gen_mob_balance.py

Writes
  mods-src/wilds_core/src/main/resources/data/wilds_core/wilds_mobs/variants.json   read by ua.wilds.core.mobs.MobData
  mods-src/wilds_core/src/main/resources/data/wilds_core/loot_table/mobs/*.json      tier and family loot
  mods-src/wilds_core/src/main/resources/assets/wilds_core/lang/{uk_ua,en_us}.json   names of the new mobs (merged)
  docs/balance/mob-levels.md                                                         formulas, tables, checks

The formulas here and in ua.wilds.core.mobs.MobMath must stay the same: the document's numbers are what the game does.
The check at the end fails (exit 1) when a stage misses the targets of the brief (4 hits early, 1-2 later, about 4 on
the new kinds, bosses very hard).
"""
import json, math, os, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RES = os.path.join(ROOT, "mods-src", "wilds_core", "src", "main", "resources")
ITEMS = set(open(os.path.join(ROOT, "docs", "registries", "item.txt"), encoding="utf-8").read().split())
ENTITIES = set(open(os.path.join(ROOT, "docs", "registries", "entity_type.txt"), encoding="utf-8").read().split())

# ---------------------------------------------------------------- formulas (mirror of MobMath.java)
def gear_damage(L):
    """damage of one full-strength hit of the reference player at level L (iron sword at 15, netherite with affixes
    and gems around 150-200, the best of the pack at 400)"""
    return 6.0 * ((L + 15.0) / 30.0) ** 0.8

# reference player's defence by level: (level, armor, toughness, protection EPF, max health)
DEFENCE = [(0, 7, 0, 0, 20), (15, 15, 0, 0, 20), (30, 17, 2, 4, 20), (60, 20, 8, 8, 22), (100, 20, 12, 12, 24),
           (150, 22, 14, 16, 28), (200, 24, 16, 18, 32), (300, 27, 18, 20, 40), (400, 30, 20, 20, 48)]

def defence(L):
    L = max(0, min(400, L))
    for (a, *va), (b, *vb) in zip(DEFENCE, DEFENCE[1:]):
        if a <= L <= b:
            t = (L - a) / (b - a)
            return [x + (y - x) * t for x, y in zip(va, vb)]
    return DEFENCE[-1][1:]

def after_armor(dmg, armor, tough):
    """vanilla 1.21 CombatRules.getDamageAfterAbsorb"""
    f = 2.0 + tough / 4.0
    eff = max(min(armor - dmg / f, 20.0), armor * 0.2)
    return dmg * (1.0 - eff / 25.0)

def to_player(raw, L):
    armor, tough, epf, hp = defence(L)
    return after_armor(raw, armor, tough) * (1.0 - min(20.0, epf) / 25.0), hp

def raw_for_hits(L, hits):
    """raw damage that kills the reference player of level L in that many hits"""
    lo, hi = 0.0, 2000.0
    for _ in range(60):
        mid = (lo + hi) / 2
        d, hp = to_player(mid, L)
        if d * hits >= hp: hi = mid
        else: lo = mid
    return hi

HP_RATE, DMG_RATE, BOSS_HP_RATE, BOSS_DMG_RATE = 0.008, 0.006, 0.005, 0.012
def hp_mult(l): return 1.0 + HP_RATE * l
def dmg_mult(l): return 1.0 + DMG_RATE * l
def armor_add(l, tier=0): return min(12.0, l / 30.0) + 0.6 * tier
def tough_add(l): return min(8.0, l / 50.0)
def speed_add(l): return min(0.20, l / 2000.0)        # multiplied base
def kb_add(l): return min(0.40, l / 1000.0)
def resist(l): return min(0.30, l / 1300.0)          # fire, lightning, explosions, magic, freezing

TIERS = [(1, 10), (2, 20), (3, 35), (4, 50), (5, 70), (6, 95), (7, 125), (8, 160), (9, 200), (10, 250), (11, 310), (12, 380)]
UNLOCK = dict(TIERS)
TIER_HITS = 3.7          # hits of the reference player at the tier's own level (ceil: 4)
TIER_PLAYER_HITS = 10    # a new kind kills the reference player of its level in this many hits
BOSS_HITS = 60           # hits the reference player needs for a boss of their level
BOSS_PLAYER_HITS = 5     # and the boss kills them in this many

def hits(hp, armor, tough, L):
    return hp / after_armor(gear_damage(L), armor, tough)

def tier_hp(k, l):
    """max health of a tier-k mob at mob level l (any base: the multiplier is computed from the base in game)"""
    u = UNLOCK[k]
    base = TIER_HITS * after_armor(gear_damage(u), armor_add(u, k), tough_add(u))
    return base * hp_mult(l) / hp_mult(u)

def tier_dmg(k, l):
    u = UNLOCK[k]
    return raw_for_hits(u, TIER_PLAYER_HITS) * dmg_mult(l) / dmg_mult(u)

def boss_hp(native, l): return max(native * (1 + BOSS_HP_RATE * l), BOSS_HITS * gear_damage(l))
def boss_dmg(native, l): return max(native * (1 + BOSS_DMG_RATE * l), raw_for_hits(l, BOSS_PLAYER_HITS))

WORLD_TIER = {"haven": 1.0, "frontier": 1.15, "ascent": 1.3, "summit": 1.5, "pinnacle": 1.75}
SETTINGS = {"radius": 128, "update_ticks": 200, "spawn_spread": [0.9, 1.05], "step_base": 1.0, "step_rate": 0.01,
            "replace_base": 0.2, "replace_rate": 1 / 500.0, "replace_max": 0.7, "world_tier": WORLD_TIER,
            "hp_rate": HP_RATE, "dmg_rate": DMG_RATE, "boss_hp_rate": BOSS_HP_RATE, "boss_dmg_rate": BOSS_DMG_RATE,
            "tier_hits": TIER_HITS, "tier_player_hits": TIER_PLAYER_HITS, "boss_hits": BOSS_HITS, "boss_player_hits": BOSS_PLAYER_HITS,
            "defence": DEFENCE, "tiers": [{"tier": k, "unlock": u} for k, u in TIERS],
            # level-unlocked attacks every leveled mob may roll at spawn: (ability, from level, chance)
            "level_abilities": [["family_hit", 40, 0.4], ["leap", 90, 0.3], ["enrage", 150, 0.3], ["shockwave", 220, 0.25], ["blink", 300, 0.25]],
            "boss_abilities": ["shockwave", "enrage", "summon_guard"],
            "plain_loot_base": 0.12, "plain_loot_rate": 1 / 1000.0}

# ---------------------------------------------------------------- families and the new kinds
FAMILIES = {
    "zombie":   {"bases": ["minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager"], "hit": ["minecraft:hunger", 100, 0], "uk": "Зомбі", "en": "Zombies"},
    "skeleton": {"bases": ["minecraft:skeleton", "minecraft:stray"], "hit": ["minecraft:slowness", 60, 0], "uk": "Скелети", "en": "Skeletons"},
    "spider":   {"bases": ["minecraft:spider", "minecraft:cave_spider"], "hit": ["minecraft:poison", 80, 0], "uk": "Павуки", "en": "Spiders"},
    "creeper":  {"bases": ["minecraft:creeper"], "hit": ["minecraft:nausea", 100, 0], "uk": "Кріпери", "en": "Creepers"},
    "witch":    {"bases": ["minecraft:witch"], "hit": ["minecraft:weakness", 100, 0], "uk": "Відьми", "en": "Witches"},
    "enderman": {"bases": ["minecraft:enderman"], "hit": ["minecraft:blindness", 40, 0], "uk": "Ендермени", "en": "Endermen"},
    "illager":  {"bases": ["minecraft:pillager", "minecraft:vindicator"], "hit": ["minecraft:mining_fatigue", 100, 0], "uk": "Розбійники", "en": "Illagers"},
    "piglin":   {"bases": ["minecraft:zombified_piglin", "minecraft:piglin"], "hit": ["minecraft:wither", 60, 0], "uk": "Пігліни", "en": "Piglins"},
    "blaze":    {"bases": ["minecraft:blaze"], "hit": ["minecraft:wither", 40, 0], "uk": "Іфрити", "en": "Blazes"},
    "slime":    {"bases": ["minecraft:slime", "minecraft:magma_cube"], "hit": ["minecraft:slowness", 60, 1], "uk": "Слизні", "en": "Slimes"},
    "drowned":  {"bases": ["minecraft:drowned"], "hit": ["minecraft:mining_fatigue", 100, 0], "uk": "Утопленики", "en": "Drowned"},
}

def V(id, family, tier, entity, uk, en, abil=(), hit=(), scale=1.0, equip=None, loot=None, extra=None, mech_uk=""):
    return {"id": id, "family": family, "tier": tier, "entity": entity, "uk": uk, "en": en, "abilities": list(abil),
            "hits": [list(h) for h in hit], "scale": scale, "equip": equip or {}, "loot": loot or family,
            "flags": extra or {}, "mech": mech_uk}

P = lambda e, d, a=0: (e, d, a)
VARIANTS = [
    # zombies: mummies -> kobold dead -> catacomb dead -> dread thralls -> draugr -> trolls -> minotaurs -> elite draugr -> dread knights -> royal draugr
    V("mummy", "zombie", 1, "minecraft:husk", "Мумія", "Mummy", [], [P("minecraft:hunger", 160, 1), P("minecraft:slowness", 40)], 1.05, mech_uk="голод і сповільнення на удар"),
    V("kobold_dead", "zombie", 2, "kobolds:kobold_zombie", "Мертвий кобольд", "Dead Kobold", ["leap"], mech_uk="стрибає на ціль"),
    V("catacomb_dead", "zombie", 3, "irons_spellbooks:catacombs_zombie", "Мрець із катакомб", "Catacomb Dead", ["summon:minecraft:zombie:2"], mech_uk="кличе двох зомбі, коли поранений"),
    V("bog_walker", "zombie", 4, None, "Болотяний ходак", "Bog Walker", ["regen"], [P("minecraft:poison", 80)], 1.15, {"head": "minecraft:leather_helmet"}, mech_uk="отрута, відновлюється, коли його не б'ють"),
    V("dread_thrall", "zombie", 5, "iceandfire:dread_thrall", "Невільник жаху", "Dread Thrall", ["shield"], [P("minecraft:weakness", 100)], mech_uk="щит поглинання, слабкість"),
    V("draugr", "zombie", 6, "cataclysm:draugr", "Драугр", "Draugr", ["shield", "leap"], [P("minecraft:slowness", 60)], mech_uk="щит, стрибок, сповільнення"),
    V("troll", "zombie", 7, "iceandfire:troll", "Троль-громило", "Troll Brute", ["shockwave"], [], mech_uk="ударна хвиля поруч"),
    V("ghoul", "zombie", 8, "iceandfire:dread_ghoul", "Гуль", "Ghoul", ["lifesteal", "leap"], [P("minecraft:hunger", 200, 2)], mech_uk="висмоктує здоров'я, стрибає"),
    V("minotaur", "zombie", 9, "twilightforest:minotaur", "Мінотавр", "Minotaur", ["leap", "shockwave", "enrage"], mech_uk="таран, хвиля, лють"),
    V("elite_draugr", "zombie", 10, "cataclysm:elite_draugr", "Елітний драугр", "Elite Draugr", ["reflect", "lifesteal", "shield"], mech_uk="віддзеркалює удари, щит, кровопивство"),
    V("dread_knight", "zombie", 11, "iceandfire:dread_knight", "Лицар жаху", "Dread Knight", ["enrage", "shockwave", "armor_break"], [P("minecraft:wither", 80, 1)], mech_uk="ламає броню, візер, лють"),
    V("royal_draugr", "zombie", 12, "cataclysm:royal_draugr", "Королівський драугр", "Royal Draugr", ["shield", "shockwave", "summon:cataclysm:draugr:2", "enrage"], [P("minecraft:slowness", 80, 1)], mech_uk="кличе драугрів, щит, хвиля, лють"),
    # skeletons
    V("frost_archer", "skeleton", 1, "minecraft:stray", "Морозний стрілець", "Frost Archer", [], [P("minecraft:slowness", 80, 1)], mech_uk="сильне сповільнення"),
    V("flame_archer", "skeleton", 2, None, "Вогняний лучник", "Flame Archer", ["ignite"], [], 1.0, {"head": "minecraft:golden_helmet"}, mech_uk="підпалює стрілами"),
    V("druid", "skeleton", 3, "twilightforest:skeleton_druid", "Кістяк-друїд", "Skeleton Druid", ["regen"], [P("minecraft:poison", 60)], mech_uk="отруйні чари, відновлення"),
    V("volley_archer", "skeleton", 4, None, "Залповий стрілець", "Volley Archer", ["volley"], [], 1.05, {"chest": "minecraft:chainmail_chestplate"}, mech_uk="залп із трьох стріл"),
    V("koboleton", "skeleton", 6, "cataclysm:koboleton", "Кобольдовий кістяк", "Koboleton", ["leap", "armor_break"], mech_uk="стрибок, ламає броню"),
    V("aptrgangr", "skeleton", 8, "cataclysm:aptrgangr", "Аптрґанґр", "Aptrgangr", ["shockwave", "armor_break"], [P("minecraft:mining_fatigue", 120, 1)], mech_uk="хвиля, ламає броню, втома"),
    V("wither_knight", "skeleton", 9, "eternalnether:wither_skeleton_knight", "Лицар-кістяк", "Wither Knight", ["shield", "enrage"], [P("minecraft:wither", 100, 1)], mech_uk="візер, щит, лють"),
    V("wraith", "skeleton", 11, "twilightforest:wraith", "Мара", "Wraith", ["blink", "lifesteal"], [P("minecraft:darkness", 80)], mech_uk="переноситься, темрява, кровопивство"),
    V("bone_king", "skeleton", 12, "minecraft:wither_skeleton", "Кістяний король", "Bone King", ["volley", "blink", "shield"], [P("minecraft:wither", 120, 2)], 1.3, {"head": "minecraft:golden_helmet", "mainhand": "minecraft:bow"}, mech_uk="залп, перенесення, щит, сильний візер"),
    # spiders
    V("venom_spider", "spider", 1, "minecraft:cave_spider", "Отруйний павук", "Venom Spider", [], [P("minecraft:poison", 120, 1)], 1.4, mech_uk="сильна отрута"),
    V("hedge_spider", "spider", 2, "twilightforest:hedge_spider", "Колючий павук", "Hedge Spider", ["leap"], mech_uk="стрибок"),
    V("swarm_spider", "spider", 3, "twilightforest:swarm_spider", "Рійний павук", "Swarm Spider", ["split:twilightforest:swarm_spider:2"], mech_uk="розпадається на двох малих"),
    V("weaver", "spider", 4, None, "Тенетник", "Weaver", ["web", "pull"], [], 1.2, mech_uk="кидає павутину, підтягує до себе"),
    V("ice_spider", "spider", 5, "irons_spellbooks:ice_spider", "Крижаний павук", "Ice Spider", ["frost_aura"], mech_uk="морозна аура"),
    V("king_spider", "spider", 8, "twilightforest:king_spider", "Павук-король", "King Spider", ["summon:minecraft:cave_spider:3", "web"], mech_uk="кличе павуків, павутина"),
    V("crimson_weaver", "spider", 10, None, "Багряний ткач", "Crimson Weaver", ["web", "leap", "lifesteal"], [P("minecraft:wither", 80, 1)], 1.6, mech_uk="павутина, стрибок, візер, кровопивство"),
    V("brood_mother", "spider", 12, None, "Матір виводку", "Brood Mother", ["summon:minecraft:cave_spider:4", "web", "shockwave", "enrage"], [P("minecraft:poison", 160, 2)], 2.2, mech_uk="кличе виводок, павутина, хвиля, лють"),
    # creepers (all on the creeper itself)
    V("silent_creeper", "creeper", 2, None, "Тихий кріпер", "Silent Creeper", ["invisible"], [], 1.0, mech_uk="майже невидимий"),
    V("fire_creeper", "creeper", 3, None, "Вогняний кріпер", "Fire Creeper", ["fire_aura", "burst_fire"], [], 1.0, mech_uk="аура вогню, вибух підпалює"),
    V("ender_creeper", "creeper", 5, None, "Ендер-кріпер", "Ender Creeper", ["blink"], [], 1.0, mech_uk="переноситься до гравця"),
    V("swarm_creeper", "creeper", 6, None, "Рійний кріпер", "Swarm Creeper", ["split:minecraft:creeper:3"], [], 1.2, mech_uk="після смерті — три малі кріпери"),
    V("storm_creeper", "creeper", 8, None, "Грозовий кріпер", "Storm Creeper", ["powered"], [], 1.0, mech_uk="заряджений"),
    V("frost_creeper", "creeper", 10, None, "Морозний кріпер", "Frost Creeper", ["frost_aura", "powered"], [P("minecraft:slowness", 100, 2)], 1.1, mech_uk="морозна аура, заряджений"),
    V("titan_creeper", "creeper", 12, None, "Кріпер-титан", "Titan Creeper", ["powered", "blink", "shield"], [], 1.8, mech_uk="заряджений, переноситься, щит"),
    # witches
    V("herbalist", "witch", 2, None, "Травниця", "Herbalist", ["regen"], [P("minecraft:poison", 80)], 1.0, mech_uk="відновлення"),
    V("cultist", "witch", 3, "irons_spellbooks:cultist", "Сектант", "Cultist", ["blink"], mech_uk="заклинання, перенесення"),
    V("illusioner", "witch", 4, "friendsandfoes:illusioner", "Ілюзіоніст", "Illusioner", ["blink"], [P("minecraft:blindness", 60)], mech_uk="двійники, сліпота"),
    # Iron's cryomancer and pyromancer are neutral wizards (they ignored players in the test): witches with their magic instead
    V("frost_witch", "witch", 6, None, "Морозна відьма", "Frost Witch", ["frost_aura", "blink"], [P("minecraft:slowness", 80, 1)], 1.05, mech_uk="морозна аура, перенесення, сповільнення"),
    V("ember_witch", "witch", 7, None, "Жарова відьма", "Ember Witch", ["fire_aura", "ignite", "shield"], [], 1.05, mech_uk="аура вогню, підпалює, щит"),
    V("necromancer", "witch", 8, "irons_spellbooks:necromancer", "Некромант", "Necromancer", ["summon:minecraft:skeleton:2", "shield"], mech_uk="піднімає мерців, щит"),
    V("archevoker", "witch", 10, "irons_spellbooks:archevoker", "Архівокер", "Archevoker", ["shield", "blink"], [P("minecraft:weakness", 120, 1)], mech_uk="ікла, вексі, щит"),
    V("hex_witch", "witch", 12, None, "Чаклунка проклять", "Hex Witch", ["blink", "pull", "shield", "summon:minecraft:vex:3"], [P("minecraft:levitation", 40, 1), P("minecraft:wither", 100, 1)], 1.15, mech_uk="левітація, візер, вексі, підтягує"),
    # endermen
    V("warped_enderman", "enderman", 3, "eternalnether:warped_enderman", "Викривлений ендермен", "Warped Enderman", [], [P("minecraft:nausea", 100)], mech_uk="нудота"),
    V("shade_walker", "enderman", 5, None, "Тіньовий мандрівник", "Shade Walker", ["blink"], [P("minecraft:darkness", 100)], 1.1, mech_uk="темрява, перенесення за спину"),
    V("endermaptera", "enderman", 8, "cataclysm:endermaptera", "Ендероптера", "Endermaptera", ["leap", "blink"], mech_uk="стрибки, перенесення"),
    V("void_caller", "enderman", 10, None, "Поклик порожнечі", "Void Caller", ["pull", "blink", "lifesteal"], [P("minecraft:levitation", 30, 1)], 1.2, mech_uk="підтягує, левітація, кровопивство"),
    V("ender_golem", "enderman", 12, "cataclysm:ender_golem", "Ендер-голем", "Ender Golem", ["shockwave", "enrage"], mech_uk="хвиля, лють"),
    # illagers
    V("orc", "illager", 4, "orc_hut:orc", "Орк", "Orc", ["leap"], mech_uk="стрибок"),
    V("goblin_knight", "illager", 6, "twilightforest:upper_goblin_knight", "Гоблін-лицар", "Goblin Knight", ["shield"], mech_uk="щит"),
    V("magehunter", "illager", 7, "irons_spellbooks:magehunter_vindicator", "Мисливець на магів", "Magehunter", ["armor_break"], [P("minecraft:weakness", 100, 1)], mech_uk="ламає броню, слабкість"),
    V("umvuthana_raptor", "illager", 9, "mowziesmobs:umvuthana_raptor", "Умвутана-хижак", "Umvuthana Raptor", ["leap", "enrage"], mech_uk="стрибок, лють"),
    V("warlord", "illager", 11, None, "Воєвода", "Warlord", ["shockwave", "shield", "summon:minecraft:vindicator:2", "enrage"], [P("minecraft:slowness", 60, 1)], 1.25, {"head": "minecraft:netherite_helmet", "mainhand": "minecraft:netherite_axe"}, mech_uk="кличе загін, щит, хвиля, лють"),
    # piglins
    V("piglin_hunter", "piglin", 3, "eternalnether:piglin_hunter", "Піґлін-мисливець", "Piglin Hunter", ["leap"], mech_uk="стрибок"),
    V("corpor", "piglin", 5, "eternalnether:corpor", "Корпор", "Corpor", ["shield"], mech_uk="щит"),
    V("ignited_revenant", "piglin", 7, "cataclysm:ignited_revenant", "Палаючий ревенант", "Ignited Revenant", ["fire_aura"], mech_uk="аура вогню"),
    V("wraither", "piglin", 9, "eternalnether:wraither", "Рейзер", "Wraither", ["blink", "lifesteal"], [P("minecraft:wither", 80, 1)], mech_uk="перенесення, візер"),
    V("ignited_berserker", "piglin", 11, "cataclysm:ignited_berserker", "Палаючий берсерк", "Ignited Berserker", ["enrage", "fire_aura", "leap"], mech_uk="лють, вогонь, стрибок"),
    # blazes and slimes
    V("hellflare", "blaze", 4, None, "Пекельний вогник", "Hellflare", ["fire_aura", "shield"], [], 1.1, mech_uk="аура вогню, щит"),
    V("wildfire", "blaze", 9, "friendsandfoes:wildfire", "Дикий вогонь", "Wildfire", ["fire_aura", "enrage"], mech_uk="щити-іфрити, аура вогню"),
    V("maze_slime", "slime", 2, "twilightforest:maze_slime", "Лабіринтовий слиз", "Maze Slime", [], [P("minecraft:slowness", 60, 1)], mech_uk="сповільнення"),
    V("acid_slime", "slime", 6, None, "Кислотний слиз", "Acid Slime", ["armor_break", "regen"], [P("minecraft:poison", 100, 1)], 1.0, mech_uk="ламає броню, отрута"),
    V("ferrouslime", "slime", 9, "alexscaves:ferrouslime", "Залізний слиз", "Ferrouslime", ["shield", "pull"], mech_uk="магнітом підтягує, щит"),
    # drowned
    V("deepling", "drowned", 3, "cataclysm:deepling", "Глибинник", "Deepling", ["leap"], mech_uk="стрибок"),
    V("deepling_angler", "drowned", 5, "cataclysm:deepling_angler", "Глибинник-рибалка", "Deepling Angler", ["pull"], mech_uk="підтягує гаком"),
    V("deepling_priest", "drowned", 7, "cataclysm:deepling_priest", "Глибинник-жрець", "Deepling Priest", ["shield", "regen"], mech_uk="щит, відновлення"),
    V("deepling_brute", "drowned", 9, "cataclysm:deepling_brute", "Глибинник-громило", "Deepling Brute", ["shockwave", "enrage"], mech_uk="хвиля, лють"),
    V("deepling_warlock", "drowned", 11, "cataclysm:deepling_warlock", "Глибинник-чорнокнижник", "Deepling Warlock", ["blink", "shield", "lifesteal"], [P("minecraft:darkness", 80)], mech_uk="перенесення, темрява, щит"),
]

ABILITY_UK = {"leap": "стрибок до цілі", "blink": "перенесення за спину цілі", "shockwave": "ударна хвиля (відкидає й ранить усіх поруч)",
              "enrage": "лють нижче 30 % здоров'я (швидкість і сила)", "regen": "відновлення, коли його не б'ють 5 с", "shield": "щит поглинання кожні 15 с",
              "lifesteal": "лікується на 25 % завданої шкоди", "reflect": "повертає 20 % шкоди ближнього бою", "armor_break": "удар знімає 4 одиниці броні на 6 с",
              "ignite": "удар підпалює", "fire_aura": "підпалює гравців поруч", "frost_aura": "морозить і сповільнює гравців поруч", "web": "кидає павутину під ноги цілі (зникає за 5 с)",
              "pull": "підтягує ціль до себе", "volley": "залп із трьох стріл", "invisible": "невидимий", "powered": "заряджений", "burst_fire": "вибух підпалює",
              "summon": "кличе підмогу, коли здоров'я нижче половини", "split": "після смерті розпадається", "family_hit": "ефект родини на удар", "summon_guard": "кличе варту на 66 % і 33 % здоров'я"}

# ---------------------------------------------------------------- loot (item, weight, min, max) by tier band and by family
def L(item, w, lo=1, hi=1): return (item, w, lo, hi)
TIER_LOOT = {
    1: [L("minecraft:iron_nugget", 30, 2, 6), L("minecraft:gold_nugget", 20, 2, 5), L("minecraft:copper_ingot", 20, 1, 3), L("minecraft:leather", 15, 1, 2), L("minecraft:arrow", 15, 3, 8), L("minecraft:book", 6), L("minecraft:experience_bottle", 8)],
    2: [L("minecraft:iron_ingot", 25, 1, 2), L("minecraft:gold_ingot", 15, 1, 2), L("minecraft:lapis_lazuli", 15, 2, 5), L("minecraft:redstone", 15, 2, 6), L("create:zinc_ingot", 12, 1, 2), L("mekanism:ingot_tin", 10, 1, 2), L("minecraft:experience_bottle", 10, 1, 2), L("minecraft:name_tag", 3), L("minecraft:saddle", 3)],
    3: [L("minecraft:emerald", 20, 1, 3), L("create:brass_ingot", 15, 1, 2), L("mekanism:ingot_osmium", 15, 1, 2), L("iceandfire:silver_ingot", 12, 1, 2), L("ars_nouveau:source_gem", 12, 1, 3), L("irons_spellbooks:arcane_essence", 10, 1, 3), L("apotheosis:gem_dust", 6, 1, 3), L("apotheosis:mysterious_scrap_metal", 4), L("minecraft:experience_bottle", 10, 2, 3)],
    4: [L("minecraft:diamond", 12), L("minecraft:emerald", 15, 2, 4), L("minecraft:amethyst_shard", 12, 2, 5), L("iceandfire:sapphire_gem", 8), L("irons_spellbooks:common_ink", 8), L("irons_spellbooks:uncommon_ink", 5), L("irons_spellbooks:mithril_scrap", 4), L("apotheosis:gem_dust", 8, 2, 4), L("apotheosis:timeworn_fabric", 4), L("minecraft:golden_apple", 4), L("create:experience_nugget", 10, 3, 8)],
    5: [L("minecraft:diamond", 15, 1, 2), L("minecraft:netherite_scrap", 5), L("irons_spellbooks:rare_ink", 5), L("irons_spellbooks:upgrade_orb", 4), L("cataclysm:ancient_metal_nugget", 8, 2, 5), L("twilightforest:knightmetal_ingot", 6, 1, 2), L("apotheosis:luminous_crystal_shard", 4), L("apotheosis:sigil_of_socketing", 2), L("minecraft:echo_shard", 4), L("minecraft:totem_of_undying", 1), L("minecraft:experience_bottle", 12, 3, 5)],
    6: [L("minecraft:netherite_scrap", 8, 1, 2), L("minecraft:ancient_debris", 4), L("irons_spellbooks:epic_ink", 4), L("cataclysm:ancient_metal_ingot", 6), L("cataclysm:witherite_ingot", 3), L("twilightforest:fiery_ingot", 4), L("apotheosis:sigil_of_enhancement", 2), L("apotheosis:godforged_pearl", 1), L("minecraft:enchanted_golden_apple", 1), L("minecraft:nether_star", 1), L("minecraft:experience_bottle", 12, 4, 8)],
}
def band(tier): return min(6, (tier + 1) // 2)

FAMILY_LOOT = {
    "zombie": [L("minecraft:rotten_flesh", 30, 1, 3), L("minecraft:iron_nugget", 15, 1, 4), L("minecraft:paper", 10, 1, 3), L("iceandfire:troll_tusk", 3), L("iceandfire:dread_shard", 4, 1, 2)],
    "skeleton": [L("minecraft:bone", 30, 2, 4), L("minecraft:arrow", 20, 4, 10), L("minecraft:spectral_arrow", 8, 2, 4), L("minecraft:wither_skeleton_skull", 1), L("irons_spellbooks:frozen_bone", 4)],
    "spider": [L("minecraft:string", 30, 2, 5), L("minecraft:spider_eye", 15, 1, 2), L("minecraft:cobweb", 8, 1, 3), L("twilightforest:naga_scale", 2)],
    "creeper": [L("minecraft:gunpowder", 30, 2, 5), L("minecraft:fire_charge", 10, 1, 3), L("minecraft:glowstone_dust", 10, 2, 4), L("irons_spellbooks:lightning_upgrade_orb", 1)],
    "witch": [L("minecraft:glowstone_dust", 15, 1, 4), L("minecraft:sugar", 15, 1, 4), L("minecraft:glass_bottle", 15, 1, 3), L("irons_spellbooks:arcane_essence", 12, 1, 3), L("ars_nouveau:source_gem", 10, 1, 2), L("apotheosis:potion_charm", 1)],
    "enderman": [L("minecraft:ender_pearl", 30, 1, 2), L("minecraft:ender_eye", 6), L("minecraft:chorus_fruit", 10, 1, 3), L("minecraft:shulker_shell", 2), L("irons_spellbooks:ender_upgrade_orb", 1)],
    "illager": [L("minecraft:emerald", 25, 1, 4), L("minecraft:arrow", 15, 3, 8), L("minecraft:goat_horn", 1), L("minecraft:ominous_trial_key", 1), L("minecraft:totem_of_undying", 1)],
    "piglin": [L("minecraft:gold_ingot", 25, 1, 3), L("minecraft:quartz", 20, 2, 5), L("minecraft:blaze_rod", 8), L("cataclysm:ignitium_ingot", 1)],
    "blaze": [L("minecraft:blaze_rod", 30, 1, 2), L("irons_spellbooks:cinder_essence", 10, 1, 2), L("minecraft:fire_charge", 15, 1, 3), L("irons_spellbooks:fire_upgrade_orb", 1)],
    "slime": [L("minecraft:slime_ball", 30, 2, 5), L("minecraft:magma_cream", 15, 1, 3), L("minecraft:iron_ingot", 6), L("twilightforest:steeleaf_ingot", 4)],
    "drowned": [L("minecraft:prismarine_shard", 20, 2, 5), L("minecraft:prismarine_crystals", 15, 1, 4), L("minecraft:nautilus_shell", 5), L("minecraft:trident", 1), L("minecraft:heart_of_the_sea", 1)],
}

def loot_table(entries, rolls):
    for it, *_ in entries:
        if it not in ITEMS: sys.exit(f"unknown item {it}")
    return {"type": "minecraft:entity", "random_sequence": "wilds_core:mobs", "pools": [{
        "rolls": rolls, "bonus_rolls": 0.5,
        "entries": [{"type": "minecraft:item", "name": it, "weight": w,
                     "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}] if hi > 1 else []}
                    for it, w, lo, hi in entries]}]}

# ---------------------------------------------------------------- output
def main():
    errs = []
    ids = set()
    for v in VARIANTS:
        if v["id"] in ids: errs.append("duplicate " + v["id"])
        ids.add(v["id"])
        if v["entity"] and v["entity"] not in ENTITIES: errs.append(f"{v['id']}: unknown entity {v['entity']}")
        for a in v["abilities"]:
            if a.startswith(("summon:", "split:")):
                e = a.split(":", 1)[1].rsplit(":", 1)[0]
                if e not in ENTITIES: errs.append(f"{v['id']}: unknown entity {e}")
            elif a not in ABILITY_UK: errs.append(f"{v['id']}: unknown ability {a}")
        for h in v["hits"]:
            pass
    if errs: sys.exit("\n".join(errs))

    data = {"settings": SETTINGS, "families": FAMILIES, "variants": [{k: v[k] for k in ("id", "family", "tier", "entity", "abilities", "hits", "scale", "equip", "loot")} for v in VARIANTS]}
    out = os.path.join(RES, "data", "wilds_core", "wilds_mobs")
    os.makedirs(out, exist_ok=True)
    json.dump(data, open(os.path.join(out, "variants.json"), "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)

    lt = os.path.join(RES, "data", "wilds_core", "loot_table", "mobs")
    os.makedirs(lt, exist_ok=True)
    for b, entries in TIER_LOOT.items():
        json.dump(loot_table(entries, {"type": "minecraft:uniform", "min": 1, "max": 1 + (b + 1) // 2}), open(os.path.join(lt, f"tier_{b}.json"), "w", newline="\n"), indent=1)
    for f, entries in FAMILY_LOOT.items():
        json.dump(loot_table(entries, 1), open(os.path.join(lt, f"family_{f}.json"), "w", newline="\n"), indent=1)

    for lang, key in (("uk_ua", "uk"), ("en_us", "en")):
        p = os.path.join(RES, "assets", "wilds_core", "lang", lang + ".json")
        d = json.load(open(p, encoding="utf-8"))
        for v in VARIANTS: d[f"wilds_core.mob.{v['id']}"] = v[key]
        d["wilds_core.mob.named"] = "%s · %s рів." if key == "uk" else "%s · lvl %s"
        d["wilds_core.mob.level"] = "Рівень істоти: %s" if key == "uk" else "Creature level: %s"
        json.dump(d, open(p, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=2)

    doc, ok = document()
    os.makedirs(os.path.join(ROOT, "docs", "balance"), exist_ok=True)
    open(os.path.join(ROOT, "docs", "balance", "mob-levels.md"), "w", encoding="utf-8", newline="\n").write(doc)
    print(f"{len(VARIANTS)} variants, {len(TIER_LOOT)} tier and {len(FAMILY_LOOT)} family loot tables; checks {'passed' if ok else 'FAILED'}")
    if not ok: sys.exit(1)


def document():
    ok = True
    o = []
    w = o.append
    w("# Рівні мобів, нові види, лут і боси\n")
    w("Згенеровано `pack/tools/gen_mob_balance.py` — не редагуйте вручну. Код: `mods-src/wilds_core/src/main/java/ua/wilds/core/mobs/`, дані: "
      "`data/wilds_core/wilds_mobs/variants.json` і `loot_table/mobs/`. Minecraft 1.21.1, NeoForge 21.1.264.\n")
    w("## Припущення (позначені, можна змінити в генераторі)\n")
    w("- **A1. «Рівень гравця»** — рівень дерева навичок «Шлях мандрівника» (Pufferfish's Skills). Він не падає після смерті й не витрачається на зачарування, на відміну від рівня досвіду. Рівень 15 — це приблизно залізне спорядження.")
    w("- **A2. «Поряд»** — радіус 128 блоків від моба (куля; це межа, у якій гра взагалі спавнить мобів біля гравця). Враховуються гравці в режимі виживання й пригод. Моб бере рівень найсильнішого з них.")
    w("- **A3. Складність світу Apotheosis** (вибір у меню) множить рівень: " + ", ".join(f"{k} ×{v}" for k, v in WORLD_TIER.items()) + ". Тепер вибір відчутний: на «Піку» гравець 100-го рівня зустрічає мобів 175-го (і кращий лут).")
    w("- **A4. Еталонний гравець**: удар повної сили без крита. Шкода зброї — крива `D(L)`. Захист — таблиця нижче (броня, міцність, захист зачарувань, здоров'я).")
    w("- **A5.** Рівень і посилення діють **проти гравців**. Мешканці колоній, селяни й улюбленці б'ють мобів і отримують від них шкоду так, ніби моби ванільні, тож колонії MineColonies не гинуть від сильних мобів поряд із гравцем.")
    w("- **A6. Боси** — тег `c:bosses`, усі істоти зі смугою боса (клас має `ServerBossEvent`), список у коді. Дракони Ice and Fire, колонії, прирученці й закликані істоти рівнів не мають.")
    w("- **A7.** Заміна стосується лише природного спавну (не спавнерів, не яєць, не структур із власними мобами).\n")
    w("## Формули\n")
    w("```")
    w("ціль       T  = max по гравцях у радіусі 128 (рівень_навичок × множник_складності_світу)")
    w("при появі  ℓ  = T × випадкове(0.9 … 1.05)")
    w("кожні 10 с ℓ += обмежити(T − ℓ, ±(1 + 0.01·ℓ))      ← рівень змінюється плавно в обидва боки; якщо поруч нікого, не змінюється")
    w("D(L)       = 6 · ((L + 15) / 30)^0.8                  ← шкода удару еталонного гравця")
    w(f"звичайний моб: здоров'я × (1 + {HP_RATE}·ℓ), шкода × (1 + {DMG_RATE}·ℓ), броня + min(12, ℓ/25), міцність + min(8, ℓ/50)")
    w("              швидкість × (1 + min(0.2, ℓ/2000)), опір відкиданню + min(0.4, ℓ/1000)")
    w("              опір вогню, блискавці, вибухам, магії й морозу: min(30 %, ℓ/1300)")
    w(f"новий вид рівня k (відкривається на U_k): здоров'я = {TIER_HITS} · D'(U_k) · (1 + {HP_RATE}ℓ)/(1 + {HP_RATE}U_k), де D' — шкода після броні моба")
    w(f"              шкода = сира шкода, що вбиває еталонного гравця рівня U_k за {TIER_PLAYER_HITS} ударів, × (1 + {DMG_RATE}ℓ)/(1 + {DMG_RATE}U_k); броня + 0.6·k")
    w(f"бос:          здоров'я = max(власне × (1 + {BOSS_HP_RATE}·ℓ), {BOSS_HITS} · D(ℓ)), шкода = max(власна × (1 + {BOSS_DMG_RATE}·ℓ), вбиває еталонного гравця за {BOSS_PLAYER_HITS} ударів)")
    w("              рівень боса тільки росте (новий сильніший гравець поруч), але не падає — його не «приспиш», підвівши слабшого")
    w("заміна при спавні: шанс = min(0.7, 0.2 + ℓ/500), якщо в родини вже є відкриті види; серед відкритих найновіший має вагу 1, попередній 1/2, далі 1/4…")
    w("```\n")
    w("Множник шкоди застосовується до всього, що завдає моб: удари, стріли, закляття. Вибухи множаться на корінь із множника, щоб кріпери не стали ваншотом.\n")
    w("## Захист еталонного гравця\n")
    w("| Рівень | Броня | Міцність | Захист (EPF) | Здоров'я | Шкода удару D(L) |\n|---|---|---|---|---|---|")
    for L_ in (0, 15, 30, 60, 100, 150, 200, 300, 400):
        a, t, e, hp = defence(L_)
        w(f"| {L_} | {a:.0f} | {t:.0f} | {e:.0f} | {hp:.0f} | {gear_damage(L_):.1f} |")
    w("\nЗбройна крива: залізний меч (6) на 15-му рівні, діамантовий із Гостротою — на 30–60-му, незерит з афіксами й самоцвітами — на 100–200-му, найкраще у збірці (драконяча сталь, міфічні афікси) — на 300–400-му.\n")

    w("## Перевірка 1: ванільний моб (зомбі, 20 здоров'я, 2 броні)\n")
    w("Ціль за брифом: 4 удари для еталону на 15-му рівні, 1–2 удари в пізній грі (≥150).\n")
    w("| Рівень гравця = рівень моба | Здоров'я | Броня | Ударів гравця | Шкода зомбі по гравцю | Ударів, щоб убити гравця | OK |\n|---|---|---|---|---|---|---|")
    for L_ in (0, 15, 30, 60, 100, 150, 200, 250, 300, 350, 400):
        hp = 20 * hp_mult(L_); ar = 2 + armor_add(L_); to = tough_add(L_)
        h = hits(hp, ar, to, L_); n = math.ceil(h - 1e-9)
        dmg = 3 * dmg_mult(L_); dp, php = to_player(dmg, L_)
        good = (n == 4) if L_ == 15 else (n <= 2) if L_ >= 150 else True
        ok &= good
        w(f"| {L_} | {hp:.0f} | {ar:.1f} | {h:.2f} → **{n}** | {dmg:.1f} → {dp:.2f} | {math.ceil(php / dp)} | {'✓' if good else '✗'} |")
    w("")

    w("## Перевірка 2: нові види на рівні відкриття і пізніше\n")
    w(f"Ціль: на рівні відкриття близько 4 ударів (стеля від {TIER_HITS}), вид убиває еталонного гравця свого рівня за ~{TIER_PLAYER_HITS} ударів. На 100 рівнів пізніше гравець сильнішає, і вид легшає (але не до 1 удару — тоді вже з'явилися новіші).\n")
    w("| Рівень виду | Відкривається | Здоров'я тоді | Ударів | Сира шкода | Ударів по гравцю | Через +100 рівнів: здоров'я / ударів | OK |\n|---|---|---|---|---|---|---|---|")
    for k, u in TIERS:
        hp = tier_hp(k, u); ar = armor_add(u, k); to = tough_add(u)
        h = hits(hp, ar, to, u); n = math.ceil(h - 1e-9)
        dmg = tier_dmg(k, u); dp, php = to_player(dmg, u)
        l2 = min(400, u + 100); hp2 = tier_hp(k, l2); h2 = hits(hp2, armor_add(l2, k), tough_add(l2), l2)
        good = n == 4 and 8 <= php / dp <= 12
        ok &= good
        w(f"| {k} | {u} | {hp:.0f} | {h:.2f} → **{n}** | {dmg:.1f} | {php / dp:.1f} | {hp2:.0f} / {h2:.1f} | {'✓' if good else '✗'} |")
    w("")

    w("## Перевірка 3: боси без зілля, яблук і заклять\n")
    w(f"Ціль: «дуже складно» для гравця зі звичайним спорядженням свого рівня — щонайменше {BOSS_HITS} ударів, а бос вбиває за {BOSS_PLAYER_HITS} ударів. Приклади з власним здоров'ям і шкодою.\n")
    w("| Бос (власні здоров'я / шкода) | Рівень | Здоров'я | Ударів гравця | Шкода | Ударів по гравцю | OK |\n|---|---|---|---|---|---|---|")
    for name, nhp, nd in (("Нага TF", 120, 6), ("Візер", 300, 8), ("Ігніс (Cataclysm)", 800, 18), ("Ендер-дракон", 200, 10)):
        for L_ in (15, 60, 150, 300, 400):
            hp = boss_hp(nhp, L_); ar = armor_add(L_); to = tough_add(L_)
            h = hits(hp, ar, to, L_); dmg = boss_dmg(nd, L_); dp, php = to_player(dmg, L_)
            good = h >= BOSS_HITS * 0.9 and php / dp <= BOSS_PLAYER_HITS + 0.01
            ok &= good
            w(f"| {name} ({nhp}/{nd}) | {L_} | {hp:.0f} | {h:.0f} | {dmg:.1f} | {php / dp:.1f} | {'✓' if good else '✗'} |")
    w("\nБоси ще отримують: ударну хвилю (кожні 8 с), лють нижче 30 % здоров'я, виклик варти на 66 % і 33 %. Рівень боса не падає.\n")

    w("## Прогресія за рівнями гравця\n")
    w("Які нові види вже можуть з'явитися замість ванільних (найновіший — найчастіше):\n")
    w("| Рівень | Шанс заміни | " + " | ".join(FAMILIES[f]["uk"] for f in FAMILIES) + " |\n|---|---|" + "---|" * len(FAMILIES))
    for L_ in (0, 10, 20, 35, 50, 70, 95, 125, 160, 200, 250, 310, 380, 400):
        cells = []
        for f in FAMILIES:
            un = [v for v in VARIANTS if v["family"] == f and UNLOCK[v["tier"]] <= L_]
            cells.append(un[-1]["uk"] if un else "—")
        chance = min(0.7, 0.2 + L_ / 500) if any(UNLOCK[v["tier"]] <= L_ for v in VARIANTS) else 0
        w(f"| {L_} | {chance * 100:.0f} % | " + " | ".join(cells) + " |")
    w("\nАтаки, які відкриваються з рівнем у **всіх** мобів (кидок при появі): " + "; ".join(f"{ABILITY_UK[a]} — від {lv} рівня, шанс {int(c * 100)} %" for a, lv, c in SETTINGS["level_abilities"]) + ".\n")

    w(f"## Нові види ({len(VARIANTS)})\n")
    w("«На місці» означає, що це той самий ванільний моб з новим ім'ям, спорядженням і механіками. В інших випадках за основу взято істоту з модів збірки.\n")
    w("| # | Вид | Родина | Рівень (відкриття) | Основа | Механіки | Лут |\n|---|---|---|---|---|---|---|")
    for i, v in enumerate(VARIANTS, 1):
        base = v["entity"] or "на місці (" + FAMILIES[v["family"]]["bases"][0] + ")"
        mech = v["mech"] or ", ".join(v["abilities"])
        w(f"| {i} | {v['uk']} | {FAMILIES[v['family']]['uk']} | {v['tier']} ({UNLOCK[v['tier']]}) | `{base}` | {mech} | родина + ярус {band(v['tier'])} |")
    w("\nПояснення механік: " + "; ".join(f"**{k}** — {t}" for k, t in ABILITY_UK.items()) + ".\n")

    w("## Лут\n")
    w("Кожен новий вид дає свій звичайний лут, а також випадіння з таблиці родини та з таблиці ярусу (ярус = ⌈рівень виду / 2⌉, кидків 1…1+⌈ярус/2⌉, удача і «Здобич» додають). "
      "Звичайні моби з рівнем мають шанс 12 % + ℓ/1000 на одне випадіння з таблиці родини. Досвід: нові види ×(1 + 0.3·k), боси ×(1 + ℓ/100), звичайні ×(1 + ℓ/200). "
      "Лут дає лише смерть від гравця; підмога, яку кличуть моби, і уламки після «розпаду» луту не дають.\n")
    for b, entries in TIER_LOOT.items():
        w(f"- **Ярус {b}** (види {2 * b - 1}–{2 * b}): " + ", ".join(f"`{it}`" for it, *_ in entries))
    w("")
    for f, entries in FAMILY_LOOT.items():
        w(f"- **{FAMILIES[f]['uk']}**: " + ", ".join(f"`{it}`" for it, *_ in entries))
    w("\n## Результат перевірки\n")
    w("Усі етапи відповідають цілям брифу." if ok else "**Є етапи, що не відповідають цілям — див. ✗ вище.**")
    return "\n".join(o) + "\n", ok


if __name__ == "__main__":
    main()
