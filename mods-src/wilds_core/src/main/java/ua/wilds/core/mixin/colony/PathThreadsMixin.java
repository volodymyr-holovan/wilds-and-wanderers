package ua.wilds.core.mixin.colony;

import com.minecolonies.core.entity.pathfinding.Pathfinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Every path of every citizen, guard, visitor and raider is computed on one thread, one after another. A long search
 * (up to 8000 nodes when a goal cannot be reached) holds up everyone queued behind it, and those citizens stand still
 * waiting for a path. Two threads: the jobs share nothing but thread-safe caches, and the server's four cores have room.
 */
@Mixin(value = Pathfinding.class, remap = false)
public abstract class PathThreadsMixin {
    @Shadow @Final private static ArrayBlockingQueue<Runnable> jobQueue;
    @Shadow private static ThreadPoolExecutor executor;

    @Inject(method = "getExecutor", at = @At("HEAD"), require = 1)
    private static void wilds$twoThreads(CallbackInfoReturnable<ThreadPoolExecutor> cir) {
        if (executor == null) {
            executor = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, jobQueue, new Pathfinding.MinecoloniesThreadFactory());
        }
    }
}
