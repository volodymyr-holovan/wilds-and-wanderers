package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.net.Net;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tells the server when the player opens an interface the start quests teach (quest book, wiki, skills, map...),
 * by the screen's class name, so it works with whatever version of those mods is installed. Each event is sent once
 * per session; the server ignores events nobody waits for.
 */
public final class TutorialTracker {
    /** part of a screen class name -> event */
    private static final Map<String, String> SCREENS = new LinkedHashMap<>();
    private static final Set<String> sent = new HashSet<>();

    static {
        SCREENS.put("ftbquests.client.gui.quests.QuestScreen", "open_quests");
        SCREENS.put("wilds.wiki.ui.WikiScreen", "open_wiki");
        SCREENS.put("puffish.skillsmod", "open_skills");
        SCREENS.put("xaero.map.gui.GuiMap", "open_map");
        SCREENS.put("client.gui.screens.inventory.InventoryScreen", "open_inventory");
        SCREENS.put("sophisticatedbackpacks.client.gui.BackpackScreen", "open_backpack");
        SCREENS.put("mezz.jei.gui.recipes.RecipesGui", "open_recipes");
        SCREENS.put("curios.client.gui.CuriosScreen", "open_curios");
        SCREENS.put("waystones.client.gui.screen", "use_waystone");
        SCREENS.put("pipbrowser.screens.BrowserScreen", "open_browser");
        SCREENS.put("ironsspellbooks.gui.inscription_table", "open_spellbook");
        SCREENS.put("arsnouveau.client.gui.book", "open_spellbook");
        SCREENS.put("arsnouveau.client.gui.documentation", "open_ars_book");
    }

    private TutorialTracker() {}

    public static void onOpen(ScreenEvent.Opening e) {
        Screen s = e.getNewScreen();
        if (s == null || Minecraft.getInstance().getConnection() == null) return;
        String name = s.getClass().getName();
        // FTB Library GUIs (the quest book) open inside a ScreenWrapper: look at the GUI it wraps
        if (name.endsWith("ftblibrary.ui.ScreenWrapper")) {
            try {
                name = s.getClass().getMethod("getGui").invoke(s).getClass().getName();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        // the equipment screen (Accessories) is the inventory and the accessories at once
        if (name.startsWith("io.wispforest.accessories.client.gui.")) {
            send("open_inventory");
            send("open_curios");
            return;
        }
        for (var m : SCREENS.entrySet()) {
            if (name.contains(m.getKey())) {
                send(m.getValue());
                return;
            }
        }
    }

    public static void send(String event) {
        if (Minecraft.getInstance().getConnection() == null || !sent.add(event)) return;
        try {
            PacketDistributor.sendToServer(new Net.Tutorial(event));
        } catch (RuntimeException ignored) {
            // the server has no Wilds Core: nothing waits for tutorial events there
        }
    }

    /** a new world or server: every event may matter again */
    public static void reset() {
        sent.clear();
    }
}
