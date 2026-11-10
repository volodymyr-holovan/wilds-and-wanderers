package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.mixin.client.ContainerTitleAccessor;
import ua.wilds.core.net.Net;

/**
 * The copper golem's status of a storage (Звичайна, Джерело, Різне, Не чіпати), on the same line as the storage's
 * title when it is open, after the name the player gave it (which the status never changes). When a click on a block
 * has just opened a container screen, the client asks the server about that block; nothing is shown for an ordinary
 * chest far from any copper golem, nor in screens no block opened (the equipment screen, backpacks).
 */
public final class SortTitle {
    private static BlockPos asked;
    private static String label = "";
    private static AbstractContainerScreen<?> screen;

    private SortTitle() {}

    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> s) || s instanceof InventoryScreen || s == screen) return;
        Minecraft mc = Minecraft.getInstance();
        screen = s;
        label = "";
        asked = null;
        if (mc.getConnection() == null || !mc.getConnection().hasChannel(Net.SortQuery.TYPE) || mc.level == null) return;
        // only a screen a click on a block has just opened (never the equipment screen, a backpack, the quest book...)
        if (clicked == null || mc.level.getGameTime() - clickedAt > 40 || ua.wilds.core.client.gear.GearScreens.isGearScreen(s)) return;
        asked = clicked;
        clicked = null;
        PacketDistributor.sendToServer(new Net.SortQuery(asked));
    }

    private static BlockPos clicked;
    private static long clickedAt;

    public static void onClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (!e.getLevel().isClientSide) return;
        clicked = e.getPos().immutable();
        clickedAt = e.getLevel().getGameTime();
    }

    /** a click on an item frame that goes through to the storage behind it (world.FrameClick) */
    public static void onFrame(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if (!e.getLevel().isClientSide || !(e.getTarget() instanceof net.minecraft.world.entity.decoration.ItemFrame frame)) return;
        BlockPos behind = ua.wilds.core.world.FrameClick.through(frame, e.getEntity());
        if (behind == null) return;
        clicked = behind;
        clickedAt = e.getLevel().getGameTime();
    }

    public static void answer(BlockPos pos, String text) {
        if (pos.equals(asked)) label = text;
    }

    public static void render(ScreenEvent.Render.Post e) {
        if (label.isEmpty() || e.getScreen() != screen) return;
        Minecraft mc = Minecraft.getInstance();
        var acc = (ContainerTitleAccessor) screen;
        int x = screen.getGuiLeft() + acc.wilds$titleX() + mc.font.width(screen.getTitle()) + 6;
        int y = screen.getGuiTop() + acc.wilds$titleY();
        // up to the screen's edge or the first button on the title's line (the sorting and search buttons of
        // Sophisticated Storage), shortened with an ellipsis when it does not fit
        int right = screen.getGuiLeft() + screen.getXSize() - 7;
        // Sophisticated's sort, search and settings buttons on that line do not tell their place
        if (screen.getClass().getName().startsWith("net.p3pp3rf1y.")) right -= 40;
        for (var c : screen.children()) {
            var r = c.getRectangle();
            if (r.width() > 0 && r.left() > x && r.top() < y + 9 && r.bottom() > y - 1) right = Math.min(right, r.left() - 3);
        }
        String text = "· " + label;
        if (mc.font.width(text) > right - x) {
            text = mc.font.plainSubstrByWidth(text, right - x - mc.font.width("…")) + "…";
            if (text.length() <= 3) return;
        }
        var g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.drawString(mc.font, text, x, y, 0xFF9A6A1E, false);
        g.pose().popPose();
    }
}
