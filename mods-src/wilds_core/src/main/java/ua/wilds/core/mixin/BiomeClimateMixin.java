package ua.wilds.core.mixin;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.wilds.core.worldgen.CaveBiomes;

/** The climate a biome is chosen by, in the overworld far from the spawn: see worldgen.CaveBiomes#climate. */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class BiomeClimateMixin {
    @Redirect(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Climate$Sampler;sample(III)Lnet/minecraft/world/level/biome/Climate$TargetPoint;"))
    private Climate.TargetPoint wilds$climate(Climate.Sampler sampler, int x, int y, int z) {
        return CaveBiomes.climate(this, sampler.sample(x, y, z), x, z);
    }
}
