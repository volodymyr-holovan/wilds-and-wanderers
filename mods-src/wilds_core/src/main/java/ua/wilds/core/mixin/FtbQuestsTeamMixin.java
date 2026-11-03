package ua.wilds.core.mixin;

import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * Personal quests in parties. FTB Quests keeps quest progress per FTB team, so a party used to share all of it. When it
 * is FTB Quests that asks which team a player is in, the answer is the player's own team, never the party: the party
 * still gives everything else (the window, chat, the map, no friendly fire), and every player's quests stay theirs.
 * (ServerQuestFileMixin stops FTB Quests merging the player's progress into a party they join.)
 */
@Mixin(targets = "dev.ftb.mods.ftbteams.data.TeamManagerImpl", remap = false)
public abstract class FtbQuestsTeamMixin {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static boolean wilds$told;

    private static boolean wilds$fromQuests() {
        // who called the team manager: skip this, the injected handler and the manager's method itself
        String caller = WALKER.walk(s -> s.skip(3).findFirst()).map(StackWalker.StackFrame::getClassName).orElse("");
        boolean quests = caller.startsWith("dev.ftb.mods.ftbquests.");
        if (quests && !wilds$told) {
            wilds$told = true;
            ua.wilds.core.WildsCore.LOG.info("Personal quests: FTB Quests ({}) gets players' own teams", caller);
        }
        return quests;
    }

    @Inject(method = "getTeamForPlayerID", at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$own(UUID id, CallbackInfoReturnable<Optional<Team>> cir) {
        if (wilds$fromQuests()) cir.setReturnValue(((TeamManager) this).getPlayerTeamForPlayerID(id));
    }

    @Inject(method = "getTeamForPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void wilds$ownPlayer(ServerPlayer player, CallbackInfoReturnable<Optional<Team>> cir) {
        if (wilds$fromQuests()) cir.setReturnValue(((TeamManager) this).getPlayerTeamForPlayerID(player.getUUID()));
    }
}
