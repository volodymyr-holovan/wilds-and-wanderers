package ua.wilds.core.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * An item frame on a chest shows what is inside, and a click meant for the chest used to turn the item in the frame.
 * Now a click on a frame that holds an item, on a block that keeps items or opens a window (chests, barrels, storages
 * of any mod), is a click on that block, as if the frame were not there; with Shift the item turns as before. An empty
 * frame takes the item in hand as always, and a punch takes it out. All on the server: it works for every client.
 */
public final class FrameClick {
    private FrameClick() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FrameClick::onInteract);
    }

    /** the block a click on the frame goes to, or null */
    public static BlockPos through(ItemFrame frame, Player player) {
        if (frame.getItem().isEmpty() || player.isShiftKeyDown()) return null;
        BlockPos behind = frame.getPos().relative(frame.getDirection().getOpposite());
        var level = frame.level();
        BlockState state = level.getBlockState(behind);
        if (state.isAir()) return null;
        if (state.getMenuProvider(level, behind) != null) return behind;
        BlockEntity be = level.getBlockEntity(behind);
        if (be == null) return null;
        if (be instanceof MenuProvider) return behind;
        try {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, behind, state, be, null) != null ? behind : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof ItemFrame frame) || !(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        BlockPos behind = through(frame, p);
        if (behind == null) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        Vec3 at = Vec3.atCenterOf(behind).relative(frame.getDirection(), 0.5);
        p.gameMode.useItemOn(p, level, p.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, new BlockHitResult(at, frame.getDirection(), behind, false));
    }
}
