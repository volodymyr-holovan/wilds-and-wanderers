// Wilds & Wanderers: links between mods that do not know about each other.
// Shared tags make equivalent materials interchangeable; the recipes below add
// processing routes so that loot from one mod is useful in the machines of another.

ServerEvents.tags('item', event => {
  // Ancient bones: dragon bones (Ice and Fire) and dinosaur bones (Alex's Caves) are interchangeable
  event.add('wilds:ancient_bones', ['iceandfire:dragonbone', 'alexscaves:heavy_bone'])
  event.add('c:bones', ['iceandfire:dragonbone', 'alexscaves:heavy_bone', 'iceandfire:witherbone'])

  // Thick hides work wherever leather is accepted (backpacks, tents, armor recipes using the tag)
  event.add('c:leathers', [
    'alexscaves:tough_hide', 'goblins_tyranny:bear_hide', 'naturalist:hide',
    'iceandfire:troll_leather_forest', 'iceandfire:troll_leather_frost', 'iceandfire:troll_leather_mountain'
  ])

  // Fiery blood: Twilight Forest hydra blood and fire dragon blood are the same reagent
  event.add('wilds:fiery_blood', ['twilightforest:fiery_blood', 'iceandfire:fire_dragon_blood'])

  // Hearts of great beasts: used as a catalyst for mana upgrades
  event.add('wilds:beast_hearts', [
    'iceandfire:fire_dragon_heart', 'iceandfire:ice_dragon_heart', 'iceandfire:lightning_dragon_heart',
    'iceandfire:hydra_heart'
  ])
})

ServerEvents.recipes(event => {
  // --- Interchangeable materials in the recipes of their own mods ---
  let replaced = [
    [{ mod: 'iceandfire', input: 'iceandfire:dragonbone' }, 'iceandfire:dragonbone', '#wilds:ancient_bones', 'ancient_bones'],
    [{ mod: 'alexscaves', input: 'alexscaves:heavy_bone' }, 'alexscaves:heavy_bone', '#wilds:ancient_bones', 'ancient_bones'],
    [{ mod: 'twilightforest', input: 'twilightforest:fiery_blood' }, 'twilightforest:fiery_blood', '#wilds:fiery_blood', 'fiery_blood']
  ]
  replaced.forEach(([filter, from, to, note]) => {
    // the wiki mod marks these recipes in JEI; mods-src/wilds_wiki/tools/gen_data.py reads this line from the log
    event.forEachRecipe(filter, r => console.info(`[wilds-changed] ${r.getId()} modified:${note}`))
    event.replaceInput(filter, from, to)
  })

  // --- Bones in machines (Create, Mekanism) ---
  event.custom({
    type: 'create:milling',
    ingredients: [{ tag: 'wilds:ancient_bones' }],
    processing_time: 200,
    results: [
      { id: 'minecraft:bone_meal', count: 9 },
      { id: 'minecraft:bone_meal', count: 3, chance: 0.5 },
      { id: 'minecraft:white_dye', chance: 0.25 }
    ]
  }).id('wilds:milling/ancient_bones')

  event.custom({
    type: 'mekanism:enriching',
    input: { count: 1, tag: 'wilds:ancient_bones' },
    output: { count: 12, id: 'minecraft:bone_meal' }
  }).id('wilds:enriching/ancient_bones')

  // --- Magic bridge: Ars Nouveau source feeds Iron's Spells and back ---
  event.custom({
    type: 'ars_nouveau:imbuement',
    input: { item: 'minecraft:amethyst_shard' },
    output: { count: 1, id: 'irons_spellbooks:arcane_essence' },
    pedestalItems: [],
    source: 200
  }).id('wilds:imbuement/arcane_essence')

  event.custom({
    type: 'ars_nouveau:imbuement',
    input: { item: 'irons_spellbooks:arcane_essence' },
    output: { count: 1, id: 'ars_nouveau:source_gem' },
    pedestalItems: [],
    source: 0
  }).id('wilds:imbuement/source_gem_from_arcane_essence')

  // A heart of a great beast replaces the eight arcane runes of a mana upgrade orb
  event.shapeless('irons_spellbooks:mana_upgrade_orb', [
    'irons_spellbooks:upgrade_orb', '#wilds:beast_hearts'
  ]).id('wilds:mana_upgrade_orb_from_beast_heart')

  // --- Technology meets beasts ---
  // Dragon scales crushed in Create give a little of the metal the dragon hoarded
  event.custom({
    type: 'create:crushing',
    ingredients: [{ tag: 'iceandfire:scales/dragon' }],
    processing_time: 250,
    results: [
      { id: 'minecraft:gold_nugget', count: 3 },
      { id: 'minecraft:gold_nugget', count: 3, chance: 0.5 },
      { id: 'create:experience_nugget', chance: 0.75 }
    ]
  }).id('wilds:crushing/dragon_scales')

  // Amber from the Primordial Caves is fossilised resin: Mekanism enriches it into slime balls
  event.custom({
    type: 'mekanism:enriching',
    input: { count: 2, item: 'alexscaves:amber' },
    output: { count: 1, id: 'minecraft:slime_ball' }
  }).id('wilds:enriching/amber_to_slime')

  // Tough dinosaur hide is pressed by Create into leather in bulk
  event.custom({
    type: 'create:pressing',
    ingredients: [{ item: 'alexscaves:tough_hide' }],
    results: [{ id: 'minecraft:leather', count: 3 }]
  }).id('wilds:pressing/tough_hide_to_leather')

  // --- World life (24.10): the new mods feed the old ones ---
  // Pebbles from the ground: a millstone grinds them into sand with a little flint
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'gew2:pebble' }],
    processing_time: 60,
    results: [{ id: 'minecraft:sand' }, { id: 'minecraft:flint', chance: 0.15 }]
  }).id('wilds:milling/pebble')

  // Goblin gold coins: melt them down, or crush them for a bonus nugget
  event.blasting('minecraft:gold_nugget', 'goblins_tyranny:coin').xp(0.1).id('wilds:blasting/goblin_coin')
  event.custom({
    type: 'create:crushing',
    ingredients: [{ item: 'goblins_tyranny:coin' }],
    processing_time: 100,
    results: [{ id: 'minecraft:gold_nugget', count: 2 }, { id: 'create:experience_nugget', chance: 0.25 }]
  }).id('wilds:crushing/goblin_coin')

  // Kobold steel goes back into the furnace like any iron tool
  ;['axe', 'hoe', 'pickaxe', 'shovel', 'spear', 'sword'].forEach(tool =>
    event.blasting('minecraft:iron_nugget', `kobolds:kobold_iron_${tool}`).xp(0.1).id(`wilds:blasting/kobold_iron_${tool}`))

  // Antlers are bone: milled into bone meal
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'naturalist:antler' }],
    processing_time: 150,
    results: [{ id: 'minecraft:bone_meal', count: 6 }]
  }).id('wilds:milling/antler')

  // Fur spins into wool
  event.shaped('minecraft:white_wool', ['FF', 'FF'], { F: 'naturalist:fur' }).id('wilds:wool_from_fur')

  // A black pearl from the deep holds a lot of source: the imbuement chamber draws it out
  event.custom({
    type: 'ars_nouveau:imbuement',
    input: { item: 'hybrid_aquatic:black_pearl' },
    output: { count: 4, id: 'ars_nouveau:source_gem' },
    pedestalItems: [],
    source: 100
  }).id('wilds:imbuement/black_pearl')
})

