package ua.wilds.core.quests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ua.wilds.core.WildsCore;

import java.util.List;
import java.util.Set;

/**
 * The quest book side of Wilds Core: pins the start quests for a new player (once), completes the custom tutorial tasks
 * when the client reports that the player opened an interface, and opens/closes the quests NPCs hand out.
 * Without FTB Quests every call is a no-op.
 */
public final class Quests {
    /** tutorial events a client may report; anything else is ignored */
    public static final Set<String> EVENTS = Set.of("open_quests", "open_wiki", "open_skills", "open_map", "open_inventory",
            "open_backpack", "open_recipes", "open_curios", "use_waystone", "open_browser", "open_spellbook",
            "open_ars_book", "talk_npc", "pin_quest");
    private static final String ONBOARDED = "wilds_core:onboarded";
    public static boolean ftb;

    private Quests() {}

    public static void init() {
        ftb = ModList.get().isLoaded("ftbquests");
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener((ResourceManagerReloadListener) QuestData::reload));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) onJoin(p);
        });
    }

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** a player's first login: the start quests appear on screen; later logins leave the player's own pins alone */
    private static void onJoin(ServerPlayer p) {
        if (!ftb || QuestData.pins.isEmpty() || data(p).getBoolean(ONBOARDED)) return;
        // FTB Quests creates the team data on login too: pin a moment later, when it is there
        p.server.execute(() -> {
            try {
                for (long id : QuestData.pins) FtbBridge.pin(p, id);
                data(p).putBoolean(ONBOARDED, true);
            } catch (Throwable t) {
                WildsCore.LOG.warn("Wilds Core: cannot pin start quests for {}", p.getGameProfile().getName(), t);
            }
        });
    }

    /** the client reports a tutorial event (opened the wiki, the skill tree...) */
    public static void tutorial(ServerPlayer p, String event) {
        if (!ftb || !EVENTS.contains(event)) return;
        List<Long> tasks = QuestData.tutorial.get(event);
        if (tasks == null) return;
        try {
            for (long t : tasks) FtbBridge.completeTask(p, t);
        } catch (Throwable t) {
            WildsCore.LOG.warn("Wilds Core: tutorial event {} failed", event, t);
        }
    }

    /** an NPC handed out its quest: it appears in the book */
    public static void reveal(ServerPlayer p, String key) {
        long[] g = QuestData.gates.get(key);
        if (!ftb || g == null) return;
        FtbBridge.completeQuest(p, g[1]);
    }

    public static boolean done(ServerPlayer p, String key) {
        long[] g = QuestData.gates.get(key);
        return ftb && g != null && FtbBridge.isComplete(p, g[0]);
    }

    /** the quest is closed without reward (refused, failed, or finished by lying): it leaves the book */
    public static void withdraw(ServerPlayer p, String key) {
        long[] g = QuestData.gates.get(key);
        if (!ftb || g == null) return;
        FtbBridge.reset(p, g[0], g[1]);
    }

    /** the job is accepted as done right away (the player talked the NPC into it) */
    public static void complete(ServerPlayer p, String key) {
        long[] g = QuestData.gates.get(key);
        if (!ftb || g == null) return;
        FtbBridge.completeQuest(p, g[1]);
        FtbBridge.completeQuest(p, g[0]);
    }
}
