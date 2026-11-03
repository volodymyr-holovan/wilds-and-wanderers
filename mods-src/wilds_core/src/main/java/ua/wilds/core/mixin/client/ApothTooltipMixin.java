package ua.wilds.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Apotheosis' buttons (the world tier screen's tiers, the augmenting table) drew their tooltips in the middle of the
 * screen's own drawing, not after it as the vanilla game does. With the pack's batched rendering the tooltip's box
 * could cover its own text: players saw empty dark boxes. They now go to the screen's tooltip for this frame, drawn
 * last, over everything; without a screen (never, in practice) as before.
 */
@Mixin(targets = {"dev.shadowsoffire.apotheosis.client.SimpleTexButton"}, remap = false)
public abstract class ApothTooltipMixin {
    @Redirect(method = "renderToolTip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"), remap = false, require = 0)
    private void wilds$later(GuiGraphics g, Font font, List<Component> lines, int x, int y) {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) {
            g.renderComponentTooltip(font, lines, x, y);
            return;
        }
        List<FormattedCharSequence> seq = lines.stream().map(Component::getVisualOrderText).toList();
        screen.setTooltipForNextRenderPass(seq);
    }
}
