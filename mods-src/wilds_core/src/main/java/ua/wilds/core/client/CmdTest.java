package ua.wilds.core.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import ua.wilds.core.WildsCore;

/**
 * -Dwilds_core.cmdtest=step;step...: from 35 s after joining, every 10 s, the player runs the next step: a command
 * (without the slash, "+" for spaces), "@key.name" to press that key binding, "!a.Class#method" to call it, or "#x,y,z" to dig that block; 3 s after the last, a screenshot wilds_ui_cmdtest.png
 * (tests of other mods' screens that open from commands and keys)
 */
public final class CmdTest {
    private static final String STEPS = System.getProperty("wilds_core.cmdtest");
    private static int t, dug;
    private static net.minecraft.core.BlockPos digging;

    private CmdTest() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (STEPS == null || mc.player == null || mc.getConnection() == null) return;
        String[] all = STEPS.split(";");
        t++;
        if (t == 700 + (all.length - 1) * 200 + 60) {
            Screenshot.grab(mc.gameDirectory, "wilds_ui_cmdtest.png", mc.getMainRenderTarget(), m -> {});
            WildsCore.LOG.info("Wilds Core: command self-test screenshot of {}", mc.screen == null ? "no screen" : mc.screen.getClass().getName());
        }
        if (digging != null && mc.gameMode != null) {
            boolean more = mc.gameMode.continueDestroyBlock(digging, net.minecraft.core.Direction.UP);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (!more || ++dug > 120 || mc.level.getBlockState(digging).isAir()) {
                WildsCore.LOG.info("Wilds Core: command self-test dug {} for {} ticks: now {}", digging, dug, mc.level.getBlockState(digging));
                digging = null;
            }
        }
        if (t < 700 || (t - 700) % 200 != 0 || (t - 700) / 200 >= all.length) return;
        String step = all[(t - 700) / 200].trim().replace('+', ' ');   // "+" for spaces: JVM arguments cannot hold them here
        WildsCore.LOG.info("Wilds Core: command self-test step {}", step);
        if (step.startsWith("*")) {   // "*some.Screen": open that screen (a public constructor without arguments)
            try {
                mc.setScreen((net.minecraft.client.gui.screens.Screen) Class.forName(step.substring(1)).getConstructor().newInstance());
            } catch (ReflectiveOperationException | RuntimeException ex) {
                WildsCore.LOG.warn("Wilds Core: command self-test cannot open {}", step, ex);
            }
            return;
        }
        if (step.startsWith("^")) {   // "^N": the mouse onto the N-th widget of the open screen (to see its tooltip)
            if (mc.screen == null) return;
            var widgets = mc.screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractWidget)
                    .map(w -> (net.minecraft.client.gui.components.AbstractWidget) w).toList();
            int n = Integer.parseInt(step.substring(1));
            if (n >= widgets.size()) return;
            var w = widgets.get(n);
            double scale = mc.getWindow().getGuiScale();
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), (w.getX() + w.getWidth() / 2.0) * scale, (w.getY() + w.getHeight() / 2.0) * scale);
            WildsCore.LOG.info("Wilds Core: command self-test mouse on {} of {}: {} at {},{}", n, widgets.size(), w.getClass().getSimpleName(), w.getX(), w.getY());
            return;
        }
        if (step.startsWith("#")) {   // "#x,y,z": dig that block for up to six seconds
            String[] c = step.substring(1).split(",");
            digging = new net.minecraft.core.BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]));
            dug = 0;
            if (mc.gameMode != null) mc.gameMode.startDestroyBlock(digging, net.minecraft.core.Direction.UP);
            return;
        }
        if (step.startsWith("!")) {   // "!some.Class#staticMethod": a mod's own way of opening its screen
            try {
                String[] cm = step.substring(1).split("#");
                Class.forName(cm[0]).getMethod(cm[1]).invoke(null);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                WildsCore.LOG.warn("Wilds Core: command self-test cannot call {}", step, ex);
            }
            return;
        }
        if (!step.startsWith("@")) {
            mc.getConnection().sendCommand(step);
            return;
        }
        for (KeyMapping k : mc.options.keyMappings) {
            if (!k.getName().equals(step.substring(1))) continue;
            try {
                var f = KeyMapping.class.getDeclaredField("clickCount");
                f.setAccessible(true);
                f.setInt(k, f.getInt(k) + 1);
            } catch (ReflectiveOperationException ex) {
                WildsCore.LOG.warn("Wilds Core: command self-test cannot press {}", step, ex);
            }
        }
    }
}
