package ua.wilds.core.skills;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;
import ua.wilds.core.WildsCore;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Skill experience for living in the world, besides fights and ores (those are Pufferfish's own sources in the tree's
 * experience.json): travelling on foot, swimming, riding, sailing and flying; discovering biomes, structures and new
 * recipes; first opening of natural chests; breeding, taming, trading; a night's sleep; advancements; bosses.
 * Small amounts each, many ways to earn them. Discoveries are remembered per player.
 */
public final class SkillXp {
    private static final ResourceLocation CATEGORY = ResourceLocation.fromNamespaceAndPath("wilds", "path");
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final String DATA = "wilds_core:xp";
    /** centimetres per point of experience, by kind of travel */
    private static final Map<ResourceLocation, Integer> TRAVEL = Map.of(
            Stats.WALK_ONE_CM, 30000, Stats.SPRINT_ONE_CM, 30000, Stats.SWIM_ONE_CM, 12000, Stats.WALK_UNDER_WATER_ONE_CM, 12000,
            Stats.HORSE_ONE_CM, 40000, Stats.BOAT_ONE_CM, 50000, Stats.AVIATE_ONE_CM, 80000, Stats.CLIMB_ONE_CM, 6000);
    private static boolean on;
    /** logs placed by players since the server started (dimension@pos): breaking them again pays nothing */
    private static final Set<String> placedLogs = new LinkedHashSet<>();
    /** chunks a player must leave behind before travel pays again: circling in one place (a water stream, a loop) does not */
    private static final int RECENT_CHUNKS = 24;

    private SkillXp() {}

    public static void register() {
        on = ModList.get().isLoaded("puffish_skills");
        if (!on) return;
        NeoForge.EVENT_BUS.addListener(SkillXp::tick);
        NeoForge.EVENT_BUS.addListener((BabyEntitySpawnEvent e) -> {
            if (e.getCausedByPlayer() instanceof ServerPlayer p) add(p, 2);
        });
        NeoForge.EVENT_BUS.addListener((AnimalTameEvent e) -> {
            if (e.getTamer() instanceof ServerPlayer p) add(p, 6);
        });
        NeoForge.EVENT_BUS.addListener((TradeWithVillagerEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) add(p, 2);
        });
        NeoForge.EVENT_BUS.addListener((PlayerWakeUpEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !e.wakeImmediately() && !p.level().isNight()) add(p, 3);
        });
        NeoForge.EVENT_BUS.addListener((AdvancementEvent.AdvancementEarnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getAdvancement().value().display().isPresent()) add(p, 10);
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            if (e.getSource().getEntity() instanceof ServerPlayer p && e.getEntity().getType().is(BOSSES)) add(p, 120);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.ItemCraftedEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && first(p, "crafted", e.getCrafting().getItem().builtInRegistryHolder().key().location().toString(), 2000)) add(p, 3);
        });
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (e.getEntity() instanceof Player && e.getPlacedBlock().is(BlockTags.LOGS) && e.getLevel() instanceof net.minecraft.world.level.Level l) {
                placedLogs.add(l.dimension().location() + "@" + e.getPos().asLong());
                if (placedLogs.size() > 50000) placedLogs.remove(placedLogs.iterator().next());
            }
        });
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> {
            if (!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
            var state = e.getState();
            if (state.getBlock() instanceof CropBlock crop) {
                if (crop.isMaxAge(state)) add(p, 1);   // a seedling broken and replanted pays nothing
            } else if (state.is(BlockTags.LOGS)) {
                if (!placedLogs.remove(p.level().dimension().location() + "@" + e.getPos().asLong())) add(p, 1);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            BlockEntity be = p.level().getBlockEntity(e.getPos());
            // once per chest and player: Lootr keeps the loot table on its chests, so the table alone says nothing
            if (be instanceof RandomizableContainerBlockEntity c && c.getLootTable() != null
                    && remember(p, "chests", p.level().dimension().location() + "@" + e.getPos().asLong(), 3000)) add(p, 3);
        });
    }

    /** adds experience to the tree (Pufferfish's Skills turns it into levels and points) */
    public static void add(ServerPlayer p, int amount) {
        if (!on || amount <= 0) return;
        try {
            Puffish.add(p, amount);
        } catch (RuntimeException | LinkageError e) {
            WildsCore.LOG.debug("Wilds Core: cannot add skill experience", e);
        }
    }

    /** the only place that touches Pufferfish's Skills, so this class loads without it */
    private static final class Puffish {
        static void add(ServerPlayer p, int amount) {
            SkillsAPI.getCategory(CATEGORY).flatMap(Category::getExperience).ifPresent(x -> x.addTotal(p, amount));
        }
    }

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!kept.contains(DATA)) kept.put(DATA, new CompoundTag());
        return kept.getCompound(DATA);
    }

    /** true the first time this player meets this thing (remembered; a list holds at most `cap` entries) */
    private static boolean first(Player p, String list, String id, int cap) {
        CompoundTag d = data(p);
        ListTag l = d.getList(list, Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) if (l.getString(i).equals(id)) return false;
        if (l.size() >= cap) return false;
        l.add(StringTag.valueOf(id));
        d.put(list, l);
        return true;
    }

    /** like {@link #first}, but a full list forgets its oldest entry instead of refusing the new one */
    private static boolean remember(Player p, String list, String id, int cap) {
        CompoundTag d = data(p);
        ListTag l = d.getList(list, Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) if (l.getString(i).equals(id)) return false;
        while (l.size() >= cap) l.remove(0);
        l.add(StringTag.valueOf(id));
        d.put(list, l);
        return true;
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 7 || p.isSpectator()) return;
        // travel: the distance since the last check, from the player's own statistics
        CompoundTag d = data(p);
        CompoundTag last = d.getCompound("travel");
        boolean seeded = d.contains("travel");
        int points = 0;
        for (var t : TRAVEL.entrySet()) {
            int now = p.getStats().getValue(Stats.CUSTOM.get(t.getKey()));
            String k = t.getKey().getPath();
            int carry = last.getInt(k + "_rest");
            int before = last.getInt(k);
            if (seeded && now > before) {
                int total = carry + (now - before);
                points += total / t.getValue();
                last.putInt(k + "_rest", total % t.getValue());
            }
            last.putInt(k, now);
        }
        d.put("travel", last);
        // only travel that gets somewhere: the current chunk must not be one of the last few the player stood in
        ChunkPos here = p.chunkPosition();
        ListTag recent = d.getList("recent_chunks", Tag.TAG_LONG);
        boolean fresh = true;
        for (int i = 0; i < recent.size(); i++) {
            if (((net.minecraft.nbt.LongTag) recent.get(i)).getAsLong() == here.toLong()) {
                fresh = false;
                break;
            }
        }
        if (fresh) {
            recent.add(net.minecraft.nbt.LongTag.valueOf(here.toLong()));
            while (recent.size() > RECENT_CHUNKS) recent.remove(0);
            d.put("recent_chunks", recent);
            add(p, points);
        }
        // discoveries: a new biome, a new kind of structure
        p.level().getBiome(p.blockPosition()).unwrapKey().ifPresent(b -> {
            if (first(p, "biomes", b.location().toString(), 400)) add(p, 12);
        });
        if (p.tickCount % 200 == 7) {
            var structures = p.serverLevel().structureManager().getAllStructuresAt(p.blockPosition());
            for (Structure s : structures.keySet()) {
                StructureStart start = p.serverLevel().structureManager().getStructureAt(p.blockPosition(), s);
                if (!start.isValid()) continue;
                ResourceKey<Structure> key = p.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).getResourceKey(s).orElse(null);
                if (key != null && first(p, "structures", key.location().toString(), 800)) add(p, 15);
            }
        }
    }
}
