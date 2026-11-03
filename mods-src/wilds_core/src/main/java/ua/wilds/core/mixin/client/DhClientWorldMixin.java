package ua.wilds.core.mixin.client;

import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distant Horizons keeps one LOD database per dimension, in a folder named after the server (as the player named it in
 * the server list, URL-encoded: "Український сервер" becomes 100+ characters) and the dimension. Every Nomadic Tents
 * tent is its own dimension named by a UUID, so inside a yurt the path passes Windows' 260-character limit, the
 * database cannot be opened, and DH retries on the render thread every frame: the game stutters until the player
 * rejoins and the chat fills with "Client level loading failed". A tent interior is a few chunks with nothing to see
 * far away, so DH simply does not load tent dimensions (null is what DH itself returns for a level it could not load).
 */
@Mixin(targets = "com.seibel.distanthorizons.core.world.DhClientWorld", remap = false)
public abstract class DhClientWorldMixin {
    @Inject(method = "getOrLoadLevel(Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)Lcom/seibel/distanthorizons/core/level/DhClientLevel;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$noLodsInTents(ILevelWrapper level, CallbackInfoReturnable<DhClientLevel> cir) {
        String dim = level == null ? null : level.getDimensionName();
        // "nomadictents:<uuid>" (DH's own folder for it is "<key>@nomadictents@@<uuid>")
        if (dim != null && (dim.startsWith("nomadictents:") || dim.contains("nomadictents@@"))) cir.setReturnValue(null);
    }
}
