package ua.wilds.core.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import ua.wilds.core.WildsCore;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Big surface structures of different mods do not know about each other: a castle grows through a village, two ports
 * share a bay. Vanilla can keep one set away from another (exclusion zones) only inside one mod's data. Here the rule is
 * global and depends only on the seed, so it gives the same answer in any order of generation: when a big structure's
 * place comes within reach of the footprint of a rarer big structure of another set that can stand there (its candidate
 * chunk, one of its biomes on the surface), the commoner one gives way at this place and appears at its other places.
 * Only sets in structure_footprints.json (surface, 40 blocks across or more, measured by /wildsgen) take part; small
 * things, underground and everything already generated are left alone. Also asked by StructureCheck, so /locate, maps
 * and compasses do not lead to a structure that gave way.
 */
public final class StructureGuard {
    private record Entry(String name, RandomSpreadStructurePlacement placement, int radius, int spacing, Set<Holder<Biome>> biomes) {}

    private static final Map<Structure, Entry> BY_STRUCTURE = new HashMap<>();
    private static List<Entry> entries = List.of();
    private static ServerLevel level;
    private static ChunkGenerator generator;
    private static ChunkGeneratorStructureState state;
    private static RandomState random;

    private StructureGuard() {}

    /** the overworld has just been created */
    static void link(ServerLevel overworld) {
        BY_STRUCTURE.clear();
        JsonObject sizes;
        try (InputStream in = StructureGuard.class.getResourceAsStream("/structure_footprints.json")) {
            if (in == null) return;
            sizes = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ex) {
            WildsCore.LOG.warn("Wilds Core: no structure footprints", ex);
            return;
        }
        List<Entry> list = new ArrayList<>();
        var sets = overworld.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (var e : sets.entrySet()) {
            String name = e.getKey().location().toString();
            StructureSet set = e.getValue();
            if (!sizes.has(name) || !(set.placement() instanceof RandomSpreadStructurePlacement p)) continue;
            Set<Holder<Biome>> biomes = new HashSet<>();
            for (var se : set.structures()) se.structure().value().biomes().forEach(biomes::add);
            Entry en = new Entry(name, p, sizes.get(name).getAsInt() / 2, p.spacing(), biomes);
            list.add(en);
            for (var se : set.structures()) BY_STRUCTURE.put(se.structure().value(), en);
        }
        entries = List.copyOf(list);
        level = overworld;
        generator = overworld.getChunkSource().getGenerator();
        state = overworld.getChunkSource().getGeneratorState();
        random = overworld.getChunkSource().randomState();
        WildsCore.LOG.info("Wilds Core: structure guard over {} structure sets", list.size());
    }

    private static boolean outranks(Entry b, Entry a) {
        return b.spacing != a.spacing ? b.spacing > a.spacing : b.name.compareTo(a.name) > 0;
    }

    /** this structure gives way at this chunk */
    public static boolean blocked(ChunkGenerator gen, Structure s, ChunkPos c) {
        if (gen != generator) return false;
        Entry a = BY_STRUCTURE.get(s);
        if (a == null) return false;
        long seed = state.getLevelSeed();
        for (Entry b : entries) {
            if (b == a || !outranks(b, a)) continue;
            int reach = (a.radius + b.radius) * 3 / 5, rc = (reach >> 4) + 1, sp = b.spacing;   // the boxes hold their roads and outskirts: the cores are what must not meet
            for (int gx = Math.floorDiv(c.x - rc, sp); gx <= Math.floorDiv(c.x + rc, sp); gx++) {
                for (int gz = Math.floorDiv(c.z - rc, sp); gz <= Math.floorDiv(c.z + rc, sp); gz++) {
                    ChunkPos q = b.placement.getPotentialStructureChunk(seed, gx * sp, gz * sp);
                    long dx = (q.x - c.x) * 16L, dz = (q.z - c.z) * 16L;
                    if (dx * dx + dz * dz >= (long) reach * reach || !b.placement.isStructureChunk(state, q.x, q.z)) continue;
                    int x = q.getMiddleBlockX(), z = q.getMiddleBlockZ();
                    int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                    if (b.biomes.contains(generator.getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, random.sampler()))) return true;
                }
            }
        }
        return false;
    }
}
