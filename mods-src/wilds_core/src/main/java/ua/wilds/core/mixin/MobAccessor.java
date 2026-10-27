package ua.wilds.core.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Hired golems and guards get extra goals (follow and defend the player who paid). */
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("goalSelector")
    GoalSelector wilds$goals();

    @Accessor("targetSelector")
    GoalSelector wilds$targets();
}
