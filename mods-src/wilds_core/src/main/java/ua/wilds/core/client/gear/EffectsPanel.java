package ua.wilds.core.client.gear;

import com.google.common.collect.Ordering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/**
 * The player's effects (poison, weakness, potions...) on the equipment screen. The vanilla inventory draws them itself
 * (it is an effect-rendering screen); Accessories' screen is not, so without this they were simply not shown. Drawn
 * like the vanilla ones, in a column right of the screen's content ({@link GearScreens#layout} gives the place): wide
 * entries with the name and time where there is room, small squares where there is not (their name and time show on
 * hover), and a row above the content on a screen too narrow even for that.
 */
final class EffectsPanel {
    static final int WIDE = 120, SMALL = 32, ROW_ICON = 20;
    private static final ResourceLocation LARGE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation SMALL_BG = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");

    enum Mode { NONE, WIDE, SMALL, ROW }

    private static Mode mode = Mode.NONE;
    private static int px, py, ph;

    private EffectsPanel() {}

    /** the effects the vanilla inventory would show, in its order */
    static List<MobEffectInstance> visible(Player p) {
        if (p == null || p.getActiveEffects().isEmpty()) return List.of();
        return Ordering.natural().sortedCopy(p.getActiveEffects().stream().filter(ClientHooks::shouldRenderEffect).toList());
    }

    /** where to draw this frame: x, y of the top left, the height the column may take (for a row: unused) */
    static void place(Mode m, int x, int y, int h) {
        mode = m;
        px = x;
        py = y;
        ph = h;
    }

    /** the area the effects take (for JEI's stand-in): x, y, w, h; null when nothing is drawn */
    static int[] area(int count) {
        if (mode == Mode.NONE || count == 0) return null;
        if (mode == Mode.ROW) return new int[]{px, py, count * ROW_ICON, ROW_ICON};
        return new int[]{px, py, mode == Mode.WIDE ? WIDE : SMALL, Math.min(ph, step(count) * (count - 1) + 32)};
    }

    /** vertical step between entries: 33 like the vanilla screen, closer together when there are many */
    private static int step(int count) {
        if (count <= 1) return 33;
        return Math.max(12, Math.min(33, (ph - 32) / (count - 1)));
    }

    static void render(ScreenEvent.Render.Post e) {
        Minecraft mc = Minecraft.getInstance();
        List<MobEffectInstance> fx = visible(mc.player);
        if (mode == Mode.NONE || fx.isEmpty() || mc.level == null) return;
        GuiGraphics g = e.getGuiGraphics();
        float tickrate = mc.level.tickRateManager().tickrate();
        int mx = e.getMouseX(), my = e.getMouseY();
        MobEffectInstance hovered = null;
        g.pose().pushPose();
        // above the screen's own parts, under its tooltips (they are drawn at 400 and keep their pixels)
        g.pose().translate(0, 0, 200);
        if (mode == Mode.ROW) {
            for (int i = 0; i < fx.size(); i++) {
                int x = px + i * ROW_ICON, y = py;
                g.fill(x, y, x + ROW_ICON - 2, y + ROW_ICON - 2, 0xC0101014);
                g.blit(x, y, 0, 18, 18, mc.getMobEffectTextures().get(fx.get(i).getEffect()));
                if (mx >= x && mx < x + ROW_ICON && my >= y && my < y + ROW_ICON) hovered = fx.get(i);
            }
        } else {
            boolean wide = mode == Mode.WIDE;
            int step = step(fx.size());
            for (int i = 0; i < fx.size(); i++) {
                MobEffectInstance m = fx.get(i);
                int y = py + i * step;
                g.blitSprite(wide ? LARGE : SMALL_BG, px, y, wide ? WIDE : SMALL, 32);
                g.blit(px + (wide ? 6 : 7), y + 7, 0, 18, 18, mc.getMobEffectTextures().get(m.getEffect()));
                if (wide) {
                    Component name = mc.font.width(name(m)) > WIDE - 32 ? Component.literal(mc.font.plainSubstrByWidth(name(m).getString(), WIDE - 34) + "…") : name(m);
                    g.drawString(mc.font, name, px + 28, y + 6, 0xFFFFFF);
                    g.drawString(mc.font, MobEffectUtil.formatDuration(m, 1f, tickrate), px + 28, y + 16, 0x7F7F7F);
                }
                // the top entry overlaps the next one when they are close: the one under the cursor is the topmost hit
                int bottom = i == fx.size() - 1 ? y + 32 : y + Math.min(32, step);
                if (mx >= px && mx < px + (wide ? WIDE : SMALL) && my >= y && my < bottom) hovered = m;
            }
        }
        g.pose().popPose();
        // the name and time of an effect whose entry has no room for them (or is cut short)
        if (hovered != null && (mode != Mode.WIDE || mc.font.width(name(hovered)) > WIDE - 32)) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderComponentTooltip(mc.font, List.of(name(hovered), MobEffectUtil.formatDuration(hovered, 1f, tickrate)), mx, my);
            g.pose().popPose();
        }
    }

    static boolean inside(Screen s, double x, double y) {
        int[] a = area(visible(Minecraft.getInstance().player).size());
        return a != null && x >= a[0] && x < a[0] + a[2] && y >= a[1] && y < a[1] + a[3];
    }

    /** a click on the effects is theirs: it must not drop the carried item "outside the screen" */
    static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (inside(e.getScreen(), e.getMouseX(), e.getMouseY())) e.setCanceled(true);
    }

    private static Component name(MobEffectInstance m) {
        MutableComponent c = m.getEffect().value().getDisplayName().copy();
        if (m.getAmplifier() >= 1 && m.getAmplifier() <= 9) {
            c.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (m.getAmplifier() + 1)));
        }
        return c;
    }
}
