package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ua.wilds.core.WildsCore;

/** -Dwilds_core.placetest=x,y,z: 35 s after joining, the player uses the item in hand on the top of that block (tests of things built by placing a block) */
public final class PlaceTest {
    private static final String AT = System.getProperty("wilds_core.placetest");
    private static int t;

    private PlaceTest() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (AT == null || mc.player == null || mc.gameMode == null || ++t != 700) return;
        String[] c = AT.split(",");
        BlockPos pos = new BlockPos(Integer.parseInt(c[0].trim()), Integer.parseInt(c[1].trim()), Integer.parseInt(c[2].trim()));
        var r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
        WildsCore.LOG.info("Wilds Core: place self-test at {} with {}: {}", pos, mc.player.getMainHandItem(), r);
    }
}
