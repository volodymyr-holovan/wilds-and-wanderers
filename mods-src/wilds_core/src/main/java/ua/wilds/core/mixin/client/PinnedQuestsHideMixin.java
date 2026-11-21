package ua.wilds.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The pinned quests are a HUD for walking around: with a screen open (inventory, a chest, JEI's list on the right) they
 * showed through it at large GUI scales and covered its items. Now they hide while any screen is open (22.11).
 */
@Mixin(targets = "dev.ftb.mods.ftbquests.client.PinnedQuestsTracker", remap = false)
public abstract class PinnedQuestsHideMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void wilds$hideUnderScreens(Minecraft mc, GuiGraphics g, CallbackInfo ci) {
        if (mc.screen != null) ci.cancel();
    }
}
