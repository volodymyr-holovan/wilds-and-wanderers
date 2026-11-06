package ua.wilds.core.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click from a worn armour piece in the equipment screen: Accessories counts its armour slots from 43, but the
 * helmet's is 42 (the slots go helmet, cosmetic helmet, chestplate...), so it took the helmet for an inventory item to
 * put on, where it already was, and nothing moved: the helmet could not be taken off with shift-click.
 */
@Mixin(targets = "io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu", remap = false)
public abstract class AccessoriesArmorShiftMixin extends AbstractContainerMenu {
    protected AccessoriesArmorShiftMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$takeOff(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= slots.size()) return;
        Slot slot = slots.get(index);
        boolean worn = slot.container.getClass().getSimpleName().contains("SlotAccess") && slot.hasItem()
                && player.getEquipmentSlotForItem(slot.getItem()).isArmor() && slot.getItem() == player.getItemBySlot(player.getEquipmentSlotForItem(slot.getItem()));
        if (!worn) return;
        ItemStack stack = slot.getItem();
        int from = Integer.MAX_VALUE, to = -1;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.container instanceof Inventory && s.getContainerSlot() < 36) {
                from = Math.min(from, i);
                to = Math.max(to, i);
            }
        }
        ItemStack copy = stack.copy();
        if (to < 0 || !moveItemStackTo(stack, from, to + 1, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.setChanged();
        cir.setReturnValue(copy);
    }
}
