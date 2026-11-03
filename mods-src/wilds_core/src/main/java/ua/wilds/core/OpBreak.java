package ua.wilds.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Operators (permission level 2 and up) can break, in survival, the chests and other containers of structures that
 * mods protect: Lootr's (only while sneaking otherwise), Twilight Forest's locked structures, the Aether's dungeon
 * chests. Such a container digs in two seconds whatever its mod says (OpBreakMixin), and a mod that refuses the
 * breaking itself is overruled. Only containers, only operators: for everyone else nothing changes.
 */
public final class OpBreak {
    /** destroy progress per tick: 2 seconds */
    public static final float SPEED = 1f / 40;

    private OpBreak() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (BlockEvent.BreakEvent e) -> {
            if (e.isCanceled() && applies(e.getPlayer(), e.getLevel(), e.getPos())) e.setCanceled(false);
        });
    }

    public static boolean applies(Player player, BlockGetter level, BlockPos pos) {
        if (player == null || player.isCreative() || player.isSpectator() || !player.hasPermissions(2)) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        if (be instanceof Container) return true;
        String name = be.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("chest") || name.contains("barrel") || name.contains("lootr") || name.contains("container") || name.contains("crate");
    }
}
