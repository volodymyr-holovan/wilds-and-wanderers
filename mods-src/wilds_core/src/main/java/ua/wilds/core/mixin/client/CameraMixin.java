package ua.wilds.core.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.wilds.core.client.npc.DialogueCamera;

/** During a conversation the camera is placed by DialogueCamera (detached, so the player's body is drawn, no hand). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void wilds$dialogue(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci) {
        Vec3 pos = DialogueCamera.position(partial);
        if (pos == null) return;
        this.detached = true;
        setRotation(DialogueCamera.yaw(partial), DialogueCamera.pitch(partial));
        setPosition(pos);
    }
}
