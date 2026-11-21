package ua.wilds.core.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A tour of the interface for screenshots, for checking the look and overlaps at every GUI scale:
 * -Dwilds_core.uitour=true (optionally =scales:2,4 or =screens:hud,quests). From 30 s after joining, every 2 s the next
 * step: a GUI scale, a screen (the HUD in first and third person, inventory, quest book, skill tree, wiki, world map,
 * chat, pause menu), then a screenshot ui_<screen>_s<scale>.png in screenshots/. The log line "[uitour] done" ends it.
 */
public final class UiTour {
    private static final String PROP = System.getProperty("wilds_core.uitour");
    private static final List<Runnable> STEPS = new ArrayList<>();
    private static int t, step;

    private UiTour() {}

    public static boolean on() {
        return PROP != null;
    }

    private static List<String> list(String key, String def) {
        for (String part : PROP.split(";")) if (part.startsWith(key + ":")) return List.of(part.substring(key.length() + 1).split(","));
        return List.of(def.split(","));
    }

    private static void build() {
        if (PROP.contains("names")) STEPS.add(UiTour::scanNames);
        List<String> scales = list("scales", "1,2,3,4,0");
        List<String> screens = list("screens", "hud,third,inventory,quests,quest,skills,wiki,map,chat,pause");
        for (String s : scales) {
            int scale = Integer.parseInt(s);
            for (String screen : screens) {
                STEPS.add(() -> open(screen, scale));
                STEPS.add(() -> shot(screen, scale));
            }
        }
        STEPS.add(() -> {
            close();
            WildsCore.LOG.info("[uitour] done");
        });
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (PROP == null || mc.player == null || mc.getConnection() == null) return;
        t++;
        if (t == 1) build();
        if (t < 600 || (t - 600) % 40 != 0 || step >= STEPS.size()) return;
        try {
            STEPS.get(step++).run();
        } catch (RuntimeException ex) {
            WildsCore.LOG.warn("[uitour] step {} failed", step, ex);
        }
    }

    private static void close() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.closeContainer();
        mc.setScreen(null);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void open(String screen, int scale) {
        Minecraft mc = Minecraft.getInstance();
        close();
        if (mc.options.guiScale().get() != scale) {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        }
        switch (screen) {
            case "hud" -> {}
            case "third" -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case "inventory" -> mc.setScreen(new InventoryScreen(mc.player));
            case "quests" -> call("dev.ftb.mods.ftbquests.client.ClientQuestFile", "openGui");
            case "quest" -> {   // the book with one quest open: its window over the map
                call("dev.ftb.mods.ftbquests.client.ClientQuestFile", "openGui");
            }
            case "skills" -> call("ua.wilds.core.colony.ColonyClientTest", "skills");
            case "wiki" -> {
                try {
                    for (Method m : Class.forName("ua.wilds.wiki.WildsWiki").getMethods())
                        if (m.getName().equals("open") && m.getParameterCount() == 3) { m.invoke(null, null, null, null); break; }
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    WildsCore.LOG.warn("[uitour] wiki", ex);
                }
            }
            case "map" -> press("gui.xaero_open_map");
            case "chat" -> mc.setScreen(new ChatScreen(""));
            case "pause" -> mc.setScreen(new PauseScreen(true));
            default -> {}
        }
    }

    private static void shot(String screen, int scale) {
        Minecraft mc = Minecraft.getInstance();
        String name = "ui_" + screen + "_s" + scale + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {});
        Screen s = mc.screen;
        WildsCore.LOG.info("[uitour] {} at scale {}: {} ({}x{})", screen, scale, s == null ? "no screen" : s.getClass().getName(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    /**
     * every item and creature name as the player sees it: raw translation keys ("item.mod.thing") and, with Ukrainian
     * chosen, names with no Cyrillic at all (left in English) go to wilds_names.txt in the game folder
     */
    private static void scanNames() {
        Minecraft mc = Minecraft.getInstance();
        List<String> raw = new ArrayList<>(), latin = new ArrayList<>();
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            try {
                classify(id.toString(), new net.minecraft.world.item.ItemStack(item).getHoverName().getString(), item.getDescriptionId(), raw, latin);
            } catch (RuntimeException ignored) {
            }
        }
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            classify("entity " + id, type.getDescription().getString(), type.getDescriptionId(), raw, latin);
        }
        try {
            java.nio.file.Files.write(mc.gameDirectory.toPath().resolve("wilds_names.txt"), java.util.stream.Stream.concat(
                    raw.stream().map(x -> "RAW " + x), latin.stream().map(x -> "EN " + x)).toList());
        } catch (java.io.IOException ex) {
            WildsCore.LOG.warn("[uitour] names", ex);
        }
        WildsCore.LOG.info("[uitour] names: {} raw keys, {} without Cyrillic (language {})", raw.size(), latin.size(), mc.options.languageCode);
    }

    private static void classify(String id, String shown, String key, List<String> raw, List<String> latin) {
        if (shown.equals(key) || shown.matches("[a-z0-9_]+\\.[a-z0-9_.:/]+")) raw.add(id + " = " + shown);
        else if (!shown.matches("(?s).*[\u0400-\u04FF].*") && shown.matches("(?s).*[A-Za-z]{3,}.*")) latin.add(id + " = " + shown);
    }

    private static void call(String cls, String method) {
        try {
            Class.forName(cls).getMethod(method).invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            WildsCore.LOG.warn("[uitour] {}#{}", cls, method, ex);
        }
    }

    /** a key binding by its name (the world map's "open map" of Xaero) */
    private static void press(String word) {
        for (KeyMapping k : Minecraft.getInstance().options.keyMappings) {
            if (k.getName().equals(word)) {
                KeyMapping.click(k.getKey());
                WildsCore.LOG.info("[uitour] pressed {}", k.getName());
                return;
            }
        }
    }
}
