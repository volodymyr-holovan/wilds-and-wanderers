// Wilds & Wanderers: loot from every mod turns up in the chests of every other.
// The extra roll comes from the pack's themed tables in kubejs/data/wilds/loot_table/inject (written by
// pack/tools/gen_loot_inject.py). Chances stay low so structures keep their own character.

// only chest tables ("chest" in the id), so a block like gravel never matches "grave"
const DUNGEONS = /^(?=.*chest)(?=.*(dungeon|crypt|tomb|grave|tower|vault|treasure|fortress|bastion|mansion|stronghold|temple|catacomb|citadel|keep|monastery|ruin|castle|prison|lair|hideout|arise)).*/
const SEAS = /^(?=.*chest)(?=.*(ocean|shipwreck|underwater|monument|buried|coral|tidal|lily|ship|seven_seas|harbor|port|lighthouse|fisher)).*/
const NETHER = /^(?=.*chest)(?=.*(nether|bastion|blaze|ruined_portal|piglin)).*/
// End cities, Enderscape ruins and vaults, Cataclysm's End structures
const END = /^(?=.*(chest|vault))(?=.*(end_city|the_end|enderscape|ender|purpur|mirestone|void)).*/

LootJS.modifiers(event => {
  event.addTableModifier(LootType.CHEST)
    .randomChance(0.30)
    .pool(p => p.addEntry(LootEntry.reference('wilds:inject/common')))
    .name('wilds:chests_common')

  event.addTableModifier(DUNGEONS)
    .randomChance(0.20)
    .pool(p => p.addEntry(LootEntry.reference('wilds:inject/rare')))
    .name('wilds:chests_rare')

  event.addTableModifier(SEAS)
    .randomChance(0.35)
    .pool(p => p.addEntry(LootEntry.reference('wilds:inject/ocean')))
    .name('wilds:chests_ocean')

  event.addTableModifier(NETHER)
    .randomChance(0.30)
    .pool(p => p.addEntry(LootEntry.reference('wilds:inject/nether')))
    .name('wilds:chests_nether')

  event.addTableModifier(END)
    .randomChance(0.35)
    .pool(p => p.addEntry(LootEntry.reference('wilds:inject/end')))
    .name('wilds:chests_end')
})
