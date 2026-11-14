package ua.wilds.core.mixin.colony;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.colony.jobs.JobBuilder;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.world.CachingBlockLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Changes to the path search itself (it runs on MineColonies' pathfinding thread).
 * <ul>
 * <li>A planned path could drop a citizen 9 blocks: the search tries drops of up to 10 blocks and only makes them
 * expensive. When nothing cheaper exists (a goal on the far side of a ravine, a best-effort path toward a goal it cannot
 * reach) it took the jump into a cave or a shaft and then could not climb out. Citizens other than builders
 * now drop at most 3 blocks, the height that does no fall damage; builders keep the old limit because unfinished
 * buildings often have no stairs down yet.</li>
 * <li>Walking along the rim of a hole is now a little more expensive, so paths keep a block away from cliffs and cave
 * openings when they can: a citizen that turns a corner at speed, or is pushed by another one, no longer falls in.</li>
 * <li>From MineColonies 1.1.1420: a block in the stairs tag without the stairs' "half" and "facing" properties crashed
 * the search for the whole path.</li>
 * </ul>
 */
@Mixin(value = AbstractPathJob.class, remap = false)
public abstract class PathJobMixin {
    @Shadow protected Mob entity;
    @Shadow protected CachingBlockLookup cachedBlockLookup;

    @Unique private static final int WILDS$SAFE_DROP_LIMIT = 4;
    @Unique private static final double WILDS$RIM_COST = 0.5;

    @ModifyConstant(method = "checkDrop", constant = @Constant(intValue = 10), require = 1)
    private int wilds$dropLimit(int limit) {
        if (entity instanceof AbstractEntityCitizen citizen && citizen.getCitizenData() != null
                && !(citizen.getCitizenData().getJob() instanceof JobBuilder)) {
            return WILDS$SAFE_DROP_LIMIT;
        }
        return limit;
    }

    @Inject(method = "computeCost", at = @At("RETURN"), cancellable = true, require = 1)
    private void wilds$rimCost(MNode parent, int dX, int dY, int dZ, boolean swimming, boolean onPath, boolean diving, boolean onRails,
                               boolean railsExit, boolean swimStart, boolean ladder, BlockState state, BlockState below, int x, int y, int z,
                               CallbackInfoReturnable<Double> cir) {
        if (swimming || ladder || onRails || !(entity instanceof AbstractEntityCitizen)) {
            return;
        }
        int rims = 0;
        if (wilds$hole(x + 1, y, z)) rims++;
        if (wilds$hole(x - 1, y, z)) rims++;
        if (rims < 2 && wilds$hole(x, y, z + 1)) rims++;
        if (rims < 2 && wilds$hole(x, y, z - 1)) rims++;
        if (rims > 0) {
            cir.setReturnValue(cir.getReturnValueD() + rims * WILDS$RIM_COST);
        }
    }

    /** Three blocks of air under the feet level beside the node: stepping there is a fall that hurts. */
    @Unique
    private boolean wilds$hole(int x, int y, int z) {
        return cachedBlockLookup.getBlockState(x, y - 1, z).isAir()
                && cachedBlockLookup.getBlockState(x, y - 2, z).isAir()
                && cachedBlockLookup.getBlockState(x, y - 3, z).isAir();
    }

    @Redirect(method = "handleTargetNotPassable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"), require = 1)
    private boolean wilds$realStairs(BlockState target, TagKey<Block> tag) {
        return target.is(tag) && target.hasProperty(StairBlock.HALF) && target.hasProperty(StairBlock.FACING);
    }
}
