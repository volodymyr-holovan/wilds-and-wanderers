package ua.wilds.core.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ua.wilds.core.skills.branch.BagSkills;

/** The box of the player's inventory in the equipment screen is 76 high (three rows, a gap, the hotbar): 18 more per extra row. */
@Mixin(targets = "io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen", remap = false)
public abstract class AccessoriesRowsScreenMixin {
    @ModifyConstant(method = "build(Lio/wispforest/owo/ui/container/FlowLayout;)V", constant = @Constant(intValue = 76), remap = false)
    private int wilds$taller(int height) {
        int rows = 0;
        for (var s : ((AbstractContainerScreen<?>) (Object) this).getMenu().slots)
            if (s instanceof BagSkills.RowSlot r && r.isActive()) rows++;
        return height + (rows + 8) / 9 * 18;
    }
}
