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
}
