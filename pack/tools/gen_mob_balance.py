"""Mob levels, new creatures and their loot (version 2, 22.11): one source for the game data and the design document.

  python pack/tools/gen_mob_balance.py

Writes
  mods-src/wilds_core/src/main/resources/data/wilds_core/wilds_mobs/variants.json   read by ua.wilds.core.mobs.MobData
  mods-src/wilds_core/src/main/resources/data/wilds_core/loot_table/mobs/*.json      tier and family loot
  mods-src/wilds_core/src/main/resources/assets/wilds_core/lang/{uk_ua,en_us}.json   names of the new creatures (merged)
  docs/balance/mob-levels.md                                                         formulas, tables, checks

Version 1 (21.11) failed in play: it took the skill tree level as "the player's level", players there are at 270-470,
so a fresh player met level-474 draugr that killed in two hits; half the "new kinds" were vanilla mobs renamed; and the
tier loot gave enchanted apples and netherite by the stack. Version 2: the player's level is measured from what they
actually wear and wield; no single hit of a mob can take more than a fixed share of the player's health; every new kind
is a creature of its own (models and AI of Born in Chaos, Rotten Creatures, Mutant Monsters, Variants&Ventures,
Undead Unleashed, Cataclysm, Ice and Fire, Twilight Forest, Iron's Spells...); loot is a small extra, not a jackpot.

The formulas here and in ua.wilds.core.mobs.MobMath must stay the same. The checks at the end fail (exit 1) when a stage
misses the brief's targets.
"""
import json, math, os, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RES = os.path.join(ROOT, "mods-src", "wilds_core", "src", "main", "resources")
ITEMS = set(open(os.path.join(ROOT, "docs", "registries", "item.txt"), encoding="utf-8").read().split())
ENTITIES = set(open(os.path.join(ROOT, "docs", "registries", "entity_type.txt"), encoding="utf-8").read().split())
VERSION = 2

# ---------------------------------------------------------------- the reference player (mirror of MobMath.java)
def gear_damage(L):
    """one full-strength hit of a player at power level L: iron sword 6 at 15, diamond + Sharpness ~10 at 50,
    netherite with affixes ~16-22 at 100-150, the best of the pack 30-45 at 250-400"""
    return 6.0 * ((L + 15.0) / 30.0) ** 0.8

def weapon_level(dmg):
    return max(0.0, 30.0 * (max(dmg, 0.1) / 6.0) ** 1.25 - 15.0)

# defence by power level: (level, armor, toughness, protection EPF, max health)
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

def ehp(armor, tough, epf, hp):
    """health measured in raw 10-damage hits (what the player's defence is worth)"""
    return hp / (after_armor(10.0, armor, tough) * (1.0 - min(20.0, epf) / 25.0))

def defence_level(armor, tough, epf, hp):
    e = ehp(armor, tough, epf, hp)
    for L in range(0, 401):
        if ehp(*defence(L)) >= e: return float(L)
    return 400.0

WEAPON_WEIGHT = 0.85  # the weapon counts more: mob health is measured in the player's hits

def power_level(dmg, armor, tough, epf, hp):
    """the player's level: what their weapon (85 %) and their defence (15 %) are worth"""
    return min(400.0, WEAPON_WEIGHT * weapon_level(dmg) + (1 - WEAPON_WEIGHT) * defence_level(armor, tough, epf, hp))

def to_player(raw, L):
    armor, tough, epf, hp = defence(L)
    return after_armor(raw, armor, tough) * (1.0 - min(20.0, epf) / 25.0), hp

# ---------------------------------------------------------------- mob side
HP_RATE, DMG_RATE, BOSS_HP_RATE, BOSS_DMG_RATE = 0.006, 0.01, 0.005, 0.012
CAP_PLAIN, CAP_KIND, CAP_BOSS = 0.20, 0.25, 0.40     # largest share of the player's max health one hit can take
KIND_DMG_STEP = 0.10                                  # a kind of tier k hits (1 + 0.10 k) times its own damage
def hp_mult(l): return 1.0 + HP_RATE * l
def dmg_mult(l): return 1.0 + DMG_RATE * l
def armor_add(l, tier=0): return min(10.0, l / 35.0) + 0.4 * tier
def tough_add(l): return min(6.0, l / 60.0)
def speed_add(l): return min(0.15, l / 2500.0)
def kb_add(l): return min(0.40, l / 1000.0)
def resist(l): return min(0.25, l / 1500.0)

TIERS = [(1, 8), (2, 15), (3, 25), (4, 35), (5, 50), (6, 65), (7, 85), (8, 105), (9, 130), (10, 160), (11, 200), (12, 250)]
UNLOCK = dict(TIERS)
TIER_HITS = 3.7
BOSS_HITS = 50

def hits(hp, armor, tough, L):
    return hp / after_armor(gear_damage(L), armor, tough)

def tier_hp(k, l):
    u = UNLOCK[k]
    return TIER_HITS * after_armor(gear_damage(u), armor_add(u, k), tough_add(u)) * hp_mult(l) / hp_mult(u)

def boss_hp(native, l): return max(native * (1 + BOSS_HP_RATE * l), BOSS_HITS * gear_damage(l))

def hits_to_die(raw, L, cap):
    d, hp = to_player(raw, L)
    return math.ceil(hp / min(d, cap * hp) - 1e-9)

WORLD_TIER = {"haven": 1.0, "frontier": 1.1, "ascent": 1.2, "summit": 1.35, "pinnacle": 1.5}
SETTINGS = {"version": VERSION, "weapon_weight": WEAPON_WEIGHT, "radius": 128, "update_ticks": 200, "spawn_spread": [0.9, 1.05], "step_base": 1.0, "step_rate": 0.01,
            "replace_base": 0.15, "replace_rate": 1 / 600.0, "replace_max": 0.55, "world_tier": WORLD_TIER,
            "hp_rate": HP_RATE, "dmg_rate": DMG_RATE, "boss_hp_rate": BOSS_HP_RATE, "boss_dmg_rate": BOSS_DMG_RATE,
            "tier_hits": TIER_HITS, "boss_hits": BOSS_HITS, "kind_dmg_step": KIND_DMG_STEP,
            "cap_plain": CAP_PLAIN, "cap_kind": CAP_KIND, "cap_boss": CAP_BOSS,
            "defence": DEFENCE, "tiers": [{"tier": k, "unlock": u} for k, u in TIERS],
            "level_abilities": [["family_hit", 30, 0.35], ["leap", 70, 0.25], ["enrage", 120, 0.25], ["shockwave", 180, 0.2]],
            "boss_abilities": ["shockwave", "enrage", "summon_guard"],
            "kind_loot_chance": 0.5, "tier_loot_chance": 0.2, "plain_loot_chance": 0.06}

# ---------------------------------------------------------------- families and the new creatures
FAMILIES = {
    "zombie":   {"bases": ["minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager"], "hit": ["minecraft:hunger", 100, 0], "uk": "Зомбі"},
    "skeleton": {"bases": ["minecraft:skeleton", "minecraft:stray", "minecraft:bogged"], "hit": ["minecraft:slowness", 60, 0], "uk": "Скелети"},
    "spider":   {"bases": ["minecraft:spider", "minecraft:cave_spider"], "hit": ["minecraft:poison", 60, 0], "uk": "Павуки"},
    "creeper":  {"bases": ["minecraft:creeper"], "hit": ["minecraft:nausea", 80, 0], "uk": "Кріпери"},
    "witch":    {"bases": ["minecraft:witch"], "hit": ["minecraft:weakness", 80, 0], "uk": "Відьми"},
    "enderman": {"bases": ["minecraft:enderman"], "hit": ["minecraft:blindness", 30, 0], "uk": "Ендермени"},
    "illager":  {"bases": ["minecraft:pillager", "minecraft:vindicator"], "hit": ["minecraft:mining_fatigue", 80, 0], "uk": "Розбійники"},
    "piglin":   {"bases": ["minecraft:zombified_piglin", "minecraft:piglin"], "hit": ["minecraft:wither", 40, 0], "uk": "Пігліни"},
    "blaze":    {"bases": ["minecraft:blaze"], "hit": ["minecraft:wither", 30, 0], "uk": "Іфрити"},
    "drowned":  {"bases": ["minecraft:drowned"], "hit": ["minecraft:mining_fatigue", 80, 0], "uk": "Утопленики"},
}

def V(entity, family, tier, uk, abil=(), mech=""):
    return {"id": entity.split(":")[1], "entity": entity, "family": family, "tier": tier, "uk": uk, "abilities": list(abil), "mech": mech}

BIC = "born_in_chaos_v1:"
VARIANTS = [
    # zombies: mummies and swamp dead -> lumberjacks, miners, barrels -> clowns, burned, frostbitten -> bruisers, thralls,
    # draugr -> immortals, trolls -> pumpkin bruisers, minotaurs -> elite draugr, dread knights -> mutant zombies, royal draugr
    V("rottencreatures:mummy", "zombie", 1, "Мумія", mech="Rotten Creatures: виходить із піску пустель, кличе скарабеїв"),
    V("variantsandventures:thicket", "zombie", 1, "Чагарник", mech="Variants&Ventures: зомбі, оброслий лісом"),
    V(BIC + "decaying_zombie", "zombie", 1, "Гниючий зомбі", mech="Born in Chaos: розпадається, отруює"),
    V("rottencreatures:swampy", "zombie", 2, "Болотяник", mech="Rotten Creatures: отруйний мешканець боліт"),
    V("variantsandventures:gelid", "zombie", 2, "Крижаник", mech="Variants&Ventures: кидає сніжки, морозить"),
    V(BIC + "zombie_lumberjack", "zombie", 2, "Мертвий лісоруб", mech="Born in Chaos: рубає сокирою"),
    V("rottencreatures:undead_miner", "zombie", 3, "Мертвий шахтар", mech="Rotten Creatures: живе в шахтах, носить кирку"),
    V(BIC + "barrel_zombie", "zombie", 3, "Зомбі-бочка", ["shield"], "Born in Chaos: ховається в бочці"),
    V(BIC + "zombie_clown", "zombie", 4, "Зомбі-клоун", ["leap"], "Born in Chaos: стрибає й сміється"),
    V("rottencreatures:burned", "zombie", 4, "Обвуглений", mech="Rotten Creatures: горить сам і підпалює"),
    V("rottencreatures:frostbitten", "zombie", 5, "Обморожений", ["frost_aura"], "Rotten Creatures: холод, сповільнює"),
    V(BIC + "zombie_bruiser", "zombie", 5, "Зомбі-громило", ["shockwave"], "Born in Chaos: важкий удар"),
    V("iceandfire:dread_thrall", "zombie", 6, "Невільник жаху", ["shield"], "Ice and Fire: воїн Лича жаху"),
    V("cataclysm:draugr", "zombie", 6, "Драугр", ["leap"], "Cataclysm: північний мертвий воїн"),
    V("rottencreatures:immortal", "zombie", 7, "Безсмертний", ["regen"], "Rotten Creatures: воскресає, кличе блискавки"),
    V("iceandfire:troll", "zombie", 7, "Троль", ["shockwave"], "Ice and Fire: великий троль із дрючком"),
    V(BIC + "pumpkin_bruiser", "zombie", 8, "Гарбузовий громило", ["shockwave"], "Born in Chaos: гарбузоголовий велетень"),
    V("twilightforest:minotaur", "zombie", 8, "Мінотавр", ["leap"], "Twilight Forest: таранить сокирою"),
    V("cataclysm:elite_draugr", "zombie", 9, "Елітний драугр", ["shield"], "Cataclysm: драугр у важкій броні"),
    V("undead_unleashed:dreadknight", "zombie", 10, "Лицар жаху", ["enrage"], "Undead Unleashed: проклятий лицар"),
    V("mutantmonsters:mutant_zombie", "zombie", 11, "Мутант-зомбі", [], "Mutant Monsters: б'є землю, воскресає, кидається"),
    V("cataclysm:royal_draugr", "zombie", 12, "Королівський драугр", ["shield", "enrage"], "Cataclysm: король драугрів"),
    # skeletons
    V("variantsandventures:murk", "skeleton", 1, "Мулистий кістяк", mech="Variants&Ventures: стріли сповільнюють"),
    V(BIC + "decrepit_skeleton", "skeleton", 1, "Трухлявий кістяк", mech="Born in Chaos: старий скелет-воїн"),
    V("variantsandventures:verdant", "skeleton", 2, "Зелений кістяк", mech="Variants&Ventures: отруйні стріли джунглів"),
    V("twilightforest:skeleton_druid", "skeleton", 3, "Кістяк-друїд", mech="Twilight Forest: природні чари"),
    V(BIC + "skeleton_thrasher", "skeleton", 3, "Кістяк-молотильник", mech="Born in Chaos: б'є з розмаху"),
    V(BIC + "skeleton_demoman", "skeleton", 4, "Кістяк-підривник", mech="Born in Chaos: кидає бомби"),
    V(BIC + "bonescaller", "skeleton", 5, "Кличник кісток", mech="Born in Chaos: піднімає скелетів"),
    V(BIC + "siamese_skeletons", "skeleton", 6, "Сіамські кістяки", mech="Born in Chaos: двоє в одному, розділяються"),
    V("cataclysm:koboleton", "skeleton", 6, "Кобольдовий кістяк", ["leap"], "Cataclysm: швидкий кістяк пустелі"),
    V("cataclysm:aptrgangr", "skeleton", 8, "Аптрґанґр", ["armor_break"], "Cataclysm: мертвий велетень із сокирою"),
    V("eternalnether:wither_skeleton_knight", "skeleton", 9, "Лицар-кістяк", ["shield"], "Eternal Nether: лицар із щитом"),
    V("undead_unleashed:wraith", "skeleton", 10, "Мара", ["blink"], "Undead Unleashed: летючий привид"),
    V(BIC + "supreme_bonescaller", "skeleton", 11, "Верховний кличник", [], "Born in Chaos: могутній некромант кісток"),
    V("mutantmonsters:mutant_skeleton", "skeleton", 12, "Мутант-скелет", [], "Mutant Monsters: багатостріли, кидки"),
    # spiders
    V("twilightforest:hedge_spider", "spider", 1, "Колючий павук", mech="Twilight Forest: живе в живоплотах"),
    V("twilightforest:swarm_spider", "spider", 2, "Рійний павук", mech="Twilight Forest: нападає зграями"),
    V(BIC + "diamond_termite", "spider", 3, "Алмазний терміт", mech="Born in Chaos: точить броню"),
    V("irons_spellbooks:ice_spider", "spider", 5, "Крижаний павук", ["frost_aura"], "Iron's Spells: морозний павук"),
    V("twilightforest:king_spider", "spider", 7, "Павук-король", ["web"], "Twilight Forest: великий павук"),
    V(BIC + "mother_spider", "spider", 9, "Матір павуків", ["enrage"], "Born in Chaos: кличе павученят"),
    # creepers (Creeper Overhaul gives every biome its own creeper; these come on top)
    V(BIC + "phantom_creeper", "creeper", 5, "Фантомний кріпер", ["blink"], "Born in Chaos: напівпрозорий, двоїться"),
    V("mutantmonsters:mutant_creeper", "creeper", 11, "Мутант-кріпер", [], "Mutant Monsters: стрибає й вибухає кілька разів"),
    # witches and casters
    V(BIC + "missioner", "witch", 3, "Місіонер", mech="Born in Chaos: темний проповідник"),
    V("irons_spellbooks:cultist", "witch", 4, "Сектант", mech="Iron's Spells: кровна магія"),
    V("friendsandfoes:illusioner", "witch", 5, "Ілюзіоніст", ["blink"], "Friends&Foes: двійники, сліпота"),
    V("irons_spellbooks:necromancer", "witch", 7, "Некромант", mech="Iron's Spells: піднімає мерців"),
    V("irons_spellbooks:archevoker", "witch", 9, "Архівокер", ["shield"], "Iron's Spells: ікла й вексі"),
    V(BIC + "lifestealer", "witch", 10, "Життєкрад", ["lifesteal"], "Born in Chaos: краде життя"),
    V(BIC + "fallen_chaos_knight", "witch", 12, "Занепалий лицар хаосу", ["enrage"], "Born in Chaos: лицар-маг хаосу"),
    # endermen (Enderman Overhaul gives every biome its own enderman)
    V(BIC + "nightmare_stalker", "enderman", 6, "Нічний переслідувач", ["blink"], "Born in Chaos: полює невидимим"),
    V("cataclysm:endermaptera", "enderman", 8, "Ендероптера", ["leap"], "Cataclysm: ендер-комаха"),
    V("mutantmonsters:mutant_enderman", "enderman", 11, "Мутант-ендермен", [], "Mutant Monsters: кидає блоки, клони"),
    V("cataclysm:ender_golem", "enderman", 12, "Ендер-голем", ["shockwave"], "Cataclysm: кам'яний вартовий Енду"),
    # illagers
    V("orc_hut:orc", "illager", 4, "Орк", ["leap"], "Orc Hut: дикий орк"),
    V("twilightforest:upper_goblin_knight", "illager", 6, "Гоблін-лицар", ["shield"], "Twilight Forest: лицар зі щитом"),
    V("irons_spellbooks:magehunter_vindicator", "illager", 7, "Мисливець на магів", ["armor_break"], "Iron's Spells: проти магів"),
    V("mowziesmobs:umvuthana_raptor", "illager", 9, "Умвутана-хижак", ["leap"], "Mowzie's Mobs: воїн у масці"),
    V(BIC + "door_knight", "illager", 11, "Лицар-двері", ["shield"], "Born in Chaos: важкий лицар-щит"),
    # piglins and blazes
    V("eternalnether:piglin_hunter", "piglin", 3, "Піґлін-мисливець", ["leap"], "Eternal Nether: мисливець Незеру"),
    V("eternalnether:corpor", "piglin", 5, "Корпор", ["shield"], "Eternal Nether: мертвий піґлін"),
    V("cataclysm:ignited_revenant", "piglin", 7, "Палаючий ревенант", ["fire_aura"], "Cataclysm: вогняний щитоносець"),
    V(BIC + "infernal_spirit", "piglin", 8, "Пекельний дух", ["fire_aura"], "Born in Chaos: вогняний дух"),
    V("eternalnether:wraither", "piglin", 9, "Рейзер", ["blink"], "Eternal Nether: привид Незеру"),
    V("undead_unleashed:flamebreather", "piglin", 10, "Вогнедих", [], "Undead Unleashed: дихає вогнем"),
    V("cataclysm:ignited_berserker", "piglin", 11, "Палаючий берсерк", ["enrage"], "Cataclysm: вогняний берсерк"),
    V(BIC + "seared_spirit", "blaze", 4, "Випалений дух", ["fire_aura"], "Born in Chaos: дух вогню"),
    V("friendsandfoes:wildfire", "blaze", 9, "Дикий вогонь", ["fire_aura"], "Friends&Foes: щити-іфрити"),
    # drowned
    V(BIC + "zombie_fisherman", "drowned", 2, "Мертвий рибалка", mech="Born in Chaos: підтягує вудкою"),
    V("cataclysm:deepling", "drowned", 3, "Глибинник", ["leap"], "Cataclysm: житель глибин"),
    V("cataclysm:deepling_angler", "drowned", 5, "Глибинник-рибалка", ["pull"], "Cataclysm: ліхтар і гак"),
    V("rottencreatures:dead_beard", "drowned", 7, "Мертва Борода", [], "Rotten Creatures: капітан-мерць із гарматами"),
    V("cataclysm:deepling_brute", "drowned", 9, "Глибинник-громило", ["shockwave"], "Cataclysm: важкий глибинник"),
    V("cataclysm:deepling_warlock", "drowned", 11, "Глибинник-чорнокнижник", ["blink"], "Cataclysm: магія глибин"),
]

# creatures of the new mods that spawn by themselves: not before the player is strong enough (their natural spawns wait)
GATE_EXTRA = {BIC + "lord_pumpkinhead": 130, BIC + "lord_the_headless": 130, BIC + "sir_pumpkinhead": 85, BIC + "sir_the_headless": 85,
              BIC + "krampus": 105, BIC + "scarlet_persecutor": 85, BIC + "dire_hound_leader": 65, BIC + "dread_hound": 35,
              BIC + "pumpkinhead": 25, BIC + "restless_spirit": 25, BIC + "spirit_guide": 50, BIC + "swarmer": 35, BIC + "bone_imp": 25,
              BIC + "glutton_fish": 25, BIC + "thornshell_crab": 15, BIC + "corpse_fly": 15, BIC + "bloody_gadfly": 25,
              "rottencreatures:glacial_hunter": 50, "rottencreatures:zap": 65, "mutantmonsters:spider_pig": 50,
              "undead_unleashed:tormentomb": 200}

ABILITY_UK = {"leap": "стрибок до цілі", "blink": "перенесення за спину цілі", "shockwave": "ударна хвиля поруч",
              "enrage": "лють нижче 30 % здоров'я", "regen": "відновлення, коли його не б'ють", "shield": "щит поглинання кожні 15 с",
              "lifesteal": "лікується на 25 % шкоди", "armor_break": "удар знімає 4 броні на 6 с", "fire_aura": "підпалює гравців поруч",
              "frost_aura": "морозить гравців поруч", "web": "павутина під ноги цілі", "pull": "підтягує ціль", "family_hit": "ефект родини на удар",
              "summon_guard": "кличе варту на 66 % і 33 % здоров'я"}

# ---------------------------------------------------------------- loot: a small extra, rarer and smaller than v1
def Lt(item, w, lo=1, hi=1): return (item, w, lo, hi)
TIER_LOOT = {
    1: [Lt("minecraft:iron_nugget", 30, 1, 4), Lt("minecraft:gold_nugget", 20, 1, 3), Lt("minecraft:copper_ingot", 20, 1, 2), Lt("minecraft:leather", 15), Lt("minecraft:arrow", 15, 2, 5)],
    2: [Lt("minecraft:iron_ingot", 25), Lt("minecraft:gold_ingot", 12), Lt("minecraft:lapis_lazuli", 15, 1, 3), Lt("minecraft:redstone", 15, 1, 4), Lt("create:zinc_ingot", 12), Lt("mekanism:ingot_tin", 10), Lt("minecraft:experience_bottle", 6)],
    3: [Lt("minecraft:emerald", 20, 1, 2), Lt("create:brass_ingot", 12), Lt("mekanism:ingot_osmium", 12), Lt("iceandfire:silver_ingot", 10), Lt("ars_nouveau:source_gem", 12, 1, 2), Lt("irons_spellbooks:arcane_essence", 10, 1, 2), Lt("minecraft:experience_bottle", 6)],
    4: [Lt("minecraft:emerald", 15, 1, 3), Lt("minecraft:amethyst_shard", 15, 1, 3), Lt("minecraft:diamond", 4), Lt("iceandfire:sapphire_gem", 5), Lt("irons_spellbooks:common_ink", 8), Lt("apotheosis:gem_dust", 6, 1, 2), Lt("create:experience_nugget", 15, 2, 5)],
    5: [Lt("minecraft:diamond", 6), Lt("irons_spellbooks:uncommon_ink", 6), Lt("cataclysm:ancient_metal_nugget", 10, 1, 3), Lt("twilightforest:knightmetal_ingot", 6), Lt("apotheosis:mysterious_scrap_metal", 4), Lt("minecraft:echo_shard", 3), Lt("minecraft:experience_bottle", 8, 1, 2)],
    6: [Lt("minecraft:diamond", 6), Lt("minecraft:netherite_scrap", 2), Lt("irons_spellbooks:rare_ink", 4), Lt("cataclysm:ancient_metal_ingot", 4), Lt("twilightforest:fiery_ingot", 3), Lt("apotheosis:timeworn_fabric", 3), Lt("minecraft:experience_bottle", 10, 1, 3)],
}
def band(tier): return min(6, (tier + 1) // 2)

FAMILY_LOOT = {
    "zombie": [Lt("minecraft:rotten_flesh", 30, 1, 2), Lt("minecraft:iron_nugget", 15, 1, 3), Lt("minecraft:paper", 10), Lt("iceandfire:dread_shard", 3)],
    "skeleton": [Lt("minecraft:bone", 30, 1, 3), Lt("minecraft:arrow", 20, 2, 6), Lt("minecraft:spectral_arrow", 6, 1, 3), Lt("irons_spellbooks:frozen_bone", 3)],
    "spider": [Lt("minecraft:string", 30, 1, 3), Lt("minecraft:spider_eye", 15), Lt("minecraft:cobweb", 6)],
    "creeper": [Lt("minecraft:gunpowder", 30, 1, 3), Lt("minecraft:fire_charge", 8), Lt("minecraft:glowstone_dust", 8, 1, 2)],
    "witch": [Lt("minecraft:glowstone_dust", 15, 1, 2), Lt("minecraft:sugar", 15, 1, 2), Lt("minecraft:glass_bottle", 15), Lt("irons_spellbooks:arcane_essence", 10), Lt("ars_nouveau:source_gem", 8)],
    "enderman": [Lt("minecraft:ender_pearl", 30), Lt("minecraft:chorus_fruit", 10, 1, 2), Lt("minecraft:ender_eye", 3)],
    "illager": [Lt("minecraft:emerald", 25, 1, 2), Lt("minecraft:arrow", 15, 2, 5), Lt("minecraft:leather", 10)],
    "piglin": [Lt("minecraft:gold_nugget", 25, 2, 5), Lt("minecraft:quartz", 20, 1, 3), Lt("minecraft:blaze_powder", 6)],
    "blaze": [Lt("minecraft:blaze_powder", 30, 1, 2), Lt("irons_spellbooks:cinder_essence", 8), Lt("minecraft:fire_charge", 12)],
    "drowned": [Lt("minecraft:prismarine_shard", 20, 1, 3), Lt("minecraft:prismarine_crystals", 15, 1, 2), Lt("minecraft:nautilus_shell", 2)],
}

def expected_items(entries, rolls=1):
    tw = sum(w for _, w, *_ in entries)
    return rolls * sum(w * (lo + hi) / 2 for _, w, lo, hi in entries) / tw

def loot_table(entries):
    for it, *_ in entries:
        if it not in ITEMS: sys.exit(f"unknown item {it}")
    return {"type": "minecraft:entity", "random_sequence": "wilds_core:mobs", "pools": [{
        "rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": it, "weight": w,
                     "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}] if hi > 1 else []}
                    for it, w, lo, hi in entries]}]}

# ---------------------------------------------------------------- output
def main():
    errs, ids = [], set()
    for v in VARIANTS:
        if v["id"] in ids: errs.append("duplicate " + v["id"])
        ids.add(v["id"])
        if v["entity"] not in ENTITIES: errs.append(f"{v['id']}: unknown entity {v['entity']} (refresh docs/registries after adding mods)")
        for a in v["abilities"]:
            if a not in ABILITY_UK: errs.append(f"{v['id']}: unknown ability {a}")
        if v["entity"].startswith("minecraft:"): errs.append(f"{v['id']}: a vanilla mob is not a new creature")
    for e in GATE_EXTRA:
        if e not in ENTITIES: errs.append(f"gate: unknown entity {e}")
    if errs: sys.exit("\n".join(errs))

    gate = {v["entity"]: UNLOCK[v["tier"]] for v in VARIANTS}
    gate.update(GATE_EXTRA)
    data = {"settings": dict(SETTINGS, gate=gate), "families": {k: {"bases": f["bases"], "hit": f["hit"]} for k, f in FAMILIES.items()},
            "variants": [{"id": v["id"], "family": v["family"], "tier": v["tier"], "entity": v["entity"], "abilities": v["abilities"],
                          "hits": [], "scale": 1.0, "equip": {}, "loot": v["family"]} for v in VARIANTS]}
    out = os.path.join(RES, "data", "wilds_core", "wilds_mobs")
    os.makedirs(out, exist_ok=True)
    json.dump(data, open(os.path.join(out, "variants.json"), "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
    lt = os.path.join(RES, "data", "wilds_core", "loot_table", "mobs")
    os.makedirs(lt, exist_ok=True)
    for f in os.listdir(lt): os.remove(os.path.join(lt, f))
    for b, entries in TIER_LOOT.items():
        json.dump(loot_table(entries), open(os.path.join(lt, f"tier_{b}.json"), "w", newline="\n"), indent=1)
    for f, entries in FAMILY_LOOT.items():
        json.dump(loot_table(entries), open(os.path.join(lt, f"family_{f}.json"), "w", newline="\n"), indent=1)

    for lang in ("uk_ua", "en_us"):
        p = os.path.join(RES, "assets", "wilds_core", "lang", lang + ".json")
        d = json.load(open(p, encoding="utf-8"))
        for k in [k for k in d if k.startswith("wilds_core.mob.")]: del d[k]
        d["wilds_core.mob.level"] = "Рівень істоти: %s" if lang == "uk_ua" else "Creature level: %s"
        d["wilds_core.mob.named"] = "%s · %s рів." if lang == "uk_ua" else "%s · lvl %s"
        d["wilds_core.power"] = ("Ваша сила: %s (зброя %s, захист %s). Моби поруч підлаштовуються під найсильнішого гравця."
                                 if lang == "uk_ua" else "Your power: %s (weapon %s, defence %s). Mobs near you follow the strongest player.")
        json.dump(d, open(p, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=2)

    doc, ok = document()
    os.makedirs(os.path.join(ROOT, "docs", "balance"), exist_ok=True)
    open(os.path.join(ROOT, "docs", "balance", "mob-levels.md"), "w", encoding="utf-8", newline="\n").write(doc)
    print(f"{len(VARIANTS)} creatures, {len(gate)} gated, {len(TIER_LOOT)} tier and {len(FAMILY_LOOT)} family loot tables; checks {'passed' if ok else 'FAILED'}")
    if not ok: sys.exit(1)


GEAR = [("Кулаки, без броні", 1, 0, 0, 0, 20), ("Кам'яний меч, шкіра", 5, 7, 0, 0, 20), ("Залізний меч, залізна броня", 6, 15, 0, 0, 20),
        ("Діамантовий меч + Гострота III, діамантова броня + Захист II", 9, 20, 8, 8, 20),
        ("Незеритовий меч + Гострота V, незеритова броня + Захист IV", 12, 20, 12, 16, 20),
        ("Незерит з афіксами й самоцвітами, +4 серця", 18, 22, 14, 16, 28), ("Найкраще у збірці (драконяча сталь, міфічні афікси)", 30, 28, 18, 20, 40)]


def document():
    ok = True
    o = []
    w = o.append
    w("# Рівні мобів, нові істоти, лут і боси — версія 2\n")
    w("Згенеровано `pack/tools/gen_mob_balance.py` — не редагуйте вручну. Код: `mods-src/wilds_core/src/main/java/ua/wilds/core/mobs/`. "
      "Minecraft 1.21.1, NeoForge 21.1.264.\n")
    w("## Що було не так у версії 1 (21.11) і що змінено\n")
    w("- «Рівнем гравця» був рівень дерева навичок. На сервері він сягає 270–470, тому новачок отримував драугрів 474-го рівня, які вбивали за два удари. **Тепер рівень гравця — це його сила**: її рахують зі зброї та захисту, які він справді носить.")
    w("- **Жоден удар моба не забирає більше** 20 % (звичайний моб), 25 % (новий вид) чи 40 % (бос) максимального здоров'я гравця, що б не трапилося з множниками.")
    w("- Половина «нових видів» була ванільними мобами з іншою назвою. **Тепер кожен новий вид — окрема істота** з власною моделлю й поведінкою.")
    w("- Лут ярусів давав зачаровані яблука, незеритові уламки й давні уламки стаками. **Тепер це невеликий додаток**: шанс, один кидок, удача не множить кидки.")
    w("- **Старі моби з рівнями версії 1 перераховуються** під нову систему, щойно завантажуються.\n")
    w("## Припущення (можна змінити в генераторі)\n")
    w("- **A1. Рівень гравця = його сила.** Вона дорівнює середньому двох складників. Перший — що варта зброя (85 %): шкода атаки з урахуванням афіксів. Другий — що вартий захист (15 %): броня, міцність, зачарування захисту, здоров'я. Обидва звіряються з еталонною кривою, де залізо — це рівень 15. Гравець бачить свою силу командою `/wildspower`.")
    w("- **A2. «Поряд»** — радіус 128 блоків (межа, у якій гра спавнить мобів біля гравця). Моб бере рівень найсильнішого гравця.")
    w("- **A3. Складність світу Apotheosis** множить рівень: " + ", ".join(f"{k} ×{v}" for k, v in WORLD_TIER.items()) + ".")
    w("- **A4.** Посилення діють лише проти гравців. Колонії, селяни й улюбленці б'ються з мобами, ніби ті ванільні.")
    w("- **A5.** Нові істоти з'являються замість ванільних мобів своєї родини при природному спавні. Їхній власний природний спавн чекає, поки поруч не буде достатньо сильного гравця.\n")
    w("## Сила гравця за спорядженням\n")
    w("| Спорядження | Шкода | Броня / міцність / захист / здоров'я | Рівень зброї | Рівень захисту | **Сила** |\n|---|---|---|---|---|---|")
    for name, dmg, a, t, e, hp in GEAR:
        wl, dl = weapon_level(dmg), defence_level(a, t, e, hp)
        w(f"| {name} | {dmg} | {a} / {t} / {e} / {hp} | {wl:.0f} | {dl:.0f} | **{power_level(dmg, a, t, e, hp):.0f}** |")
    w("")
    w("## Формули\n")
    w("```")
    w("сила гравця P = 0.85·рівень_зброї + 0.15·рівень_захисту,   рівень_зброї = 30·(шкода/6)^1.25 − 15")
    w("ціль         T = max по гравцях у радіусі 128 (P × множник_складності_світу)")
    w("моб          ℓ = T × (0.9…1.05) при появі; далі кожні 10 с ℓ += обмежити(T − ℓ, ±(1 + 0.01ℓ))")
    w("D(L)         = 6 · ((L + 15) / 30)^0.8        ← удар гравця сили L")
    w(f"звичайний    здоров'я × (1 + {HP_RATE}ℓ), шкода × (1 + {DMG_RATE}ℓ), броня + min(10, ℓ/35), міцність + min(6, ℓ/60)")
    w(f"новий вид k  здоров'я = {TIER_HITS} · D'(U_k) · (1+{HP_RATE}ℓ)/(1+{HP_RATE}U_k), D' — удар після ВЛАСНОЇ броні істоти (тож броньовані мають менше здоров'я); шкода = власна × (1 + {DMG_RATE}ℓ) × (1 + {KIND_DMG_STEP}k)")
    w(f"бос          здоров'я = max(власне × (1 + {BOSS_HP_RATE}ℓ), {BOSS_HITS}·D(ℓ)); шкода = власна × (1 + {BOSS_DMG_RATE}ℓ); рівень не падає")
    w(f"стеля удару  ≤ {int(CAP_PLAIN*100)} % / {int(CAP_KIND*100)} % / {int(CAP_BOSS*100)} % максимального здоров'я гравця (звичайний / новий вид / бос) — після броні й зачарувань")
    w("заміна       шанс = min(0.55, 0.15 + ℓ/600); серед відкритих видів найновіший має вагу 1, попередній 1/2…")
    w("```\n")

    w("## Перевірка 1: ванільний зомбі (20 здоров'я, 2 броні)\n")
    w("Ціль: 4 удари для еталону на 15-му рівні, 1–2 удари з 150-го.\n")
    w("| Сила = рівень моба | Здоров'я | Броня | Ударів гравця | OK |\n|---|---|---|---|---|")
    for L_ in (0, 15, 30, 60, 100, 150, 200, 300, 400):
        hp = 20 * hp_mult(L_); ar = 2 + armor_add(L_); to = tough_add(L_)
        h = hits(hp, ar, to, L_); n = math.ceil(h - 1e-9)
        good = (n == 4) if L_ == 15 else (n <= 2) if L_ >= 150 else True
        ok &= good
        w(f"| {L_} | {hp:.0f} | {ar:.1f} | {h:.2f} → **{n}** | {'✓' if good else '✗'} |")
    w("")
    w("## Перевірка 2: нові види — близько 4 ударів, коли відкриваються\n")
    w("| Ярус | Відкривається (сила) | Здоров'я | Ударів тоді | Через +100 сили | OK |\n|---|---|---|---|---|---|")
    for k, u in TIERS:
        hp = tier_hp(k, u); h = hits(hp, armor_add(u, k), tough_add(u), u); n = math.ceil(h - 1e-9)
        l2 = min(400, u + 100); h2 = hits(tier_hp(k, l2), armor_add(l2, k), tough_add(l2), l2)
        good = n == 4
        ok &= good
        w(f"| {k} | {u} | {hp:.0f} | {h:.2f} → **{n}** | {h2:.1f} | {'✓' if good else '✗'} |")
    w("")
    w("## Перевірка 3: скільки ударів моба витримує гравець\n")
    w("Стеля гарантує, що звичайний моб убиває щонайменше за 5 ударів, новий вид — за 4, бос — за 3, навіть якщо гравець слабший за свою силу (наприклад, зняв броню). Нижче приклади для еталонного гравця.\n")
    w("| Хто | Сила | Сира шкода | Ударів до смерті | OK |\n|---|---|---|---|---|")
    for name, nat, k, cap, mult in (("Зомбі (3)", 3, 0, CAP_PLAIN, dmg_mult), ("Новий вид ярусу 6, власна 8", 8, 6, CAP_KIND, None),
                                     ("Мутант-зомбі (ярус 11, власна 15)", 15, 11, CAP_KIND, None), ("Бос (власна 20)", 20, 0, CAP_BOSS, "boss")):
        for L_ in (15, 65, 150, 250):
            if mult == "boss": raw = nat * (1 + BOSS_DMG_RATE * L_)
            elif mult is None: raw = nat * dmg_mult(L_) * (1 + KIND_DMG_STEP * k)
            else: raw = nat * dmg_mult(L_)
            n = hits_to_die(raw, L_, cap)
            good = n >= math.ceil(1 / cap - 1e-9)
            ok &= good
            w(f"| {name} | {L_} | {raw:.1f} | {n} | {'✓' if good else '✗'} |")
    w("")
    w("## Перевірка 4: боси без зілля, яблук і заклять\n")
    w(f"| Бос (власне здоров'я) | Рівень | Здоров'я | Ударів гравця | OK |\n|---|---|---|---|---|")
    for name, nhp in (("Нага TF (120)", 120), ("Візер (300)", 300), ("Ігніс (800)", 800)):
        for L_ in (15, 100, 250):
            hp = boss_hp(nhp, L_); h = hits(hp, armor_add(L_), tough_add(L_), L_)
            good = h >= BOSS_HITS * 0.9
            ok &= good
            w(f"| {name} | {L_} | {hp:.0f} | {h:.0f} | {'✓' if good else '✗'} |")
    w("\nЗдоров'я понад межу гри (1024) стає дільником шкоди, тож боси справді мають стільки, скільки в таблиці.\n")

    w("## Прогресія: хто з'являється замість ванільних\n")
    w("| Сила | Шанс заміни | " + " | ".join(FAMILIES[f]["uk"] for f in FAMILIES) + " |\n|---|---|" + "---|" * len(FAMILIES))
    for L_ in (0, 8, 15, 25, 35, 50, 65, 85, 105, 130, 160, 200, 250, 300, 400):
        cells = []
        for f in FAMILIES:
            un = [v for v in VARIANTS if v["family"] == f and UNLOCK[v["tier"]] <= L_]
            cells.append(", ".join(v["uk"] for v in un if v["tier"] == un[-1]["tier"]) if un else "—")
        any_ = any(UNLOCK[v["tier"]] <= L_ for v in VARIANTS)
        w(f"| {L_} | {(min(0.55, 0.15 + L_ / 600) if any_ else 0) * 100:.0f} % | " + " | ".join(cells) + " |")
    w("\nПісля сили 250 нові яруси не відкриваються. Далі ростуть рівні мобів, частка замін і боси, а найсильніші види трапляються частіше.\n")

    w(f"## Нові істоти ({len(VARIANTS)})\n")
    w("| # | Істота | Родина | Ярус (сила) | Мод і поведінка | Додатково від Wilds |\n|---|---|---|---|---|---|")
    for i, v in enumerate(VARIANTS, 1):
        w(f"| {i} | {v['uk']} (`{v['entity']}`) | {FAMILIES[v['family']]['uk']} | {v['tier']} ({UNLOCK[v['tier']]}) | {v['mech']} | {', '.join(ABILITY_UK[a] for a in v['abilities']) or '—'} |")
    w("\nВласний природний спавн цих мод-істот і ще " + str(len(GATE_EXTRA)) + " інших (" + ", ".join(f"`{e}` від {l}" for e, l in GATE_EXTRA.items()) + ") чекає, поки поруч не буде гравця такої сили.\n")

    w("## Лут\n")
    w(f"- Новий вид: зі своїм лутом мода, а також з шансом {int(SETTINGS['kind_loot_chance']*100)} % один предмет родини і з шансом {int(SETTINGS['tier_loot_chance']*100)} % один предмет ярусу (ярус = ⌈рівень виду / 2⌉). Кидок один, удача й «Здобич» кидків не множать.")
    w(f"- Звичайний моб з рівнем: шанс {int(SETTINGS['plain_loot_chance']*100)} % на один предмет родини. Боси — лише власний лут.")
    w("- Досвід: нові види ×(1 + 0,1·ярус), звичайні ×(1 + ℓ/400), боси ×(1 + ℓ/200).")
    w("- **Без** зачарованих золотих яблук, незерових зірок, тотемів і давніх уламків. Незеритовий уламок трапляється лише в ярусі 6 з вагою 2 зі 34.\n")
    w("| Ярус | Предмети | Середньо штук за кидок | Середньо за вбивство виду |\n|---|---|---|---|")
    for b, entries in TIER_LOOT.items():
        e = expected_items(entries)
        w(f"| {b} | " + ", ".join(f"`{it}`" for it, *_ in entries) + f" | {e:.1f} | {e * SETTINGS['tier_loot_chance']:.2f} |")
    w("")
    for f, entries in FAMILY_LOOT.items():
        w(f"- **{FAMILIES[f]['uk']}**: " + ", ".join(f"`{it}`" for it, *_ in entries))
    w("\n## Результат перевірки\n")
    w("Усі етапи відповідають цілям." if ok else "**Є етапи, що не відповідають цілям — див. ✗ вище.**")
    return "\n".join(o) + "\n", ok


if __name__ == "__main__":
    main()
