package ua.wilds.core.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import ua.wilds.core.npc.Wanderer;

/**
 * A quest giver looks like a player with one of Minecraft's own skins (wide or slim arms) and carries a sign above
 * the name, bobbing, so they are noticed from afar: a gold "!" (a job for you), a gold "?" (your job is done, come for
 * the reward), a grey "?" (you are still at it); no sign once you did every job of this giver.
 */
public class WandererRenderer extends HumanoidMobRenderer<Wanderer, PlayerModel<Wanderer>> {
    private final PlayerModel<Wanderer> wide, slim;

    public WandererRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        wide = model;
        slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    public ResourceLocation getTextureLocation(Wanderer e) {
        return e.skin();
    }

    public void render(Wanderer e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        model = e.slim() ? slim : wide;
        super.render(e, yaw, partial, pose, buffers, light);
        int sign = e.mark();
        if (e.talking() || e.isInvisible() || sign == 0) return;
        pose.pushPose();
        float bob = Mth.sin((e.tickCount + partial) * 0.12f) * 0.06f;
        pose.translate(0, e.getBbHeight() + 0.95 + bob, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(-0.05f, -0.05f, 0.05f);
        Matrix4f m = pose.last().pose();
        Font font = getFont();
        String mark = sign == 1 ? "!" : "?";
        int colour = sign == 3 ? 0xFFA8A8A8 : 0xFFFFC23A;
        float x = -font.width(mark) / 2f;
        font.drawInBatch(mark, x, 0, colour, true, m, buffers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        pose.popPose();
    }
}
