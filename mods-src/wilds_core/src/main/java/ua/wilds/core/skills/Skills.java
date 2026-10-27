package ua.wilds.core.skills;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.WildsCore;
import ua.wilds.core.net.Net;
import ua.wilds.core.skills.branch.CombatSkills;
import ua.wilds.core.skills.branch.CraftSkills;
import ua.wilds.core.skills.branch.DiplomatSkills;
import ua.wilds.core.skills.branch.ExplorerSkills;
import ua.wilds.core.skills.branch.HunterSkills;
import ua.wilds.core.skills.branch.MagicSkills;
import ua.wilds.core.skills.branch.SeaSkills;
import ua.wilds.core.skills.branch.TraderSkills;

import java.util.HashSet;
import java.util.Set;

/**
 * The skill tree's own mechanics. The tree (Pufferfish's Skills) decides what a player has unlocked; Wilds Core turns
 * that into abilities and perks (see {@link SkillRewards}) and runs them. Each branch of the tree has its class in
 * skills.branch; all listeners check {@link SkillState} first, so a player without the skill costs nothing.
 */
public final class Skills {
    private static final Set<ServerPlayer> dirty = new HashSet<>();

    private Skills() {}

    public static void init() {
        if (ModList.get().isLoaded("puffish_skills")) {
            SkillRewards.register();
        } else {
            WildsCore.LOG.warn("Wilds Core: Pufferfish's Skills is not installed, the skill tree abilities are off");
        }
        CombatSkills.register();
        HunterSkills.register();
        MagicSkills.register();
        CraftSkills.register();
        SeaSkills.register();
        ExplorerSkills.register();
        DiplomatSkills.register();
        TraderSkills.register();
        SkillXp.register();
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> SkillState.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> flush());
    }

    /** the player's abilities changed: tell their client at the end of the tick (once, however many nodes changed) */
    static void changed(ServerPlayer p) {
        synchronized (dirty) {
            dirty.add(p);
        }
    }

    private static void flush() {
        ServerPlayer[] list;
        synchronized (dirty) {
            if (dirty.isEmpty()) return;
            list = dirty.toArray(new ServerPlayer[0]);
            dirty.clear();
        }
        for (ServerPlayer p : list) {
            if (p.connection == null || p.hasDisconnected()) continue;
            try {
                PacketDistributor.sendToPlayer(p, new Net.SkillSync(new java.util.HashMap<>(SkillState.of(p).abilities)));
            } catch (RuntimeException ignored) {
                // a client without Wilds Core: it simply has no client-side abilities
            }
        }
    }
}
