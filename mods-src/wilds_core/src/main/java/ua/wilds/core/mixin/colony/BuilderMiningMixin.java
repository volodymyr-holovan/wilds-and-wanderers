package ua.wilds.core.mixin.colony;

import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The builder clears and breaks blocks twice as fast as well (see BuilderSpeedMixin). */
@Mixin(value = EntityAIStructureBuilder.class, remap = false)
public abstract class BuilderMiningMixin {
    @Inject(method = "getBlockMiningTime", at = @At("RETURN"), cancellable = true, require = 1)
    private void wilds$fasterMining(BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValueI() / 2);
    }
}
