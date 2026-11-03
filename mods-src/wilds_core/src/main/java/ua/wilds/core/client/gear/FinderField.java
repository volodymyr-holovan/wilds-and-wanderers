package ua.wilds.core.client.gear;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.net.Net;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Item finder, the client half: a search field in the inventory. What the player types is matched against every
 * item's name in the game's language, its id and the same text typed in the other keyboard layout (ліжко / kspmrj),
 * the matching ids go to the server ({@link ua.wilds.core.finder.Finder}), and the places it answers with are outlined
 * in gold through walls for {@link #SHOW} seconds, with the count beside the field.
 */
public final class FinderField {
    private static final int SHOW = 20;
    static final int W = 120, H = 12;
    private static EditBox box;
    private static String sent = "";
    private static int typedAt = -1;
    private static List<BlockPos> hits = List.of();
    private static long shownUntil;
    private static String status = "";

    private FinderField() {}

    static void onInit(ScreenEvent.Init.Post e) {
        Minecraft mc = Minecraft.getInstance();
        box = new EditBox(mc.font, 0, -100, W, H, Component.literal("Знайти поруч"));
        box.setHint(Component.literal("Знайти в скринях…").withStyle(s -> s.withColor(0x8a8a8a)));
        box.setMaxLength(48);
        box.setBordered(true);
        box.setValue(sent);
        box.setTooltip(Tooltip.create(Component.literal("Напишіть назву предмета будь-якою мовою: підсвітяться скрині й інші сховища в радіусі "
                + ua.wilds.core.finder.Finder.RADIUS + " блоків, де він є, і такі предмети на землі. Руду й інші блоки не шукає.")));
        box.setResponder(v -> typedAt = mc.player == null ? 0 : mc.player.tickCount);
        e.addListener(box);
    }

    /** from the layout: top left corner, or MIN_VALUE to hide */
    static void place(int x, int y, int w) {
        if (box == null) return;
        box.visible = x > Integer.MIN_VALUE;
        box.setX(x);
        box.setY(y);
        if (w > 0) box.setWidth(w);
    }

    static boolean focused() {
        return box != null && box.visible && box.isFocused();
    }

    static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (!focused()) return;
        if (e.getKeyCode() == 256) {   // Escape: leave the field, the next one closes the screen
            box.setFocused(false);
            e.getScreen().setFocused(null);
            e.setCanceled(true);
            return;
        }
        box.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers());
        e.setCanceled(true);   // E, numbers and the rest type into the field, never close the screen or move items
    }

    static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        if (focused() && box.charTyped(e.getCodePoint(), e.getModifiers())) e.setCanceled(true);
    }

    /** after the screen: the field over the screen's panels (owo draws its layout over the screen's widgets), the count under it */
    static void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (box == null || !box.visible) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        box.render(g, mouseX, mouseY, partial);
        if (!status.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            g.drawString(mc.font, status, box.getX() + box.getWidth() - mc.font.width(status), box.getY() + H + 2, hits.isEmpty() ? 0xA0A0A0 : 0xF2C94C, true);
        }
        g.pose().popPose();
    }

    /** every client tick: send the search a moment after the player stops typing */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (TEST != null && mc.player != null) selfTest(mc);
        if (mc.player == null || box == null || typedAt < 0 || mc.player.tickCount - typedAt < 6) return;
        typedAt = -1;
        String q = box.getValue().trim();
        sent = q;
        if (q.length() < 2) {
            hits = List.of();
            status = "";
            return;
        }
        List<String> ids = match(q);
        if (ids.isEmpty()) {
            hits = List.of();
            status = "немає такого предмета";
            return;
        }
        status = "шукаю…";
        PacketDistributor.sendToServer(new Net.FinderQuery(ids));
    }

    /** -Dwilds_core.findertest=<what to type>: opens the inventory, types it, and takes screenshots of the field and the outlines */
    private static final String TEST = System.getProperty("wilds_core.findertest");
    private static int testT;

    private static void selfTest(Minecraft mc) {
        switch (++testT) {
            case 500 -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            case 540 -> {
                if (box != null) {
                    box.setFocused(true);
                    box.setValue(TEST);
                }
            }
            case 600 -> GearScreens.shot(mc, "finder_inventory");
            case 610 -> mc.setScreen(null);
            case 640 -> {
                ua.wilds.core.WildsCore.LOG.info("Wilds Core: finder self-test: '{}' -> {} ids, status '{}', hits {}", TEST, match(TEST).size(), status, hits);
                GearScreens.shot(mc, "finder_world");
            }
            default -> {
            }
        }
    }

    public static void answer(List<BlockPos> found) {
        hits = found;
        shownUntil = System.currentTimeMillis() + SHOW * 1000L;
        status = found.isEmpty() ? "поруч немає" : "знайдено: " + found.size();
    }

    static List<String> match(String query) {
        String q = query.toLowerCase(Locale.ROOT), alt = Layout.swap(q);
        List<String> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            String name = item.getDescription().getString().toLowerCase(Locale.ROOT);
            String path = id.getPath().replace('_', ' ');
            if (name.contains(q) || name.contains(alt) || path.contains(q) || id.toString().equals(q)) {
                out.add(id.toString());
                if (out.size() >= 512) break;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- outlines through walls

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || hits.isEmpty()) return;
        long left = shownUntil - System.currentTimeMillis();
        if (left <= 0) {
            hits = List.of();
            status = "";
            return;
        }
        float a = (float) Math.min(1.0, left / 3000.0);
        Vec3 cam = e.getCamera().getPosition();
        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
        var vc = buf.getBuffer(Lines.THROUGH_WALLS);
        for (BlockPos p : hits) {
            AABB bb = new AABB(p).inflate(0.02).move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(pose, vc, bb, 0.95f, 0.79f, 0.30f, a);
        }
        buf.endBatch(Lines.THROUGH_WALLS);
    }

    /** a line render type without the depth test: the outline shows through the walls */
    private static final class Lines extends RenderType {
        static final RenderType THROUGH_WALLS = create("wilds_core_finder", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536,
                false, false, CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(OptionalDouble.of(2.5)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false));

        private Lines(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean b, boolean c, Runnable a, Runnable z) {
            super(n, f, m, s, b, c, a, z);
        }
    }

    /** text typed with the wrong keyboard layout (Ukrainian keys on an English layout and back) */
    static final class Layout {
        private static final String EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`", UK = "йцукенгшщзхїфівапролджєячсмитьбю'";

        static String swap(String s) {
            StringBuilder b = new StringBuilder(s.length());
            for (char ch : s.toCharArray()) {
                int i = EN.indexOf(ch), j = UK.indexOf(ch);
                b.append(i >= 0 ? UK.charAt(i) : j >= 0 ? EN.charAt(j) : ch);
            }
            return b.toString();
        }
    }
}
