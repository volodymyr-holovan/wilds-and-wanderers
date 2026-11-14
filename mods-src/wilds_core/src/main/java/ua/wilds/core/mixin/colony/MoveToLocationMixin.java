package ua.wilds.core.mixin.colony;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When the goal cannot be reached, the path ends at the node with the lowest score, and the score was the plain
 * Manhattan distance to the goal: one block down counts the same as one block sideways, and the cost of getting there
 * did not count. A cave right under a fenced field or a walled-in chest is "closer" than the ground beside it, so the
 * best-effort path ended down in the cave. A node lower than both the start and the goal now scores three times the
 * blocks it lies below them, so the path stops on the surface next to the goal.
 */
@Mixin(value = PathJobMoveToLocation.class, remap = false)
public abstract class MoveToLocationMixin extends AbstractPathJob {
    @Shadow @Final protected BlockPos destination;

    private MoveToLocationMixin(Level world, BlockPos start, int range, PathResult result, Mob entity) {
        super(world, start, range, result, entity);
    }

    @Inject(method = "getEndNodeScore", at = @At("RETURN"), cancellable = true, require = 1)
    private void wilds$stayUp(MNode n, CallbackInfoReturnable<Double> cir) {
        if (!(entity instanceof AbstractEntityCitizen)) {
            return;
        }
        final int below = Math.min(start.getY(), destination.getY()) - n.y;
        if (below > 2) {
            cir.setReturnValue(cir.getReturnValueD() + below * 3.0);
        }
    }
}
