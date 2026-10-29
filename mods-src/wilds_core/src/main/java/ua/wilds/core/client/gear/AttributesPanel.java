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
 * mod) in a panel under the equipment screen, in as many columns as fit (beside it on a screen too low for that; placed
 * by {@link GearScreens#layout}), opened and closed with its own button. Values that equipment,
 * skills or effects changed are gold and listed first; untouched ones are grey, and only the ones a player looks at
 * (health, armour, damage, speed, mana...) unless «усі» in the header shows every one. It stays open (or closed) across openings of the inventory and
 * across game restarts until the player closes it, like every side window of the pack.
 */
public final class AttributesPanel {
    private static final int W = 168, ROW = 11;
    private static final Path STATE = FMLPaths.CONFIGDIR.get().resolve("wilds_core-client.properties");
    private static Boolean open;
    private static int scroll;
    private static Button button;
    private static Screen lastScreen;

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

    /**
     * the window over the content (a screen with no room for the panel) comes only when the button is pressed in this
     * opening of the inventory: else a player who left the panel open would find the inventory covered every time
     */
    private static boolean windowAsked;

    static boolean windowAllowed() {
        return windowAsked;
    }

    static void toggle() {
        windowAsked = !isOpen();
        open = !isOpen();
        try {
            Files.writeString(STATE, "attributes_open=" + open + "\n");
        } catch (IOException ignored) {
        }
    }

    public static void onInit(ScreenEvent.Init.Post e) {
        button = null;
        if (!(e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) return;
        // a resize re-inits the same screen: only a new opening forgets the window
        if (e.getScreen() != lastScreen) windowAsked = false;
        lastScreen = e.getScreen();
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

    /** for the pack's UI self-test */
    static int[] buttonCenter() {
        return button == null || !button.visible ? null : new int[]{button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
    }

    static void placeButton(int x, int y) {
        if (button == null) return;
        button.visible = x > Integer.MIN_VALUE;
        button.setX(x);
        button.setY(y);
    }

    /** where the panel is drawn this frame, set by {@link GearScreens#layout}: x, y, width, height */
    private static int px, py, pw, ph;

    /** drawn over the content, like a window (a screen with no room beside or under it) */
    private static boolean overlay;

    static void place(int x, int y, int w, int h, boolean over) {
        px = x;
        py = y;
        pw = w;
        ph = h;
        overlay = over;
    }

    static int columnsAt(int w) {
        return columns(w);
    }

    /** the width the panel wants (names and values in full) */
    static int naturalWidth() {
        return W;
    }

    private record Row(String name, double value, double base, boolean percent, String id) {}

    /** every attribute, or (by default) only the ones a player looks at and the ones something changed */
    private static boolean all;
    /** how many the short list leaves out */
    private static int hidden;
    private static final java.util.Set<String> IMPORTANT = java.util.Set.of("max_health", "armor", "armor_toughness",
            "attack_damage", "attack_speed", "movement_speed", "luck", "knockback_resistance", "max_mana", "mana_regen",
            "spell_power", "crit_chance", "crit_damage", "dodge_chance", "life_steal", "armor_pierce", "projectile_damage",
            "draw_speed", "experience_gained", "mining_speed", "block_break_speed");

    private static boolean important(String id) {
        String last = id.substring(Math.max(id.lastIndexOf('.'), id.lastIndexOf(':')) + 1);
        return IMPORTANT.contains(last);
    }

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
        hidden = 0;
        if (!all) {
            int before = out.size();
            out.removeIf(r -> Math.abs(r.value - r.base) < 1e-6 && !important(r.id));
            hidden = before - out.size();
        }
        return out;
    }

    /** the width of one column (a name and its value) */
    static final int COL = 150;

    /** columns the panel has at this width: one under a column and a half, more when there is room */
    private static int columns(int w) {
        return Math.max(1, w / COL);
    }

    /** the height the panel wants at this width to show every attribute without scrolling */
    static int naturalHeight(int w) {
        Minecraft mc = Minecraft.getInstance();
        int n = mc.player == null ? 20 : rows(mc.player).size();
        return 20 + (n + columns(w) - 1) / columns(w) * ROW + 4;
    }

    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!GearScreens.isGearScreen(s) || !isOpen() || pw <= 0 || ph <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        int mx = e.getMouseX(), my = e.getMouseY();
        // a panel narrower than one column gets smaller letters rather than cut names
        float k = pw >= W ? 1f : Math.max(0.55f, (float) pw / W);
        int w = Math.round(pw / k), h = Math.round(ph / k);
        int cols = columns(w), cw = w / cols;
        g.pose().pushPose();
        // over the content when it is a window of its own, else under the item tooltips (drawn at 400, they keep their pixels)
        // a window of its own covers the screen's items too (items are drawn up to ~530); else under the item tooltips
        g.pose().translate(px, py, overlay ? 700 : 200);
        g.pose().scale(k, k, 1);
        g.fill(0, 0, w, h, overlay ? 0xFF101014 : 0xE0101014);
        g.fill(0, 0, w, 1, 0xFFC8A050);
        if (overlay) {
            g.fill(0, h - 1, w, h, 0xFFC8A050);
            g.fill(0, 0, 1, h, 0xFFC8A050);
            g.fill(w - 1, 0, w, h, 0xFFC8A050);
        }
        g.drawString(font, Component.literal("Характеристики").withStyle(ChatFormatting.GOLD), 6, 5, 0xFFFFFF, false);
        List<Row> rows = rows(mc.player);
        int perColumn = Math.max(1, (h - 20 - 2) / ROW), total = (rows.size() + cols - 1) / cols;
        // scrolled by whole lines of the table
        scroll = Math.max(0, Math.min(scroll, total - perColumn));
        // the switch: «усі (N)» / «основні», a link in the header
        String sw = all ? "основні" : "усі (+" + hidden + ")";
        int swX = w - 6 - font.width(sw);
        double lx = (mx - px) / k, ly = (my - py) / k;
        boolean over = lx >= swX - 2 && lx < w - 2 && ly >= 2 && ly < 15;
        if (all || hidden > 0) {
            g.drawString(font, Component.literal(sw).withStyle(over ? ChatFormatting.YELLOW : ChatFormatting.AQUA).withStyle(ChatFormatting.UNDERLINE), swX, 5, 0xFFFFFF, false);
        }
        switchArea = new int[]{px + Math.round((swX - 2) * k), py, Math.round((font.width(sw) + 4) * k) + 2, Math.round(15 * k)};
        if (total > perColumn) {
            String hint = "↕ " + (scroll + 1) + "–" + Math.min(total, scroll + perColumn) + " з " + total;
            g.drawString(font, Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY), swX - 8 - font.width(hint), 5, 0xFFFFFF, false);
        }
        for (int line = 0; line < perColumn; line++) {
            for (int c = 0; c < cols; c++) {
                int i = (scroll + line) * cols + c;
                if (i >= rows.size()) break;
                Row r = rows.get(i);
                int x = c * cw, y = 18 + line * ROW;
                String val = format(r.value, r.percent);
                int colour = Math.abs(r.value - r.base) < 1e-6 ? 0xA8A8A8 : 0xFFC23A;
                int vw = font.width(val);
                String name = font.plainSubstrByWidth(r.name, cw - 16 - vw);
                g.drawString(font, name, x + 6, y, 0xE8E2D4, false);
                g.drawString(font, val, x + cw - 6 - vw, y, colour, false);
            }
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
        scroll -= (int) Math.signum(e.getScrollDeltaY()) * 2;
        e.setCanceled(true);
    }

    private static int[] switchArea;

    /** a click on the panel is the panel's: it must not drop the carried item "outside the screen" */
    static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!GearScreens.isGearScreen(e.getScreen())) return;
        if (!inside(e.getMouseX(), e.getMouseY())) {
            // the window over the content closes with a click elsewhere (that click does nothing else)
            if (overlay && isOpen() && pw > 0 && (button == null || !button.isMouseOver(e.getMouseX(), e.getMouseY()))) {
                toggle();
                e.setCanceled(true);
            }
            return;
        }
        e.setCanceled(true);
        int[] a = switchArea;
        if (a != null && e.getMouseX() >= a[0] && e.getMouseX() < a[0] + a[2] && e.getMouseY() >= a[1] && e.getMouseY() < a[1] + a[3]) {
            all = !all;
            scroll = 0;
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
        }
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
