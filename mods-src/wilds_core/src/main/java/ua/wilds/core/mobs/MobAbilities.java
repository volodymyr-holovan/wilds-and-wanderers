package ua.wilds.core.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ua.wilds.core.WildsCore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** The attacks of leveled mobs and bosses (names in docs/balance/mob-levels.md). Runs every 10 ticks per mob that has a player as target. */
final class MobAbilities {
    private static final ResourceLocation ARMOR_BREAK = ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "armor_break");
    private record Web(ResourceKey<Level> dim, BlockPos pos, long until) {}
    private static final List<Web> WEBS = new ArrayList<>();
    /** players whose armor is broken, until which tick */
    private static final Map<UUID, Long> BROKEN = new ConcurrentHashMap<>();

    private MobAbilities() {}

    private static boolean ready(MobLevels.Info in, String key, long now, int cooldownTicks) {
        Long t = in.cooldown.get(key);
        if (t != null && now < t) return false;
        in.cooldown.put(key, now + cooldownTicks);
        return true;
    }

    static void tick(ServerLevel level, Mob mob, MobLevels.Info in, long now) {
        float hpFrac = mob.getHealth() / Math.max(1f, mob.getMaxHealth());
        List<String> ab = in.abilities;
        // things that do not need a target
        if (ab.contains("regen") && now - in.lastHurt > 100 && hpFrac < 1 && (now % 40) < 10) mob.heal(mob.getMaxHealth() * 0.03f);
        if (ab.contains("shield") && ready(in, "shield", now, 300)) mob.setAbsorptionAmount(mob.getMaxAbsorption());
        if (!(mob.getTarget() instanceof Player target) || !target.isAlive() || target.isSpectator() || target.isCreative()) return;
        double dist = mob.distanceTo(target);
        if (dist > 24) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (hpFrac < 0.3f && !in.enraged && (ab.contains("enrage"))) {
            in.enraged = true;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getEyeY(), mob.getZ(), 8, 0.5, 0.5, 0.5, 0);
            level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1f, 1.2f);
        }
        for (String a : ab) {
            if (a.startsWith("summon:") && hpFrac < 0.5f && !in.summoned) {
                in.summoned = true;
                String[] p = a.substring(7).split(":");
                summon(level, mob, ResourceLocation.parse(p[0] + ":" + p[1]), Integer.parseInt(p[2]), 0.7f);
            }
        }
        if (in.boss && ab.contains("summon_guard")) {
            if (hpFrac < 0.66f && !in.guard66) { in.guard66 = true; summon(level, mob, guardType(level), 3, 0.6f); }
            if (hpFrac < 0.33f && !in.guard33) { in.guard33 = true; summon(level, mob, guardType(level), 3, 0.6f); }
        }
        if (ab.contains("leap") && dist > 4 && dist < 12 && mob.onGround() && ready(in, "leap", now, 120)) {
            Vec3 d = target.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(d.x * 0.9, 0.5, d.z * 0.9);
            mob.hasImpulse = true;
        }
        if (ab.contains("blink") && (dist > 7 || rnd.nextInt(8) == 0) && ready(in, "blink", now, 200)) blink(level, mob, target);
        if (ab.contains("shockwave") && dist < 3.5 && ready(in, "shockwave", now, in.boss ? 160 : 200)) shockwave(level, mob);
        if (ab.contains("pull") && dist > 5 && dist < 16 && mob.hasLineOfSight(target) && ready(in, "pull", now, 200)) {
            Vec3 d = mob.position().subtract(target.position()).normalize();
            target.setDeltaMovement(d.x * 1.1, 0.35, d.z * 1.1);
            target.hurtMarked = true;
            level.playSound(null, target.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.HOSTILE, 1f, 0.7f);
        }
        if (ab.contains("web") && dist < 16 && mob.hasLineOfSight(target) && ready(in, "web", now, 240)) web(level, target);
        if (ab.contains("volley") && dist > 3 && dist < 20 && mob.hasLineOfSight(target) && ready(in, "volley", now, 120)) volley(level, mob, target);
        if (ab.contains("frost_aura") && ready(in, "frost", now, 40)) {
            for (Player p : level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(4), MobAbilities::victim)) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                p.setTicksFrozen(Math.max(p.getTicksFrozen(), 100));
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, mob.getX(), mob.getY() + 1, mob.getZ(), 12, 2, 0.5, 2, 0.02);
        }
        if (ab.contains("fire_aura") && ready(in, "fire", now, 40)) {
            for (Player p : level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(3), MobAbilities::victim)) p.igniteForSeconds(3);
            level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 1, mob.getZ(), 12, 1.5, 0.5, 1.5, 0.02);
        }
    }

    private static boolean victim(Player p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    private static ResourceLocation guardType(ServerLevel level) {
        String dim = level.dimension().location().toString();
        if (dim.equals("minecraft:the_nether")) return ResourceLocation.withDefaultNamespace("wither_skeleton");
        if (dim.equals("minecraft:the_end")) return ResourceLocation.withDefaultNamespace("endermite");
        if (dim.startsWith("twilightforest:")) return ResourceLocation.fromNamespaceAndPath("twilightforest", "redcap");
        return ResourceLocation.withDefaultNamespace(level.random.nextBoolean() ? "zombie" : "skeleton");
    }

    private static void summon(ServerLevel level, Mob mob, ResourceLocation id, int n, float factor) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return;
        for (int i = 0; i < n; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            MobLevels.spawnHelper(level, mob, type, mob.getX() + Math.cos(a) * 2, mob.getY(), mob.getZ() + Math.sin(a) * 2, factor, 1f);
        }
        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 1.5, 0.5, 1.5, 0.02);
        level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1f, 1f);
    }

    private static void blink(ServerLevel level, Mob mob, Player target) {
        Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
        for (int i = 0; i < 4; i++) {
            Vec3 to = target.position().subtract(look.scale(2 + i * 0.5)).add(0, i == 3 ? 1 : 0, 0);
            if (level.noCollision(mob, mob.getBoundingBox().move(to.subtract(mob.position()))) && level.getBlockState(BlockPos.containing(to).below()).isSolid()) {
                level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.4, 0.8, 0.4, 0.1);
                mob.teleportTo(to.x, to.y, to.z);
                level.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1f);
                return;
            }
        }
    }

    private static void shockwave(ServerLevel level, Mob mob) {
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        float dmg = (float) ((atk != null && atk.getBaseValue() > 0 ? atk.getBaseValue() : 4) * 0.6);
        for (Player p : level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(4), MobAbilities::victim)) {
            p.hurt(mob.damageSources().mobAttack(mob), dmg);
            p.knockback(1.2, mob.getX() - p.getX(), mob.getZ() - p.getZ());
            p.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.5, mob.getZ(), 6, 1.5, 0.2, 1.5, 0);
        level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.4f);
    }

    private static void web(ServerLevel level, Player target) {
        BlockPos at = target.blockPosition();
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || !level.getBlockState(at).isAir()) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            return;
        }
        level.setBlockAndUpdate(at, Blocks.COBWEB.defaultBlockState());
        synchronized (WEBS) {
            WEBS.add(new Web(level.dimension(), at, level.getGameTime() + 100));
        }
    }

    private static void volley(ServerLevel level, Mob mob, Player target) {
        for (int i = -1; i <= 1; i++) {
            Arrow arrow = new Arrow(level, mob, new ItemStack(Items.ARROW), null);
            double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
            double dy = target.getY(0.33) - arrow.getY();
            double h = Math.sqrt(dx * dx + dz * dz);
            double ang = Math.toRadians(i * 8);
            double rx = dx * Math.cos(ang) - dz * Math.sin(ang), rz = dx * Math.sin(ang) + dz * Math.cos(ang);
            arrow.shoot(rx, dy + h * 0.2, rz, 1.6f, 4f);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            level.addFreshEntity(arrow);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1f, 0.8f);
    }

    /** a leveled mob's hit landed on a player (amount already scaled) */
    static void onHitPlayer(ServerLevel level, Mob mob, MobLevels.Info in, Player p, DamageSource src, float amount) {
        MobData.Variant v = in.kind();
        if (v != null) for (MobData.Effect h : v.hits()) effect(p, h);
        if (in.abilities.contains("family_hit")) {
            MobData.Family f = v != null ? MobData.families.get(v.family()) : MobData.byBase.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
            if (f != null && f.hit() != null) effect(p, f.hit());
        }
        if (in.abilities.contains("ignite") || (in.abilities.contains("burst_fire") && src.is(DamageTypeTags.IS_EXPLOSION))) p.igniteForSeconds(4);
        if (in.abilities.contains("lifesteal")) mob.heal(amount * 0.25f);
        if (in.abilities.contains("armor_break")) {
            AttributeInstance a = p.getAttribute(Attributes.ARMOR);
            if (a != null) {
                a.addOrUpdateTransientModifier(new AttributeModifier(ARMOR_BREAK, -4, AttributeModifier.Operation.ADD_VALUE));
                BROKEN.put(p.getUUID(), level.getGameTime() + 120);
            }
        }
    }

    private static void effect(LivingEntity e, MobData.Effect h) {
        BuiltInRegistries.MOB_EFFECT.getHolder(h.id()).ifPresent(holder -> e.addEffect(new MobEffectInstance((Holder<MobEffect>) holder, h.ticks(), h.amp())));
    }

    /** a player hit a leveled mob (amount after scaling) */
    static void onHurtByPlayer(ServerLevel level, Mob mob, MobLevels.Info in, ServerPlayer p, DamageSource src, float amount, long now) {
        if (in.abilities.contains("reflect") && src.getDirectEntity() == p) p.hurt(mob.damageSources().thorns(mob), (float) (amount * 0.2f / in.dmgMult)); // the hit back is scaled up again as the mob's
        if (in.abilities.contains("blink") && src.getDirectEntity() instanceof Projectile && ready(in, "blink", now, 100)) blink(level, mob, p);
        if (mob.getTarget() == null) mob.setTarget(p);
    }

    static void onDeath(ServerLevel level, Mob mob, MobLevels.Info in) {
        for (String a : in.abilities) {
            if (!a.startsWith("split:") || in.minion) continue;
            String[] p = a.substring(6).split(":");
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(p[0] + ":" + p[1])).orElse(null);
            if (type == null) continue;
            int n = Integer.parseInt(p[2]);
            for (int i = 0; i < n; i++) {
                double ang = Math.PI * 2 * i / n;
                MobLevels.spawnHelper(level, mob, type, mob.getX() + Math.cos(ang), mob.getY(), mob.getZ() + Math.sin(ang), 0.6f, 0.6f);
            }
        }
    }

    /** remove temporary webs and give back broken armor */
    static void expire(MinecraftServer server, long tick) {
        if (tick % 20 != 0) return;
        synchronized (WEBS) {
            for (Iterator<Web> it = WEBS.iterator(); it.hasNext(); ) {
                Web w = it.next();
                ServerLevel level = server.getLevel(w.dim());
                if (level == null) { it.remove(); continue; }
                if (level.getGameTime() < w.until()) continue;
                if (level.isLoaded(w.pos()) && level.getBlockState(w.pos()).is(Blocks.COBWEB)) level.removeBlock(w.pos(), false);
                it.remove();
            }
        }
        if (BROKEN.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Iterator<Map.Entry<UUID, Long>> it = BROKEN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            if (now < en.getValue()) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p != null) {
                AttributeInstance a = p.getAttribute(Attributes.ARMOR);
                if (a != null) a.removeModifier(ARMOR_BREAK);
            }
            it.remove();
        }
    }

    static int clampInt(double v) { return Mth.floor(v); }
}
