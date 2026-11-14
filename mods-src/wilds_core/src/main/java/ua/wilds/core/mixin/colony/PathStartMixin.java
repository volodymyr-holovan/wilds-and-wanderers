package ua.wilds.core.mixin.colony;

import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MineColonies decides whether a citizen stands inside a partial block (stairs, slabs) from its offset within the block,
 * computed as {@code Math.abs(x % 1)}. For x < 0 the remainder is negative, so -563.3 gives 0.3 instead of 0.7: in every
 * negative quarter of the world the check looks at the mirrored half of the block, and a citizen on a stair starts its
 * path inside the stair and finds no way out. The offset within the block is {@code x - floor(x)}.
 */
@Mixin(value = PathfindingUtils.class, remap = false)
public abstract class PathStartMixin {
    @Redirect(method = "prepareStart", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 1), require = 1)
    private static double wilds$fracX(double remainder) {
        return remainder < 0 ? remainder + 1 : remainder;
    }

    @Redirect(method = "prepareStart", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 2), require = 1)
    private static double wilds$fracZ(double remainder) {
        return remainder < 0 ? remainder + 1 : remainder;
    }
}
