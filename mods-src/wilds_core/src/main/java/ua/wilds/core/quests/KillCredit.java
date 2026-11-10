package ua.wilds.core.quests;

import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import ua.wilds.core.WildsCore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Who a kill counts for in the quests. FTB Quests gives it only to the player who dealt the last blow, and only when
 * that blow was the player's own: a boss that dies of its own reflected fireball (the Ur-ghast), of a pet, a golem, fire,
 * a fall or an explosion counted for nobody, and a boss fought by several counted for one. Here every player who hurt
 * the creature in its last ten minutes (themselves or by their pets) gets the kill, whoever or whatever finished it.
 */
public final class KillCredit {
    private static final long MEMORY = 12000;
    private static final Map<LivingEntity, Map<UUID, Long>> hurtBy = new WeakHashMap<>();

    private KillCredit() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(KillCredit::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KillCredit::onDeath);
    }

    /** the player behind a blow: the player, or the owner of the pet or summon that dealt it */
    private static ServerPlayer player(DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer p) return p;
        if (e instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer p) return p;
        return null;
    }

    private static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || victim instanceof ServerPlayer || e.getNewDamage() <= 0) return;
        ServerPlayer p = player(e.getSource());
        if (p == null || p.isFakePlayer()) return;
        hurtBy.computeIfAbsent(victim, v -> new HashMap<>()).put(p.getUUID(), victim.level().getGameTime());
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || e.isCanceled()) return;
        Map<UUID, Long> by = hurtBy.remove(victim);
        if (by == null || ServerQuestFile.INSTANCE == null) return;
        // FTB Quests itself counts it for a player whose own blow was the last
        UUID counted = e.getSource().getEntity() instanceof ServerPlayer killer && !killer.isFakePlayer() ? killer.getUUID() : null;
        long now = victim.level().getGameTime();
        try {
            List<KillTask> tasks = ServerQuestFile.INSTANCE.collect(KillTask.class);
            if (tasks.isEmpty()) return;
            for (var en : by.entrySet()) {
                if (en.getKey().equals(counted) || now - en.getValue() > MEMORY) continue;
                ServerPlayer p = victim.getServer() == null ? null : victim.getServer().getPlayerList().getPlayer(en.getKey());
                if (p == null) continue;
                ServerQuestFile.INSTANCE.getTeamData(p).ifPresent(data -> {
                    for (KillTask t : tasks) t.kill(data, victim);
                });
                WildsCore.LOG.info("Wilds Core: the kill of {} counts for {} too (hurt it, did not deal the last blow)", victim.getType().toShortString(), p.getGameProfile().getName());
            }
        } catch (RuntimeException ex) {
            WildsCore.LOG.warn("Wilds Core: could not count a kill of {} for the quests", victim.getType(), ex);
        }
    }
}
