package ua.wilds.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The armour value as a number. Vanilla's bar draws ten icons and stops at 20, while gear of this pack goes well past
 * it (netherite, dragon scales, affixes, skills), so the player could not tell 21 from 45. The bar stays as players
 * know it; the exact value is written just left of it, in the game's font with its shadow, right-aligned so it never
 * runs into the bar. Same visibility as the bar itself: only when it is drawn (armour above 0, survival/adventure).
 */
public final class ArmorNumber {
    private static int barY = Integer.MIN_VALUE;

    private ArmorNumber() {}

    /** before the bar: where vanilla is about to draw it (the left column grows upwards as bars are added) */
    public static void pre(RenderGuiLayerEvent.Pre e) {
        if (!e.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) return;
        Minecraft mc = Minecraft.getInstance();
        barY = e.getGuiGraphics().guiHeight() - mc.gui.leftHeight;
    }

    /** tooling: -Dwilds_core.hudshot=N takes screenshots/wilds_ui_hud.png N seconds after joining a world */
    private static final int SHOT = Integer.getInteger("wilds_core.hudshot", -1);
    private static int inWorld;

    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (SHOT < 0 || mc.player == null) return;
        if (++inWorld == SHOT * 20)
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "wilds_ui_hud.png", mc.getMainRenderTarget(), m -> {});
    }

    public static void post(RenderGuiLayerEvent.Post e) {
        if (!e.getName().equals(VanillaGuiLayers.ARMOR_LEVEL) || barY == Integer.MIN_VALUE) return;
        int y = barY;
        barY = Integer.MIN_VALUE;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        int armor = p.getArmorValue();
        if (armor <= 0) return;
        GuiGraphics g = e.getGuiGraphics();
        String text = Integer.toString(armor);
        int x = g.guiWidth() / 2 - 91 - 3 - mc.font.width(text);
        g.drawString(mc.font, text, x, y + 1, 0xFFFFFF, true);
    }
}
