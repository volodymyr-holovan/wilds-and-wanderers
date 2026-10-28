// Wilds & Wanderers: the Nether and the End update (28.10).
// Incendium, Eternal Nether, Nether Depths Upgrade, Nether's Exoticism and Enderscape brought in with the rest of the
// pack: their ores and fruit go through Create, Mekanism and Farmer's Delight, and the recipes they ship for older
// loaders are replaced with working ones.

ServerEvents.recipes(event => {
  // --- Nether's Exoticism: its Farmer's Delight recipes ask for the old Forge knife tag, so no knife cuts the fruit ---
  ;['netherexotics:sliced_kiwano_recipe', 'netherexotics:cut_pitaya_recipe', 'netherexotics:chopped_bouddha_s_hand_recipe']
    .forEach(id => event.remove({ id: id }))
  let cut = (input, results, id) => event.custom({
    type: 'farmersdelight:cutting',
    ingredients: [{ item: input }],
    result: results.map(([item, count]) => ({ item: { id: item, count: count } })),
    tool: [{ type: 'farmersdelight:item_ability', action: 'knife_dig' }, { tag: 'c:tools/knife' }]
  }).id(id)
  // the peel is what the kiwano armour and tools are made of; cutting the fruit is the way to get it
  cut('netherexotics:kiwano', [['netherexotics:sliced_kiwano', 2], ['netherexotics:kiwano_peel', 1]], 'wilds:cutting/kiwano')
  cut('netherexotics:pitaya', [['netherexotics:cut_pitaya', 2]], 'wilds:cutting/pitaya')
  cut('netherexotics:bouddha_s_hand', [['netherexotics:chopped_bouddha_s_hand', 2]], 'wilds:cutting/bouddha_s_hand')

  // --- Enderscape ores in Create and Mekanism, like every other ore of the pack ---
  ;['enderscape:shadoline_ore', 'enderscape:mirestone_shadoline_ore'].forEach((ore, i) => {
    event.custom({
      type: 'create:crushing',
      ingredients: [{ item: ore }],
      processing_time: 300,
      results: [
        { id: 'enderscape:raw_shadoline' },
        { id: 'enderscape:raw_shadoline', chance: 0.75 },
        { id: 'create:experience_nugget', chance: 0.75 },
        { id: i ? 'enderscape:mirestone' : 'minecraft:end_stone', chance: 0.12 }
      ]
    }).id(`wilds:crushing/${ore.split(':')[1]}`)
    event.custom({
      type: 'mekanism:enriching',
      input: { count: 1, item: ore },
      output: { count: 2, id: 'enderscape:raw_shadoline' }
    }).id(`wilds:enriching/${ore.split(':')[1]}`)
  })
  ;['enderscape:nebulite_ore', 'enderscape:mirestone_nebulite_ore'].forEach(ore => {
    event.custom({
      type: 'create:crushing',
      ingredients: [{ item: ore }],
      processing_time: 300,
      results: [
        { id: 'enderscape:nebulite_shards', count: 2 },
        { id: 'enderscape:nebulite_shards', chance: 0.5 },
        { id: 'create:experience_nugget', chance: 0.75 }
      ]
    }).id(`wilds:crushing/${ore.split(':')[1]}`)
    event.custom({
      type: 'mekanism:enriching',
      input: { count: 1, item: ore },
      output: { count: 3, id: 'enderscape:nebulite_shards' }
    }).id(`wilds:enriching/${ore.split(':')[1]}`)
  })
  // raw shadoline in the Mekanism smelter and Create's bulk blasting work through the vanilla smelting recipe

  // --- the withered dead of Eternal Nether: their bones in the millstone ---
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'eternalnether:withered_bone' }],
    processing_time: 120,
    results: [{ id: 'eternalnether:withered_bone_meal', count: 3 }, { id: 'minecraft:black_dye', chance: 0.25 }]
  }).id('wilds:milling/withered_bone')
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'netherexotics:bone_column' }],
    processing_time: 100,
    results: [{ id: 'minecraft:bone_meal', count: 4 }, { id: 'minecraft:bone_meal', count: 2, chance: 0.5 }]
  }).id('wilds:milling/bone_column')

  // --- lava fish (Nether Depths Upgrade) in the machines ---
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'netherdepthsupgrade:blazefish' }],
    processing_time: 100,
    results: [{ id: 'minecraft:blaze_powder', count: 2 }, { id: 'minecraft:blaze_powder', chance: 0.5 }]
  }).id('wilds:milling/blazefish')
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'netherdepthsupgrade:glowdine' }],
    processing_time: 100,
    results: [{ id: 'minecraft:glowstone_dust', count: 2 }, { id: 'minecraft:glowstone_dust', chance: 0.5 }]
  }).id('wilds:milling/glowdine')
  event.custom({
    type: 'mekanism:crushing',
    input: { count: 1, item: 'netherdepthsupgrade:obsidianfish' },
    output: { count: 2, id: 'mekanism:dust_obsidian' }
  }).id('wilds:crushing/obsidianfish')

  // --- Nether fruit juice by the bottle in Create's spout-less way: the mixer presses three fruit into a juice ---
  ;[['kiwano', 'kiwano_juice'], ['rambutan', 'rambutan_juice'], ['pitaya', 'pitaya_juice'], ['bouddha_s_hand', 'bouddha_s_hand_juice']]
    .forEach(([fruit, juice]) => event.custom({
      type: 'create:mixing',
      ingredients: [{ item: `netherexotics:${fruit}` }, { item: `netherexotics:${fruit}` }, { item: 'minecraft:glass_bottle' }],
      results: [{ id: `netherexotics:${juice}`, count: 1 }, { id: `netherexotics:${juice}`, chance: 0.5 }]
    }).id(`wilds:mixing/${juice}`))

  // --- Ars Nouveau: nebulite, the energy stone of the End, charges into source gems ---
  event.custom({
    type: 'ars_nouveau:imbuement',
    input: { item: 'enderscape:nebulite_shards' },
    output: { count: 1, id: 'ars_nouveau:source_gem' },
    pedestalItems: [],
    source: 100
  }).id('wilds:imbuement/source_gem_from_nebulite')
})

// Piglins sometimes trade away the fruit of their forests
LootJS.modifiers(event => {
  event.addTableModifier('minecraft:gameplay/piglin_bartering')
    .randomChance(0.06)
    .pool(p => p.addEntry(LootEntry.reference('wilds:inject/nether_fruit')))
    .name('wilds:bartering_fruit')
})
