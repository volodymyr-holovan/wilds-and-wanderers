package ua.wilds.core.mixin.colony;

import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A worker took only tools no better than its hut allowed: a level 1 hut stone at most, level 2 iron, and every
 * enchantment counted on top (an iron pickaxe with Efficiency III was "level 4"). In this pack nearly every good tool
 * is enchanted or carries Apotheosis affixes, so a worker given a new pickaxe, in its inventory, its chest or its hut,
 * ignored it, asked for another, kept hitting with a wooden one that broke at once, and stood waiting for the next.
 * Now any tool of the right kind that is good enough is good: only the minimum still counts. (The separate rule that a
 * low hut cannot mine blocks needing a better tool, InteractionValidator, is kept: that is the colony's progression.)
 */
@Mixin(value = ItemStackUtils.class, remap = false)
public abstract class WorkerToolLevelMixin {
    @Inject(method = "verifyEquipmentLevel", at = @At("HEAD"), cancellable = true, require = 1)
    private static void wilds$noCeiling(ItemStack stack, int equipmentLevel, int minimalLevel, int maximumLevel, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(equipmentLevel + ItemStackUtils.getMaxEnchantmentLevel(stack) >= minimalLevel);
    }
}
