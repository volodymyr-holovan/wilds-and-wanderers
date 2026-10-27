package ua.wilds.core.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The equipment screen (Accessories) is a recipe-book menu with a crafting grid, but reports its result slot as -1. The
 * vanilla recipe book, which the pack puts on that screen, places a recipe's ingredients by counting grid slots from the
 * start of the menu and skipping the result slot: with -1 it would put the first ingredient into the result slot. This
 * reports where the result slot really is (the first slot, as in the vanilla inventory; found, not assumed, so a mod
 * that enlarges the grid keeps working).
 */
@Mixin(targets = "io.wispforest.accessories.menu.variants.AccessoriesMenuBase", remap = false)
public abstract class AccessoriesResultSlotMixin {
    @Inject(method = "getResultSlotIndex", at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$resultSlot(CallbackInfoReturnable<Integer> cir) {
        var slots = ((AbstractContainerMenu) (Object) this).slots;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) instanceof ResultSlot) {
                cir.setReturnValue(i);
                return;
            }
        }
    }
}
