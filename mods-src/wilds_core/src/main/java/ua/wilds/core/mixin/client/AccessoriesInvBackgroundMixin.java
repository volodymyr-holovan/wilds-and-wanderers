package ua.wilds.core.mixin.client;

import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The extra inventory rows (skill extra_row): Accessories draws the player's inventory box as one picture, 88 high, with
 * the slot frames in it (a border, three rows, a gap, the hotbar with the off hand). A taller box stretched it, and the
 * frames no longer met the items. Now the picture is drawn in parts: the border and the three rows, the third row
 * again for each extra row, then the gap and the hotbar, every slot with its frame at any interface scale.
 */
@Mixin(targets = "io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen", remap = false)
public abstract class AccessoriesInvBackgroundMixin {
    private static final int PICTURE = 88, ROWS_END = 60, ROW = 18;

    @Redirect(method = "lambda$build$8", at = @At(value = "INVOKE",
            target = "Lio/wispforest/owo/ui/core/OwoUIDrawContext;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), remap = false, require = 0)
    private void wilds$rows(OwoUIDrawContext g, ResourceLocation tex, int x, int y, int w, int h, float u, float v, int rw, int rh, int tw, int th) {
        if (h <= rh || rh != PICTURE) {
            g.blit(tex, x, y, w, h, u, v, rw, rh, tw, th);
            return;
        }
        g.blit(tex, x, y, w, ROWS_END, u, v, rw, ROWS_END, tw, th);
        int extra = (h - PICTURE) / ROW, at = y + ROWS_END;
        for (int i = 0; i < extra; i++, at += ROW) g.blit(tex, x, at, w, ROW, u, v + ROWS_END - ROW, rw, ROW, tw, th);
        g.blit(tex, x, at, w, PICTURE - ROWS_END, u, v + ROWS_END, rw, PICTURE - ROWS_END, tw, th);
    }
}
