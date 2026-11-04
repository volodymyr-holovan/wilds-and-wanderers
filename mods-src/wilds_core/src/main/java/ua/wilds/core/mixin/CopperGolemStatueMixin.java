package ua.wilds.core.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

/**
 * Friends and Foes 5.0.3: a copper golem that oxidizes all the way turns into a statue and, on the server too, sends
 * its walking animation "to the server" (a client-only message): the server crashes ("Cannot send serverbound payloads
 * on the server", Ticking entity) and keeps crashing on every start while the golem is loaded. Only the client sends it
 * now; the server has nothing to sync there.
 */
@Mixin(targets = "com.faboslav.friendsandfoes.common.entity.CopperGolemEntity", remap = false)
public abstract class CopperGolemStatueMixin {
    @Redirect(method = "becomeStatue", remap = false, at = @At(value = "INVOKE",
            target = "Lcom/faboslav/friendsandfoes/common/network/packet/SyncCopperGolemWalkAnimationPacket;sendToServer(Ljava/util/UUID;FF)V"))
    private void wilds$onlyClient(UUID id, float a, float b) {
        if (((Entity) (Object) this).level().isClientSide())
            com.faboslav.friendsandfoes.common.network.packet.SyncCopperGolemWalkAnimationPacket.sendToServer(id, a, b);
    }
}
