package ua.wilds.core.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.client.RowsPanel;

/**
 * The extra inventory rows panel under the screen of any menu (RowsPanel, ExtraRows): room for it (a smaller interface
 * scale while the screen is open when it would not fit), and a click on it is never a click outside the screen (which
 * would drop the cursor item).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenRowsMixin {
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int topPos;
    @Shadow protected int imageHeight;

    /**
     * a screen too low for the rows panel under it: the interface scale goes down while it is open. The screen itself
     * is never moved: many screens (vanilla's chest among them) draw their background centred by themselves, not where
     * the screen says it is.
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void wilds$fit(CallbackInfo ci) {
        var self = (AbstractContainerScreen<?>) (Object) this;
        int bottom = RowsPanel.bottom(self);   // the lowest point of the rows panel, from the screen's top
        if (bottom <= imageHeight || topPos + bottom <= self.height - 2) return;
        // centred, the screen starts at (H - imageHeight) / 2: the panel fits when that plus bottom <= H - 2
        ua.wilds.core.client.gear.GearScreens.needHeight(self, 2 * bottom - imageHeight + 4);
    }

    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void wilds$panelIsInside(double x, double y, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && RowsPanel.over((AbstractContainerScreen<?>) (Object) this, x, y)) cir.setReturnValue(false);
    }
}
