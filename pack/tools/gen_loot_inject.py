"""Write the pack's loot tables: injected chest loot (kubejs/data/wilds/loot_table/inject/*.json) and the quest
reward bundles (kubejs/data/wilds/loot_table/reward/*.json).

Every vanilla and modded chest gets, with a small chance, one extra roll from a themed table, so loot from one mod
turns up in the structures of another. Weights keep the useful-but-common things common and the strong things rare.
Quests hand out a few rolls from a reward table (FTB Quests reward "loot:wilds:reward/<name>"), so two players
finishing the same quest get different things.
  python gen_loot_inject.py   (checks every id against the pack's items first)
"""
import json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "overrides", "kubejs", "data", "wilds", "loot_table", "inject")

# (item, weight, min, max)
TABLES = {
    # anywhere: practical materials of the tech and magic mods, food, travel
    "common": [
        ("create:andesite_alloy", 12, 2, 6), ("create:zinc_ingot", 10, 1, 4), ("create:copper_sheet", 8, 1, 3),
        ("mekanism:ingot_osmium", 10, 1, 4), ("mekanism:ingot_tin", 8, 1, 4), ("mekanism:ingot_lead", 6, 1, 3),
        ("ae2:certus_quartz_crystal", 8, 1, 4), ("iceandfire:silver_ingot", 6, 1, 3),
        ("ars_nouveau:source_gem", 6, 1, 3), ("irons_spellbooks:arcane_essence", 6, 1, 3),
        ("farmersdelight:cabbage_seeds", 6, 2, 5), ("farmersdelight:tomato_seeds", 6, 2, 5), ("farmersdelight:rice", 5, 2, 6),
        ("goblins_tyranny:coin", 8, 2, 7), ("naturalist:fur", 4, 1, 3), ("naturalist:hide", 4, 1, 3),
        ("comforts:rope_and_nail", 3, 1, 2), ("gew2:pebble", 4, 4, 12), ("smallships:cotton_sails", 2, 1, 2),
    ],
    # dungeons, crypts, towers, fortresses: rarer and stronger
    "rare": [
        ("create:brass_ingot", 10, 2, 5), ("create:precision_mechanism", 4, 1, 1), ("mekanism:alloy_infused", 8, 1, 3),
        ("mekanism:basic_control_circuit", 7, 1, 2), ("mekanism:alloy_reinforced", 3, 1, 1), ("ae2:printed_engineering_processor", 3, 1, 1),
        ("ars_nouveau:blank_parchment", 6, 1, 3), ("irons_spellbooks:common_ink", 6, 1, 3), ("irons_spellbooks:uncommon_ink", 3, 1, 2),
        ("irons_spellbooks:rare_ink", 1, 1, 1), ("iceandfire:dragonbone", 4, 1, 3), ("alexscaves:amber", 4, 1, 3),
        ("kobolds:kobold_template", 2, 1, 1), ("goblins_tyranny:goblin_spear", 2, 1, 1), ("naturalist:antler", 3, 1, 2),
        ("waystones:warp_dust", 5, 2, 6), ("hybrid_aquatic:black_pearl", 2, 1, 1), ("minecraft:name_tag", 3, 1, 1),
        ("minecolonies:ancienttome", 2, 1, 1),
    ],
    # shipwrecks, ocean ruins, monuments, sunken ships and ocean villages
    "ocean": [
        ("hybrid_aquatic:black_pearl", 3, 1, 2), ("hybrid_aquatic:barbed_hook", 4, 1, 1), ("fishofthieves:coconut", 8, 1, 4),
        ("fishofthieves:banana", 6, 1, 4), ("oceansdelight:fugu_roll", 5, 1, 3), ("oceansdelight:squid_rings", 6, 1, 4),
        ("upgrade_aquatic:beachgrass", 6, 2, 6), ("smallships:cannon_ball", 6, 2, 8), ("smallships:copper_plating", 3, 1, 2),
        ("smallships:cotton_sails", 4, 1, 2), ("minecraft:nautilus_shell", 4, 1, 2), ("minecraft:prismarine_crystals", 6, 2, 6),
        ("create:copper_sheet", 6, 1, 4), ("mekanism:ingot_tin", 5, 1, 3),
    ],
    # the Nether and fiery places
    "nether": [
        ("create:brass_ingot", 6, 1, 3), ("mekanism:ingot_refined_glowstone", 4, 1, 2), ("mekanism:ingot_refined_obsidian", 2, 1, 1),
        ("irons_spellbooks:fire_rune", 3, 1, 1), ("ars_nouveau:source_gem", 6, 2, 4), ("twilightforest:fiery_blood", 1, 1, 1),
        ("goblins_tyranny:fire_bomb", 4, 1, 3), ("minecraft:blaze_powder", 6, 2, 5), ("minecraft:magma_cream", 5, 1, 4),
        # the Nether update (28.10): fruit of the Nether forests, lava fishing, the withered dead
        ("netherexotics:kiwano", 4, 1, 3), ("netherexotics:rambutan", 4, 1, 3), ("netherexotics:pitaya", 3, 1, 2),
        ("netherexotics:jabuticaba", 3, 1, 3), ("netherexotics:bouddha_s_hand", 2, 1, 2),
        ("netherdepthsupgrade:lava_fishing_rod", 1, 1, 1), ("eternalnether:withered_bone", 4, 1, 4),
        ("enderscape:nebulite_shards", 1, 1, 2),
    ],
    # the End (28.10): End cities, Enderscape ruins and anything End-themed
    "end": [
        ("enderscape:nebulite_shards", 8, 1, 4), ("enderscape:raw_shadoline", 6, 1, 3), ("enderscape:shadoline_ingot", 3, 1, 2),
        ("enderscape:drift_jelly_bottle", 5, 1, 2), ("enderscape:flanger_berry", 6, 2, 5), ("enderscape:rubble_chitin", 4, 1, 3),
        ("minecraft:ender_pearl", 8, 1, 3), ("minecraft:popped_chorus_fruit", 6, 2, 6), ("minecraft:shulker_shell", 2, 1, 1),
        ("mekanism:ingot_refined_obsidian", 3, 1, 2), ("ars_nouveau:source_gem", 6, 2, 4), ("irons_spellbooks:ender_rune", 2, 1, 1),
        ("waystones:warp_dust", 5, 2, 6), ("irons_spellbooks:rare_ink", 1, 1, 1),
    ],
}

# quest rewards: (rolls min, rolls max) and entries like TABLES
REWARDS = {
    "explorer": ((2, 3), [
        ("waystones:warp_dust", 10, 2, 6), ("waystones:return_scroll", 6, 1, 2), ("waystones:warp_scroll", 3, 1, 1),
        ("naturescompass:naturescompass", 1, 1, 1), ("explorerscompass:explorerscompass", 1, 1, 1),
        ("comforts:sleeping_bag_brown", 3, 1, 1), ("simplytents:tent", 3, 1, 1), ("comforts:rope_and_nail", 4, 2, 4),
        ("farmersdelight:beef_stew", 8, 2, 4), ("minecraft:golden_apple", 4, 1, 2), ("minecraft:experience_bottle", 8, 3, 8),
        ("goblins_tyranny:coin", 8, 4, 12),
        ("minecolonies:supplycampdeployer", 1, 1, 1),
    ]),
    "sea": ((2, 4), [
        ("hybrid_aquatic:pearl", 8, 1, 3), ("hybrid_aquatic:black_pearl", 3, 1, 1), ("hybrid_aquatic:shark_tooth", 6, 1, 3),
        ("hybrid_aquatic:barbed_hook", 3, 1, 1), ("minecraft:nautilus_shell", 5, 1, 2), ("minecraft:heart_of_the_sea", 1, 1, 1),
        ("minecraft:prismarine_crystals", 6, 3, 8), ("smallships:cannon_ball", 5, 4, 12), ("smallships:cotton_sails", 3, 1, 2),
        ("oceansdelight:fugu_roll", 6, 2, 4), ("fishofthieves:coconut", 5, 2, 5), ("minecraft:experience_bottle", 6, 3, 8),
    ]),
    "magic": ((2, 3), [
        ("ars_nouveau:source_gem", 10, 2, 6), ("ars_nouveau:magebloom_fiber", 5, 1, 3), ("ars_nouveau:wilden_horn", 3, 1, 1),
        ("irons_spellbooks:arcane_essence", 10, 2, 6), ("irons_spellbooks:common_ink", 6, 1, 3), ("irons_spellbooks:uncommon_ink", 4, 1, 2),
        ("irons_spellbooks:rare_ink", 2, 1, 1), ("irons_spellbooks:epic_ink", 1, 1, 1), ("irons_spellbooks:blank_rune", 4, 1, 2),
        ("apotheosis:gem_dust", 5, 2, 6), ("minecraft:experience_bottle", 6, 4, 10),
    ]),
    "tech": ((2, 3), [
        ("create:brass_ingot", 10, 3, 8), ("create:precision_mechanism", 4, 1, 2), ("mekanism:basic_control_circuit", 8, 1, 3),
        ("mekanism:advanced_control_circuit", 3, 1, 1), ("mekanism:alloy_infused", 8, 2, 4), ("mekanism:alloy_reinforced", 3, 1, 2),
        ("ae2:printed_logic_processor", 3, 1, 2), ("ae2:printed_calculation_processor", 3, 1, 2), ("ae2:printed_engineering_processor", 3, 1, 2),
        ("sophisticatedbackpacks:upgrade_base", 4, 1, 2), ("iceandfire:silver_ingot", 6, 2, 5),
    ]),
    "fantasy": ((2, 3), [
        ("goblins_tyranny:coin", 12, 6, 16), ("goblins_tyranny:goblin_ingot", 6, 1, 3), ("goblins_tyranny:bomb", 6, 2, 4),
        ("goblins_tyranny:healing_ball", 5, 1, 3), ("goblins_tyranny:goblin_shield", 2, 1, 1), ("goblins_tyranny:goblin_mace", 2, 1, 1),
        ("kobolds:kobold_iron_pickaxe", 2, 1, 1), ("kobolds:kobold_template", 2, 1, 1), ("naturalist:fur", 5, 2, 4),
        ("minecraft:emerald", 8, 3, 8), ("minecraft:experience_bottle", 5, 3, 6),
    ]),
    "boss": ((3, 4), [
        ("minecraft:diamond", 10, 2, 5), ("minecraft:netherite_scrap", 4, 1, 2), ("minecraft:enchanted_golden_apple", 1, 1, 1),
        ("minecraft:experience_bottle", 10, 8, 16), ("apotheosis:sigil_of_socketing", 3, 1, 1), ("apotheosis:gem_dust", 6, 4, 10),
        ("apotheosis:mysterious_scrap_metal", 6, 2, 4), ("apotheosis:timeworn_fabric", 4, 1, 3), ("apotheosis:luminous_crystal_shard", 2, 1, 2),
        ("irons_spellbooks:rare_ink", 3, 1, 2), ("mekanism:alloy_reinforced", 4, 1, 3), ("cataclysm:ancient_metal_ingot", 2, 1, 2),
        ("create:precision_mechanism", 4, 1, 2),
    ]),
}


def pools(entries, rolls):
    return [{"rolls": rolls, "entries": [
        {"type": "minecraft:item", "name": i, "weight": w,
         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": a, "max": b}}]}
        for i, w, a, b in entries]}]


def main():
    ids = [i for t in TABLES.values() for i, *_ in t] + [i for _, t in REWARDS.values() for i, *_ in t]
    r = subprocess.run([sys.executable, os.path.join(HERE, "item_ids.py"), "--check", *ids], capture_output=True, text=True)
    if r.stdout.strip():
        sys.exit(r.stdout)
    os.makedirs(OUT, exist_ok=True)
    for name, entries in TABLES.items():
        json.dump({"type": "minecraft:chest", "pools": pools(entries, 1)}, open(os.path.join(OUT, name + ".json"), "w"), indent=1)
    rdir = os.path.join(OUT, "..", "reward")
    os.makedirs(rdir, exist_ok=True)
    for name, ((a, b), entries) in REWARDS.items():
        json.dump({"type": "minecraft:chest", "pools": pools(entries, {"type": "minecraft:uniform", "min": a, "max": b})},
                  open(os.path.join(rdir, name + ".json"), "w"), indent=1)
    print("written", ", ".join(TABLES), "and rewards", ", ".join(REWARDS))


main()
