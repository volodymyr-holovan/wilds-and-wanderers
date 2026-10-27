package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.net.Net;
import ua.wilds.core.skills.ClientAbilities;
import ua.wilds.core.skills.branch.CombatSkills;

/**
 * Inputs of the abilities that need the player's hands:
 * <ul>
 * <li>weapon throw: with the use button held and a melee weapon in hand, the attack button throws it;</li>
 * <li>heavy strike: holding its own key (the one left of 1 by default, so Better Combat's hold-to-attack keeps working) while aiming
 * at a creature, for the weapon's full recharge (at least a second), strikes by itself; a bar under the crosshair fills;</li>
 * <li>peacemaker: four quick presses of sneak.</li>
 * </ul>
 * The server checks everything again (ability, reach, sight, cooldowns).
 */
public final class SkillInput {
    public static final net.minecraft.client.KeyMapping HEAVY = new net.minecraft.client.KeyMapping("key.wilds_core.heavy_strike",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.wilds_core");

    private static int charge, need;
    private static boolean latched;
    private static long[] sneaks = new long[4];
    private static int sneakIdx;
    private static boolean wasSneaking;

    private SkillInput() {}

    /** attack pressed while holding use: throw instead of swinging */
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !e.isAttack() || !ClientAbilities.has("weapon_throw")) return;
        if (!mc.options.keyUse.isDown() || !CombatSkills.isMeleeWeapon(p.getMainHandItem())) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        PacketDistributor.sendToServer(new Net.Act("throw", 0));
    }

    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) {
            charge = 0;
            return;
        }
        // heavy strike
        boolean aiming = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) mc.hitResult).getEntity() instanceof LivingEntity;
        if (ClientAbilities.has("heavy_strike") && HEAVY.isDown() && aiming && !latched) {
            need = Math.max(20, (int) Math.ceil(p.getCurrentItemAttackStrengthDelay()));
            if (++charge >= need) {
                PacketDistributor.sendToServer(new Net.Act("heavy", ((EntityHitResult) mc.hitResult).getEntity().getId()));
                charge = 0;
                latched = true;
            }
        } else {
            charge = 0;
            if (!HEAVY.isDown()) latched = false;
        }
        // peacemaker: four sneaks within a second and a half
        boolean sneaking = mc.options.keyShift.isDown();
        if (sneaking && !wasSneaking && ClientAbilities.has("peacemaker")) {
            long now = p.level().getGameTime();
            sneaks[sneakIdx++ % 4] = now;
            long oldest = Long.MAX_VALUE;
            for (long t : sneaks) oldest = Math.min(oldest, t);
            if (sneakIdx >= 4 && now - oldest <= 30) {
                PacketDistributor.sendToServer(new Net.Act("pacify", 0));
                sneaks = new long[4];
                sneakIdx = 0;
            }
        }
        wasSneaking = sneaking;
    }

    /** the charge bar of the heavy strike, under the crosshair */
    public static void render(RenderGuiLayerEvent.Post e) {
        if (!e.getName().equals(VanillaGuiLayers.CROSSHAIR) || charge <= 4 || need <= 0) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = 32, x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 12;
        int filled = Math.min(w, w * charge / need);
        g.fill(x - 1, y - 1, x + w + 1, y + 3, 0x90000000);
        g.fill(x, y, x + filled, y + 2, charge >= need - 2 ? 0xFFFF5040 : 0xFFE0B040);
    }
}
