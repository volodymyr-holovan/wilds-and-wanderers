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

    /**
     * "inv": open the inventory (the pack turns it into the equipment screen); "quick:N": shift-click the slot holding
     * the player's inventory slot N (39 the helmet, 40 the off hand, 0-8 the hotbar); "click:N" a plain left click (pick up or put down); "dump": the menu's slots in the log
     */
    private static void inventoryStep(Minecraft mc, String what) {
        var player = mc.player;
        if (what.equals("inv")) {
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(player));
            return;
        }
        if (what.startsWith("select:")) {   // "select:N": the hotbar slot in hand
            player.getInventory().selected = Integer.parseInt(what.substring(7));
            return;
        }
        if (what.equals("use")) {   // use the item in the main hand (opens a backpack)
            mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            return;
        }
        if (what.startsWith("sneak:")) {   // "sneak:on" / "sneak:off": hold the sneak key
            mc.options.keyShift.setDown(what.endsWith("on"));
            return;
        }
        if (what.equals("close")) {
            player.closeContainer();
            mc.setScreen(null);   // also the pause menu of a window that lost the focus
            return;
        }
        if (what.startsWith("shot:")) {   // "shot:name": a screenshot now
            Screenshot.grab(mc.gameDirectory, "wilds_ui_" + what.substring(5) + ".png", mc.getMainRenderTarget(), m -> {});
            return;
        }
        if (what.equals("frame") || what.equals("frameshift")) {   // right click the nearest item frame (sneaking: "frameshift")
            net.minecraft.world.entity.decoration.ItemFrame frame = null;
            for (var f : mc.level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class, player.getBoundingBox().inflate(5)))
                if (frame == null || f.distanceToSqr(player) < frame.distanceToSqr(player)) frame = f;
            WildsCore.LOG.info("Wilds Core: command self-test frame {}", frame);
            if (frame == null) return;
            mc.gameMode.interact(player, frame, net.minecraft.world.InteractionHand.MAIN_HAND);
            return;
        }
        var menu = player.containerMenu;
        if (what.equals("dump")) {
            StringBuilder sb = new StringBuilder(menu.getClass().getSimpleName()).append(" carried=").append(menu.getCarried()).append(':');
            for (int i = 0; i < menu.slots.size(); i++) {
                var s = menu.getSlot(i);
                if (s.hasItem()) sb.append(' ').append(i).append('[').append(s.container.getClass().getSimpleName()).append('#').append(s.getContainerSlot())
                        .append("]=").append(s.getItem());
            }
            WildsCore.LOG.info("Wilds Core: command self-test menu {}", sb);
            return;
        }
        String[] kv = what.split(":");
        int target = kv[1].startsWith("m") ? Integer.parseInt(kv[1].substring(1)) : -1;   // "mN": the menu's own slot N
        for (int i = 0; i < menu.slots.size() && target < 0; i++) {
            var s = menu.getSlot(i);
            if (s.container instanceof net.minecraft.world.entity.player.Inventory && s.getContainerSlot() == Integer.parseInt(kv[1])) target = i;
        }
        WildsCore.LOG.info("Wilds Core: command self-test {} -> menu slot {}", what, target);
        if (target < 0) return;
        if (kv[0].equals("mouse") && mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr) {
            // a real left click on the slot, through the screen (its hovering, its "clicked outside" rules)
            var slot = menu.getSlot(target);
            double x = scr.getGuiLeft() + slot.x + 8, y = scr.getGuiTop() + slot.y + 8;
            scr.mouseClicked(x, y, 0);
            scr.mouseReleased(x, y, 0);
            return;
        }
        mc.gameMode.handleInventoryMouseClick(menu.containerId, target, 0,
                kv[0].equals("quick") ? net.minecraft.world.inventory.ClickType.QUICK_MOVE : net.minecraft.world.inventory.ClickType.PICKUP, player);
    }

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
        if (step.startsWith("=")) {   // inventory steps: "=inv" opens it, "=quick:N" shift-clicks inventory slot N, "=dump" logs the menu
            inventoryStep(mc, step.substring(1));
            return;
        }
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
