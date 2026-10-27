package ua.wilds.core.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.wilds.core.client.music.ReactiveMusicBridge;

/**
 * Reactive Music decides its situation flags in SongPicker.tickEventMap(); right after that the music director corrects
 * two of them: BOSS (it also sees bosses whose mod draws its own boss bar, like Twilight Forest) and NEARBY_MOBS, which in
 * this pack means "the player is fighting" rather than "a monster is somewhere near".
 */
@Mixin(targets = "circuitlord.reactivemusic.SongPicker", remap = false)
public abstract class RMSongPickerMixin {
    @Inject(method = "tickEventMap", at = @At("TAIL"), remap = false)
    private static void wilds$situation(CallbackInfo ci) {
        ReactiveMusicBridge.applyFlags();
    }
}
