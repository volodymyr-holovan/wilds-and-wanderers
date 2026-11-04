package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.inventory.ExtraRows;
import ua.wilds.core.net.Net;
import ua.wilds.core.skills.branch.BagSkills;

import java.util.List;

/**
 * The extra inventory rows under the screens of other mods' menus (see inventory.ExtraRows): a panel drawn like the
 * vanilla inventory (light grey, bevelled, every slot with its frame) under the hotbar, with the rows' items from the
 * copy the server sends (RowsSync). It is not part of the menu: a click on it is sent to the server as a RowClick
 * (left, right, shift, the number keys, the off hand key, dropping), which moves the items with the menu's cursor item,
 * and the server's answer updates the panel and the cursor. Input goes through the screen events before the screen sees
 * it, so it works for screens that handle clicks their own way too. The equipment screen has its rows as slots inside
 * its own layout instead (AccessoriesPlayerInvMixin).
 */
public final class RowsPanel {
    private static final int BORDER = 7;
    /** the first row's slots this far below the hotbar's */
    private static final int BELOW_HOTBAR = 32;
    private static final ItemStack[] items = new ItemStack[BagSkills.ROWS * 9];
    /** a press on the panel: its release is the panel's too */
    private static boolean pressed;

    static {
        java.util.Arrays.fill(items, ItemStack.EMPTY);
    }

    private RowsPanel() {}

    /** Net.RowsSync */
    public static void set(List<ItemStack> list) {
        for (int i = 0; i < items.length; i++) items[i] = i < list.size() ? list.get(i) : ItemStack.EMPTY;
    }

    public static void clear() {
        java.util.Arrays.fill(items, ItemStack.EMPTY);
    }

    /** the place of the hotbar's first slot in the menu, or null */
    private static int[] hotbar(AbstractContainerMenu menu, Player player) {
        int x = Integer.MAX_VALUE, y = Integer.MIN_VALUE;
        for (Slot s : menu.slots) {
            if (s.container != player.getInventory() || s.getContainerSlot() < 0 || s.getContainerSlot() > 8 || !s.isActive()) continue;
            x = Math.min(x, s.x);
            y = Math.max(y, s.y);
        }
        return x == Integer.MAX_VALUE || x < -1000 || y < -1000 ? null : new int[]{x, y};
    }

    /** the panel's slots, relative to the screen's corner: {x of the first, y of the first, how many}; or null */
    private static int[] layout(AbstractContainerScreen<?> s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || !mc.getConnection().hasChannel(Net.RowsV1.TYPE)) return null;
        int n = BagSkills.unlocked(mc.player) * 9;
        if (n <= 0 || !ExtraRows.fits(s.getMenu(), mc.player)) return null;
        int[] h = hotbar(s.getMenu(), mc.player);
        return h == null ? null : new int[]{h[0], h[1] + BELOW_HOTBAR, Math.min(n, items.length)};
    }

    /** the panel relative to the screen's corner: x, y, width, height; or null */
    private static int[] panel(AbstractContainerScreen<?> s) {
        int[] l = layout(s);
        if (l == null) return null;
        int rows = (l[2] + 8) / 9;
        return new int[]{l[0] - 1 - BORDER, l[1] - 1 - BORDER, 9 * 18 + 2 * BORDER, rows * 18 + 2 * BORDER};
    }

    /** the row slot under the mouse, or -1 */
    private static int slotAt(AbstractContainerScreen<?> s, double mx, double my) {
        int[] l = layout(s);
        if (l == null) return -1;
        double x = mx - s.getGuiLeft() - l[0] + 1, y = my - s.getGuiTop() - l[1] + 1;
        if (x < 0 || y < 0 || x >= 9 * 18) return -1;
        int i = (int) (y / 18) * 9 + (int) (x / 18);
        return i < l[2] ? i : -1;
    }

    /** the self-test: the middle of row slot i on the screen, or null */
    public static double[] slotCentre(AbstractContainerScreen<?> s, int i) {
        int[] l = layout(s);
        if (l == null || i >= l[2]) return null;
        return new double[]{s.getGuiLeft() + l[0] + (i % 9) * 18 + 8, s.getGuiTop() + l[1] + (i / 9) * 18 + 8};
    }

    public static ItemStack item(int i) {
        return items[i];
    }

    /** how far down the screen reaches with its rows panel, from its top (its own height when it has none) */
    public static int bottom(AbstractContainerScreen<?> s) {
        int[] p = panel(s);
        return p == null ? s.getYSize() : Math.max(s.getYSize(), p[1] + p[3]);
    }

    public static boolean over(AbstractContainerScreen<?> s, double mx, double my) {
        int[] p = panel(s);
        if (p == null) return false;
        double x = mx - s.getGuiLeft(), y = my - s.getGuiTop();
        return x >= p[0] && x < p[0] + p[2] && y >= p[1] && y < p[1] + p[3];
    }

    public static boolean has(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> s && panel(s) != null;
    }

    // ---------------------------------------------------------------- drawing

    /** behind everything the screen draws over its background */
    public static void render(ContainerScreenEvent.Render.Background e) {
        AbstractContainerScreen<?> s = e.getContainerScreen();
        int[] p = panel(s), l = layout(s);
        if (p == null) return;
        GuiGraphics g = e.getGuiGraphics();
        int x = s.getGuiLeft() + p[0], y = s.getGuiTop() + p[1], w = p[2], h = p[3];
        // the vanilla window: black outline with cut corners, white and dark bevel, light grey inside
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        for (int i = 0; i < l[2]; i++) {
            int sx = s.getGuiLeft() + l[0] + (i % 9) * 18 - 1, sy = s.getGuiTop() + l[1] + (i / 9) * 18 - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            g.fill(sx, sy, sx + 17, sy + 1, 0xFF373737);
            g.fill(sx, sy, sx + 1, sy + 17, 0xFF373737);
            g.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
        }
    }

    /** the items, and the highlight under the mouse: with the screen's own slots, under the cursor item */
    public static void renderItems(ContainerScreenEvent.Render.Foreground e) {
        AbstractContainerScreen<?> s = e.getContainerScreen();
        int[] l = layout(s);
        if (l == null) return;
        GuiGraphics g = e.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int hover = slotAt(s, e.getMouseX(), e.getMouseY());
        for (int i = 0; i < l[2]; i++) {
            int x = l[0] + (i % 9) * 18, y = l[1] + (i / 9) * 18;
            ItemStack st = items[i];
            if (!st.isEmpty()) {
                g.renderItem(st, x, y, i);
                g.renderItemDecorations(font, st, x, y);
            }
            if (i == hover) g.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 16, 0x80FFFFFF, 0x80FFFFFF, 0);
        }
    }

    /** the tooltip of the item under the mouse, over everything */
    public static void renderTooltip(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> s) || !s.getMenu().getCarried().isEmpty()) return;
        int i = slotAt(s, e.getMouseX(), e.getMouseY());
        if (i < 0 || items[i].isEmpty()) return;
        e.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, items[i], e.getMouseX(), e.getMouseY());
    }

    // ---------------------------------------------------------------- input

    private static void send(AbstractContainerScreen<?> s, int slot, int button, int mode) {
        PacketDistributor.sendToServer(new Net.RowClick(s.getMenu().containerId, slot, button, mode));
    }

    public static void onPress(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> s) || !over(s, e.getMouseX(), e.getMouseY())) return;
        e.setCanceled(true);   // the screen never sees it: no "click outside" that would drop the cursor item
        pressed = true;
        int i = slotAt(s, e.getMouseX(), e.getMouseY());
        if (i < 0) return;
        Minecraft mc = Minecraft.getInstance();
        int b = e.getButton();
        if (mc.options.keyPickItem.matchesMouse(b)) return;
        for (int k = 0; k < 9; k++) {
            if (mc.options.keyHotbarSlots[k].matchesMouse(b)) {
                send(s, i, k, ExtraRows.SWAP);
                return;
            }
        }
        if (b > 1) return;
        send(s, i, b, Screen.hasShiftDown() && s.getMenu().getCarried().isEmpty() ? ExtraRows.QUICK : ExtraRows.PICKUP);
    }

    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre e) {
        if (!pressed) return;
        pressed = false;
        e.setCanceled(true);
    }

    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> s)) return;
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        int i = slotAt(s, mx, my);
        if (i < 0) return;
        int key = e.getKeyCode(), scan = e.getScanCode();
        for (int k = 0; k < 9; k++) {
            if (mc.options.keyHotbarSlots[k].matches(key, scan)) {
                send(s, i, k, ExtraRows.SWAP);
                e.setCanceled(true);
                return;
            }
        }
        if (mc.options.keySwapOffhand.matches(key, scan)) {
            send(s, i, 40, ExtraRows.SWAP);
            e.setCanceled(true);
        } else if (mc.options.keyDrop.matches(key, scan)) {
            send(s, i, Screen.hasControlDown() ? 1 : 0, ExtraRows.THROW);
            e.setCanceled(true);
        }
    }
}
