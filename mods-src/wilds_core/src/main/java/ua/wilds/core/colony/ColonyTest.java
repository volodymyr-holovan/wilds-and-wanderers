package ua.wilds.core.colony;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.CreativeBuildingStructureHandler;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Test bench for the MineColonies fixes, only with -Dwilds_core.colonytest=true (the local test server). Run as the
 * test player: {@code execute as <player> at <player> run wildscolonytest setup}.
 * <ul>
 * <li>setup: a colony at the player with a town hall and a builder's hut (level 1, Medieval Oak), 4 citizens, one of
 * them hired as the builder</li>
 * <li>upgrade: ask the builder to upgrade his own hut (needs creativeresolve=true on the test server)</li>
 * <li>status: colony, citizens, builder state and placement timing</li>
 * <li>pit &lt;citizen&gt;: shut the citizen in a closed chamber 8 blocks under its feet and order it to the surface</li>
 * <li>drop &lt;citizen&gt;: dig a 6-deep shaft beside the citizen and order it to the bottom</li>
 * </ul>
 */
public final class ColonyTest {
    private static final String PACK = "Medieval Oak";
    private static final List<Walk> WALKS = new ArrayList<>();

    private record Walk(AbstractEntityCitizen citizen, BlockPos target, long until, String name) {}

    private ColonyTest() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ColonyTest::onTick);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(Commands.literal("wildscolonytest")
                .requires(s -> s.hasPermission(4))
                .then(Commands.literal("setup").executes(c -> setup(c.getSource())))
                .then(Commands.literal("upgrade").executes(c -> upgrade(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("supply").executes(c -> supply(c.getSource())))
                .then(Commands.literal("summon").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                    final ICitizenData data = colony(c.getSource()).getCitizenManager().getCivilian(IntegerArgumentType.getInteger(c, "id"));
                    data.setNextRespawnPosition(BlockPos.containing(c.getSource().getPosition()));
                    data.updateEntityIfNecessary();
                    say(c.getSource(), "summoned " + data.getId() + ": " + data.getEntity().map(ent -> ent.blockPosition().toShortString()).orElse("failed"));
                    return 1;
                })))
                .then(Commands.literal("pit").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(c -> pit(c.getSource(), IntegerArgumentType.getInteger(c, "id")))))
                .then(Commands.literal("drop").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(c -> drop(c.getSource(), IntegerArgumentType.getInteger(c, "id")))))));
    }

    private static void say(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), false);
        com.mojang.logging.LogUtils.getLogger().info("[colonytest] {}", text);
    }

    private static int setup(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = src.getPlayerOrException();
        final ServerLevel level = src.getLevel();
        final BlockPos hall = player.blockPosition().offset(0, 0, 12);
        final BlockPos hut = hall.offset(24, 0, 0);
        final IColony colony = IColonyManager.getInstance().createColony(level, hall, player, "Тест", PACK);
        place(player, level, ModBlocks.blockHutTownHall, hall, "fundamentals/townhall1.blueprint");
        place(player, level, ModBlocks.blockHutBuilder, hut, "fundamentals/builder1.blueprint");
        final List<ICitizenData> citizens = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            citizens.add(colony.getCitizenManager().spawnOrCreateCivilian(null, level, List.of(hall.above()), true));
        }
        final IBuilding builder = colony.getServerBuildingManager().getBuilding(hut);
        if (builder != null && !citizens.isEmpty() && citizens.get(0) != null) {
            builder.getFirstModuleOccurance(WorkerBuildingModule.class).assignCitizen(citizens.get(0));
        }
        say(src, "colony " + colony.getID() + " at " + hall.toShortString() + ", builder hut " + hut.toShortString()
                + " registered=" + (builder != null) + ", citizens " + colony.getCitizenManager().getCitizens().size());
        return 1;
    }

    /** What the build tool's creative paste does: let the hut register itself, then place the blueprint at once. */
    private static void place(ServerPlayer player, ServerLevel level, AbstractBlockHut<?> block, BlockPos pos, String path) {
        final Blueprint blueprint = StructurePacks.getBlueprint(PACK, path, level.registryAccess());
        block.setup(player, level, pos, blueprint, RotationMirror.NONE, true, PACK, path);
        // the blueprint lands at center - primaryOffset; its hut block must land on the hut placed above
        BlockPos hutLocal = blueprint.getPrimaryBlockOffset();
        for (int x = 0; x < blueprint.getSizeX(); x++) for (int y = 0; y < blueprint.getSizeY(); y++) for (int z = 0; z < blueprint.getSizeZ(); z++) {
            final BlockPos local = new BlockPos(x, y, z);
            final var state = blueprint.getBlockState(local);
            if (state != null && state.getBlock() instanceof AbstractBlockHut<?>) hutLocal = local;
        }
        final BlockPos center = pos.subtract(hutLocal).offset(blueprint.getPrimaryBlockOffset());
        say(player.createCommandSourceStack(), path + ": hut in blueprint at " + hutLocal.toShortString() + ", primary offset "
                + blueprint.getPrimaryBlockOffset().toShortString() + ", placed from " + center.toShortString());
        CreativeBuildingStructureHandler.loadAndPlaceStructureWithRotation(level, CompletableFuture.completedFuture(blueprint), center,
                RotationMirror.NONE, true, player);
    }

    private static IColony colony(CommandSourceStack src) {
        return IColonyManager.getInstance().getClosestColony(src.getLevel(), BlockPos.containing(src.getPosition()));
    }

    private static int upgrade(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final IColony colony = colony(src);
        for (IBuilding b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b instanceof AbstractBuildingStructureBuilder) {
                b.requestUpgrade(src.getPlayerOrException(), b.getPosition());
                say(src, "upgrade requested for " + b.getPosition().toShortString() + " level " + b.getBuildingLevel());
            }
        }
        return 1;
    }

    /** Iron tools for every builder and the materials his hut still lacks (64 of each), as a courier would bring them. */
    private static int supply(CommandSourceStack src) {
        final IColony colony = colony(src);
        int stacks = 0;
        for (IBuilding b : colony.getServerBuildingManager().getBuildings().values()) {
            if (!(b instanceof AbstractBuildingStructureBuilder sb)) continue;
            for (ICitizenData worker : b.getAllAssignedCitizen()) {
                for (var tool : List.of(net.minecraft.world.item.Items.IRON_PICKAXE, net.minecraft.world.item.Items.IRON_SHOVEL,
                        net.minecraft.world.item.Items.IRON_AXE, net.minecraft.world.item.Items.IRON_HOE)) {
                    com.minecolonies.api.util.InventoryUtils.addItemStackToItemHandler(worker.getInventory(), new net.minecraft.world.item.ItemStack(tool));
                }
            }
            for (var res : sb.getNeededResources().values()) {
                final var stack = res.getItemStack().copy();
                stack.setCount(Math.min(64, Math.max(1, res.getAmount())));
                com.minecolonies.api.util.InventoryUtils.addItemStackToProviderWithResult(b.getTileEntity(), stack);
                stacks++;
            }
        }
        say(src, "supplied tools and " + stacks + " material stacks");
        return 1;
    }

    private static int status(CommandSourceStack src) {
        final IColony colony = colony(src);
        if (colony == null) {
            say(src, "no colony");
            return 0;
        }
        for (ICitizenData data : colony.getCitizenManager().getCitizens()) {
            final String where = data.getEntity().map(e -> e.blockPosition().toShortString() + " state " + (e instanceof com.minecolonies.core.entity.citizen.EntityCitizen ec ? String.valueOf(ec.getCitizenAI().getState()) : "?") + " job-ai " + (data.getJob() != null && data.getJob().getWorkerAI() != null ? String.valueOf(data.getJob().getWorkerAI().getState()) : "-")).orElse("not loaded");
            say(src, "citizen " + data.getId() + " " + data.getName() + " job " + (data.getJob() == null ? "-" : data.getJob().getJobRegistryEntry().getKey())
                    + " at " + where + " hp " + data.getEntity().map(e -> String.valueOf(e.getHealth())).orElse("?"));
        }
        for (IBuilding b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b instanceof AbstractBuildingStructureBuilder sb) {
                for (ICitizenData worker : b.getAllAssignedCitizen()) {
                    for (var req : b.getOpenRequests(worker.getId())) {
                        say(src, "  open request of " + worker.getId() + ": " + req.getShortDisplayString().getString() + " state " + req.getState());
                    }
                    for (int s = 0; s < worker.getInventory().getSlots(); s++) {
                        if (!worker.getInventory().getStackInSlot(s).isEmpty()) say(src, "  inv: " + worker.getInventory().getStackInSlot(s));
                    }
                }
                say(src, "builder hut level " + b.getBuildingLevel() + ", work order " + (sb.getWorkOrder() == null ? "-" : sb.getWorkOrder().getDisplayName().getString())
                        + ", progress " + sb.getProgress());
            }
        }
        say(src, ColonyStats.report());
        return 1;
    }

    private static AbstractEntityCitizen citizen(CommandSourceStack src, int id) {
        final IColony colony = colony(src);
        final ICitizenData data = colony == null ? null : colony.getCitizenManager().getCivilian(id);
        return data == null ? null : data.getEntity().orElse(null);
    }

    private static int pit(CommandSourceStack src, int id) {
        final AbstractEntityCitizen citizen = citizen(src, id);
        if (citizen == null) {
            say(src, "citizen " + id + " not loaded");
            return 0;
        }
        final ServerLevel level = src.getLevel();
        final BlockPos surface = citizen.blockPosition();
        final BlockPos floor = surface.below(9);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = -1; y <= 4; y++) {
            final boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2 || y == -1 || y == 4;
            level.setBlockAndUpdate(floor.offset(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState());
        }
        citizen.teleportTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        WALKS.add(new Walk(citizen, surface, level.getGameTime() + 20 * 60, "pit"));
        say(src, "pit: citizen " + id + " shut in at " + floor.toShortString() + ", ordered to " + surface.toShortString());
        return 1;
    }

    private static int drop(CommandSourceStack src, int id) {
        final AbstractEntityCitizen citizen = citizen(src, id);
        if (citizen == null) {
            say(src, "citizen " + id + " not loaded");
            return 0;
        }
        final ServerLevel level = src.getLevel();
        final BlockPos at = citizen.blockPosition().above(40);  // in the air: the shaft is the only way down, off the edges the ground is 40 blocks below
        // a 9x9 stone floor with room above it, and in it a stone-walled 1x1 shaft: its bottom is 6 blocks down and
        // can be reached only by jumping in
        final BlockPos top = at.offset(4, 0, 0);
        for (int x = -4; x <= 8; x++) for (int z = -4; z <= 4; z++) {
            level.setBlockAndUpdate(at.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y <= 2; y++) level.setBlockAndUpdate(at.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 1; y <= 7; y++) {
            level.setBlockAndUpdate(top.offset(x, -y, z), Blocks.STONE.defaultBlockState());
        }
        for (int y = 1; y <= 6; y++) level.setBlockAndUpdate(top.below(y), Blocks.AIR.defaultBlockState());
        final BlockPos bottom = top.below(6);
        citizen.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        WALKS.add(new Walk(citizen, bottom, level.getGameTime() + 20 * 30, "drop"));
        say(src, "drop: citizen " + id + " at " + at.toShortString() + " ordered into a 6-deep shaft at " + bottom.toShortString());
        return 1;
    }

    private static void onTick(ServerTickEvent.Post e) {
        if (WALKS.isEmpty()) return;
        for (var it = WALKS.iterator(); it.hasNext(); ) {
            final Walk w = it.next();
            final long now = w.citizen.level().getGameTime();
            if (!w.citizen.isAlive() || now > w.until) {
                com.mojang.logging.LogUtils.getLogger().info("[colonytest] {} finished: citizen at {} (target {}), hp {}", w.name,
                        w.citizen.blockPosition().toShortString(), w.target.toShortString(), w.citizen.getHealth());
                it.remove();
                continue;
            }
            if (now % 20 == 0) {
                com.mojang.logging.LogUtils.getLogger().info("[colonytest] {} t={}s citizen at {}", w.name, (w.until - now) / 20, w.citizen.blockPosition().toShortString());
            }
            EntityNavigationUtils.walkToPos(w.citizen, w.target, 2, true);
        }
    }
}
