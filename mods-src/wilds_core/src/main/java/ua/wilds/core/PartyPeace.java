package ua.wilds.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;

/**
 * Friends do not hurt each other: players in the same party (Open Parties and Claims, the one that also shares the
 * players' positions on the map) or in the same FTB Teams party (the one that shares quest progress) take no damage
 * from each other - hits, arrows and other projectiles, spells, and their tamed animals. Neither mod has such a switch
 * of its own (OPAC protects players only inside claims). Duels outside a party stay possible as before.
 * Both mods are optional: each is asked only when it is installed.
 */
final class PartyPeace {
    private static final boolean OPAC = ModList.get().isLoaded("openpartiesandclaims");
    private static final boolean FTB_TEAMS = ModList.get().isLoaded("ftbteams");

    private PartyPeace() {}

    static void register() {
        if (OPAC || FTB_TEAMS) NeoForge.EVENT_BUS.addListener(PartyPeace::onDamage);
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim)) return;
        UUID attacker = playerBehind(e.getSource().getEntity());
        if (attacker == null) attacker = playerBehind(e.getSource().getDirectEntity());
        if (attacker == null || attacker.equals(victim.getUUID())) return;
        if (friends(victim, attacker)) e.setCanceled(true);
    }

    /** the player who caused it: the player, a projectile's shooter, a tamed animal's owner */
    private static UUID playerBehind(Entity e) {
        for (int i = 0; e != null && i < 4; i++) {
            if (e instanceof ServerPlayer p) return p.getUUID();
            if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return o.getOwnerUUID();
            e = e instanceof Projectile pr ? pr.getOwner() : null;
        }
        return null;
    }

    private static boolean friends(ServerPlayer victim, UUID other) {
        try {
            if (OPAC) {
                var parties = xaero.pac.common.server.api.OpenPACServerAPI.get(victim.server).getPartyManager();
                var a = parties.getPartyByMember(victim.getUUID());
                var b = parties.getPartyByMember(other);
                if (a != null && b != null && a.getId().equals(b.getId())) return true;
            }
            if (FTB_TEAMS && dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().isManagerLoaded()
                    && dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getManager().arePlayersInSameTeam(victim.getUUID(), other)) return true;
        } catch (RuntimeException | LinkageError ex) {
            WildsCore.LOG.debug("Wilds Core: party check failed", ex);
        }
        return false;
    }
}
