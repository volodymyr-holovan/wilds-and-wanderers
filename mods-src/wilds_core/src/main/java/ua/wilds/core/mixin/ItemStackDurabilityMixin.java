package ua.wilds.core.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

import java.util.function.Consumer;

/** Perk tool_saver: each point of wear a player's tool or weapon would take is skipped with that chance (max 75%). */
@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {
    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int wilds$saveWear(int amount, int a, ServerLevel level, ServerPlayer player, Consumer<Item> onBreak) {
        if (player == null || amount <= 0) return amount;
        double chance = Math.min(0.75, SkillState.perk(player, Perks.TOOL_SAVER));
        if (chance <= 0) return amount;
        int left = 0;
        for (int i = 0; i < amount; i++) if (player.getRandom().nextDouble() >= chance) left++;
        return left;
    }
}
