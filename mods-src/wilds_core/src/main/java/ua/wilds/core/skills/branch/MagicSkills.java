package ua.wilds.core.skills.branch;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

/**
 * Маг. Big abilities:
 * <ul>
 * <li>spell_reflect "Дзеркало чар": a spell or any other non-arrow projectile (fireball, shulker bullet, wither skull,
 * a mod's spell) that hits a player who is swinging a weapon or raising a shield flies back at its caster;</li>
 * <li>free_spell "Подих мани" (Iron's Spells): once an hour a spell costs no mana;</li>
 * <li>mana_harvest "Жнива мани" (Iron's): a kill restores 10% of the mana pool per level; fallback soul_siphon heals 1 heart;</li>
 * <li>mana_shield "Щит мани" (Iron's): 15% of the damage taken is paid with mana while there is enough; fallback
 * arcane_ward ignores 20% of magic damage.</li>
 * </ul>
 * Perk magic_guard: a share of all magic damage is ignored.
 */
public final class MagicSkills {
    private static boolean irons;

    private MagicSkills() {}

    public static void register() {
        irons = ModList.get().isLoaded("irons_spellbooks");
        NeoForge.EVENT_BUS.addListener(MagicSkills::reflect);
        NeoForge.EVENT_BUS.addListener(MagicSkills::onKill);
        NeoForge.EVENT_BUS.addListener(MagicSkills::onIncoming);
        if (irons) IronsMana.register();
    }

    private static void reflect(ProjectileImpactEvent e) {
        Projectile proj = e.getProjectile();
        if (proj.level().isClientSide || proj instanceof AbstractArrow || !(e.getRayTraceResult() instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof ServerPlayer p) || !SkillState.has(p, "spell_reflect")) return;
        Entity caster = proj.getOwner();
        if (caster == p || !(p.isBlocking() || p.swinging)) return;
        e.setCanceled(true);
        Vec3 dir = caster instanceof LivingEntity c ? c.getEyePosition().subtract(proj.position()).normalize() : proj.getDeltaMovement().reverse().normalize();
        double speed = Math.max(0.6, proj.getDeltaMovement().length());
        proj.setDeltaMovement(dir.scale(speed));
        if (proj instanceof AbstractHurtingProjectile h) h.accelerationPower = Math.max(0.1, h.accelerationPower);
        proj.setOwner(p);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, proj.getX(), proj.getY(), proj.getZ(), 20, 0.3, 0.3, 0.3, 0.5);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.6f);
    }

    private static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p) return;
        int lv = SkillState.level(p, "mana_harvest");
        if (lv > 0 && irons) IronsMana.restoreFraction(p, 0.10f * lv);
        int siphon = SkillState.level(p, "soul_siphon");
        if (siphon > 0) p.heal(2f * siphon);
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean magic = e.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO) || e.getSource().getMsgId().contains("magic")
                || e.getSource().type().msgId().startsWith("irons_spellbooks");
        if (magic) {
            double guard = Math.min(0.6, SkillState.perk(p, Perks.MAGIC_GUARD) + 0.2 * SkillState.level(p, "arcane_ward"));
            if (guard > 0) e.setAmount((float) (e.getAmount() * (1 - guard)));
        }
        int shield = SkillState.level(p, "mana_shield");
        if (shield > 0 && irons && e.getAmount() > 0) {
            float part = e.getAmount() * 0.15f * shield;
            if (IronsMana.pay(p, part * 10)) e.setAmount(e.getAmount() - part);
        }
    }
}
