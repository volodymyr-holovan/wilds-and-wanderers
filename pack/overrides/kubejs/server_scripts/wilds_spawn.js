// Wilds & Wanderers: protected world spawn.
// Within 20 blocks of the world spawn (a full column, any height) players who are not operators cannot break or place
// blocks, pour liquids or light fires, nobody takes damage, and explosions leave blocks and players there alone.
// Hostile mobs do not spawn within 5 blocks of it. Vanilla spawn-protection stays 0: chests, doors and NPCs at the
// spawn keep working for everyone.

const SPAWN_SAFE_RADIUS = 20
const SPAWN_NO_MOBS_RADIUS = 5
const OVERWORLD = 'minecraft:overworld'

function spawnDistanceSq(level, x, z) {
  if (level.dimension.toString() !== OVERWORLD) return Infinity
  let s = level.getSharedSpawnPos()
  let dx = x - (s.x + 0.5), dz = z - (s.z + 0.5)
  return dx * dx + dz * dz
}

function inSafeZone(level, x, z) {
  return spawnDistanceSq(level, x, z) <= SPAWN_SAFE_RADIUS * SPAWN_SAFE_RADIUS
}

function isAdmin(entity) {
  return entity && entity.isPlayer() && entity.hasPermissions(2)
}

function warn(player) {
  if (player && player.isPlayer()) player.setStatusMessage(Text.translate('wilds.spawn.protected').gold())
}

BlockEvents.broken(event => {
  let b = event.block
  if (inSafeZone(event.level, b.x, b.z) && !isAdmin(event.player)) {
    warn(event.player)
    event.cancel()
  }
})

BlockEvents.placed(event => {
  let b = event.block
  if (inSafeZone(event.level, b.x, b.z) && !isAdmin(event.entity)) {
    warn(event.entity)
    event.cancel()
  }
})

// buckets, flint and steel, fire charges: liquids and fire do not go through the placed event
BlockEvents.rightClicked(event => {
  let b = event.block
  if (!inSafeZone(event.level, b.x, b.z) || isAdmin(event.player)) return
  let id = event.item.id
  if (id.endsWith('bucket') || id === 'minecraft:flint_and_steel' || id === 'minecraft:fire_charge') {
    warn(event.player)
    event.cancel()
  }
})

// no damage at all in the zone (damage that ignores invulnerability, like /kill and the void, still applies)
const BYPASSES_INVULNERABILITY = Java.loadClass('net.minecraft.tags.DamageTypeTags').BYPASSES_INVULNERABILITY
let safeDamage = (entity, source) => entity.isPlayer() && inSafeZone(entity.level, entity.x, entity.z)
  && !source.is(BYPASSES_INVULNERABILITY)
try {
  // an error thrown in a native listener is not caught by KubeJS and would crash the server: catch everything here
  NativeEvents.onEvent(Java.loadClass('net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent'), e => {
    try {
      if (safeDamage(e.entity, e.source)) e.setCanceled(true)
    } catch (err) {
      console.error('wilds_spawn: damage check failed: ' + err)
    }
  })
} catch (err) {
  console.warn('wilds_spawn: incoming damage event unavailable, falling back to zero damage: ' + err)
}
EntityEvents.beforeHurt(event => {
  if (safeDamage(event.entity, event.source)) event.setDamage(0)
})

LevelEvents.afterExplosion(event => {
  let level = event.level
  // collect first, then remove: the lists are views of the explosion
  let blocks = [], players = []
  for (let b of event.getAffectedBlocks()) if (inSafeZone(level, b.x, b.z)) blocks.push(b)
  for (let e of event.getAffectedEntities()) if (e.isPlayer() && inSafeZone(level, e.x, e.z)) players.push(e)
  blocks.forEach(b => event.removeAffectedBlock(b))
  players.forEach(e => event.removeAffectedEntity(e))
})

EntityEvents.checkSpawn(event => {
  let e = event.entity
  if (e.isMonster()
    && spawnDistanceSq(event.level, event.x, event.z) <= SPAWN_NO_MOBS_RADIUS * SPAWN_NO_MOBS_RADIUS) {
    event.cancel()
  }
})
