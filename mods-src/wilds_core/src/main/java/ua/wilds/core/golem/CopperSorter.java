package ua.wilds.core.golem;

import com.faboslav.friendsandfoes.common.entity.CopperGolemEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import ua.wilds.core.WildsCore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The copper golem as a sorter, on Friends &amp; Foes' golem (its model, animations and oxidation stay).
 * <p>
 * <b>Building</b>: a carved pumpkin (or a jack o'lantern) put right on a copper block, any of the eight. The block
 * becomes a copper chest (Sophisticated Storage's, a vanilla chest without the mod) and the golem stands on it,
 * as rusty and as waxed as the block was. F&amp;F's own way, with a lightning rod between, still works too.
 * <p>
 * <b>Work</b>: the golem takes a whole stack out of a copper chest near its home and carries it to the storage where
 * that item already is: the one with the most of it that still has room. It knows what every storage nearby holds
 * without opening it (it is the server that looks), so it walks straight there. Copper chests are taken from in turn.
 * What fits nowhere goes to a chest named «Різне» (an anvil names it); with no such chest it stays where it was.
 * <p>
 * <b>Modes</b>, set on each copper chest by clicking it with an empty hand while sneaking:
 * <ul>
 * <li>exact (the default, as in vanilla): only to a storage that has the very same item;</li>
 * <li>by likeness: to a storage with the same item first, else with the most alike ({@link Likeness}): spruce logs go
 * to the logs, leggings to the armour, a modded gem to the diamonds and emeralds.</li>
 * </ul>
 * Every storage counts whatever its size (iron, gold and diamond chests, barrels, drawers...), but not furnaces,
 * machines and other blocks that merely hold items, not the copper chests themselves, not the claims of another
 * party, and not Lootr's chests. Everything runs on the server: nothing to install for the players.
 */
public final class CopperSorter {
    private static final ResourceLocation GOLEM = ResourceLocation.fromNamespaceAndPath("friendsandfoes", "copper_golem");
    private static final ResourceLocation CHEST = ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "copper_chest");
    private static final ResourceLocation BUTTON_COOLDOWN = ResourceLocation.fromNamespaceAndPath("friendsandfoes", "copper_golem_press_button_cooldown");
    static final int RANGE = 20, HEIGHT = 8;
    private static final Set<String> MISC_NAMES = Set.of("різне", "misc", "miscellaneous", "разное", "rizne");
    private static final String HOME = "wilds_sorter_home", MODE = "wilds_sort_mode";

    /** what each golem is doing; lost on a restart, when the golem simply starts over with what it holds */
    private static final Map<UUID, Job> jobs = new HashMap<>();

    private static final class Job {
        BlockPos source, target;
        int steps, rest, turn;
        final Map<BlockPos, Long> avoid = new HashMap<>();   // storages it could not reach or that were full, until then
    }

    private CopperSorter() {}

    // ---------------------------------------------------------------- building

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        BlockState head = e.getPlacedBlock();
        if (!head.is(Blocks.CARVED_PUMPKIN) && !head.is(Blocks.JACK_O_LANTERN)) return;
        BlockPos pos = e.getPos().immutable();
        if (copper(level.getBlockState(pos.below())) == null) return;
        Direction facing = head.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? head.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        // the next tick: the placing is not over yet while its event runs
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> build(level, pos, facing)));
    }

    private static void build(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState head = level.getBlockState(pos), body = level.getBlockState(pos.below());
        Copper copper = copper(body);
        if (copper == null || (!head.is(Blocks.CARVED_PUMPKIN) && !head.is(Blocks.JACK_O_LANTERN))) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(GOLEM).orElse(null);
        if (type == null) return;
        Entity entity = type.create(level);
        if (!(entity instanceof CopperGolemEntity golem)) return;
        level.levelEvent(2001, pos, Block.getId(head));
        level.levelEvent(2001, pos.below(), Block.getId(body));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        Block chest = BuiltInRegistries.BLOCK.getOptional(CHEST).orElse(Blocks.CHEST);
        BlockState chestState = chest.defaultBlockState();
        if (chestState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) chestState = chestState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        level.setBlock(pos.below(), chestState, 3);
        golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.toYRot(), 0);
        golem.setSpawnYaw(facing.toYRot());
        golem.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        golem.setOxidationLevel(copper.state);
        golem.setPrevOxidationLevel(copper.state);
        golem.setIsWaxed(copper.waxed);
        golem.getPersistentData().put(HOME, NbtUtils.writeBlockPos(pos.below()));
        level.addFreshEntity(golem);
        level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1f, 1f);
        level.playSound(null, pos, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 0.7f, 1.6f);
    }

    private record Copper(WeatheringCopper.WeatherState state, boolean waxed) {}

    private static Copper copper(BlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.COPPER_BLOCK) return new Copper(WeatheringCopper.WeatherState.UNAFFECTED, false);
        if (b == Blocks.EXPOSED_COPPER) return new Copper(WeatheringCopper.WeatherState.EXPOSED, false);
        if (b == Blocks.WEATHERED_COPPER) return new Copper(WeatheringCopper.WeatherState.WEATHERED, false);
        if (b == Blocks.OXIDIZED_COPPER) return new Copper(WeatheringCopper.WeatherState.OXIDIZED, false);
        if (b == Blocks.WAXED_COPPER_BLOCK) return new Copper(WeatheringCopper.WeatherState.UNAFFECTED, true);
        if (b == Blocks.WAXED_EXPOSED_COPPER) return new Copper(WeatheringCopper.WeatherState.EXPOSED, true);
        if (b == Blocks.WAXED_WEATHERED_COPPER) return new Copper(WeatheringCopper.WeatherState.WEATHERED, true);
        if (b == Blocks.WAXED_OXIDIZED_COPPER) return new Copper(WeatheringCopper.WeatherState.OXIDIZED, true);
        return null;
    }

    // ---------------------------------------------------------------- the mode of a copper chest

    public static void onClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !p.getMainHandItem().isEmpty() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(p.level() instanceof ServerLevel level) || !isSource(level.getBlockState(e.getPos()))) return;
        BlockEntity be = level.getBlockEntity(e.getPos());
        if (be == null) return;
        boolean likeness = !be.getPersistentData().getBoolean(MODE);
        be.getPersistentData().putBoolean(MODE, likeness);
        be.setChanged();
        p.displayClientMessage(Component.literal("Мідний голем сортує з цієї скрині: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(likeness ? "за схожістю" : "лише точно такі самі").withStyle(ChatFormatting.YELLOW)), true);
        level.playSound(null, e.getPos(), SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1f, likeness ? 1.4f : 0.9f);
        e.setCanceled(true);
    }

    private static boolean isSource(BlockState s) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return id.getPath().endsWith("copper_chest");
    }

    public static void onTags(TagsUpdatedEvent e) {
        Likeness.clear();
    }

    // ---------------------------------------------------------------- the work

    public static void onTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof CopperGolemEntity golem) || !(golem.level() instanceof ServerLevel level)) return;
        if ((golem.tickCount + golem.getId()) % 10 != 0) return;
        if (golem.isImmobilized() || golem.isPassenger() || golem.isLeashed() || !golem.isAlive()) return;
        try {
            work(level, golem);
        } catch (RuntimeException ex) {
            WildsCore.LOG.warn("Copper golem sorter at {} failed", golem.blockPosition(), ex);
            jobs.remove(golem.getUUID());
        }
    }

    private static void work(ServerLevel level, CopperGolemEntity golem) {
        BlockPos home = home(level, golem);
        if (home == null) return;
        Job job = jobs.computeIfAbsent(golem.getUUID(), u -> new Job());
        long now = level.getGameTime();
        job.avoid.values().removeIf(until -> until < now);
        ItemStack held = golem.getMainHandItem();

        if (held.isEmpty()) {
            if (job.source == null) {
                if (job.rest > 0) {
                    job.rest--;
                    return;
                }
                job.source = pickSource(level, golem, home, job);
                job.steps = 0;
                if (job.source == null) {
                    job.rest = 6;   // nothing to sort: look again in three seconds
                    return;
                }
            }
            if (!near(golem, job.source)) {
                walk(golem, job, job.source);
                return;
            }
            BlockPos at = job.source;
            job.source = null;
            IItemHandler h = handler(level, at);
            if (h == null) return;
            Survey survey = Survey.around(level, home, job);
            boolean likeness = mode(level, at);
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty() || survey.best(s, likeness) == null) continue;
                ItemStack taken = h.extractItem(i, s.getMaxStackSize(), false);
                if (taken.isEmpty()) continue;
                open(level, at, golem);
                golem.setItemSlot(EquipmentSlot.MAINHAND, taken);
                golem.setDropChance(EquipmentSlot.MAINHAND, 2f);   // a golem that dies drops what it carries
                golem.getPersistentData().put("wilds_sorter_from", NbtUtils.writeBlockPos(at));
                golem.getPersistentData().putBoolean("wilds_sorter_likeness", likeness);
                job.target = survey.best(taken, likeness);
                job.steps = 0;
                return;
            }
            return;
        }

        // carrying: to the best storage, then the next best while some is left, then back where it came from
        if (job.target == null || handler(level, job.target) == null) {
            job.target = Survey.around(level, home, job).best(held, golem.getPersistentData().getBoolean("wilds_sorter_likeness"));
            job.steps = 0;
            if (job.target == null) job.target = giveBack(level, golem, home, job);
            if (job.target == null) {
                // nowhere at all (every storage full): leave it on the copper chest's top, never keep it forever
                golem.spawnAtLocation(held);
                golem.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                return;
            }
        }
        if (!near(golem, job.target)) {
            walk(golem, job, job.target);
            return;
        }
        IItemHandler h = handler(level, job.target);
        ItemStack left = h == null ? held : ItemHandlerHelper.insertItemStacked(h, held.copy(), false);
        if (left.getCount() != held.getCount()) open(level, job.target, golem);
        if (!left.isEmpty()) job.avoid.put(job.target, now + 1200);   // full: not again for a minute
        golem.setItemSlot(EquipmentSlot.MAINHAND, left);
        job.target = null;
        if (left.isEmpty()) job.rest = 1;
    }

    /** the copper chest the golem works from: the one it was built on, else the nearest within reach (an F&F golem) */
    private static BlockPos home(ServerLevel level, CopperGolemEntity golem) {
        var data = golem.getPersistentData();
        BlockPos home = NbtUtils.readBlockPos(data, HOME).orElse(null);
        if (home != null && level.isLoaded(home) && isSource(level.getBlockState(home))) return home;
        if ((golem.tickCount / 10) % 6 != 0 && home == null) return null;   // a golem with no copper chest looks every 3 s
        BlockPos found = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : storages(level, golem.blockPosition(), 8, 4)) {
            if (!isSource(level.getBlockState(p))) continue;
            double d = p.distSqr(golem.blockPosition());
            if (d < best) {
                best = d;
                found = p;
            }
        }
        if (found != null) data.put(HOME, NbtUtils.writeBlockPos(found));
        else data.remove(HOME);
        return found;
    }

    /** the next copper chest, in turn, that holds something the golem has somewhere to take */
    private static BlockPos pickSource(ServerLevel level, CopperGolemEntity golem, BlockPos home, Job job) {
        List<BlockPos> sources = new ArrayList<>();
        for (BlockPos p : storages(level, home, RANGE, HEIGHT)) if (isSource(level.getBlockState(p)) && !job.avoid.containsKey(p)) sources.add(p);
        if (sources.isEmpty()) return null;
        sources.sort(Comparator.comparingLong(BlockPos::asLong));
        Survey survey = null;
        for (int k = 0; k < sources.size(); k++) {
            BlockPos p = sources.get((job.turn + k) % sources.size());
            IItemHandler h = handler(level, p);
            if (h == null) continue;
            boolean likeness = mode(level, p);
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty()) continue;
                if (survey == null) survey = Survey.around(level, home, job);
                if (survey.best(s, likeness) != null) {
                    job.turn = (job.turn + k + 1) % sources.size();
                    return p;
                }
            }
        }
        return null;
    }

    /** where a stack nobody wants goes back to: the chest it came from, or another copper chest with room */
    private static BlockPos giveBack(ServerLevel level, CopperGolemEntity golem, BlockPos home, Job job) {
        BlockPos from = NbtUtils.readBlockPos(golem.getPersistentData(), "wilds_sorter_from").orElse(home);
        List<BlockPos> tries = new ArrayList<>(List.of(from));
        for (BlockPos p : storages(level, home, RANGE, HEIGHT)) if (isSource(level.getBlockState(p)) && !p.equals(from)) tries.add(p);
        for (BlockPos p : tries) {
            IItemHandler h = handler(level, p);
            if (h != null && !job.avoid.containsKey(p) && ItemHandlerHelper.insertItemStacked(h, golem.getMainHandItem().copy(), true).getCount() < golem.getMainHandItem().getCount())
                return p;
        }
        return null;
    }

    private static boolean mode(ServerLevel level, BlockPos source) {
        BlockEntity be = level.getBlockEntity(source);
        return be != null && be.getPersistentData().getBoolean(MODE);
    }

    private static boolean near(CopperGolemEntity golem, BlockPos p) {
        double dx = golem.getX() - (p.getX() + 0.5), dz = golem.getZ() - (p.getZ() + 0.5), dy = golem.getY() - p.getY();
        return dx * dx + dz * dz <= 2.6 * 2.6 && dy > -2.5 && dy < 2.5;
    }

    @SuppressWarnings("unchecked")
    private static void walk(CopperGolemEntity golem, Job job, BlockPos to) {
        if (++job.steps > 40) {
            // twenty seconds without getting there: something is in the way; that storage is left alone for a while
            job.avoid.put(to, golem.level().getGameTime() + 2400);
            if (to.equals(job.source)) job.source = null;
            if (to.equals(job.target)) job.target = null;
            job.steps = 0;
            return;
        }
        var brain = golem.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(to, 1.0f, 1));
        BuiltInRegistries.MEMORY_MODULE_TYPE.getOptional(BUTTON_COOLDOWN)
                .ifPresent(t -> brain.setMemory((MemoryModuleType<Integer>) t, 200));   // no button pressing while at work
    }

    /** the chest opens and shuts as the golem reaches in */
    private static void open(ServerLevel level, BlockPos p, CopperGolemEntity golem) {
        BlockState s = level.getBlockState(p);
        golem.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        golem.swing(InteractionHand.MAIN_HAND);
        boolean barrel = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().contains("barrel");
        level.playSound(null, p, barrel ? SoundEvents.BARREL_OPEN : SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.4f, 1.1f);
        level.blockEvent(p, s.getBlock(), 1, 1);
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 12, () -> {
            if (level.getBlockState(p).getBlock() != s.getBlock()) return;
            level.blockEvent(p, s.getBlock(), 1, 0);
            level.playSound(null, p, barrel ? SoundEvents.BARREL_CLOSE : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.4f, 1.1f);
        }));
    }

    // ---------------------------------------------------------------- the storages around

    static IItemHandler handler(ServerLevel level, BlockPos p) {
        if (!level.isLoaded(p)) return null;
        BlockEntity be = level.getBlockEntity(p);
        if (be == null) return null;
        try {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, p, be.getBlockState(), be, null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** every block with an item storage in the box around a place */
    static List<BlockPos> storages(ServerLevel level, BlockPos center, int range, int height) {
        List<BlockPos> out = new ArrayList<>();
        for (int cx = (center.getX() - range) >> 4; cx <= (center.getX() + range) >> 4; cx++) {
            for (int cz = (center.getZ() - range) >> 4; cz <= (center.getZ() + range) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (Math.abs(p.getX() - center.getX()) > range || Math.abs(p.getZ() - center.getZ()) > range || Math.abs(p.getY() - center.getY()) > height) continue;
                    out.add(p.immutable());
                }
            }
        }
        return out;
    }

    /** what is where around the golem's home: every storage it may put things in, with what it holds */
    static final class Survey {
        private record Box(BlockPos pos, IItemHandler handler, Map<Item, Integer> items, double dist) {}

        private final List<Box> boxes = new ArrayList<>();
        private Box misc;

        static Survey around(ServerLevel level, BlockPos home, Job job) {
            Survey s = new Survey();
            UUID owner = Claims.owner(level, home);
            for (BlockPos p : storages(level, home, RANGE, HEIGHT)) {
                if (job.avoid.containsKey(p)) continue;
                BlockState state = level.getBlockState(p);
                if (isSource(state) || !storage(level, p, state)) continue;
                if (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.LEFT) continue;   // a double chest once
                if (!Claims.mayUse(level, owner, p)) continue;
                IItemHandler h = handler(level, p);
                if (h == null || h.getSlots() < 5) continue;
                Map<Item, Integer> items = new HashMap<>();
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack st = h.getStackInSlot(i);
                    if (!st.isEmpty()) items.merge(st.getItem(), st.getCount(), Integer::sum);
                }
                Box box = new Box(p, h, items, p.distSqr(home));
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof Nameable n && n.hasCustomName() && n.getCustomName() != null
                        && MISC_NAMES.contains(n.getCustomName().getString().trim().toLowerCase(java.util.Locale.ROOT))) {
                    if (s.misc == null || box.dist < s.misc.dist) s.misc = box;
                } else s.boxes.add(box);
            }
            return s;
        }

        /** where the stack goes: the most alike storage with room (the most of it, then the nearest), else «Різне» */
        BlockPos best(ItemStack stack, boolean likeness) {
            Box best = null;
            int bestD = Likeness.NONE, bestN = 0;
            for (Box b : boxes) {
                if (b.items.isEmpty()) continue;
                int d = Likeness.NONE, n = 0;
                for (var e : b.items.entrySet()) {
                    int di = e.getKey() == stack.getItem() ? 0 : likeness ? Likeness.distance(stack.getItem(), e.getKey()) : Likeness.NONE;
                    if (di < d) {
                        d = di;
                        n = e.getValue();
                    } else if (di == d) n += e.getValue();
                }
                if (d == Likeness.NONE || d > bestD) continue;
                if (d == bestD && (n < bestN || (n == bestN && best != null && b.dist >= best.dist))) continue;
                if (!room(b.handler, stack)) continue;
                best = b;
                bestD = d;
                bestN = n;
            }
            if (best != null) return best.pos;
            return misc != null && room(misc.handler, stack) ? misc.pos : null;
        }

        private static boolean room(IItemHandler h, ItemStack stack) {
            return ItemHandlerHelper.insertItemStacked(h, stack.copy(), true).getCount() < stack.getCount();
        }

        /** a storage, not a furnace, a machine or a hopper: chests, barrels, crates, drawers, shulker boxes and the like */
        private static boolean storage(ServerLevel level, BlockPos p, BlockState state) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id.getNamespace().equals("lootr")) return false;
            String path = id.getPath();
            return path.contains("chest") || path.contains("barrel") || path.contains("crate") || path.contains("drawer")
                    || path.contains("shulker") || path.contains("storage") || path.contains("cabinet") || path.contains("basket")
                    || path.contains("locker") || path.contains("cupboard");
        }
    }

    /** Open Parties and Claims, when installed: the golem puts things only where the owner of its home could */
    static final class Claims {
        private static final boolean OPAC = net.neoforged.fml.ModList.get().isLoaded("openpartiesandclaims");

        static UUID owner(ServerLevel level, BlockPos p) {
            if (!OPAC) return null;
            try {
                var claim = xaero.pac.common.server.api.OpenPACServerAPI.get(level.getServer()).getServerClaimsManager()
                        .get(level.dimension().location(), new ChunkPos(p).x, new ChunkPos(p).z);
                return claim == null ? null : claim.getPlayerId();
            } catch (RuntimeException | LinkageError ex) {
                return null;
            }
        }

        static boolean mayUse(ServerLevel level, UUID homeOwner, BlockPos p) {
            UUID o = owner(level, p);
            if (o == null || o.equals(homeOwner)) return true;
            if (homeOwner == null) return false;
            try {
                var parties = xaero.pac.common.server.api.OpenPACServerAPI.get(level.getServer()).getPartyManager();
                var a = parties.getPartyByMember(homeOwner);
                var b = parties.getPartyByMember(o);
                return a != null && b != null && a.getId().equals(b.getId());
            } catch (RuntimeException | LinkageError ex) {
                return false;
            }
        }
    }
}
