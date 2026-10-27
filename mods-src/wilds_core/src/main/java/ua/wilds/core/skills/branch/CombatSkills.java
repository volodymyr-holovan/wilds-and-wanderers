package ua.wilds.core.skills.branch;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import ua.wilds.core.skills.SkillState;
import ua.wilds.core.skills.ThrownWeapon;

/**
 * Воїн. Big abilities:
 * <ul>
 * <li>weapon_throw "Метальник": hold use (right button) with a melee weapon and press attack - the weapon flies at the aim
 * for twice its damage and comes back;</li>
 * <li>heavy_strike "Нищівний удар": hold attack for at least the weapon's recharge (1 s minimum) - a blow of triple damage
 * that knocks a raised shield out of the target's hands (the client times it, the server checks reach and sight);</li>
 * <li>retribution "Відплата": 10% (per level) of the damage taken goes back to the attacker;</li>
 * <li>last_stand "Останній рубіж": once in 20 minutes a deadly blow leaves 1 heart, with a few seconds of protection.</li>
 * </ul>
 */
public final class CombatSkills {
    private CombatSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CombatSkills::retribution);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CombatSkills::lastStand);
    }

    /** the weapon's own attack damage (attribute + the item's modifiers) */
    private static double attackDamage(ServerPlayer p) {
        return p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public static boolean isMeleeWeapon(ItemStack s) {
        if (s.isEmpty() || s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem || s.getItem() instanceof ShieldItem
                || s.getItem() instanceof TridentItem) return false;
        ItemAttributeModifiers mods = s.getAttributeModifiers();
        boolean[] dmg = {false};
        mods.forEach(EquipmentSlotGroup.MAINHAND, (attr, mod) -> {
            if (attr.equals(Attributes.ATTACK_DAMAGE)) dmg[0] = true;
        });
        return dmg[0];
    }

    public static void throwWeapon(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (!SkillState.has(p, "weapon_throw") || !isMeleeWeapon(held) || p.getCooldowns().isOnCooldown(held.getItem())) return;
        float damage = (float) (attackDamage(p) * (2 + 0.25 * (SkillState.level(p, "weapon_throw") - 1)));
        ThrownWeapon w = ThrownWeapon.of(p, held.copy(), damage);
        w.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.2f, 0.5f);
        p.level().addFreshEntity(w);
        p.getInventory().removeItem(p.getInventory().selected, held.getCount());
        p.getCooldowns().addCooldown(held.getItem(), 40);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, 1.2f);
    }

    public static void heavyStrike(ServerPlayer p, int targetId) {
        if (!SkillState.has(p, "heavy_strike")) return;
        Entity e = p.level().getEntity(targetId);
        if (!(e instanceof LivingEntity target) || !target.isAlive() || target == p) return;
        double reach = p.entityInteractionRange() + 1;
        if (p.distanceToSqr(target) > reach * reach || !p.hasLineOfSight(target)) return;
        // the client fires this only after holding attack for the full recharge; the server keeps a minimum gap too
        if (!SkillState.use(p, "heavy_strike", 15)) return;
        float damage = (float) (attackDamage(p) * (3 + 0.5 * (SkillState.level(p, "heavy_strike") - 1)));
        if (target instanceof Player tp && tp.isBlocking()) tp.disableShield();
        else if (target.isBlocking()) target.stopUsingItem();
        target.hurt(p.damageSources().playerAttack(p), damage);
        target.knockback(0.8, p.getX() - target.getX(), p.getZ() - target.getZ());
        p.resetAttackStrengthTicker();
        ServerLevel lvl = p.serverLevel();
        lvl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 16, 0.4, 0.4, 0.4, 0.2);
        lvl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    private static void retribution(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getSource().getEntity() instanceof LivingEntity attacker) || attacker == p) return;
        int lv = SkillState.level(p, "retribution");
        if (lv <= 0 || e.getNewDamage() <= 0) return;
        attacker.hurt(p.damageSources().thorns(p), e.getNewDamage() * 0.10f * lv);
    }

    private static void lastStand(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !SkillState.has(p, "last_stand")) return;
        if (e.getAmount() < p.getHealth() || p.getHealth() <= 1) return;
        if (!SkillState.use(p, "last_stand", 24000)) return;
        e.setAmount(Math.max(0, p.getHealth() - 2));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        p.displayClientMessage(Component.translatable("wilds_core.skill.last_stand.used").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY(1), p.getZ(), 30, 0.5, 0.8, 0.5, 0.3);
    }
}
