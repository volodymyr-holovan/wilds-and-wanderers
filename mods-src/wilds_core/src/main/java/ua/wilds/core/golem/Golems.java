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
        // /wildssort theme <pos> | fit <pos> <item>: what the golem thinks a storage is about (operators)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> e.getDispatcher().register(
                net.minecraft.commands.Commands.literal("wildssort").requires(s -> s.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                .executes(c -> {
                                    var pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "pos");
                                    String text = CopperSorter.Survey.explain(c.getSource().getLevel(), pos, null);
                                    c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(text), false);
                                    return 1;
                                })
                                .then(net.minecraft.commands.Commands.argument("item", net.minecraft.commands.arguments.item.ItemArgument.item(e.getBuildContext()))
                                        .executes(c -> {
                                            var pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "pos");
                                            var item = net.minecraft.commands.arguments.item.ItemArgument.getItem(c, "item").getItem();
                                            String text = CopperSorter.Survey.explain(c.getSource().getLevel(), pos, item);
                                            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(text), false);
                                            return 1;
                                        })))));
    }

    /** the status line of an open storage (see CopperSorter.label) */
    public static String label(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return CopperSorter.label(level, pos);
    }
}
