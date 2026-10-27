package ua.wilds.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Development helper for the Wilds Wiki, used only on the local test server (never shipped to players).
 * /wildsdump biomes      - biomes.json: every biome of every dimension with its mob spawns, the blocks its features place
 *                          and the blocks found on the ground where it really generates
 * /wildsdump structures  - structures.jsonl: every structure placed into the flat wilds_dump:photo dimension, one per tick,
 *                          with what landed there (blocks, loot tables of chests, spawners, mobs) and where to look at it
 * Output: wilds_dump/ in the server directory.
 */
@Mod(WildsDump.ID)
public final class WildsDump {
    public static final String ID = "wilds_dump";
    static final Logger LOG = LoggerFactory.getLogger("WildsDump");
    static final ResourceKey<Level> PHOTO = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(ID, "photo"));
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    static final Pattern ID_STRING = Pattern.compile("\"([a-z0-9_.-]+:[a-z0-9_/.-]+)\"");
    static final Pattern LOOT = Pattern.compile("LootTable:\"([^\"]+)\"");
    static final Pattern SPAWN_ID = Pattern.compile("SpawnData:\\{.*?id:\"([^\"]+)\"");
    static final int SPACING_CHUNKS = 48;
    /** the flat photo world: bedrock at -64, air, one layer of grass at -41 (low, so underground structures stand in daylight above it) */
    static final int PLANE_Y = -41;

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private int done, total;

    public WildsDump(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private static Path out(MinecraftServer server, String name) throws IOException {
        Path dir = server.getServerDirectory().resolve("wilds_dump");
        Files.createDirectories(dir);
        return dir.resolve(name);
    }

    private void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("wildsdump").requires(s -> s.hasPermission(4))
                .then(Commands.literal("biomes").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    queueBiomes(server);
                    c.getSource().sendSuccess(() -> Component.literal("biomes queued: " + total), false);
                    return 1;
                }))
                .then(Commands.literal("structures").executes(c -> {
                    queueStructures(c.getSource().getServer(), 0);
                    c.getSource().sendSuccess(() -> Component.literal("structures queued: " + total), false);
                    return 1;
                }).then(Commands.argument("from", IntegerArgumentType.integer(0)).executes(c -> {
                    queueStructures(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "from"));
                    c.getSource().sendSuccess(() -> Component.literal("structures queued: " + total), false);
                    return 1;
                })))
                .then(Commands.literal("trades").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    queue.add(() -> dumpTrades(server));
                    total = 1;
                    done = 0;
                    c.getSource().sendSuccess(() -> Component.literal("trades queued"), false);
                    return 1;
                }))
                .then(Commands.literal("status").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal("dump " + done + "/" + total + (queue.isEmpty() ? " idle" : " running")), false);
                    return 1;
                })));
    }

    private void tick(ServerTickEvent.Post e) {
        Runnable r = queue.poll();
        if (r == null) return;
        try {
            r.run();
        } catch (Throwable t) {
            LOG.warn("WildsDump: step failed", t);
        }
        done++;
        if (done % 20 == 0 || queue.isEmpty()) LOG.info("WildsDump: {}/{}", done, total);
    }

    // ================================================================== biomes

    private void queueBiomes(MinecraftServer server) {
        queue.clear();
        done = 0;
        Registry<Biome> reg = server.registryAccess().registryOrThrow(Registries.BIOME);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        Map<ResourceLocation, List<String>> dims = new TreeMap<>();
        Map<ResourceLocation, ServerLevel> home = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().equals(PHOTO)) continue;
            for (Holder<Biome> h : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
                ResourceLocation id = h.unwrapKey().map(ResourceKey::location).orElse(null);
                if (id == null) continue;
                dims.computeIfAbsent(id, k -> new ArrayList<>()).add(level.dimension().location().toString());
                home.putIfAbsent(id, level);
            }
        }
        JsonObject all = new JsonObject();
        Map<String, Set<String>> featureBlocks = new HashMap<>();
        for (var entry : reg.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            Biome b = entry.getValue();
            JsonObject o = new JsonObject();
            JsonArray dl = new JsonArray();
            for (String d : dims.getOrDefault(id, List.of())) dl.add(d);
            o.add("dimensions", dl);
            o.addProperty("temperature", b.getBaseTemperature());
            o.addProperty("precipitation", b.hasPrecipitation());
            JsonArray tags = new JsonArray();
            reg.getHolderOrThrow(entry.getKey()).tags().forEach(t -> tags.add(t.location().toString()));
            o.add("tags", tags);
            JsonObject spawns = new JsonObject();
            MobSpawnSettings ms = b.getMobSettings();
            for (MobCategory c : MobCategory.values()) {
                JsonArray l = new JsonArray();
                for (MobSpawnSettings.SpawnerData s : ms.getMobs(c).unwrap()) {
                    JsonArray one = new JsonArray();
                    one.add(BuiltInRegistries.ENTITY_TYPE.getKey(s.type).toString());
                    one.add(s.getWeight().asInt());
                    one.add(s.minCount);
                    one.add(s.maxCount);
                    l.add(one);
                }
                if (!l.isEmpty()) spawns.add(c.getName(), l);
            }
            o.add("spawns", spawns);
            JsonArray features = new JsonArray();
            Set<String> blocks = new HashSet<>();
            for (var set : b.getGenerationSettings().features()) {
                for (Holder<PlacedFeature> pf : set) {
                    pf.unwrapKey().ifPresent(k -> features.add(k.location().toString()));
                    collectBlocks(server, ops, pf.value(), blocks, new HashSet<>());
                }
            }
            o.add("features", features);
            JsonArray fb = new JsonArray();
            blocks.stream().sorted().forEach(fb::add);
            o.add("feature_blocks", fb);
            all.add(id.toString(), o);
        }
        // an interrupted run resumes: samples already in biomes.json are kept (finding a biome is the slow part)
        try {
            Path prev = out(server, "biomes.json");
            if (Files.exists(prev)) {
                JsonObject old = com.google.gson.JsonParser.parseString(Files.readString(prev)).getAsJsonObject();
                for (var e : old.entrySet()) {
                    JsonObject now = all.getAsJsonObject(e.getKey());
                    if (now != null && e.getValue().getAsJsonObject().has("sample")) now.add("sample", e.getValue().getAsJsonObject().get("sample"));
                }
            }
        } catch (Exception ex) {
            LOG.warn("WildsDump: previous biomes.json unreadable, sampling everything", ex);
        }
        home.keySet().removeIf(id -> all.has(id.toString()) && all.getAsJsonObject(id.toString()).has("sample"));
        total = 1 + home.size();
        for (var h : home.entrySet()) {
            ResourceLocation id = h.getKey();
            ServerLevel level = h.getValue();
            queue.add(() -> {
                JsonObject o = all.getAsJsonObject(id.toString());
                if (o != null) sampleBiome(level, id, o);
                if (done % 10 == 0) write(server, "biomes.json", GSON.toJson(all));
            });
        }
        queue.add(() -> write(server, "biomes.json", GSON.toJson(all)));
    }

    /** Every block a placed feature can put down, following references to other features. */
    private static void collectBlocks(MinecraftServer server, RegistryOps<JsonElement> ops, PlacedFeature pf, Set<String> blocks, Set<Object> seen) {
        if (!seen.add(pf)) return;
        ConfiguredFeature<?, ?> cf = pf.feature().value();
        String json;
        try {
            json = ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, cf).result().map(JsonElement::toString).orElse("");
        } catch (Throwable t) {
            return;
        }
        Registry<PlacedFeature> placed = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Registry<ConfiguredFeature<?, ?>> configured = server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Matcher m = ID_STRING.matcher(json);
        while (m.find()) {
            ResourceLocation rl = ResourceLocation.tryParse(m.group(1));
            if (rl == null) continue;
            PlacedFeature p = placed.get(rl);
            if (p != null) {
                collectBlocks(server, ops, p, blocks, seen);
                continue;
            }
            ConfiguredFeature<?, ?> c = configured.get(rl);
            if (c != null && seen.add(c)) {
                try {
                    String j = ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, c).result().map(JsonElement::toString).orElse("");
                    Matcher m2 = ID_STRING.matcher(j);
                    while (m2.find()) {
                        ResourceLocation r2 = ResourceLocation.tryParse(m2.group(1));
                        if (r2 == null) continue;
                        if (placed.get(r2) != null) collectBlocks(server, ops, placed.get(r2), blocks, seen);
                        else if (BuiltInRegistries.BLOCK.containsKey(r2) && !r2.getPath().endsWith("air")) blocks.add(r2.toString());
                    }
                } catch (Throwable ignored) {
                }
                continue;
            }
            if (BuiltInRegistries.BLOCK.containsKey(rl) && !rl.getPath().endsWith("air")) blocks.add(rl.toString());
        }
    }

    /** Find the biome in the real world and count the blocks on the ground there (and around, for cave biomes). */
    private static void sampleBiome(ServerLevel level, ResourceLocation id, JsonObject o) {
        BlockPos origin = level.dimension() == Level.OVERWORLD ? level.getSharedSpawnPos() : BlockPos.ZERO;
        var found = level.findClosestBiome3d(h -> h.is(id), origin, 4800, 32, 64);
        if (found == null) {
            o.addProperty("sample", "not_found");
            return;
        }
        BlockPos p = found.getFirst();
        Map<String, Integer> counts = new HashMap<>();
        int columns = 0;
        int surfaceY = 0;
        boolean cave = false;
        LevelChunk center = level.getChunk(p.getX() >> 4, p.getZ() >> 4);
        int top = center.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX() & 15, p.getZ() & 15);
        // a cave biome is one that is not on the ground where it was found (the 3D search may find a surface biome deep down too)
        BlockPos ground = new BlockPos(p.getX(), top, p.getZ());
        if (level.dimensionType().hasCeiling() || (p.getY() < top - 12 && !level.getBiome(ground).is(id))) cave = true;
        if (!cave) p = ground;
        for (int dx = -16; dx < 16; dx++) {
            for (int dz = -16; dz < 16; dz++) {
                int x = p.getX() + dx, z = p.getZ() + dz;
                LevelChunk ch = level.getChunk(x >> 4, z >> 4);
                if (cave) {
                    for (int y = p.getY() - 12; y <= p.getY() + 12; y++) {
                        BlockPos bp = new BlockPos(x, y, z);
                        if (!level.getBiome(bp).is(id)) continue;
                        BlockState s = ch.getBlockState(bp);
                        if (!s.isAir()) counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                    }
                    columns++;
                } else {
                    int y = ch.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                    if (!level.getBiome(new BlockPos(x, y, z)).is(id)) continue;
                    columns++;
                    surfaceY += y;
                    for (int yy = y - 5; yy <= y + 1; yy++) {
                        BlockState s = ch.getBlockState(new BlockPos(x, yy, z));
                        if (!s.isAir()) counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                    }
                }
            }
        }
        JsonObject s = new JsonObject();
        s.addProperty("x", p.getX());
        s.addProperty("y", cave ? p.getY() : columns > 0 ? surfaceY / columns : p.getY());
        s.addProperty("z", p.getZ());
        s.addProperty("cave", cave);
        s.addProperty("columns", columns);
        s.addProperty("dimension", level.dimension().location().toString());
        JsonObject c = new JsonObject();
        counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(40).forEach(en -> c.addProperty(en.getKey(), en.getValue()));
        s.add("blocks", c);
        o.add("sample", s);
    }

    // ================================================================== structures

    private void queueStructures(MinecraftServer server, int from) {
        queue.clear();
        done = 0;
        ServerLevel photo = server.getLevel(PHOTO);
        if (photo == null) {
            LOG.error("WildsDump: no {} dimension", PHOTO.location());
            return;
        }
        Registry<Structure> reg = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<ResourceLocation, JsonObject> placement = new HashMap<>();
        for (var se : server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).entrySet()) {
            StructureSet set = se.getValue();
            for (StructureSet.StructureSelectionEntry s : set.structures()) {
                s.structure().unwrapKey().ifPresent(k -> {
                    JsonObject p = new JsonObject();
                    p.addProperty("set", se.getKey().location().toString());
                    if (set.placement() instanceof RandomSpreadStructurePlacement r) {
                        p.addProperty("spacing", r.spacing());
                        p.addProperty("separation", r.separation());
                    }
                    p.addProperty("set_size", set.structures().size());
                    p.addProperty("weight", s.weight());
                    placement.put(k.location(), p);
                });
            }
        }
        List<ResourceLocation> ids = new ArrayList<>(reg.keySet());
        ids.sort(ResourceLocation::compareTo);
        try {
            if (from == 0) Files.deleteIfExists(out(server, "structures.jsonl"));
        } catch (IOException ignored) {
        }
        total = ids.size() - from;
        for (int i = from; i < ids.size(); i++) {
            int index = i;
            ResourceLocation id = ids.get(i);
            queue.add(() -> {
                JsonObject o = placeAndScan(server, photo, reg, id, index);
                if (placement.containsKey(id)) o.add("placement", placement.get(id));
                append(server, "structures.jsonl", GSON.toJson(o));
            });
        }
    }

    private static JsonObject placeAndScan(MinecraftServer server, ServerLevel level, Registry<Structure> reg, ResourceLocation id, int index) {
        Structure s = reg.get(id);
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString());
        o.addProperty("index", index);
        o.addProperty("type", String.valueOf(BuiltInRegistries.STRUCTURE_TYPE.getKey(s.type())));
        o.addProperty("step", s.step().getName());
        JsonArray biomes = new JsonArray();
        for (Holder<Biome> b : s.biomes()) b.unwrapKey().ifPresent(k -> biomes.add(k.location().toString()));
        o.add("biomes", biomes);
        JsonObject overrides = new JsonObject();
        for (var e : s.spawnOverrides().entrySet()) {
            JsonArray l = new JsonArray();
            for (MobSpawnSettings.SpawnerData d : e.getValue().spawns().unwrap()) {
                JsonArray one = new JsonArray();
                one.add(BuiltInRegistries.ENTITY_TYPE.getKey(d.type).toString());
                one.add(d.getWeight().asInt());
                l.add(one);
            }
            overrides.add(e.getKey().getName(), l);
        }
        o.add("spawn_overrides", overrides);

        ChunkGenerator gen = level.getChunkSource().getGenerator();
        ChunkPos cp = new ChunkPos(index * SPACING_CHUNKS, 0);
        StructureStart start;
        try {
            start = s.generate(server.registryAccess(), gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                    level.getStructureManager(), level.getSeed(), cp, 0, level, h -> true);
        } catch (Throwable t) {
            o.addProperty("error", t.toString());
            return o;
        }
        if (!start.isValid()) {
            o.addProperty("error", "no_start");
            return o;
        }
        BoundingBox bb = start.getBoundingBox();
        ChunkPos min = new ChunkPos(bb.minX() >> 4, bb.minZ() >> 4), max = new ChunkPos(bb.maxX() >> 4, bb.maxZ() >> 4);
        if ((max.x - min.x) > 40 || (max.z - min.z) > 40) {
            o.addProperty("error", "too_big " + bb);
            return o;
        }
        for (int x = min.x; x <= max.x; x++) for (int z = min.z; z <= max.z; z++) level.getChunk(x, z);
        try {
            ChunkPos.rangeClosed(min, max).forEach(c -> start.placeInChunk(level, level.structureManager(), gen, level.getRandom(),
                    new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxBuildHeight(), c.getMaxBlockZ()), c));
        } catch (Throwable t) {
            o.addProperty("place_error", t.toString());
        }
        JsonArray box = new JsonArray();
        for (int v : new int[]{bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()}) box.add(v);
        o.add("box", box);
        o.addProperty("pieces", start.getPieces().size());

        Map<String, Integer> blocks = new HashMap<>();
        Map<String, Integer> loot = new TreeMap<>();
        Map<String, Integer> spawners = new TreeMap<>();
        Map<String, Integer> containers = new TreeMap<>();
        List<String> special = new ArrayList<>();
        int minSection = level.getSectionIndex(bb.minY()), maxSection = level.getSectionIndex(bb.maxY());
        int plane = 0;
        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                LevelChunk ch = level.getChunk(x, z);
                LevelChunkSection[] sections = ch.getSections();
                for (int si = Math.max(0, minSection); si <= Math.min(sections.length - 1, maxSection); si++) {
                    sections[si].getStates().count((state, n) -> {
                        if (!state.isAir()) blocks.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), n, Integer::sum);
                    });
                }
                if (level.getSectionIndex(PLANE_Y) >= minSection && level.getSectionIndex(PLANE_Y) <= maxSection) plane += 256;
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    if (!bb.isInside(be.getBlockPos())) continue;
                    String nbt;
                    try {
                        nbt = be.saveWithoutMetadata(server.registryAccess()).toString();
                    } catch (Throwable t) {
                        continue;
                    }
                    String block = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString();
                    Matcher m = LOOT.matcher(nbt);
                    if (m.find()) {
                        loot.merge(m.group(1), 1, Integer::sum);
                        containers.merge(block, 1, Integer::sum);
                    }
                    if (block.matches(".*(respawn|altar|tombstone|summon|trial_spawner|vault).*") && special.size() < 12)
                        special.add(block + " " + (nbt.length() > 400 ? nbt.substring(0, 400) : nbt));
                    Matcher sp = SPAWN_ID.matcher(nbt);
                    if (sp.find()) spawners.merge(sp.group(1), 1, Integer::sum);
                }
            }
        }
        // the flat world's grass is not part of the structure
        int grass = blocks.getOrDefault("minecraft:grass_block", 0) - plane;
        if (grass > 0) blocks.put("minecraft:grass_block", grass);
        else blocks.remove("minecraft:grass_block");
        JsonObject bl = new JsonObject();
        blocks.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(60).forEach(en -> bl.addProperty(en.getKey(), en.getValue()));
        o.add("blocks", bl);
        o.add("loot", GSON.toJsonTree(loot));
        o.add("containers", GSON.toJsonTree(containers));
        o.add("spawners", GSON.toJsonTree(spawners));
        o.add("special", GSON.toJsonTree(special));
        Map<String, Integer> mobs = new TreeMap<>();
        for (Entity e : level.getEntities((Entity) null, AABB.of(bb).inflate(4), e -> !(e instanceof Player))) {
            mobs.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), 1, Integer::sum);
        }
        o.add("entities", GSON.toJsonTree(mobs));
        return o;
    }

    // ================================================================== trades

    /** Every villager profession and the wandering trader: what each level sells and buys, each listing rolled a few times. */
    private static void dumpTrades(MinecraftServer server) {
        ServerLevel level = server.overworld();
        JsonObject all = new JsonObject();
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(1);
        net.minecraft.world.entity.npc.Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        for (var e : net.minecraft.world.entity.npc.VillagerTrades.TRADES.entrySet()) {
            villager.setVillagerData(villager.getVillagerData().setProfession(e.getKey()));
            all.add(BuiltInRegistries.VILLAGER_PROFESSION.getKey(e.getKey()).toString(), levels(e.getValue(), villager, rnd));
        }
        Entity trader = net.minecraft.world.entity.EntityType.WANDERING_TRADER.create(level);
        all.add("wandering_trader", levels(net.minecraft.world.entity.npc.VillagerTrades.WANDERING_TRADER_TRADES, trader, rnd));
        write(server, "trades.json", GSON.toJson(all));
    }

    private static JsonObject levels(it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraft.world.entity.npc.VillagerTrades.ItemListing[]> map, Entity who,
                                     net.minecraft.util.RandomSource rnd) {
        JsonObject lv = new JsonObject();
        for (var le : map.int2ObjectEntrySet()) {
            JsonArray offers = new JsonArray();
            Set<String> seen = new HashSet<>();
            for (var listing : le.getValue()) {
                for (int roll = 0; roll < 6; roll++) {
                    net.minecraft.world.item.trading.MerchantOffer o;
                    try {
                        o = listing.getOffer(who, rnd);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (o == null) continue;
                    String res = BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).toString();
                    String a = BuiltInRegistries.ITEM.getKey(o.getCostA().getItem()).toString();
                    String b = o.getCostB().isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(o.getCostB().getItem()).toString();
                    if (!seen.add(res + a + b)) continue;
                    JsonArray one = new JsonArray();
                    one.add(res);
                    one.add(o.getResult().getCount());
                    one.add(a);
                    one.add(o.getCostA().getCount());
                    one.add(b);
                    one.add(o.getCostB().getCount());
                    offers.add(one);
                }
            }
            lv.add(String.valueOf(le.getIntKey()), offers);
        }
        return lv;
    }

    // ================================================================== files

    private static void write(MinecraftServer server, String name, String text) {
        try (Writer w = Files.newBufferedWriter(out(server, name), StandardCharsets.UTF_8)) {
            w.write(text);
            LOG.info("WildsDump: wrote {}", name);
        } catch (IOException e) {
            LOG.error("WildsDump: cannot write {}", name, e);
        }
    }

    private static void append(MinecraftServer server, String name, String line) {
        try {
            Files.writeString(out(server, name), line + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("WildsDump: cannot write {}", name, e);
        }
    }
}
