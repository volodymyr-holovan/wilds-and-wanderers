// Wilds & Wanderers: villagers trade the goods of the pack's mods, so a village is worth visiting for an engineer,
// a mage or a sailor too. Vanilla trades stay; these are added on top (MoreJS). Levels: 1 novice ... 5 master.
// Goblin gold coins are a second currency: cartographers and clerics take them.

MoreJS.villagerTrades(event => {
  let t = (profession, level, cost, result, uses) =>
    event.addTrade(profession, level, cost, result).maxUses(uses || 8).villagerExperience(level * 4)

  // toolsmith: the tools every tech player needs
  t('minecraft:toolsmith', 2, ['6x minecraft:emerald'], 'create:wrench')
  t('minecraft:toolsmith', 3, ['10x minecraft:emerald'], 'mekanism:configurator')
  t('minecraft:toolsmith', 3, ['8x minecraft:emerald', 'minecraft:copper_ingot'], 'create:goggles')
  t('minecraft:toolsmith', 4, ['14x minecraft:emerald'], 'create:super_glue')

  // armorer and weaponsmith: silver against the undead, kobold steel
  t('minecraft:armorer', 3, ['16x minecraft:emerald'], 'iceandfire:armor_silver_metal_chestplate', 3)
  t('minecraft:armorer', 2, ['4x minecraft:emerald'], '2x iceandfire:silver_ingot', 12)
  t('minecraft:weaponsmith', 3, ['12x minecraft:emerald'], 'iceandfire:silver_sword', 3)
  t('minecraft:weaponsmith', 2, ['9x minecraft:emerald'], 'kobolds:kobold_iron_sword', 4)

  // mason: materials of the machines
  t('minecraft:mason', 1, ['2x minecraft:emerald'], '6x create:andesite_alloy', 16)
  t('minecraft:mason', 2, ['3x minecraft:emerald'], '4x mekanism:ingot_osmium', 16)
  t('minecraft:mason', 3, ['5x minecraft:emerald'], '4x create:brass_ingot', 12)
  t('minecraft:mason', 3, ['4x minecraft:emerald'], '6x ae2:certus_quartz_crystal', 12)

  // cleric: the two schools of magic, amber
  t('minecraft:cleric', 1, ['3x minecraft:emerald'], '2x ars_nouveau:source_gem', 12)
  t('minecraft:cleric', 2, ['4x minecraft:emerald'], '3x irons_spellbooks:arcane_essence', 12)
  t('minecraft:cleric', 3, ['5x minecraft:emerald'], '2x irons_spellbooks:common_ink', 8)
  t('minecraft:cleric', 3, ['12x goblins_tyranny:coin'], '2x minecraft:emerald', 12)
  t('minecraft:cleric', 4, ['8x minecraft:emerald'], 'irons_spellbooks:blank_rune', 6)

  // librarian: parchment for spells, scrolls for the road
  t('minecraft:librarian', 2, ['3x minecraft:emerald'], '4x ars_nouveau:blank_parchment', 12)
  t('minecraft:librarian', 3, ['6x minecraft:emerald'], 'waystones:return_scroll', 6)
  t('minecraft:librarian', 4, ['10x minecraft:emerald'], 'waystones:warp_scroll', 4)

  // cartographer: compasses that find structures and biomes; takes goblin gold
  t('minecraft:cartographer', 3, ['16x minecraft:emerald', 'minecraft:compass'], 'naturescompass:naturescompass', 2)
  t('minecraft:cartographer', 4, ['24x minecraft:emerald', 'minecraft:compass'], 'explorerscompass:explorerscompass', 2)
  t('minecraft:cartographer', 2, ['10x goblins_tyranny:coin'], 'minecraft:emerald', 16)

  // farmer and butcher: Farmer's Delight crops, wild game
  t('minecraft:farmer', 1, ['minecraft:emerald'], '4x farmersdelight:cabbage_seeds', 16)
  t('minecraft:farmer', 1, ['minecraft:emerald'], '4x farmersdelight:tomato_seeds', 16)
  t('minecraft:farmer', 2, ['minecraft:emerald'], '6x farmersdelight:onion', 16)
  t('minecraft:farmer', 2, ['2x minecraft:emerald'], '4x farmersdelight:rice_panicle', 16)
  t('minecraft:butcher', 2, ['2x minecraft:emerald'], '4x naturalist:venison', 12)
  t('minecraft:butcher', 2, ['2x minecraft:emerald'], '4x naturalist:bushmeat', 12)

  // fisherman: fish of the seven seas, harpoon hook, sailing
  t('minecraft:fisherman', 2, ['3x minecraft:emerald'], '2x fishofthieves:splashtail', 12)
  t('minecraft:fisherman', 3, ['8x minecraft:emerald'], 'hybrid_aquatic:barbed_hook', 4)
  t('minecraft:fisherman', 4, ['20x minecraft:emerald', '8x minecraft:oak_planks'], 'smallships:oak_cog', 2)
  t('minecraft:fisherman', 5, ['18x minecraft:emerald', 'minecraft:iron_block'], 'smallships:cannon', 2)

  // leatherworker: gear for the traveller
  t('minecraft:leatherworker', 2, ['6x minecraft:emerald'], 'comforts:sleeping_bag_brown', 4)
  t('minecraft:leatherworker', 3, ['10x minecraft:emerald'], 'simplytents:tent', 3)
  t('minecraft:leatherworker', 4, ['16x minecraft:emerald'], 'sophisticatedbackpacks:backpack', 2)
  t('minecraft:leatherworker', 3, ['8x minecraft:emerald'], 'naturalist:capture_net', 4)

  // shepherd: beachgrass and thatch from the coast
  t('minecraft:shepherd', 2, ['minecraft:emerald'], '8x upgrade_aquatic:beachgrass', 16)
})

MoreJS.wandererTrades(event => {
  event.addTrade(1, ['4x minecraft:emerald'], '2x alexscaves:amber').maxUses(6)
  event.addTrade(1, ['3x minecraft:emerald'], '2x ars_nouveau:source_gem').maxUses(6)
  event.addTrade(2, ['20x goblins_tyranny:coin'], 'mekanism:basic_control_circuit').maxUses(3)
  event.addTrade(2, ['12x minecraft:emerald'], 'naturescompass:naturescompass').maxUses(1)
  // a colony to start: the supply camp unpacks a chest with the town hall and the build tool (MineColonies)
  event.addTrade(2, ['24x minecraft:emerald'], 'minecolonies:supplycampdeployer').maxUses(1)
})
