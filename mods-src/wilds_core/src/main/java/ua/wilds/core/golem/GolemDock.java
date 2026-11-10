package ua.wilds.core.golem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * The copper golem's docking station: a copper pad with a back panel, like a robot vacuum's. The golem a pumpkin on a
 * copper block makes lives here: with nothing to sort it comes back, stands on the pad and waits (CopperSorter), and
 * the lamp on the panel is lit while it is home ({@link #OCCUPIED}). The block itself keeps nothing: the golem
 * remembers its station. FACING is the open side, where the golem looks.
 */
public final class GolemDock extends HorizontalDirectionalBlock {
    public static final MapCodec<GolemDock> CODEC = simpleCodec(GolemDock::new);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    /** how high the golem stands on the pad */
    public static final double PAD = 2 / 16.0;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        VoxelShape pad = Block.box(0, 0, 0, 16, 2, 16);
        SHAPES.put(Direction.NORTH, Shapes.or(pad, Block.box(0, 2, 13, 16, 15, 16)));
        SHAPES.put(Direction.SOUTH, Shapes.or(pad, Block.box(0, 2, 0, 16, 15, 3)));
        SHAPES.put(Direction.WEST, Shapes.or(pad, Block.box(13, 2, 0, 16, 15, 16)));
        SHAPES.put(Direction.EAST, Shapes.or(pad, Block.box(0, 2, 0, 3, 15, 16)));
    }

    public GolemDock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OCCUPIED, false));
    }

    protected MapCodec<GolemDock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, OCCUPIED);
    }

    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    protected boolean isPathfindable(BlockState s, PathComputationType type) {
        return type == PathComputationType.LAND;
    }

    /** the lamp goes out when the golem that stood here is gone (killed, carried away) */
    protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!s.getValue(OCCUPIED)) return;
        if (golemOn(level, pos)) level.scheduleTick(pos, this, 40);
        else level.setBlock(pos, s.setValue(OCCUPIED, false), 3);
    }

    static boolean golemOn(ServerLevel level, BlockPos pos) {
        for (Entity e : level.getEntities((Entity) null, new AABB(pos).inflate(0.2, 0.6, 0.2), Entity::isAlive)) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("copper_golem")) return true;
        }
        return false;
    }

    static void setOccupied(ServerLevel level, BlockPos pos, boolean on) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof GolemDock dock) || s.getValue(OCCUPIED) == on) return;
        level.setBlock(pos, s.setValue(OCCUPIED, on), 3);
        if (on) level.scheduleTick(pos, dock, 40);
    }
}
