package ua.wilds.core.mixin.client;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.skills.branch.BagSkills;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The extra inventory rows (skill extra_row) in the equipment screen: Accessories builds the player's inventory as rows
 * of nine slots, the hotbar last with a gap above it; the unlocked extra rows go in right above that gap, so they look
 * like more rows of the inventory.
 */
@Mixin(targets = "io.wispforest.accessories.client.gui.components.ComponentUtils", remap = false)
public abstract class AccessoriesPlayerInvMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "createPlayerInv", at = @At("RETURN"), remap = false)
    private static void wilds$rows(int start, int end, Function slotComponent, Consumer enable, CallbackInfoReturnable<Component> cir) {
        if (!(cir.getReturnValue() instanceof FlowLayout layout)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        List<Integer> rows = new ArrayList<>();
        List<Slot> slots = mc.player.containerMenu.slots;
        for (int i = 0; i < slots.size(); i++) if (slots.get(i) instanceof BagSkills.RowSlot s && s.isActive()) rows.add(i);
        if (rows.isEmpty()) return;
        int at = Math.min(3, layout.children().size());
        for (int r = 0; r < rows.size(); r += 9) {
            FlowLayout row = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            for (int k = r; k < Math.min(r + 9, rows.size()); k++) {
                Object c = slotComponent.apply(rows.get(k));
                if (c instanceof BaseComponent b) b.margins(Insets.of(1));
                if (c instanceof Component comp) row.child(comp);
                enable.accept(rows.get(k));
            }
            layout.child(at++, row);
        }
    }
}
