package ua.wilds.core.skills.branch;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.wilds.core.WildsCore;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Мисливець. Big abilities:
 * <ul>
 * <li>hunters_mark "Мітка мисливця": an arrow (or any shot) that hits marks the target for 10 s: it glows and takes 15% more
 * damage from everyone;</li>
 * <li>volley "Залп": every fifth arrow (fourth at level 2) flies with two more beside it;</li>
 * <li>beast_master "Володар звірів": the player's tamed animals and horses get +25% health and damage per level (+ the
 * tamer perk);</li>
 * <li>twin_births "Двійня": animals the player breeds have a 25% (per level) chance of twins.</li>
 * </ul>
 * Perk far_shot: everything the player launches (arrows, tridents, spell projectiles) flies faster and so further.
 */
public final class HunterSkills {
    private static final ResourceLocation PET_HEALTH = ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "beast_master_health");
    private static final ResourceLocation PET_DAMAGE = ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "beast_master_damage");
    private static final Map<Entity, Long> marked = new WeakHashMap<>();
    private static final Map<UUID, Integer> shots = new java.util.HashMap<>();
    private static boolean spawningVolley;

    private HunterSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HunterSkills::onJoin);
        NeoForge.EVENT_BUS.addListener(HunterSkills::onHit);
        NeoForge.EVENT_BUS.addListener(HunterSkills::onIncoming);
        NeoForge.EVENT_BUS.addListener(HunterSkills::onBirth);
        NeoForge.EVENT_BUS.addListener(HunterSkills::onTick);
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof Projectile proj) || e.loadedFromDisk()) return;
        if (!(proj.getOwner() instanceof ServerPlayer p)) return;
        double far = Math.min(1.0, SkillState.perk(p, Perks.FAR_SHOT));
        if (far > 0 && proj.tickCount == 0) proj.setDeltaMovement(proj.getDeltaMovement().scale(1 + far));
        if (spawningVolley || !(proj instanceof AbstractArrow arrow) || !SkillState.has(p, "volley")) return;
        int every = SkillState.level(p, "volley") >= 2 ? 4 : 5;
        int n = shots.merge(p.getUUID(), 1, Integer::sum);
        if (n % every != 0) return;
        spawningVolley = true;
        try {
            for (int side : new int[]{-1, 1}) {
                Arrow extra = new Arrow(p.level(), p, arrow.getPickupItemStackOrigin().copyWithCount(1), null);
                extra.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                extra.setDeltaMovement(arrow.getDeltaMovement().yRot((float) Math.toRadians(6 * side)));
                extra.setBaseDamage(arrow.getBaseDamage());
                extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                p.level().addFreshEntity(extra);
            }
        } finally {
            spawningVolley = false;
        }
    }

    private static void onHit(LivingDamageEvent.Post e) {
        if (!(e.getSource().getDirectEntity() instanceof Projectile) || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (!SkillState.has(p, "hunters_mark") || e.getEntity() == p) return;
        LivingEntity t = e.getEntity();
        marked.put(t, p.level().getGameTime() + 200);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        Long until = marked.get(e.getEntity());
        if (until == null) return;
        if (until < e.getEntity().level().getGameTime()) {
            marked.remove(e.getEntity());
            return;
        }
        e.setAmount(e.getAmount() * 1.15f);
    }

    private static void onBirth(BabyEntitySpawnEvent e) {
        if (!(e.getCausedByPlayer() instanceof ServerPlayer p) || !(e.getParentA() instanceof Animal a) || !(e.getParentB() instanceof AgeableMob b)) return;
        int lv = SkillState.level(p, "twin_births");
        if (lv <= 0 || p.getRandom().nextFloat() >= 0.25f * lv) return;
        AgeableMob twin = a.getBreedOffspring((ServerLevel) p.level(), b);
        if (twin == null) return;
        twin.setBaby(true);
        twin.moveTo(a.getX(), a.getY(), a.getZ(), 0, 0);
        p.level().addFreshEntity(twin);
    }

    /** every 5 seconds: the player's pets get (or lose) the beast master bonus */
    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 37) return;
        double bonus = 0.25 * SkillState.level(p, "beast_master") + SkillState.perk(p, Perks.TAMER);
        if (bonus <= 0 && !p.getPersistentData().getBoolean("wilds_core:had_pets_bonus")) return;
        p.getPersistentData().putBoolean("wilds_core:had_pets_bonus", bonus > 0);
        for (LivingEntity pet : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(48),
                le -> le instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID()) || le instanceof AbstractHorse h && h.isTamed() && p.getUUID().equals(h.getOwnerUUID()))) {
            apply(pet.getAttribute(Attributes.MAX_HEALTH), PET_HEALTH, bonus);
            apply(pet.getAttribute(Attributes.ATTACK_DAMAGE), PET_DAMAGE, bonus);
        }
    }

    private static void apply(AttributeInstance inst, ResourceLocation id, double bonus) {
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(id);
        if (old != null && old.amount() == bonus) return;
        inst.removeModifier(id);
        if (bonus > 0) inst.addTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}
