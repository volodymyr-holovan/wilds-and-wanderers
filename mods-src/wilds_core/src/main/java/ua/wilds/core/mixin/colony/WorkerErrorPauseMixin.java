package ua.wilds.core.mixin.colony;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When a worker's AI throws, MineColonies pauses that worker for 5 seconds times a multiplier that doubles on every
 * error and is never reset while the citizen lives: one error a day (another mod's inventory, a broken block entity)
 * and after a week the worker stands still for hours per error, after 31 errors the int overflows. The multiplier now
 * stops at 8 (40 seconds) and starts over once the worker has run ten minutes without an error.
 */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class WorkerErrorPauseMixin {
    @Shadow private int exceptionTimer;

    @Unique private static final int WILDS$MAX_MULTIPLIER = 8;
    @Unique private static final long WILDS$CALM_MS = 10 * 60 * 1000L;
    @Unique private long wilds$lastError;

    @Inject(method = "onException", at = @At("HEAD"), require = 1)
    private void wilds$boundPause(RuntimeException e, CallbackInfo ci) {
        final long now = System.currentTimeMillis();
        if (now - wilds$lastError > WILDS$CALM_MS || exceptionTimer < 1) {
            exceptionTimer = 1;
        }
        wilds$lastError = now;
        if (exceptionTimer > WILDS$MAX_MULTIPLIER) {
            exceptionTimer = WILDS$MAX_MULTIPLIER;
        }
    }
}
