package ua.wilds.core.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.wilds.core.WildsCore;

import ua.wilds.core.net.Net;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Where wandering quest givers come from and what happens to them:
 * <ul>
 * <li>the first time a player stands inside a structure (above ground or below), there is a chance a quest giver is
 * there: one whose story fits the place (sea stories in sea structures, the Nether's in the Nether...) and who is not
 * already somewhere else in the world, standing on a safe spot (solid floor, light, no lava, no monsters around);</li>
 * <li>no monster spawns by itself within 20 blocks of a quest giver, so they live long enough to be found;</li>
 * <li>when one dies, everyone who took their quest fails it (it leaves their quest book).</li>
 * <li>a player who has never met a quest giver meets one in the first village they walk into, so the journey's first
 * lesson (the start quest «Поговоріть з мандрівником») does not depend on luck;</li>
 * <li>every player sees their own sign above each giver near them («!» a job to take, «?» come for the reward, grey
 * «?» a job in progress, nothing when they did every job of this giver), sent from here.</li>
 * </ul>
 * The structures already looked at and the quest givers alive are kept in the world save.
 */
public final class NpcWorld extends SavedData {
    private static final String ID = WildsCore.ID + "_npc";
    /** chance that a structure seen for the first time has a quest giver */
    static double CHANCE = 0.3;
    private static final int MAX_ALIVE = 60, PROTECT = 20;

    private final Set<String> seen = new HashSet<>();
    /** npc uuid -> quest key, for every quest giver alive anywhere */
    final Map<UUID, String> alive = new HashMap<>();
    /** loaded quest givers by level, for the spawn protection */
    private static final Map<ServerLevel, Map<UUID, BlockPos>> loaded = new HashMap<>();
    /** the signs last sent to each player, so only changes go over the network */
    private static final Map<UUID, Map<Integer, Integer>> sentMarks = new HashMap<>();
    private static final List<String> VILLAGE_WORDS = List.of("village", "town", "hamlet", "colony");

    public static void init() {
        NeoForge.EVENT_BUS.addListener(NpcWorld::tick);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 3) syncMarks(p, false);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) ->
                sentMarks.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(NpcWorld::protect);
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getEntity() instanceof Wanderer w && e.getLevel() instanceof ServerLevel lvl) {
                loaded.computeIfAbsent(lvl, k -> new HashMap<>()).put(w.getUUID(), w.blockPosition());
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (e.getEntity() instanceof Wanderer w && e.getLevel() instanceof ServerLevel lvl) {
                Map<UUID, BlockPos> m = loaded.get(lvl);
                if (m != null) m.remove(w.getUUID());
            }
        });
    }

    static NpcWorld get(ServerLevel any) {
        return any.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(NpcWorld::new, NpcWorld::load, null), ID);
    }

    // ---------------------------------------------------------------- save

    private static NpcWorld load(CompoundTag tag, HolderLookup.Provider p) {
        NpcWorld w = new NpcWorld();
        ListTag s = tag.getList("seen", Tag.TAG_STRING);
        for (int i = 0; i < s.size(); i++) w.seen.add(s.getString(i));
        CompoundTag a = tag.getCompound("alive");
        for (String k : a.getAllKeys()) w.alive.put(UUID.fromString(k), a.getString(k));
        return w;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        ListTag s = new ListTag();
        for (String k : seen) s.add(StringTag.valueOf(k));
        tag.put("seen", s);
        CompoundTag a = new CompoundTag();
        for (var e : alive.entrySet()) a.putString(e.getKey().toString(), e.getValue());
        tag.put("alive", a);
        return tag;
    }

    // ---------------------------------------------------------------- spawning

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 80 != 41 || p.isSpectator() || NpcQuest.all().isEmpty()) return;
        ServerLevel lvl = p.serverLevel();
        var structures = lvl.structureManager().getAllStructuresAt(p.blockPosition());
        if (structures.isEmpty()) return;
        NpcWorld w = get(lvl);
        for (Structure s : structures.keySet()) {
            StructureStart start = lvl.structureManager().getStructureAt(p.blockPosition(), s);
            if (!start.isValid()) continue;
            ResourceLocation id = lvl.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(s);
            if (id == null) continue;
            String key = lvl.dimension().location() + "|" + id + "|" + start.getChunkPos().toLong();
            if (!w.seen.add(key)) continue;
            w.setDirty();
            BoundingBox bb = start.getBoundingBox();
            boolean firstMeeting = !Dialogue.hasMet(p) && VILLAGE_WORDS.stream().anyMatch(id.getPath()::contains);
            if (bb.getXSpan() * bb.getZSpan() < 120 || w.alive.size() >= MAX_ALIVE || (!firstMeeting && p.getRandom().nextDouble() >= CHANCE)) continue;
            NpcQuest q = choose(w, p, lvl, id.toString());
            if (q == null) continue;
            BlockPos spot = findSpot(lvl, bb, p, p.getRandom());
            if (spot == null) continue;
            Wanderer npc = WildsCore.WANDERER.get().create(lvl);
            if (npc == null) continue;
            npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
            npc.setup(q, spot);
            npc.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null);
            lvl.addFreshEntity(npc);
            w.alive.put(npc.getUUID(), q.persona);
            WildsCore.LOG.info("Wilds Core: quest giver {} ({}) in {} at {}", q.name, q.key, id, spot);
        }
    }

    /** a giver whose story fits the place and who still has a job for this player (their persona's own quest) */
    private static NpcQuest choose(NpcWorld w, ServerPlayer p, ServerLevel lvl, String structure) {
        String dim = lvl.dimension().location().toString();
        long day = lvl.getDayTime() / 24000L;
        Set<String> busy = new HashSet<>(w.alive.values());
        List<NpcQuest> fresh = new ArrayList<>(), any = new ArrayList<>();
        for (String persona : NpcQuest.chains().keySet()) {
            NpcQuest q = NpcQuest.get(persona);
            if (q == null) continue;
            if (q.dims.isEmpty() ? !dim.equals("minecraft:overworld") : !q.dims.contains(dim)) continue;
            if (day < q.minDay) continue;
            if (!q.words.isEmpty() && q.words.stream().noneMatch(structure::contains)) continue;
            if (Dialogue.current(p, persona) == null) continue;
            (busy.contains(persona) ? any : fresh).add(q);
        }
        // a persona already standing somewhere else is used only when nobody else fits, and rarely
        if (!fresh.isEmpty()) return fresh.get(p.getRandom().nextInt(fresh.size()));
        return !any.isEmpty() && p.getRandom().nextFloat() < 0.15f ? any.get(p.getRandom().nextInt(any.size())) : null;
    }

    /** a spot inside the structure where a person can stand and live: floor, two free blocks, light, no lava or monsters */
    private static BlockPos findSpot(ServerLevel lvl, BoundingBox bb, ServerPlayer p, RandomSource rnd) {
        for (int tries = 0; tries < 80; tries++) {
            int x = bb.minX() + 1 + rnd.nextInt(Math.max(1, bb.getXSpan() - 2));
            int z = bb.minZ() + 1 + rnd.nextInt(Math.max(1, bb.getZSpan() - 2));
            if (!lvl.isLoaded(new BlockPos(x, bb.minY(), z))) continue;
            int startY = bb.minY() + rnd.nextInt(Math.max(1, bb.getYSpan()));
            for (int dy = 0; dy < bb.getYSpan(); dy++) {
                int y = bb.minY() + (startY - bb.minY() + dy) % Math.max(1, bb.getYSpan());
                BlockPos pos = new BlockPos(x, y, z);
                if (safe(lvl, pos) && p.blockPosition().distSqr(pos) > 36) return pos;
            }
        }
        return null;
    }

    static boolean safe(ServerLevel lvl, BlockPos feet) {
        BlockState floor = lvl.getBlockState(feet.below());
        if (!floor.isFaceSturdy(lvl, feet.below(), Direction.UP) || floor.is(BlockTags.FIRE) || floor.getBlock().getDescriptionId().contains("magma")) return false;
        for (BlockPos q : new BlockPos[]{feet, feet.above()}) {
            BlockState s = lvl.getBlockState(q);
            if (!s.getCollisionShape(lvl, q).isEmpty() || !s.getFluidState().isEmpty()) return false;
        }
        for (BlockPos q : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 2, 2))) {
            BlockState s = lvl.getBlockState(q);
            if (s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) || s.is(BlockTags.FIRE) || s.getBlock().getDescriptionId().contains("cactus")) return false;
        }
        if (lvl.getMaxLocalRawBrightness(feet) < 7) return false;
        return lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(feet).inflate(12), m -> m instanceof Enemy).isEmpty();
    }

    /** no monster appears by itself near a quest giver */
    private static void protect(MobSpawnEvent.PositionCheck e) {
        if (!(e.getEntity() instanceof Enemy) || !(e.getLevel() instanceof ServerLevel lvl)) return;
        MobSpawnType t = e.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION && t != MobSpawnType.SPAWNER && t != MobSpawnType.PATROL) return;
        Map<UUID, BlockPos> m = loaded.get(lvl.getLevel());
        if (m == null || m.isEmpty()) return;
        BlockPos at = BlockPos.containing(e.getX(), e.getY(), e.getZ());
        for (BlockPos npc : m.values()) {
            if (npc.distSqr(at) < PROTECT * PROTECT) {
                e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
    }

    static void moved(Wanderer w) {
        if (w.level() instanceof ServerLevel lvl) loaded.computeIfAbsent(lvl, k -> new HashMap<>()).put(w.getUUID(), w.blockPosition());
    }

    /** the quest giver died: the job they gave fails for everyone who had it (players offline learn it from their book) */
    static void npcDied(Wanderer w) {
        if (!(w.level() instanceof ServerLevel lvl)) return;
        NpcWorld world = get(lvl);
        world.alive.remove(w.getUUID());
        world.setDirty();
        for (ServerPlayer p : lvl.getServer().getPlayerList().getPlayers()) {
            for (NpcQuest q : NpcQuest.chain(w.questKey())) {
                if (Dialogue.state(p, q.key) == Dialogue.ACCEPTED && w.getUUID().equals(Dialogue.giver(p, q.key))) {
                    Dialogue.fail(p, q);
                    p.sendSystemMessage(Component.translatable("wilds_core.npc.died", q.name, q.line("dead")).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    /** sends the player the signs above the givers around them, when they changed (or always, with force) */
    static void syncMarks(ServerPlayer p, boolean force) {
        Map<Integer, Integer> now = new HashMap<>();
        for (Wanderer w : p.serverLevel().getEntitiesOfClass(Wanderer.class, p.getBoundingBox().inflate(72))) {
            int m = Dialogue.mark(p, w);
            if (m != Dialogue.MARK_NONE) now.put(w.getId(), m);
        }
        if (!force && now.equals(sentMarks.get(p.getUUID()))) return;
        sentMarks.put(p.getUUID(), now);
        try {
            PacketDistributor.sendToPlayer(p, new Net.NpcMarks(now));
        } catch (RuntimeException ignored) {
            // a client without Wilds Core sees no signs
        }
    }
}
