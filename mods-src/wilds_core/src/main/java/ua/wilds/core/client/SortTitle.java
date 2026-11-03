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
 * title when it is open, after the name the player gave it (which the status never changes). The client asks the
 * server about the block under the crosshair when a container screen opens; nothing is shown for an ordinary chest
 * far from any copper golem.
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
        // the block the player just clicked, else the one under the crosshair
        if (clicked != null && mc.level.getGameTime() - clickedAt < 40) asked = clicked;
        else if (mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) asked = bh.getBlockPos();
        else return;
        PacketDistributor.sendToServer(new Net.SortQuery(asked));
    }

    private static BlockPos clicked;
    private static long clickedAt;

    public static void onClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (!e.getLevel().isClientSide) return;
        clicked = e.getPos().immutable();
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
        var g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.drawString(mc.font, "· " + label, x, y, 0xFF9A6A1E, false);
        g.pose().popPose();
    }
}
