package ua.wilds.core.golem;

import net.neoforged.neoforge.common.NeoForge;

/** registers the copper golem sorter; called only with Friends &amp; Foes installed, whose classes it uses */
public final class Golems {
    private Golems() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CopperSorter::onPlace);
        NeoForge.EVENT_BUS.addListener(CopperSorter::onClick);
        NeoForge.EVENT_BUS.addListener(CopperSorter::onTick);
        NeoForge.EVENT_BUS.addListener(CopperSorter::onTags);
    }

    /** the status line of an open storage (see CopperSorter.label) */
    public static String label(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return CopperSorter.label(level, pos);
    }
}
