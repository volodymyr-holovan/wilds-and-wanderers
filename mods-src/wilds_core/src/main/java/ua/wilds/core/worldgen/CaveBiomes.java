package ua.wilds.core.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import ua.wilds.core.WildsCore;
import ua.wilds.core.mixin.LithostitchedSourceMixin;

/**
 * Alex's Caves puts its cave biomes in from inside MultiNoiseBiomeSource, and only once it knows which level the source
 * belongs to: it looks for a level whose generator has exactly that source. Lithostitched wraps the overworld's source
 * in its InjectorBiomeSource, so no level ever matches, the dimension stays unknown and all six cave biomes are silently
 * off (they are listed as possible, so /locate searches its whole radius and finds nothing). Here the wrapped source is
 * told its level as soon as the level exists, before any chunk is generated.
 * <p>
 * Tectonic also keeps continentalness under about 0.56, where the biome lists (Terralith's, vanilla's) go up to 1: what
 * lives only far inland can never be chosen. {@link #climate} stretches it, for choosing the biome only: the terrain is
 * made from the noise itself and does not change. Beyond STRETCH_FROM blocks from the spawn, fading in over STRETCH_FADE
 * along an uneven line, so the biomes of the part of the world already explored stay what they were.
 */
public final class CaveBiomes {
    private static final int STRETCH_FROM = 5000, STRETCH_FADE = 700;
    private static final long INLAND = 300;
    private static final double STRETCH = 1.75;
    /** the overworld's own source, and its spawn */
    private static volatile Object overworld;
    private static int spawnX, spawnZ;

    private CaveBiomes() {}

    /** BiomeClimateMixin: the climate for choosing the biome at quart x, z */
    public static Climate.TargetPoint climate(Object source, Climate.TargetPoint t, int qx, int qz) {
        if (source != overworld || t.continentalness() <= INLAND) return t;
        int x = qx << 2, z = qz << 2;
        double dx = x - spawnX, dz = z - spawnZ;
        double r = Math.sqrt(dx * dx + dz * dz) + 220 * Math.sin(x / 613.0 + 1.3) + 170 * Math.sin(z / 487.0 - x / 1311.0);
        double w = (r - STRETCH_FROM) / STRETCH_FADE;
        if (w <= 0) return t;
        if (w < 1) w = w * w * (3 - 2 * w);
        else w = 1;
        long c = t.continentalness() + Math.round((t.continentalness() - INLAND) * (STRETCH - 1) * w);
        return new Climate.TargetPoint(t.temperature(), t.humidity(), c, t.erosion(), t.depth(), t.weirdness());
    }

    public static void register() {
        if (!ModList.get().isLoaded("alexscaves") || !ModList.get().isLoaded("lithostitched")) return;
        NeoForge.EVENT_BUS.addListener((LevelEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel level) link(level);
        });
    }

    private static void link(ServerLevel level) {
        BiomeSource src = level.getChunkSource().getGenerator().getBiomeSource();
        if (!(src instanceof LithostitchedSourceMixin wrapper)) return;
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            spawnX = level.getSharedSpawnPos().getX();
            spawnZ = level.getSharedSpawnPos().getZ();
            overworld = wrapper.wilds$root();
            StructureGuard.link(level);
        }
        if (!(wrapper.wilds$root() instanceof com.github.alexmodguy.alexscaves.server.level.biome.MultiNoiseBiomeSourceAccessor root)) return;
        root.setLastSampledSeed(level.getSeed());
        root.setLastSampledDimension(level.dimension());
        try {   // it would keep looking for its level on every biome asked
            var f = wrapper.wilds$root().getClass().getDeclaredField("ac_resolvedOwningLevel");
            f.setAccessible(true);
            f.setBoolean(wrapper.wilds$root(), true);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            WildsCore.LOG.debug("Wilds Core: Alex's Caves source field not found", ex);
        }
        WildsCore.LOG.info("Wilds Core: Alex's Caves biomes linked to {}", level.dimension().location());
    }
}
