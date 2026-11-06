package ua.wilds.core.mixin;

import net.minecraft.world.level.biome.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The source a biome injector wraps (worldgen.CaveBiomes). */
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource", remap = false)
public interface LithostitchedSourceMixin {
    @Accessor(value = "rootDelegate", remap = false)
    BiomeSource wilds$root();
}
