package ua.wilds.core.worldgen;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ua.wilds.core.WildsCore;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * /wildsgen RADIUS (operators): asks the overworld's real generator, on background threads and without loading a
 * chunk, which biomes it gives (a 3D grid around the spawn) and which structures really start (every candidate chunk of
 * every structure set within the radius, through Structure.generate as chunk generation does). Writes wildsgen_audit.txt
 * in the server folder: the way to see what the world can and cannot generate.
 */
public final class WorldgenAudit {
    private static volatile boolean running;

    private WorldgenAudit() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(Commands.literal("wildsgen").requires(s -> s.hasPermission(4))
                .then(Commands.argument("radius", IntegerArgumentType.integer(256, 30000)).executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    int r = IntegerArgumentType.getInteger(c, "radius");
                    if (running) return 0;
                    running = true;
                    Thread t = new Thread(() -> {
                        try {
                            for (ServerLevel l : server.getAllLevels()) run(server, l, r);
                        } catch (Throwable ex) {
                            WildsCore.LOG.error("Wilds Core: world generation audit failed", ex);
                        } finally {
                            running = false;
                        }
                    }, "wilds-genaudit");
                    t.setDaemon(true);
                    t.start();
                    return 1;
                }))));
    }

    private static String id(Holder<?> h) {
        return h.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    private static void run(MinecraftServer server, ServerLevel level, int radius0) throws Exception {
        long start = System.currentTimeMillis();
        int radius = level.dimension() == net.minecraft.world.level.Level.OVERWORLD ? radius0 : Math.min(radius0, 4000);
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().randomState();
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        BiomeSource src = gen.getBiomeSource();
        Climate.Sampler sampler = rs.sampler();
        var spawn = level.dimension() == net.minecraft.world.level.Level.OVERWORLD ? level.getSharedSpawnPos() : net.minecraft.core.BlockPos.ZERO;
        Path file = server.getServerDirectory().resolve("wildsgen_" + level.dimension().location().toString().replace(':', '_') + ".txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            out.println("generator " + gen.getClass().getName() + " source " + src.getClass().getName() + " spawn " + spawn + " radius " + radius);
            // ---- biomes
            Set<String> possible = new HashSet<>();
            for (Holder<Biome> b : src.possibleBiomes()) possible.add(id(b));
            var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
            out.println("\n== biomes in the registry, not possible in the overworld");
            for (var e : biomes.entrySet()) if (!possible.contains(e.getKey().location().toString())) out.println(e.getKey().location());
            int[] ys = {-60, -45, -30, -15, 0, 20, 45, 64, 90, 130, 180, 230};
            Map<String, int[]> seen = new TreeMap<>();
            Map<String, String> first = new TreeMap<>();
            int step = 96;
            for (int x = spawn.getX() - radius; x <= spawn.getX() + radius; x += step) {
                for (int z = spawn.getZ() - radius; z <= spawn.getZ() + radius; z += step) {
                    for (int yi = 0; yi < ys.length; yi++) {
                        String b = id(src.getNoiseBiome(x >> 2, ys[yi] >> 2, z >> 2, sampler));
                        seen.computeIfAbsent(b, k -> new int[ys.length])[yi]++;
                        first.putIfAbsent(b, x + " " + ys[yi] + " " + z);
                    }
                }
            }
            out.println("\n== biomes sampled (count at y " + java.util.Arrays.toString(ys) + ", first place)");
            for (var e : seen.entrySet()) out.println(e.getKey() + " " + java.util.Arrays.toString(e.getValue()) + " at " + first.get(e.getKey()));
            // on the surface itself, and the climate there
            Map<String, Integer> surface = new TreeMap<>();
            long[][] range = new long[6][2];
            for (long[] r : range) {
                r[0] = Long.MAX_VALUE;
                r[1] = Long.MIN_VALUE;
            }
            int[] contHist = new int[21], eroHist = new int[21], weirdHist = new int[21];
            int sstep = 128;
            for (int x = spawn.getX() - radius; x <= spawn.getX() + radius; x += sstep) {
                for (int z = spawn.getZ() - radius; z <= spawn.getZ() + radius; z += sstep) {
                    int y = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
                    surface.merge(id(src.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler)), 1, Integer::sum);
                    var t = sampler.sample(x >> 2, y >> 2, z >> 2);
                    long[] v = {t.temperature(), t.humidity(), t.continentalness(), t.erosion(), t.depth(), t.weirdness()};
                    for (int i = 0; i < 6; i++) {
                        range[i][0] = Math.min(range[i][0], v[i]);
                        range[i][1] = Math.max(range[i][1], v[i]);
                    }
                    contHist[(int) Math.max(0, Math.min(20, (t.continentalness() + 10000) / 1000))]++;
                    eroHist[(int) Math.max(0, Math.min(20, (t.erosion() + 10000) / 1000))]++;
                    weirdHist[(int) Math.max(0, Math.min(20, (t.weirdness() + 10000) / 1000))]++;
                }
            }
            out.println("\n== surface biomes (grid " + sstep + ")");
            surface.forEach((k, v) -> out.println(k + " " + v));
            String[] names = {"temperature", "humidity", "continentalness", "erosion", "depth", "weirdness"};
            out.println("\n== climate ranges");
            for (int i = 0; i < 6; i++) out.println(names[i] + " " + range[i][0] / 10000f + " .. " + range[i][1] / 10000f);
            out.println("continentalness by 0.1 from -1: " + java.util.Arrays.toString(contHist));
            out.println("erosion by 0.1 from -1: " + java.util.Arrays.toString(eroHist));
            out.println("weirdness by 0.1 from -1: " + java.util.Arrays.toString(weirdHist));
            out.println("\n== possible but never sampled (with their climate ranges)");
            Map<String, StringBuilder> params = new TreeMap<>();
            if (src instanceof ua.wilds.core.mixin.LithostitchedSourceMixin w && w.wilds$root() instanceof net.minecraft.world.level.biome.MultiNoiseBiomeSource m) {
                var pm = net.minecraft.world.level.biome.MultiNoiseBiomeSource.class.getDeclaredMethod("parameters");
                pm.setAccessible(true);
                @SuppressWarnings("unchecked")
                var plist = (Climate.ParameterList<Holder<Biome>>) pm.invoke(m);
                for (var pair : plist.values()) {
                    var pt = pair.getFirst();
                    params.computeIfAbsent(id(pair.getSecond()), k -> new StringBuilder()).append(" T").append(pt.temperature()).append(" H").append(pt.humidity()).append(" C").append(pt.continentalness())
                            .append(" E").append(pt.erosion()).append(" D").append(pt.depth()).append(" W").append(pt.weirdness()).append(" |");
                }
            }
            for (String p : new java.util.TreeSet<>(possible)) if (!seen.containsKey(p) && !surface.containsKey(p)) {
                StringBuilder sb = params.get(p);
                out.println(p + (sb == null ? " (not in the parameter list)" : " " + sb.substring(0, Math.min(sb.length(), 420))));
            }
            out.flush();
            WildsCore.LOG.info("Wilds Core: audit biomes done in {} s", (System.currentTimeMillis() - start) / 1000);
            // ---- structures
            Set<StructureSet> active = new HashSet<>();
            for (Holder<StructureSet> h : state.possibleStructureSets()) active.add(h.value());
            var sets = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            var structures = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
            out.println("\n== structure sets that can never generate in the overworld (no biome of theirs is possible)");
            for (var e : sets.entrySet()) if (!active.contains(e.getValue())) {
                StringBuilder sb = new StringBuilder(e.getKey().location().toString()).append(':');
                for (var se : e.getValue().structures()) sb.append(' ').append(id(se.structure()));
                out.println(sb);
            }
            out.flush();
            Map<String, int[]> counts = new ConcurrentHashMap<>();   // structure: tries, valid
            Map<String, String> where = new ConcurrentHashMap<>();
            Map<String, String> errors = new ConcurrentHashMap<>();
            Map<String, String> placement = new ConcurrentHashMap<>();
            Map<String, Set<String>> setOf = new ConcurrentHashMap<>();
            List<Object[]> boxes = new ArrayList<>();
            Map<String, Integer> gaveWay = new ConcurrentHashMap<>();
            int cRadius = radius >> 4, cx0 = spawn.getX() >> 4, cz0 = spawn.getZ() >> 4;
            ExecutorService pool = Executors.newFixedThreadPool(4);
            AtomicInteger done = new AtomicInteger();
            List<Holder<StructureSet>> list = new ArrayList<>(state.possibleStructureSets());
            for (Holder<StructureSet> h : list) {
                pool.submit(() -> {
                    try {
                        StructureSet set = h.value();
                        StructurePlacement p = set.placement();
                        String name = id(h);
                        placement.put(name, p.getClass().getSimpleName() + (p instanceof RandomSpreadStructurePlacement r ? " " + r.spacing() + "/" + r.separation() : ""));
                        List<ChunkPos> chunks = new ArrayList<>();
                        if (p instanceof RandomSpreadStructurePlacement r) {
                            int sp = r.spacing();
                            for (int gx = Math.floorDiv(cx0 - cRadius, sp); gx <= Math.floorDiv(cx0 + cRadius, sp); gx++)
                                for (int gz = Math.floorDiv(cz0 - cRadius, sp); gz <= Math.floorDiv(cz0 + cRadius, sp); gz++) {
                                    ChunkPos c = r.getPotentialStructureChunk(state.getLevelSeed(), gx * sp, gz * sp);
                                    if (p.isStructureChunk(state, c.x, c.z)) chunks.add(c);
                                }
                        } else {
                            for (int x = cx0 - cRadius; x <= cx0 + cRadius; x++)
                                for (int z = cz0 - cRadius; z <= cz0 + cRadius; z++)
                                    if (p.isStructureChunk(state, x, z)) chunks.add(new ChunkPos(x, z));
                        }
                        java.util.Collections.shuffle(chunks, new java.util.Random(name.hashCode()));
                        if (chunks.size() > 150000) chunks = new ArrayList<>(chunks.subList(0, 150000));
                        for (ChunkPos c : chunks) {
                            for (StructureSet.StructureSelectionEntry se : set.structures()) {
                                Structure s = se.structure().value();
                                String sn = id(se.structure());
                                boolean inBiome = false;   // a candidate only counts where one of its biomes is
                                for (int y : new int[]{-50, -20, 10, 40, 64, 90, 130}) {
                                    if (s.biomes().contains(src.getNoiseBiome(c.getMiddleBlockX() >> 2, y >> 2, c.getMiddleBlockZ() >> 2, sampler))) {
                                        inBiome = true;
                                        break;
                                    }
                                }
                                if (!inBiome) continue;
                                setOf.computeIfAbsent(sn, k -> ConcurrentHashMap.newKeySet()).add(name);
                                int[] n = counts.computeIfAbsent(sn, k -> new int[2]);
                                synchronized (n) {
                                    if (n[1] >= 14 || n[0] >= 900) continue;   // enough to know that it can or cannot start
                                    n[0]++;
                                }
                                try {
                                    var st = s.generate(server.registryAccess(), gen, src, rs, server.getStructureManager(), state.getLevelSeed(), c, 0, level, s.biomes()::contains);
                                    if (st.isValid() && StructureGuard.blocked(gen, s, c)) {
                                        gaveWay.merge(sn, 1, Integer::sum);
                                    } else if (st.isValid()) {
                                        synchronized (n) {
                                            n[1]++;
                                        }
                                        synchronized (boxes) {
                                            boxes.add(new Object[]{sn, name, st.getBoundingBox()});
                                        }
                                        where.putIfAbsent(sn, st.getBoundingBox().getCenter().toShortString());
                                    }
                                } catch (Throwable ex) {
                                    errors.putIfAbsent(sn, ex.toString());
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        errors.putIfAbsent(id(h), ex.toString());
                    }
                    int d = done.incrementAndGet();
                    if (d % 40 == 0) WildsCore.LOG.info("Wilds Core: audit {} of {} structure sets", d, list.size());
                });
            }
            pool.shutdown();
            pool.awaitTermination(6, TimeUnit.HOURS);
            out.println("\n== structures: tries, starts, sets, placement, one place");
            for (var e : new TreeMap<>(counts).entrySet()) {
                String sets0 = String.join(",", setOf.getOrDefault(e.getKey(), Set.of()));
                String pl = setOf.containsKey(e.getKey()) ? placement.get(setOf.get(e.getKey()).iterator().next()) : "";
                var sv = structures.get(ResourceLocation.parse(e.getKey()));
                int bAll = 0, bSeen = 0;
                StringBuilder missing = new StringBuilder();
                if (sv != null) for (Holder<Biome> b : sv.biomes()) {
                    bAll++;
                    if (seen.containsKey(id(b)) || surface.containsKey(id(b))) bSeen++;
                    else if (missing.length() < 120) missing.append(' ').append(id(b));
                }
                out.println((e.getValue()[1] == 0 ? "ZERO " : "ok   ") + e.getKey() + " tries " + e.getValue()[0] + " starts " + e.getValue()[1] + " biomes " + bSeen + "/" + bAll + " [" + sets0 + "] " + pl + " " + where.getOrDefault(e.getKey(), "") + (e.getValue()[1] == 0 ? " missing:" + missing : ""));
            }
            out.println("\n== structures in no active set");
            for (var e : structures.entrySet()) {
                ResourceLocation k = e.getKey().location();
                if (!counts.containsKey(k.toString())) out.println(k);
            }
            out.println("\n== gave way to a rarer big structure");
            gaveWay.forEach((k, v) -> out.println("GAVE " + v + " " + k + " kept " + counts.get(k)[1]));
            out.println("\n== footprints: structure, step, widest side, lowest, highest");
            Map<String, int[]> foot = new TreeMap<>();
            for (Object[] b : boxes) {
                var box = (net.minecraft.world.level.levelgen.structure.BoundingBox) b[2];
                int[] f = foot.computeIfAbsent((String) b[0], k -> new int[]{0, Integer.MAX_VALUE, Integer.MIN_VALUE});
                f[0] = Math.max(f[0], Math.max(box.getXSpan(), box.getZSpan()));
                f[1] = Math.min(f[1], box.minY());
                f[2] = Math.max(f[2], box.maxY());
            }
            for (var e : foot.entrySet()) {
                var sv = structures.get(ResourceLocation.parse(e.getKey()));
                out.println("FOOT " + e.getKey() + " " + (sv == null ? "?" : sv.step()) + " " + e.getValue()[0] + " " + e.getValue()[1] + " " + e.getValue()[2] + " " + String.join(",", setOf.getOrDefault(e.getKey(), Set.of())));
            }
            out.println("\n== overlaps of the starts sampled (different sets, boxes touching)");
            Map<String, Integer> pairs = new TreeMap<>();
            int total = 0;
            for (int i = 0; i < boxes.size(); i++) {
                var a = (net.minecraft.world.level.levelgen.structure.BoundingBox) boxes.get(i)[2];
                for (int j = i + 1; j < boxes.size(); j++) {
                    var b = (net.minecraft.world.level.levelgen.structure.BoundingBox) boxes.get(j)[2];
                    if (boxes.get(i)[1].equals(boxes.get(j)[1]) || !a.intersects(b)) continue;
                    total++;
                    String x = (String) boxes.get(i)[0], y = (String) boxes.get(j)[0];
                    pairs.merge(x.compareTo(y) < 0 ? x + " + " + y : y + " + " + x, 1, Integer::sum);
                }
            }
            out.println("starts " + boxes.size() + ", overlapping pairs " + total);
            pairs.forEach((k, v) -> out.println(v + " " + k));
            out.println("\n== errors");
            errors.forEach((k, v) -> out.println(k + ": " + v));
        }
        WildsCore.LOG.info("Wilds Core: world generation audit written to {} in {} s", file, (System.currentTimeMillis() - start) / 1000);
    }
}
