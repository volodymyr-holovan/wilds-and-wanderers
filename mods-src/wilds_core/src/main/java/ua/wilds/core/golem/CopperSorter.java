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
import java.util.UUID;

/**
 * The copper golem as a sorter, on Friends &amp; Foes' golem (its model, animations and oxidation stay).
 * <p>
 * <b>Building</b>: a carved pumpkin (or a jack o'lantern) put right on a copper block, any of the eight. The block
 * becomes a docking station ({@link GolemDock}) and the golem stands on it, as rusty and as waxed as the block was.
 * F&amp;F's own way, with a lightning rod between, still works too.
 * <p>
 * <b>Home</b>: like a robot vacuum, the golem with nothing to sort goes back to its station, stands on the pad and
 * waits there (it does not wander, cannot be pushed off or hurt, and costs the server nothing but a look for work every
 * three seconds); one that cannot get home, or is found far from it, is put back on it. A station placed by hand
 * (copper block + lightning rod) takes in the nearest golem that has none: so do the golems of before the stations,
 * who lived on copper chests, and until then they wait beside their chest.
 * <p>
 * <b>Work</b>: the golem takes a whole stack out of a source storage near its home and carries it to the storage where
 * that item already is: the one with the most of it that still has room. It knows what every storage nearby holds
 * without opening it (it is the server that looks), so it walks straight there. Sources are taken from in turn.
 * <p>
 * <b>Statuses</b> ({@link SortStatus}) of every storage within the golem's reach, set by clicking it with an empty
 * hand while sneaking, round and round: normal (things go in), source exact (things are taken out and go where the very
 * same item is), source by likeness (where the same or the most alike items are, {@link Likeness}: spruce logs to the
 * logs, leggings to the armour, a modded gem to the diamonds), misc (what fits nowhere else; with none it stays in the
 * source), do not touch. A placed storage is normal; a new golem has no source until the player marks one.
 * Every storage counts whatever its size (iron, gold and diamond chests, barrels, drawers...), but not furnaces,
 * machines and other blocks that merely hold items, not the sources themselves, not the claims of another
 * party, and not Lootr's chests. Everything runs on the server: nothing to install for the players.
 */
public final class CopperSorter {
    private static final ResourceLocation GOLEM = ResourceLocation.fromNamespaceAndPath("friendsandfoes", "copper_golem");
    private static final ResourceLocation BUTTON_COOLDOWN = ResourceLocation.fromNamespaceAndPath("friendsandfoes", "copper_golem_press_button_cooldown");
    static final int RANGE = 20, HEIGHT = 8;
    private static final String HOME = "wilds_sorter_home";
    /** the golem stands on its station, switched off */
    private static final String DOCKED = "wilds_sorter_docked";
    /** -Dwilds_core.golemdebug=true: every decision of every golem in the log */
    private static final boolean DEBUG = Boolean.getBoolean("wilds_core.golemdebug");

    /** what each golem is doing; lost on a restart, when the golem simply starts over with what it holds */
    private static final Map<UUID, Job> jobs = new HashMap<>();

    private static final class Job {
        BlockPos source, target;
        int steps, rest, turn, homeSteps;
        boolean idle;
        final Map<BlockPos, Long> avoid = new HashMap<>();   // storages it could not reach or that were full, until then
    }

    private CopperSorter() {}

    // ---------------------------------------------------------------- building

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        BlockState placed = e.getPlacedBlock();
        if (placed.getBlock() instanceof GolemDock) {
            BlockPos at = e.getPos().immutable();
            Entity by = e.getEntity();
            level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> adopt(level, at, by)));
            return;
        }
        if (placed.hasBlockEntity()) {
            // a placed storage is normal, or (a chest that made a double chest) what the chest beside it was
            BlockPos at = e.getPos().immutable();
            level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
                if (!Survey.storage(level, at, level.getBlockState(at))) return;
                BlockPos other = SortStatus.partner(level, at);
                BlockEntity be = level.getBlockEntity(at);
                if (be == null) return;
                be.getPersistentData().putString(SortStatus.KEY, (other != null ? SortStatus.of(level, other) : SortStatus.NORMAL).id);
                be.setChanged();
            }));
            return;
        }
        BlockState head = placed;
        if (!head.is(Blocks.CARVED_PUMPKIN) && !head.is(Blocks.JACK_O_LANTERN)) return;
        BlockPos pos = e.getPos().immutable();
        if (copper(level.getBlockState(pos.below())) == null) return;
        Direction facing = head.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? head.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        // the next tick: the placing is not over yet while its event runs
        Entity by = e.getEntity();
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> build(level, pos, facing, by)));
    }

    /** a station placed by hand takes in the nearest golem without one */
    private static void adopt(ServerLevel level, BlockPos dock, Entity by) {
        if (!isDock(level, dock)) return;
        CopperGolemEntity found = null;
        double best = Double.MAX_VALUE;
        for (CopperGolemEntity g : level.getEntitiesOfClass(CopperGolemEntity.class, new net.minecraft.world.phys.AABB(dock).inflate(16, 6, 16))) {
            BlockPos home = NbtUtils.readBlockPos(g.getPersistentData(), HOME).orElse(null);
            if (home != null && isDock(level, home)) continue;
            double d = g.distanceToSqr(dock.getX() + 0.5, dock.getY(), dock.getZ() + 0.5);
            if (d < best) {
                best = d;
                found = g;
            }
        }
        if (found != null) found.getPersistentData().put(HOME, NbtUtils.writeBlockPos(dock));
        if (by instanceof ServerPlayer p) p.displayClientMessage(Component.literal(found != null ? "Мідний голем оселився в цій станції."
                : "Станція порожня: поставте поруч різьблений гарбуз на мідний блок або приведіть голема без станції.").withStyle(ChatFormatting.GOLD), true);
    }

    static boolean isDock(ServerLevel level, BlockPos p) {
        return level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof GolemDock;
    }

    private static void build(ServerLevel level, BlockPos pos, Direction facing, Entity by) {
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
        BlockPos dock = pos.below();
        level.setBlock(dock, WildsCore.GOLEM_DOCK.get().defaultBlockState().setValue(GolemDock.FACING, facing), 3);
        golem.moveTo(dock.getX() + 0.5, dock.getY() + GolemDock.PAD, dock.getZ() + 0.5, facing.toYRot(), 0);
        golem.setSpawnYaw(facing.toYRot());
        golem.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        golem.setOxidationLevel(copper.state);
        golem.setPrevOxidationLevel(copper.state);
        golem.setIsWaxed(copper.waxed);
        golem.getPersistentData().put(HOME, NbtUtils.writeBlockPos(dock));
        golem.setPersistenceRequired();
        level.addFreshEntity(golem);
        if (by instanceof ServerPlayer p) p.sendSystemMessage(Component.literal("Мідний голем готовий. Позначте скриню, з якої він братиме речі: присядьте й клацніть по ній порожньою рукою (Shift+ПКМ), доки не побачите «Джерело».").withStyle(ChatFormatting.GOLD));
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

    // ---------------------------------------------------------------- the status of a storage

    /** sneak + right click with an empty hand on a storage a copper golem works with: its next status */
    public static void onClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !p.getMainHandItem().isEmpty() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(p.level() instanceof ServerLevel level)) return;
        BlockPos pos = e.getPos();
        if (!Survey.storage(level, pos, level.getBlockState(pos)) || handler(level, pos) == null || !golemNear(level, pos)) return;
        SortStatus next = SortStatus.of(level, pos).next();
        SortStatus.set(level, pos, next);
        p.displayClientMessage(Component.literal("Скриня для мідного голема: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(next.label).withStyle(ChatFormatting.YELLOW)), true);
        level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1f, 0.8f + next.ordinal() * 0.15f);
        e.setCanceled(true);
    }

    /**
     * a copper golem works near enough for the storage to matter: only then does sneak-clicking change its status (and
     * a normal status show in its title); elsewhere chests open as always
     */
    public static boolean golemNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(CopperGolemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(RANGE + 4, HEIGHT + 4, RANGE + 4)).isEmpty();
    }

    /** the text shown after the title of an open storage: its status, or nothing for a normal one with no golem near */
    public static String label(ServerLevel level, BlockPos pos) {
        if (!Survey.storage(level, pos, level.getBlockState(pos)) || handler(level, pos) == null) return "";
        SortStatus s = SortStatus.of(level, pos);
        return s == SortStatus.NORMAL && !golemNear(level, pos) ? "" : s.label;
    }

    public static void onTags(TagsUpdatedEvent e) {
        Likeness.clear();
    }

    // ---------------------------------------------------------------- the work

    public static void onTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof CopperGolemEntity golem) || !(golem.level() instanceof ServerLevel level)) return;
        if ((golem.tickCount + golem.getId()) % 10 != 0) return;
        if (golem.isImmobilized() || golem.isPassenger() || golem.isLeashed() || !golem.isAlive()) {
            if (golem.getPersistentData().getBoolean(DOCKED)) undock(level, golem);
            return;
        }
        try {
            work(level, golem);
        } catch (RuntimeException ex) {
            WildsCore.LOG.warn("Copper golem sorter at {} failed", golem.blockPosition(), ex);
            jobs.remove(golem.getUUID());
        }
    }

    private static void work(ServerLevel level, CopperGolemEntity golem) {
        BlockPos home = home(level, golem);
        boolean docked = golem.getPersistentData().getBoolean(DOCKED);
        if (home == null || (docked && !isDock(level, home))) {
            if (docked) undock(level, golem);   // the station is gone
            if (home == null) return;
        }
        Job job = jobs.computeIfAbsent(golem.getUUID(), u -> new Job());
        // found far from home (pushed, fallen, carried by water): back on the station, never lost
        double far = RANGE + 12;
        if (!docked && golem.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > far * far) {
            bringHome(level, golem, home);
            return;
        }
        long now = level.getGameTime();
        job.avoid.values().removeIf(until -> until < now);
        ItemStack held = golem.getMainHandItem();

        if (held.isEmpty()) {
            if (job.source == null) {
                if (job.rest > 0) {
                    job.rest--;
                    if (job.idle) idle(level, golem, home, job);
                    return;
                }
                job.source = pickSource(level, golem, home, job);
                job.steps = 0;
                if (job.source == null) {
                    job.rest = 6;   // nothing to sort: home, and look again in three seconds
                    job.idle = true;
                    idle(level, golem, home, job);
                    return;
                }
                job.idle = false;
                job.homeSteps = 0;
                undock(level, golem);
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
                if (DEBUG) WildsCore.LOG.info("Copper golem {}: took {} from {}, to {} ({})", golem.getId(), taken, at, job.target,
                        job.target == null ? "" : Survey.explain(level, job.target, taken.getItem()));
                return;
            }
            return;
        }

        // carrying: to the best storage, then the next best while some is left, then back where it came from
        job.idle = false;
        undock(level, golem);
        if (job.target == null || handler(level, job.target) == null) {
            if (job.rest > 0) {
                job.rest--;
                return;
            }
            job.target = Survey.around(level, home, job).best(held, golem.getPersistentData().getBoolean("wilds_sorter_likeness"));
            job.steps = 0;
            if (job.target == null) job.target = giveBack(level, golem, home, job);
            if (DEBUG) WildsCore.LOG.info("Copper golem {}: carrying {}, to {}", golem.getId(), held, job.target);
            if (job.target == null) {
                // nowhere now (everything full, or out of reach): hold it and look again in ten seconds, never drop it
                job.avoid.clear();
                job.rest = 20;
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

    // ---------------------------------------------------------------- home

    /** nothing to do: to the station and onto its pad; a golem of before the stations waits beside its chest */
    private static void idle(ServerLevel level, CopperGolemEntity golem, BlockPos home, Job job) {
        var data = golem.getPersistentData();
        if (!isDock(level, home)) {
            double dx = golem.getX() - (home.getX() + 0.5), dz = golem.getZ() - (home.getZ() + 0.5);
            if (dx * dx + dz * dz > 9) walkHome(level, golem, home, job);
            else golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);   // no strolling away
            return;
        }
        double x = home.getX() + 0.5, y = home.getY() + GolemDock.PAD, z = home.getZ() + 0.5;
        if (data.getBoolean(DOCKED)) {
            if (golem.distanceToSqr(x, y, z) > 0.01) golem.moveTo(x, y, z, yaw(level, home), 0);   // pushed: back in place
            GolemDock.setOccupied(level, home, true);
            return;
        }
        double dx = golem.getX() - x, dz = golem.getZ() - z, dy = golem.getY() - y;
        if (dx * dx + dz * dz > 1.6 * 1.6 || Math.abs(dy) > 1.5) {
            walkHome(level, golem, home, job);
            return;
        }
        // another golem already stands there (two golems, one station): wait beside it
        for (CopperGolemEntity other : level.getEntitiesOfClass(CopperGolemEntity.class, new net.minecraft.world.phys.AABB(home).inflate(0.2, 0.6, 0.2))) {
            if (other != golem && other.getPersistentData().getBoolean(DOCKED)) {
                golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                return;
            }
        }
        float yaw = yaw(level, home);
        golem.getNavigation().stop();
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        golem.moveTo(x, y, z, yaw, 0);
        golem.setYHeadRot(yaw);
        golem.setYBodyRot(yaw);
        golem.setDeltaMovement(0, 0, 0);
        golem.setNoAi(true);
        golem.setInvulnerable(true);
        golem.setPersistenceRequired();
        data.putBoolean(DOCKED, true);
        job.homeSteps = 0;
        GolemDock.setOccupied(level, home, true);
        level.playSound(null, home, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6f, 1.2f);
    }

    private static float yaw(ServerLevel level, BlockPos dock) {
        BlockState s = level.getBlockState(dock);
        return s.hasProperty(GolemDock.FACING) ? s.getValue(GolemDock.FACING).toYRot() : 0;
    }

    /** off the pad and switched on again */
    private static void undock(ServerLevel level, CopperGolemEntity golem) {
        var data = golem.getPersistentData();
        if (!data.getBoolean(DOCKED)) return;
        data.remove(DOCKED);
        golem.setNoAi(false);
        golem.setInvulnerable(false);
        BlockPos home = NbtUtils.readBlockPos(data, HOME).orElse(null);
        if (home != null && isDock(level, home)) {
            GolemDock.setOccupied(level, home, false);
            level.playSound(null, home, SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }

    @SuppressWarnings("unchecked")
    private static void walkHome(ServerLevel level, CopperGolemEntity golem, BlockPos home, Job job) {
        if (++job.homeSteps > 40) {   // twenty seconds and still not home: something is in the way
            job.homeSteps = 0;
            bringHome(level, golem, home);
            return;
        }
        var brain = golem.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(home, 1.0f, 0));
        BuiltInRegistries.MEMORY_MODULE_TYPE.getOptional(BUTTON_COOLDOWN).ifPresent(t -> brain.setMemory((MemoryModuleType<Integer>) t, 200));
    }

    /** puts the golem beside its home (on the station's pad) wherever it is */
    private static void bringHome(ServerLevel level, CopperGolemEntity golem, BlockPos home) {
        if (DEBUG) WildsCore.LOG.info("Copper golem {}: brought home from {}", golem.getId(), golem.blockPosition());
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, golem.getX(), golem.getY() + 0.6, golem.getZ(), 12, 0.2, 0.3, 0.2, 0.05);
        golem.getNavigation().stop();
        if (isDock(level, home)) golem.teleportTo(home.getX() + 0.5, home.getY() + GolemDock.PAD, home.getZ() + 0.5);
        else {
            BlockPos spot = home.above();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = home.relative(d);
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                    spot = p;
                    break;
                }
            }
            golem.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, golem.getX(), golem.getY() + 0.6, golem.getZ(), 12, 0.2, 0.3, 0.2, 0.05);
        level.playSound(null, golem.blockPosition(), SoundEvents.COPPER_PLACE, SoundSource.NEUTRAL, 0.6f, 1.4f);
    }

    /** the place the golem works around: its station, else (a golem of before the stations) its chest, else the nearest source within 8 blocks (an F&F golem) */
    private static BlockPos home(ServerLevel level, CopperGolemEntity golem) {
        var data = golem.getPersistentData();
        BlockPos home = NbtUtils.readBlockPos(data, HOME).orElse(null);
        if (home != null && level.isLoaded(home) && (isDock(level, home) || handler(level, home) != null)) return home;
        if ((golem.tickCount / 10) % 6 != 0 && home == null) return null;   // a golem with no source near looks every 3 s
        BlockPos found = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : storages(level, golem.blockPosition(), 8, 4)) {
            if (!SortStatus.of(level, p).source()) continue;
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

    /** the next source, in turn, that holds something the golem has somewhere to take */
    private static BlockPos pickSource(ServerLevel level, CopperGolemEntity golem, BlockPos home, Job job) {
        List<BlockPos> sources = new ArrayList<>();
        for (BlockPos p : storages(level, home, RANGE, HEIGHT)) {
            BlockState st = level.getBlockState(p);
            if (st.hasProperty(BlockStateProperties.CHEST_TYPE) && st.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.LEFT) continue;
            if (SortStatus.of(level, p).source() && !job.avoid.containsKey(p) && Survey.storage(level, p, st)) sources.add(p);
        }
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

    /** where a stack nobody wants goes back to: the source it came from, or another source with room */
    private static BlockPos giveBack(ServerLevel level, CopperGolemEntity golem, BlockPos home, Job job) {
        BlockPos from = NbtUtils.readBlockPos(golem.getPersistentData(), "wilds_sorter_from").orElse(home);
        List<BlockPos> tries = new ArrayList<>(List.of(from));
        for (BlockPos p : storages(level, home, RANGE, HEIGHT)) if (SortStatus.of(level, p).source() && !p.equals(from)) tries.add(p);
        for (BlockPos p : tries) {
            IItemHandler h = handler(level, p);
            if (h != null && !job.avoid.containsKey(p) && ItemHandlerHelper.insertItemStacked(h, golem.getMainHandItem().copy(), true).getCount() < golem.getMainHandItem().getCount())
                return p;
        }
        return null;
    }

    private static boolean mode(ServerLevel level, BlockPos source) {
        return SortStatus.of(level, source) == SortStatus.SOURCE_LIKE;
    }

    private static boolean near(CopperGolemEntity golem, BlockPos p) {
        double dx = golem.getX() - (p.getX() + 0.5), dz = golem.getZ() - (p.getZ() + 0.5), dy = golem.getY() - p.getY();
        return dx * dx + dz * dz <= 2.6 * 2.6 && dy > -2.5 && dy < 2.5;
    }

    @SuppressWarnings("unchecked")
    private static void walk(CopperGolemEntity golem, Job job, BlockPos to) {
        if (++job.steps > 40) {
            if (DEBUG) WildsCore.LOG.info("Copper golem {}: cannot reach {}", golem.getId(), to);
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
        private record Box(BlockPos pos, IItemHandler handler, Map<Item, Integer> items, double dist, Likeness.Theme[] cache) {
            Likeness.Theme theme() {
                if (cache[0] == null) cache[0] = Likeness.theme(items.keySet());
                return cache[0];
            }

            int total() {
                int n = 0;
                for (int c : items.values()) n += c;
                return n;
            }
        }

        private final List<Box> boxes = new ArrayList<>();
        private Box misc;

        static Survey around(ServerLevel level, BlockPos home, Job job) {
            Survey s = new Survey();
            UUID owner = Claims.owner(level, home);
            for (BlockPos p : storages(level, home, RANGE, HEIGHT)) {
                if (job.avoid.containsKey(p)) continue;
                BlockState state = level.getBlockState(p);
                if (!storage(level, p, state)) continue;
                SortStatus status = SortStatus.of(level, p);
                if (status.source() || status == SortStatus.IGNORE) continue;
                if (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.LEFT) continue;   // a double chest once
                if (!Claims.mayUse(level, owner, p)) continue;
                IItemHandler h = handler(level, p);
                if (h == null || h.getSlots() < 5) continue;
                Map<Item, Integer> items = new HashMap<>();
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack st = h.getStackInSlot(i);
                    if (!st.isEmpty()) items.merge(st.getItem(), st.getCount(), Integer::sum);
                }
                Box box = new Box(p, h, items, p.distSqr(home), new Likeness.Theme[1]);
                if (status == SortStatus.MISC) {
                    if (s.misc == null || box.dist < s.misc.dist) s.misc = box;
                } else s.boxes.add(box);
            }
            return s;
        }

        /** where the stack goes: the most alike storage with room (the most of it, then the nearest), else the misc one */
        BlockPos best(ItemStack stack, boolean likeness) {
            Box best = null;
            int bestD = Likeness.NONE, bestN = 0;
            for (Box b : boxes) {
                if (b.items.isEmpty()) continue;
                // where the very same item lies always comes first; then the storage whose theme fits it best
                Integer same = b.items.get(stack.getItem());
                int d = same != null ? 0 : likeness ? Likeness.fit(stack.getItem(), b.theme()) : Likeness.NONE;
                int n = same != null ? same : b.total();
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

        /** /wildssort: what the storage is about and how a thing fits it */
        static String explain(ServerLevel level, BlockPos p, Item item) {
            IItemHandler h = handler(level, p);
            if (h == null) return "not a storage";
            java.util.Set<Item> items = new java.util.HashSet<>();
            for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) items.add(h.getStackInSlot(i).getItem());
            if (items.isEmpty()) return "empty";
            Likeness.Theme t = Likeness.theme(items);
            return item == null ? Likeness.describe(t) : Likeness.why(item, t);
        }

        /** a storage, not a furnace, a machine or a hopper: chests, barrels, crates, drawers, shulker boxes and the like */
        static boolean storage(ServerLevel level, BlockPos p, BlockState state) {
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
