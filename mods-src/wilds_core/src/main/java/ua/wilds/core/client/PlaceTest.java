package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ua.wilds.core.WildsCore;

/**
 * -Dwilds_core.placetest=x,y,z[;x,y,z...]: from 35 s after joining, every 10 s, the player uses what is in hand on the
 * top of the next of those blocks (places a block, or opens a chest with an empty hand); 3 s after the last, a
 * screenshot wilds_ui_placetest.png (tests of things built by placing blocks, and of what an open screen shows)
 */
public final class PlaceTest {
    private static final String AT = System.getProperty("wilds_core.placetest");
    private static int t;

    private PlaceTest() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (AT == null || mc.player == null || mc.gameMode == null) return;
        String[] all = AT.split(";");
        t++;
        if (t == 700 + (all.length - 1) * 200 + 60) {
            Screenshot.grab(mc.gameDirectory, "wilds_ui_placetest.png", mc.getMainRenderTarget(), m -> {});
            WildsCore.LOG.info("Wilds Core: place self-test screenshot of {}", mc.screen == null ? "no screen" : mc.screen.getClass().getName());
        }
        if (t < 700 || (t - 700) % 200 != 0 || (t - 700) / 200 >= all.length) return;
        String[] c = all[(t - 700) / 200].split(",");
        BlockPos pos = new BlockPos(Integer.parseInt(c[0].trim()), Integer.parseInt(c[1].trim()), Integer.parseInt(c[2].trim()));
        var r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
        WildsCore.LOG.info("Wilds Core: place self-test at {} with {}: {}", pos, mc.player.getMainHandItem(), r);
    }
}
