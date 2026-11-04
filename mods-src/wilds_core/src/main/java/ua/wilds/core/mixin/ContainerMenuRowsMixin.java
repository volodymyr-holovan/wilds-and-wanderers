package ua.wilds.core.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.wilds.core.inventory.ExtraRows;
import ua.wilds.core.skills.branch.BagSkills;

/**
 * Shift-click and the extra inventory rows (see ExtraRows). In every menu of every mod: after a shift-click, what the
 * menu could not place goes into the rows (the server). In the equipment screen, the one menu with the rows as slots: a
 * shift-click on a row slot is moved here, before the menu's own rules, and the menu's own moving never reaches the rows
 * (they are its last slots, and "from the inventory to the end of the menu", filling from the end, would have put
 * things into them before the hotbar).
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuRowsMixin {
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void wilds$fromRow(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (type != ClickType.QUICK_MOVE) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size() || !(menu.slots.get(slotId) instanceof BagSkills.RowSlot row)) return;
        ExtraRows.quickMoveFromRow(menu, player, row);
        ci.cancel();
    }

    @Inject(method = "doClick", at = @At("RETURN"))
    private void wilds$restToRows(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (type != ClickType.QUICK_MOVE) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size()) return;
        ExtraRows.quickMoveRest(menu, player, menu.slots.get(slotId));
    }

    @ModifyVariable(method = "moveItemStackTo", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int wilds$notIntoRows(int end, net.minecraft.world.item.ItemStack stack, int start) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        int first = menu.slots.size();
        while (first > 0 && menu.slots.get(first - 1) instanceof BagSkills.RowSlot) first--;
        return start < first ? Math.min(end, first) : end;
    }
}
