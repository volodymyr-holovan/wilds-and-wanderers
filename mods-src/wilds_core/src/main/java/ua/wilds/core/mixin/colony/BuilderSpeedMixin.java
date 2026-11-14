package ua.wilds.core.mixin.colony;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The builder places blocks twice as fast (the pack's choice: colonies grow at the pace of the rest of the game).
 * <p>
 * A worker's AI runs every 5 ticks. One placement takes that AI step plus a wait of {@code ceil(delay / 5)} steps,
 * where the delay is 15 ticks for a new builder and shrinks with skill and research. Halving the delay alone would not
 * halve the time (a 15-tick delay would become 8, still two waiting steps: 15 ticks instead of 20). So the wait is set
 * so the whole cycle takes half as many steps; when that is a half step, it is rounded up or down at random so the
 * average is exactly half.
 */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public abstract class BuilderSpeedMixin {
    @Unique private static final int WILDS$AI_STEP = 5;

    @ModifyArg(method = "structureStep", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIStructure;setDelay(I)V"), require = 1)
    private int wilds$fasterPlacement(int delay) {
        if (!((Object) this instanceof EntityAIStructureBuilder)) {
            return delay;
        }
        if (ua.wilds.core.colony.ColonyStats.ON) {
            ua.wilds.core.colony.ColonyStats.placement(this, delay);
        }
        final int steps = 1 + (delay > 0 ? (delay + WILDS$AI_STEP - 1) / WILDS$AI_STEP : 0);
        final double wait = steps / 2.0 - 1;
        if (wait <= 0) {
            return 0;
        }
        int whole = (int) wait;
        if (ThreadLocalRandom.current().nextDouble() < wait - whole) {
            whole++;
        }
        return whole * WILDS$AI_STEP;
    }
}
