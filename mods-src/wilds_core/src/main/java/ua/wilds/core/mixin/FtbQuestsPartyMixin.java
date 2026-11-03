package ua.wilds.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Personal quests in parties (see FtbQuestsTeamMixin): joining or leaving a party neither merges nor moves quest progress. */
@Mixin(targets = "dev.ftb.mods.ftbquests.quest.ServerQuestFile", remap = false)
public abstract class FtbQuestsPartyMixin {
    @Inject(method = "playerChangedTeam", at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$keepOwn(dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent e, CallbackInfo ci) {
        ci.cancel();
    }
}
