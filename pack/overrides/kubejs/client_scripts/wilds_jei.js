// JEI "information" pages for the cross-mod links added in server_scripts/wilds_links.js,
// so players can see in the recipe viewer why an item suddenly works in another mod's recipes.
// Texts live in assets/wilds/lang (uk_ua / en_us).

RecipeViewerEvents.addInformation('item', event => {
  let info = (items, key) => event.add(items, [Text.translate('jei.wilds.' + key)])

  info(['iceandfire:dragonbone', 'alexscaves:heavy_bone'], 'ancient_bones')
  info(['twilightforest:fiery_blood', 'iceandfire:fire_dragon_blood'], 'fiery_blood')
  info(['iceandfire:fire_dragon_heart', 'iceandfire:ice_dragon_heart', 'iceandfire:lightning_dragon_heart',
    'iceandfire:hydra_heart', 'irons_spellbooks:mana_upgrade_orb'], 'beast_hearts')
  info(['irons_spellbooks:arcane_essence', 'ars_nouveau:source_gem'], 'arcane_bridge')
  info(['alexscaves:amber'], 'amber')
  info(['alexscaves:tough_hide'], 'tough_hide')
})
