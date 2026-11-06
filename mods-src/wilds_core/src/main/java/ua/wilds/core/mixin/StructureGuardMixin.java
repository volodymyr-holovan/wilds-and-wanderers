package ua.wilds.core.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.worldgen.StructureGuard;

/** See worldgen.StructureGuard. */
@Mixin(ChunkGenerator.class)
public abstract class StructureGuardMixin {
    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void wilds$giveWay(StructureSet.StructureSelectionEntry entry, StructureManager manager, RegistryAccess access, RandomState random,
                               StructureTemplateManager templates, long seed, ChunkAccess chunk, ChunkPos pos, SectionPos section, CallbackInfoReturnable<Boolean> cir) {
        if (StructureGuard.blocked((ChunkGenerator) (Object) this, entry.structure().value(), pos)) cir.setReturnValue(false);
    }
}
