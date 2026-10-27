package ua.wilds.core.client.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * «Характеристики»: the player's attributes (health, armour, damage, speed, mana, spell power, crit, dodge... of every
 * mod) in a panel beside the equipment screen (right of it; placed by {@link GearScreens#layout}), opened and closed with
 * its own button. Values that equipment,
 * skills or effects changed are gold and listed first; untouched ones are grey. It stays open (or closed) across openings of the inventory and
 * across game restarts until the player closes it, like every side window of the pack.
 */
public final class AttributesPanel {
    private static final int W = 168, ROW = 11;
    private static final Path STATE = FMLPaths.CONFIGDIR.get().resolve("wilds_core-client.properties");
    private static Boolean open;
    private static int scroll;
    private static Button button;

    private AttributesPanel() {}

    private static boolean isOpen() {
        if (open == null) {
            try {
                open = Files.exists(STATE) && Files.readString(STATE).contains("attributes_open=true");
            } catch (IOException e) {
                open = false;
            }
        }
        return open;
    }

    static void toggle() {
        open = !isOpen();
        try {
            Files.writeString(STATE, "attributes_open=" + open + "\n");
        } catch (IOException ignored) {
        }
    }

    public static void onInit(ScreenEvent.Init.Post e) {
        button = null;
        if (!GearScreens.isGearScreen(e.getScreen())) return;
        button = Button.builder(Component.literal("⚔"), b -> toggle()).bounds(0, -100, 20, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        "Характеристики: здоров'я, броня, шкода, швидкість, мана, сила заклять... Показати чи сховати.")))
                .build();
        e.addListener(button);
    }

    static boolean shown() {
        return isOpen();
    }

    static void placeButton(int x, int y) {
        if (button == null) return;
        button.visible = x > Integer.MIN_VALUE;
        button.setX(x);
        button.setY(y);
    }

    /** where the panel is drawn this frame, set by {@link GearScreens#layout}: x, y, width, height */
    private static int px, py, pw, ph;

    static void place(int x, int y, int w, int h) {
        px = x;
        py = y;
        pw = w;
        ph = h;
    }

    /** the width the panel wants (names and values in full) */
    static int naturalWidth() {
        return W;
    }

    private record Row(String name, double value, double base, boolean percent, String id) {}

    private static List<Row> rows(Player p) {
        List<Row> out = new ArrayList<>();
        for (AttributeInstance a : p.getAttributes().getSyncableAttributes()) {
            Holder<Attribute> h = a.getAttribute();
            // the player's own base (not the attribute's default, which is for mobs): what equipment and skills changed
            double v = a.getValue(), base = a.getBaseValue();
            var key = BuiltInRegistries.ATTRIBUTE.getKey(h.value());
            String id = key == null ? "" : key.getPath();
            boolean percent = id.contains("chance") || id.contains("crit") || id.contains("reduction") || id.contains("resist")
                    || id.contains("spell_power") || id.contains("experience") || id.contains("efficiency") || id.contains("ratio");
            // the mods' own names are made to stand inside tooltips («+2 захисту»): the panel has its own, in the plain form
            String desc = h.value().getDescriptionId(), last = id.substring(id.lastIndexOf('.') + 1);
            String own = "wilds_core.attr." + (key == null ? "" : key.getNamespace()) + "." + last, any = "wilds_core.attr.any." + last;
            String name = I18n.exists(own) ? I18n.get(own) : I18n.exists(any) ? I18n.get(any)
                    : I18n.exists(desc) ? capital(Component.translatable(desc).getString().replace("%s", "").replace("%", "").trim()) : pretty(id);
            out.add(new Row(name, v, base, percent && Math.abs(v) <= 10, key == null ? id : key.toString()));
        }
        // Iron's Lib keeps its own copies of Apothic's attributes (crit, dodge, lifesteal...): one of each is enough
        java.util.Set<String> apothic = new java.util.HashSet<>();
        for (Row r : out) if (r.id.startsWith("apothic_attributes:")) apothic.add(r.id.substring(r.id.indexOf(':') + 1));
        out.removeIf(r -> r.id.startsWith("irons_lib:") && apothic.contains(r.id.substring(r.id.indexOf(':') + 1)) && Math.abs(r.value - r.base) < 1e-6);
        out.sort(Comparator.comparing((Row r) -> Math.abs(r.value - r.base) < 1e-6).thenComparing(r -> r.name));
        return out;
    }

    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!GearScreens.isGearScreen(s) || !isOpen() || pw <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        // a narrow screen gets smaller letters rather than cut names
        float k = pw >= W ? 1f : Math.max(0.55f, (float) pw / W);
        int w = Math.round(pw / k), h = Math.round(ph / k);
        g.pose().pushPose();
        g.pose().translate(px, py, 400);
        g.pose().scale(k, k, 1);
        g.fill(0, 0, w, h, 0xE0101014);
        g.fill(0, 0, w, 1, 0xFFC8A050);
        g.drawString(font, Component.literal("Характеристики").withStyle(ChatFormatting.GOLD), 6, 5, 0xFFFFFF, false);
        List<Row> rows = rows(mc.player);
        int visible = Math.max(1, (h - 32) / ROW);
        scroll = Math.max(0, Math.min(scroll, rows.size() - visible));
        int ry = 18;
        for (int i = scroll; i < rows.size() && i < scroll + visible; i++) {
            Row r = rows.get(i);
            String val = format(r.value, r.percent);
            int colour = Math.abs(r.value - r.base) < 1e-6 ? 0xA8A8A8 : 0xFFC23A;
            int vw = font.width(val);
            String name = font.plainSubstrByWidth(r.name, w - 16 - vw);
            g.drawString(font, name, 6, ry, 0xE8E2D4, false);
            g.drawString(font, val, w - 6 - vw, ry, colour, false);
            ry += ROW;
        }
        if (rows.size() > visible) {
            g.drawString(font, Component.literal("↕ коліщатко миші").withStyle(ChatFormatting.DARK_GRAY), 6, h - 11, 0xFFFFFF, false);
        }
        g.pose().popPose();
    }

    /** for the pack's UI self-test: every attribute with the name the panel shows */
    static String describe(Player p) {
        StringBuilder sb = new StringBuilder();
        for (Row r : rows(p)) sb.append(r.id).append('=').append(r.name).append("; ");
        return sb.toString();
    }

    static boolean inside(double x, double y) {
        return isOpen() && pw > 0 && x >= px && x < px + pw && y >= py && y < py + ph;
    }

    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (!GearScreens.isGearScreen(e.getScreen()) || !inside(e.getMouseX(), e.getMouseY())) return;
        scroll -= (int) Math.signum(e.getScrollDeltaY()) * 3;
        e.setCanceled(true);
    }

    /** a click on the panel is the panel's: it must not drop the carried item "outside the screen" */
    static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (GearScreens.isGearScreen(e.getScreen()) && inside(e.getMouseX(), e.getMouseY())) e.setCanceled(true);
    }

    private static String capital(String t) {
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** a name for an attribute its mod did not translate: "charge_time" -> "Charge time" */
    private static String pretty(String id) {
        String t = id.substring(id.lastIndexOf('.') + 1).replace('_', ' ');
        return t.isEmpty() ? id : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String format(double v, boolean percent) {
        if (percent) return String.format("%.0f%%", v * 100);
        if (Math.abs(v - Math.rint(v)) < 1e-6) return String.valueOf((long) Math.rint(v));
        return String.format("%.2f", v);
    }
}
