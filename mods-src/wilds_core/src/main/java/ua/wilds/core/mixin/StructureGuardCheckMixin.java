package ua.wilds.core.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.worldgen.StructureGuard;

/** /locate, maps and compasses ask here whether a structure can stand at a place not generated yet. */
@Mixin(StructureCheck.class)
public abstract class StructureGuardCheckMixin {
    @Shadow @Final private ChunkGenerator chunkGenerator;

    @Inject(method = "canCreateStructure", at = @At("HEAD"), cancellable = true)
    private void wilds$giveWay(ChunkPos pos, Structure structure, CallbackInfoReturnable<Boolean> cir) {
        if (StructureGuard.blocked(chunkGenerator, structure, pos)) cir.setReturnValue(false);
    }
}
