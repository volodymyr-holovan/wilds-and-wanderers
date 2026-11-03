package ua.wilds.core.quests;

import dev.ftb.mods.ftbquests.net.SyncTeamDataMessage;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ua.wilds.core.WildsCore;

/**
 * Quests are personal now, also in a party (FtbQuestsTeamMixin). A player who was already in a party kept their quest
 * progress in the party's data: once, at their first login with personal quests, it is merged into their own, so nothing
 * they did is lost.
 */
public final class PersonalQuests {
    private static final String DONE = "wilds_personal_quests";

    private PersonalQuests() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.getPersistentData().getBoolean(DONE)) return;
            p.server.tell(new TickTask(p.server.getTickCount() + 40, () -> migrate(p)));
        });
    }

    private static void migrate(ServerPlayer p) {
        if (p.hasDisconnected() || ServerQuestFile.INSTANCE == null) return;
        try {
            var teams = FTBTeamsAPI.api().getManager();
            Team party = teams.getTeamForPlayer(p).orElse(null);   // not from FTB Quests: the real team, the party
            Team own = teams.getPlayerTeamForPlayerID(p.getUUID()).orElse(null);
            p.getPersistentData().putBoolean(DONE, true);
            if (party == null || own == null || !party.isPartyTeam()) return;
            TeamData from = ServerQuestFile.INSTANCE.getOrCreateTeamData(party), to = ServerQuestFile.INSTANCE.getOrCreateTeamData(own);
            to.mergeData(from);
            to.mergeClaimedRewards(from);
            to.markDirty();
            dev.ftb.mods.ftblibrary.util.NetworkHelper.sendTo(p, new SyncTeamDataMessage(to));
            WildsCore.LOG.info("Personal quests: {} took their progress from the party {}", p.getScoreboardName(), party.getShortName());
        } catch (RuntimeException | LinkageError ex) {
            WildsCore.LOG.warn("Personal quests: could not move the party progress of {}", p.getScoreboardName(), ex);
        }
    }
}
