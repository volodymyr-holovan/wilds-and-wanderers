package ua.wilds.core.skills.branch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.wilds.core.WildsCore;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Слідопит. Big abilities:
 * <ul>
 * <li>danger_sense "Чуття небезпеки": every monster that has chosen the player as its target (20 blocks, +12 per level)
 * shows angry sparks above it, seen only by this player, even behind walls;</li>
 * <li>rider "Вершник": the animal the player rides is 15% faster per level (+ the mount perk) and neither of them takes
 * fall damage;</li>
 * <li>well_rested "Добрий відпочинок": waking up after a night's sleep (in a bed or a tent) gives regeneration and
 * absorption for a while;</li>
 * <li>pathfinder "Дорожня хода": 20% faster (per level) on roads: dirt paths, gravel, stone bricks and similar.</li>
 * </ul>
 * Perks: fire_guard (less fire damage), regen (hearts every 5 s while fed), hearty (more saturation from food).
 */
public final class ExplorerSkills {
    private static final ResourceLocation MOUNT_SPEED = ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "rider");
    private static final ResourceLocation ROAD_SPEED = ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "pathfinder");
    private static final Map<UUID, Entity> mounts = new HashMap<>();

    private ExplorerSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ExplorerSkills::tick);
        NeoForge.EVENT_BUS.addListener(ExplorerSkills::onIncoming);
        NeoForge.EVENT_BUS.addListener(ExplorerSkills::fall);
        NeoForge.EVENT_BUS.addListener(ExplorerSkills::wake);
        NeoForge.EVENT_BUS.addListener(ExplorerSkills::eat);
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % 10 == 3) {
            mount(p);
            road(p);
        }
        if (p.tickCount % 40 == 21) {
            int sense = SkillState.level(p, "danger_sense");
            if (sense > 0) {
                double r = 20 + 12 * (sense - 1);
                for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), m -> m instanceof Enemy && m.getTarget() == p)) {
                    p.serverLevel().sendParticles(p, ParticleTypes.ANGRY_VILLAGER, true, m.getX(), m.getY() + m.getBbHeight() + 0.5, m.getZ(), 2, 0.2, 0.1, 0.2, 0);
                }
            }
        }
        if (p.tickCount % 100 == 57) {
            double regen = SkillState.perk(p, Perks.REGEN);
            if (regen > 0 && p.getFoodData().getFoodLevel() >= 7 && p.getHealth() < p.getMaxHealth() && p.isAlive()) p.heal((float) (regen * 2));
        }
    }

    private static void mount(ServerPlayer p) {
        Entity v = p.getVehicle();
        Entity old = mounts.get(p.getUUID());
        if (old != null && old != v) {
            speed(old, MOUNT_SPEED, 0);
            mounts.remove(p.getUUID());
        }
        if (v instanceof LivingEntity) {
            double bonus = 0.15 * SkillState.level(p, "rider") + SkillState.perk(p, Perks.MOUNT);
            speed(v, MOUNT_SPEED, bonus);
            if (bonus > 0) mounts.put(p.getUUID(), v);
        }
    }

    private static void road(ServerPlayer p) {
        int lv = SkillState.level(p, "pathfinder");
        if (lv <= 0) return;
        BlockState under = p.level().getBlockState(p.getOnPos());
        boolean road = p.onGround() && (under.is(Blocks.DIRT_PATH) || under.is(Blocks.GRAVEL) || under.is(BlockTags.STONE_BRICKS)
                || under.is(Blocks.COBBLESTONE) || under.is(Blocks.MUD_BRICKS) || under.is(Blocks.SANDSTONE) || under.is(Blocks.PACKED_MUD)
                || under.getBlock().getDescriptionId().contains("path") || under.getBlock().getDescriptionId().contains("cobble"));
        speed(p, ROAD_SPEED, road ? 0.20 * lv : 0);
    }

    private static void speed(Entity e, ResourceLocation id, double bonus) {
        if (!(e instanceof LivingEntity le)) return;
        AttributeInstance inst = le.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && cur.amount() == bonus) return;
        inst.removeModifier(id);
        if (bonus > 0) inst.addTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.getSource().is(DamageTypeTags.IS_FIRE)) return;
        double g = Math.min(0.8, SkillState.perk(p, Perks.FIRE_GUARD));
        if (g > 0) e.setAmount((float) (e.getAmount() * (1 - g)));
    }

    private static void fall(LivingFallEvent e) {
        LivingEntity le = e.getEntity();
        if (le instanceof ServerPlayer p && p.isPassenger() && SkillState.has(p, "rider")) e.setCanceled(true);
        else if (le.getFirstPassenger() instanceof ServerPlayer rider && SkillState.has(rider, "rider")) e.setCanceled(true);
    }

    private static void wake(PlayerWakeUpEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.wakeImmediately() || !SkillState.has(p, "well_rested")) return;
        // a whole night's sleep (the game woke everyone at dawn)
        if (p.level().isNight()) return;
        int lv = SkillState.level(p, "well_rested");
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, lv - 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400 * lv, 1));
        p.displayClientMessage(Component.translatable("wilds_core.skill.well_rested.used").withStyle(ChatFormatting.GREEN), true);
    }

    private static void eat(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        double h = SkillState.perk(p, Perks.HEARTY);
        FoodProperties food = e.getItem().getFoodProperties(p);
        if (h <= 0 || food == null) return;
        var fd = p.getFoodData();
        fd.setSaturation((float) Math.min(fd.getFoodLevel(), fd.getSaturationLevel() + food.saturation() * h));
    }
}
