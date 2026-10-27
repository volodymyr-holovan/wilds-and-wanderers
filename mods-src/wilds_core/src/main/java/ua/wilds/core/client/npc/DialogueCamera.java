package ua.wilds.core.client.npc;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The camera of a conversation: it glides from the player's eyes to a close shot of the NPC's face, a little to the
 * side, as in Fallout or The Outer Worlds, and back when the conversation ends. CameraMixin applies it.
 */
public final class DialogueCamera {
    private static final float GLIDE = 14f;
    private static Entity npc;
    private static boolean closing;
    private static long started;
    private static Vec3 fromPos, toPos;
    private static float fromYaw, fromPitch, toYaw, toPitch;

    private DialogueCamera() {}

    public static boolean active() {
        return npc != null;
    }

    public static void start(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (npc == null || closing) {
            fromPos = mc.gameRenderer.getMainCamera().getPosition();
            fromYaw = mc.gameRenderer.getMainCamera().getYRot();
            fromPitch = mc.gameRenderer.getMainCamera().getXRot();
            started = mc.level.getGameTime();
        }
        npc = target;
        closing = false;
        aim();
    }

    /** where the camera should be: in front of the NPC's face, toward the player and a bit to the side */
    private static void aim() {
        Minecraft mc = Minecraft.getInstance();
        Vec3 face = npc.getEyePosition();
        Vec3 toPlayer = mc.player.getEyePosition().subtract(face).multiply(1, 0, 1);
        if (toPlayer.lengthSqr() < 1e-4) toPlayer = Vec3.directionFromRotation(0, npc.getYRot());
        toPlayer = toPlayer.normalize();
        Vec3 side = new Vec3(-toPlayer.z, 0, toPlayer.x);
        toPos = face.add(toPlayer.scale(1.7)).add(side.scale(0.55)).add(0, 0.05, 0);
        Vec3 look = face.add(side.scale(-0.15)).subtract(toPos);
        toYaw = (float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
        toPitch = (float) (-Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
    }

    public static void stop() {
        if (npc == null || closing) return;
        Minecraft mc = Minecraft.getInstance();
        closing = true;
        started = mc.level == null ? 0 : mc.level.getGameTime();
    }

    private static float progress(float partial) {
        Minecraft mc = Minecraft.getInstance();
        float t = Mth.clamp((mc.level.getGameTime() - started + partial) / GLIDE, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** null when the normal camera applies */
    public static Vec3 position(float partial) {
        if (npc == null || Minecraft.getInstance().level == null) return null;
        if (!npc.isAlive()) {
            npc = null;
            return null;
        }
        float t = progress(partial);
        if (closing) {
            if (t >= 1) {
                npc = null;
                return null;
            }
            Vec3 eye = Minecraft.getInstance().player.getEyePosition(partial);
            return toPos.lerp(eye, t);
        }
        return fromPos.lerp(toPos, t);
    }

    public static float yaw(float partial) {
        float t = progress(partial);
        if (closing) return Mth.rotLerp(t, toYaw, Minecraft.getInstance().player.getViewYRot(partial));
        return Mth.rotLerp(t, fromYaw, toYaw);
    }

    public static float pitch(float partial) {
        float t = progress(partial);
        if (closing) return Mth.lerp(t, toPitch, Minecraft.getInstance().player.getViewXRot(partial));
        return Mth.lerp(t, fromPitch, toPitch);
    }
}
