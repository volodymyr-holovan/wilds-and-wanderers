package ua.wilds.core.mixin.colony;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.entity.pathfinding.Pathfinding;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathjobs.IDestinationPathJob;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;

/**
 * Fixes in the navigation every MineColonies entity uses.
 * <ul>
 * <li>Options set after a path job is handed out arrive too late. {@code setPathJob} submits the job to the pathfinding
 * thread at once, and its callers then switch the job's options: wandering, the builder's search for a place to stand
 * and the search for a free spot all forbid dropping down ({@code canDrop = false}) only after the job was submitted.
 * An idle pathfinding thread had already begun the search with drops of up to 10 blocks allowed: wandering citizens
 * walked into caves and ravines. The job is now submitted on the navigation's next tick, after the caller's changes.</li>
 * <li>Two paths in a row into an unloaded chunk made the citizen {@code discard()} itself: it disappeared until the
 * colony's respawn timer (every 5 minutes) or the next morning brought it back, and lost what it was doing. ServerCore
 * shrinks the view distance under load, so this happens near the edge of what players have loaded. The citizen now
 * waits two seconds and tries again.</li>
 * <li>Every failed path made the next one wait 10 ticks longer, with no limit: after many failures a citizen stood still
 * for half a minute between attempts. The wait is now capped at 5 seconds.</li>
 * <li>A citizen that fell into a cave keeps failing to path out and stays there for minutes (the stuck handler
 * teleports only after max(2 minutes, 10 seconds per block of distance), or never for paths without a safe goal). After
 * three failed paths in a row while it is underground at least 4 blocks below its goal, it is moved next to the goal,
 * the way MineColonies' own stuck handler does it.</li>
 * <li>From MineColonies 1.1.1420: a finished calculation without a path no longer crashes the node bookkeeping.</li>
 * </ul>
 */
@Mixin(value = MinecoloniesAdvancedPathNavigate.class, remap = false)
public abstract class ColonyNavigateMixin {
    @Shadow private PathResult<? extends AbstractPathJob> pathResult;
    @Shadow private int pauseTicks;
    @Shadow private int pauseTickBackupAmount;

    @Unique private static final int WILDS$MAX_FAIL_PAUSE = 100;
    @Unique private static final int WILDS$PIT_DEPTH = 4;
    @Unique private static final int WILDS$PIT_FAILS = 3;
    @Unique private static final long WILDS$PIT_WINDOW = 1200;

    @Unique private PathResult<?> wilds$pending;
    @Unique private int wilds$pitFails;
    @Unique private long wilds$lastPitFail;

    @Redirect(method = "setPathJob", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/pathfinding/pathresults/PathResult;startJob(Ljava/util/concurrent/ExecutorService;)V"), require = 1)
    private void wilds$deferStart(PathResult<?> result, ExecutorService executor) {
        wilds$pending = result;
    }

    @Redirect(method = "setPathJob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"), require = 1)
    private void wilds$keepCitizen(Mob mob) {
        if (!(mob instanceof AbstractEntityCitizen)) {
            mob.discard();  // raiders and other MineColonies mobs keep the old behaviour
            return;
        }
        pauseTicks = Math.max(pauseTicks, 40);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void wilds$startPending(CallbackInfo ci) {
        final PathResult<?> pending = wilds$pending;
        if (pending == null) {
            return;
        }
        wilds$pending = null;
        if (pending == pathResult && !pending.isCancelled() && pending.getJob() != null && !pending.isDone()) {
            pending.startJob(Pathfinding.getExecutor());
        }
    }

    @Inject(method = "updateNodeReferences", at = @At("HEAD"), cancellable = true)
    private void wilds$noPath(CallbackInfo ci) {
        if (((MinecoloniesAdvancedPathNavigate) (Object) this).getPath() == null) {
            ci.cancel();
        }
    }

    @Inject(method = "processCompletedCalculationResult", at = @At("HEAD"), cancellable = true)
    private void wilds$climbOutOfPit(CallbackInfo ci) {
        final PathResult<?> result = pathResult;
        if (result == null) {
            return;
        }
        final MinecoloniesAdvancedPathNavigate self = (MinecoloniesAdvancedPathNavigate) (Object) this;
        final Mob mob = self.getOurEntity();
        if (!(mob instanceof AbstractEntityCitizen citizen) || !(result.getJob() instanceof IDestinationPathJob job)) {
            return;
        }
        if (result.isPathReachingDestination()) {
            wilds$pitFails = 0;
            return;
        }
        final Level level = mob.level();
        final BlockPos at = mob.blockPosition();
        final BlockPos goal = job.getDestination();
        // a goal that is merely not standable (a hut block on a shelf, a chest on a wall) also "fails": the path then
        // ends right under it, so what counts is how far below the goal the best reachable point stays
        final BlockPos end = result.getPath() != null && result.getPath().getTarget() != null ? result.getPath().getTarget() : at;
        if (goal == null || goal.getY() - at.getY() < WILDS$PIT_DEPTH || goal.getY() - end.getY() < WILDS$PIT_DEPTH
                || level.canSeeSky(at.above()) || wilds$insideBuilding(citizen, at)) {
            wilds$pitFails = 0;
            return;
        }
        final long now = level.getGameTime();
        if (now - wilds$lastPitFail > WILDS$PIT_WINDOW) {
            wilds$pitFails = 0;
        }
        wilds$lastPitFail = now;
        if (++wilds$pitFails < WILDS$PIT_FAILS) {
            return;
        }
        wilds$pitFails = 0;
        BlockPos to = wilds$standAround(level, goal);
        final ICitizenData data = citizen.getCitizenData();
        if (to == null && data != null && data.getWorkBuilding() != null) {
            to = wilds$standAround(level, data.getWorkBuilding().getPosition());
        }
        if (to == null && data != null && data.getHomeBuilding() != null) {
            to = wilds$standAround(level, data.getHomeBuilding().getPosition());
        }
        if (to == null) {
            return;
        }
        if (ua.wilds.core.colony.ColonyStats.ON) {
            com.mojang.logging.LogUtils.getLogger().info("[colonytest] pit rescue: {} from {} (path end {}) to {} for goal {}",
                    mob.getName().getString(), at.toShortString(), end.toShortString(), to.toShortString(), goal.toShortString());
        }
        mob.stopRiding();
        mob.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        mob.fallDistance = 0;
        self.stop();
        ci.cancel();
    }

    @Inject(method = "processCompletedCalculationResult", at = @At("RETURN"))
    private void wilds$capFailPause(CallbackInfo ci) {
        if (pauseTickBackupAmount > WILDS$MAX_FAIL_PAUSE) {
            pauseTickBackupAmount = WILDS$MAX_FAIL_PAUSE;
        }
        if (pauseTicks > WILDS$MAX_FAIL_PAUSE + 50) {
            pauseTicks = WILDS$MAX_FAIL_PAUSE + 50;
        }
    }

    /** Basements, mines and halls of the colony are not caves to be rescued from. */
    @Unique
    private static boolean wilds$insideBuilding(AbstractEntityCitizen citizen, BlockPos at) {
        final var colony = citizen.getCitizenColonyHandler().getColony();
        if (colony == null) {
            return false;
        }
        for (var building : colony.getServerBuildingManager().getBuildings().values()) {
            if (building.isInBuilding(at)) {
                return true;
            }
        }
        return false;
    }

    /** Same test MineColonies' stuck handler uses for its teleport: solid ground, two blocks of room, near the goal. */
    @Unique
    private static BlockPos wilds$standAround(Level level, BlockPos center) {
        if (!level.isLoaded(center)) {
            return null;
        }
        return BlockPosUtil.findAround(level, center, 10, 10,
                (world, pos) -> SurfaceType.getSurfaceType(world, world.getBlockState(pos.below()), pos.below()) == SurfaceType.WALKABLE
                        && SurfaceType.getSurfaceType(world, world.getBlockState(pos), pos) == SurfaceType.DROPABLE
                        && SurfaceType.getSurfaceType(world, world.getBlockState(pos.above()), pos.above()) == SurfaceType.DROPABLE);
    }
}
