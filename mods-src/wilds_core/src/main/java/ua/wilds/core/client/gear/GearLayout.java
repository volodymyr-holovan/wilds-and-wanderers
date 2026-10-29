package ua.wilds.core.client.gear;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.gui.screens.Screen;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Field;

/**
 * The parts of the equipment screen's own layout (Accessories draws it with owo-lib) the pack changes; only touched
 * when that screen is open, so without Accessories this class is never loaded.
 * <ul>
 * <li>its content moves to make room for the recipe book beside it, the effects right of it and the attributes under it;</li>
 * <li>those get invisible stand-ins in its layout, so JEI's item list keeps clear of them as it does of the screen
 * (JEI asks the screen for the areas of its layout's top-level parts);</li>
 * <li>the button that left it for the vanilla inventory is removed: everything that screen had is here now, and the
 * vanilla one has the old troubles (two buttons in one spot, a player model looking past the cursor);</li>
 * <li>the player always looks at the cursor, done right: owo turns only the head (and only its previous-frame
 * angle) while the body keeps its angle in the world, so the player nodded up and down but stared sideways. Around
 * the screen's drawing the player's head and body angles are set as the vanilla inventory sets them, then put back
 * ({@link #poseBefore}, {@link #poseAfter}); the option for it is removed, it is simply on.</li>
 * </ul>
 */
final class GearLayout {
    private static final String STAND_IN = "wilds_core_area_";
    private static Field adapterField;

    private GearLayout() {}

    @SuppressWarnings("unchecked")
    private static ParentComponent root(Screen s) {
        if (!(s instanceof BaseOwoHandledScreen<?, ?>)) return null;
        try {
            if (adapterField == null) {
                adapterField = BaseOwoHandledScreen.class.getDeclaredField("uiAdapter");
                adapterField.setAccessible(true);
            }
            OwoUIAdapter<?> a = (OwoUIAdapter<?>) adapterField.get(s);
            return a == null ? null : a.rootComponent;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** left, top, right, bottom of the screen's own content (without the stand-ins); null if unknown */
    static int[] content(Screen s) {
        ParentComponent root = root(s);
        if (root == null) return null;
        int l = Integer.MAX_VALUE, t = Integer.MAX_VALUE, r = Integer.MIN_VALUE, b = Integer.MIN_VALUE;
        for (Component c : root.children()) {
            if (c.id() != null && c.id().startsWith(STAND_IN)) continue;
            if (c.width() <= 0 || c.height() <= 0) continue;
            l = Math.min(l, c.x());
            t = Math.min(t, c.y());
            r = Math.max(r, c.x() + c.width());
            b = Math.max(b, c.y() + c.height());
        }
        return l > r ? null : new int[]{l, t, r, b};
    }

    /**
     * moves the content so that its top left corner ends up at {@code left}, {@code top} (the layout centres it both ways;
     * uneven padding shifts the centre by half the difference)
     */
    static void moveContentTo(Screen s, int left, int top) {
        ParentComponent root = root(s);
        int[] c = content(s);
        if (root == null || c == null) return;
        Insets now = root.padding().get();
        int naturalX = c[0] - (now.left() - now.right()) / 2, naturalY = c[1] - (now.top() - now.bottom()) / 2;
        int dx = left - naturalX, dy = top - naturalY;
        Insets next = Insets.of(Math.max(0, 2 * dy), Math.max(0, -2 * dy), Math.max(0, 2 * dx), Math.max(0, -2 * dx));
        if (!next.equals(now)) root.padding(next);
    }

    /** an invisible part of the layout covering a panel of the pack (w = 0: none) */
    static void standIn(Screen s, String name, int x, int y, int w, int h) {
        ParentComponent root = root(s);
        if (root == null) return;
        String id = STAND_IN + name;
        Component c = root.childById(Component.class, id);
        if (w <= 0 || h <= 0) {
            if (c != null) root.removeChild(c);
            return;
        }
        Insets pad = root.padding().get();
        if (c == null) {
            if (!(root instanceof io.wispforest.owo.ui.container.FlowLayout flow)) return;
            c = Components.spacer().id(id);
            flow.child(c);
        }
        Positioning p = Positioning.absolute(x - root.x() - pad.left(), y - root.y() - pad.top());
        if (!p.equals(c.positioning().get())) c.positioning(p);
        if (c.width() != w || c.height() != h) c.sizing(Sizing.fixed(w), Sizing.fixed(h));
    }

    private static net.minecraft.world.entity.LivingEntity posed;
    private static final float[] saved = new float[8];

    /** the player in the screen faces the cursor: head fully, body halfway, like the vanilla inventory */
    static void poseBefore(Screen s, int mouseX, int mouseY) {
        ParentComponent root = root(s);
        if (root == null) return;
        EntityComponent<?>[] found = new EntityComponent<?>[1];
        root.forEachDescendant(c -> {
            if (found[0] == null && c instanceof EntityComponent<?> e && e.lookAtCursor()) found[0] = e;
        });
        if (found[0] == null || !(found[0].entity() instanceof net.minecraft.world.entity.LivingEntity living)) return;
        var c = found[0];
        float yaw = (float) Math.toDegrees(Math.atan((mouseX - c.x() - c.width() / 2f) / 40f));
        float pitch = (float) Math.toDegrees(Math.atan((mouseY - c.y() - c.height() / 2f) / 40f));
        posed = living;
        saved[0] = living.yBodyRot;
        saved[1] = living.yBodyRotO;
        saved[2] = living.yHeadRot;
        saved[3] = living.yHeadRotO;
        saved[4] = living.getYRot();
        saved[5] = living.yRotO;
        saved[6] = living.getXRot();
        saved[7] = living.xRotO;
        // owo's own rule for the head (yaw -angle, pitch 0.65 x angle) for the current and the previous frame alike
        living.yBodyRot = living.yBodyRotO = -yaw * 0.5f;
        living.yHeadRot = living.yHeadRotO = -yaw;
        living.setYRot(-yaw);
        living.yRotO = -yaw;
        living.setXRot(pitch * 0.65f);
        living.xRotO = pitch * 0.65f;
    }

    /** the middle of the player in the screen (for the pack's UI self-test); null if none */
    static int[] entityCenter(Screen s) {
        ParentComponent root = root(s);
        if (root == null) return null;
        int[][] at = new int[1][];
        root.forEachDescendant(c -> {
            if (at[0] == null && c instanceof EntityComponent<?>) at[0] = new int[]{c.x() + c.width() / 2, c.y() + c.height() / 2};
        });
        return at[0];
    }

    static void poseAfter() {
        var living = posed;
        if (living == null) return;
        posed = null;
        living.yBodyRot = saved[0];
        living.yBodyRotO = saved[1];
        living.yHeadRot = saved[2];
        living.yHeadRotO = saved[3];
        living.setYRot(saved[4]);
        living.yRotO = saved[5];
        living.setXRot(saved[6]);
        living.xRotO = saved[7];
    }

    /** removes what the pack replaces (see the class comment); cheap enough to run every frame (options are built late) */
    static void tidy(Screen s) {
        ParentComponent root = root(s);
        if (root == null) return;
        try {
            // owo makes vanilla buttons components at run time: seen as Component here
            Component look = root.childById(Component.class, "entity_look_at_cursor");
            if (look != null && look.parent() != null && look.parent().parent() != null) {
                look.parent().parent().removeChild(look.parent());
            }
            root.forEachDescendant(c -> {
                // the «back to the vanilla inventory» arrow: a 10-pixel button with no text, pinned to the player's top right
                Object o = c;
                if (o instanceof ButtonComponent && c.width() == 10 && c.positioning().get().type == Positioning.Type.RELATIVE
                        && c.positioning().get().x == 100) {
                    ButtonComponent b = (ButtonComponent) o;
                    if (b.getMessage().getString().isEmpty()) {
                        b.visible = false;
                        b.active = false;
                    }
                }
                if (c instanceof EntityComponent<?> e && !e.lookAtCursor()) e.lookAtCursor(true);
            });
        } catch (RuntimeException e) {
            WildsCore.LOG.debug("Wilds Core: equipment screen tidy-up skipped", e);
        }
    }
}
