package ua.wilds.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

/**
 * FTB Teams finds the player among the players it knows by the launcher session's UUID. This server runs in offline
 * mode (EasyLogin): it gives every player the offline UUID of their name, never the session's, so FTB Teams never found
 * the player ("Local player id ... was not found"), and the party window hid inviting, allies, kicking and promoting:
 * parties worked only by commands. It now looks for the UUID the server gave the player.
 */
@Mixin(targets = "dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl", remap = false)
public abstract class FtbTeamsSelfMixin {
    @Redirect(method = "initSelfDetails", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/User;getProfileId()Ljava/util/UUID;"), remap = false)
    private UUID wilds$serverId(User user) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.getConnection().getLocalGameProfile() != null) return mc.getConnection().getLocalGameProfile().getId();
        return mc.player != null ? mc.player.getUUID() : user.getProfileId();
    }
}
