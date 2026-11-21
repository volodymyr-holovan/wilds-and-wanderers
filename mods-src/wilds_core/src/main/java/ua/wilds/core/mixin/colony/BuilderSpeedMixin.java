package ua.wilds.core.mixin.colony;

import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The builder places blocks four times as fast as in MineColonies (the pack's choice: colonies grow at the pace of the
 * rest of the game).
 * <p>
 * A worker's AI runs every 5 ticks. One placement takes that AI step plus a wait of {@code ceil(delay / 5)} steps,
 * where the delay is 15 ticks for a new builder and shrinks with skill and research. Shrinking the delay alone cannot
 * go below one placement per step, so the time a placement should take is worked out in steps: a quarter of
 * MineColonies' own. When that is a step or more, the wait is shortened; when it is less than a step, the step places
 * more than one block. Fractions are rounded up or down at random so the average is exactly a quarter.
 */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public abstract class BuilderSpeedMixin {
    @Unique private static final int WILDS$AI_STEP = 5;
    @Unique private static final double WILDS$SPEED = 4.0;
    @Unique private static final int WILDS$MAX_PER_STEP = 4;

    @Unique private int wilds$extra;
    @Unique private boolean wilds$inExtra;

    @Shadow protected abstract IAIState structureStep();

    @ModifyArg(method = "structureStep", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIStructure;setDelay(I)V"), require = 1)
    private int wilds$fasterPlacement(int delay) {
        if (!((Object) this instanceof EntityAIStructureBuilder)) {
            return delay;
        }
        if (ua.wilds.core.colony.ColonyStats.ON) {
            ua.wilds.core.colony.ColonyStats.placement(this, delay);
        }
        if (wilds$inExtra) {
            return 0;  // an extra placement of a step already counted in the first one's plan
        }
        final int steps = 1 + (delay > 0 ? (delay + WILDS$AI_STEP - 1) / WILDS$AI_STEP : 0);
        final double target = steps / WILDS$SPEED;  // steps one placement should take
        if (target >= 1) {
            wilds$extra = 0;
            return wilds$round(target - 1) * WILDS$AI_STEP;
        }
        wilds$extra = Math.min(WILDS$MAX_PER_STEP, wilds$round(1 / target)) - 1;
        return 0;
    }

    @Inject(method = "structureStep", at = @At("RETURN"), cancellable = true, require = 1)
    private void wilds$morePerStep(CallbackInfoReturnable<IAIState> cir) {
        if (wilds$inExtra || wilds$extra <= 0 || !((Object) this instanceof EntityAIStructureBuilder)) {
            return;
        }
        int extra = wilds$extra;
        wilds$extra = 0;
        wilds$inExtra = true;
        try {
            IAIState state = cir.getReturnValue();
            // only while the builder keeps placing: walking off, fetching material, mining or finishing ends the step
            while (extra-- > 0 && state == AIWorkerState.BUILDING_STEP) {
                state = structureStep();
            }
            cir.setReturnValue(state);
        } finally {
            wilds$inExtra = false;
        }
    }

    @Unique
    private static int wilds$round(double value) {
        int whole = (int) value;
        if (ThreadLocalRandom.current().nextDouble() < value - whole) {
            whole++;
        }
        return whole;
    }
}
