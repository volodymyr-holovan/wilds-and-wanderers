package ua.wilds.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.OpBreak;

/** OpBreak: an operator digs a container that its mod made unbreakable (Lootr, Twilight Forest, the Aether) like a chest. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class OpBreakMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void wilds$opBreak(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        float now = cir.getReturnValueF();
        if (now < OpBreak.SPEED && OpBreak.applies(player, level, pos)) cir.setReturnValue(OpBreak.SPEED);
    }
}
