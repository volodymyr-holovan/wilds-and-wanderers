package ua.wilds.core.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.skills.branch.BagSkills;

/**
 * The extra inventory rows of the skill extra_row, as slots of the equipment screen's menu (Accessories). Both sides
 * always add all of them (the server and the client must agree on the menu's slots whatever the client knows yet);
 * rows the player has not unlocked are inactive: not drawn, not clickable, nothing goes in. Shift-click moves from
 * them to the inventory.
 */
@Mixin(targets = "io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu", remap = false)
public abstract class AccessoriesRowsMenuMixin extends AbstractContainerMenu {
    protected AccessoriesRowsMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"), remap = false)
    private void wilds$rows(int id, Inventory inv, LivingEntity target, CallbackInfo ci) {
        Player p = inv.player;
        if (target != null && target != p) return;
        // a client of an older pack would have fewer slots than the server and be thrown out when the menu opens
        if (p instanceof net.minecraft.server.level.ServerPlayer sp && (sp.connection == null || !sp.connection.hasChannel(ua.wilds.core.net.Net.RowsV1.TYPE))) return;
        var h = BagSkills.rows(p);
        for (int i = 0; i < h.getSlots(); i++) addSlot(new BagSkills.RowSlot(h, i, p, -2000, -2000));
    }

    /**
     * Shift-click from an extra row: into the inventory. Shift-click from a worn armour piece too: Accessories counts its
     * armour slots from 43, but the helmet's is 42 (the slots go helmet, cosmetic helmet, chestplate...), so it took the
     * helmet for an inventory item to put on, where it already was, and nothing moved: the helmet could not be taken off
     * with shift-click.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$fromRow(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= slots.size()) return;
        Slot slot = slots.get(index);
        boolean worn = slot.container.getClass().getSimpleName().contains("SlotAccess") && slot.hasItem()
                && player.getEquipmentSlotForItem(slot.getItem()).isArmor() && slot.getItem() == wilds$worn(player, slot.getItem());
        if (!worn) return;   // a row slot never gets here: ContainerMenuRowsMixin moves it before the menu is asked
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
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

    /** the stack the player wears in the armour slot that item goes to (the same object when the slot holds what is worn) */
    private static ItemStack wilds$worn(Player player, ItemStack stack) {
        return player.getItemBySlot(player.getEquipmentSlotForItem(stack));
    }
}
