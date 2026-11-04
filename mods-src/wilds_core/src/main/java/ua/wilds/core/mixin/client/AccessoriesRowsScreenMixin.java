package ua.wilds.core.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.wilds.core.skills.branch.BagSkills;

/**
 * The extra inventory rows in the equipment screen: the box of the player's inventory is 76 high (three rows, a gap,
 * the hotbar), 18 more per extra row; the off hand slot beside the hotbar sits under two buttons and a gap of 18, which
 * grows by the same, so it stays level with the hotbar.
 */
@Mixin(targets = "io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen", remap = false)
public abstract class AccessoriesRowsScreenMixin {
    @ModifyConstant(method = "build(Lio/wispforest/owo/ui/container/FlowLayout;)V", constant = @Constant(intValue = 76), remap = false)
    private int wilds$taller(int height) {
        return height + wilds$extra();
    }

    /** the gap above the off hand slot: only its height grows (sizing(Sizing) would make it wider too) */
    @Redirect(method = "build(Lio/wispforest/owo/ui/container/FlowLayout;)V", at = @At(value = "INVOKE",
            target = "Lio/wispforest/owo/ui/component/SpacerComponent;sizing(Lio/wispforest/owo/ui/core/Sizing;)Lio/wispforest/owo/ui/core/Component;",
            ordinal = 0), remap = false, require = 0)
    private io.wispforest.owo.ui.core.Component wilds$offHandLower(io.wispforest.owo.ui.component.SpacerComponent spacer, io.wispforest.owo.ui.core.Sizing size) {
        int extra = wilds$extra();
        return extra == 0 ? spacer.sizing(size) : spacer.sizing(size, io.wispforest.owo.ui.core.Sizing.fixed(18 + extra));
    }

    private int wilds$extra() {
        int slots = 0;
        for (var s : ((AbstractContainerScreen<?>) (Object) this).getMenu().slots)
            if (s instanceof BagSkills.RowSlot r && r.isActive()) slots++;
        return (slots + 8) / 9 * 18;
    }
}
