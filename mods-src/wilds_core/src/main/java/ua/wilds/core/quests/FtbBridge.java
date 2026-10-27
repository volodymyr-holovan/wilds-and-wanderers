package ua.wilds.core.quests;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.net.TogglePinnedResponseMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;
import ua.wilds.core.WildsCore;

/**
 * Everything Wilds Core does to FTB Quests. Only touched behind {@link Quests#ftb} (FTB Quests installed),
 * so the mod loads without it.
 */
final class FtbBridge {
    private FtbBridge() {}

    private static TeamData team(ServerPlayer p) {
        ServerQuestFile f = ServerQuestFile.INSTANCE;
        return f == null ? null : f.getOrCreateTeamData(p);
    }

    static void pin(ServerPlayer p, long questId) {
        TeamData d = team(p);
        if (d == null || ServerQuestFile.INSTANCE.getQuest(questId) == null) return;
        if (!d.isQuestPinned(p, questId)) {
            d.setQuestPinned(p, questId, true);
            NetworkManager.sendToPlayer(p, new TogglePinnedResponseMessage(questId, true));
        }
    }

    /** completes a task (custom tutorial tasks, NPC gates); true when it was not complete before */
    static boolean completeTask(ServerPlayer p, long taskId) {
        TeamData d = team(p);
        Task t = d == null ? null : ServerQuestFile.INSTANCE.getTask(taskId);
        if (t == null || d.isCompleted(t)) return false;
        d.setProgress(t, t.getMaxProgress());
        return true;
    }

    /** completes every task of a quest (an NPC gate quest) */
    static void completeQuest(ServerPlayer p, long questId) {
        TeamData d = team(p);
        Quest q = d == null ? null : ServerQuestFile.INSTANCE.getQuest(questId);
        if (q == null) return;
        for (Task t : q.getTasks()) if (!d.isCompleted(t)) d.setProgress(t, t.getMaxProgress());
    }

    static boolean isComplete(ServerPlayer p, long questId) {
        TeamData d = team(p);
        Quest q = d == null ? null : ServerQuestFile.INSTANCE.getQuest(questId);
        return q != null && d.isCompleted(q);
    }

    /** takes a quest and its gate back (an NPC died before the job was done): it disappears from the book again */
    static void reset(ServerPlayer p, long questId, long gateId) {
        TeamData d = team(p);
        if (d == null) return;
        for (long id : new long[]{questId, gateId}) {
            Quest q = ServerQuestFile.INSTANCE.getQuest(id);
            if (q == null) continue;
            try {
                q.forceProgress(d, new dev.ftb.mods.ftbquests.util.ProgressChange(q, p.getUUID()).setReset(true));
            } catch (Throwable e) {
                WildsCore.LOG.debug("Wilds Core: cannot reset quest {}", Long.toHexString(id), e);
            }
        }
    }
}
