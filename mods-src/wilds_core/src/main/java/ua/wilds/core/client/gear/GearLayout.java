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
 * <li>its content moves sideways to make room for the recipe book and the attributes panel beside it;</li>
 * <li>those two get invisible stand-ins in its layout, so JEI's item list keeps clear of them as it does of the screen
 * (JEI asks the screen for the areas of its layout's top-level parts);</li>
 * <li>the button that left it for the vanilla inventory is removed: everything that screen had is here now, and the
 * vanilla one has the old troubles (two buttons in one spot, a player model looking past the cursor);</li>
 * <li>the «look at the cursor» option is removed: owo turns only the head after the cursor while the body keeps its
 * angle, so the player stared sideways.</li>
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

    /** moves the content so that its left edge ends up at {@code left} (the layout centres it; padding shifts the centre) */
    static void moveContentTo(Screen s, int left) {
        ParentComponent root = root(s);
        int[] c = content(s);
        if (root == null || c == null) return;
        Insets now = root.padding().get();
        int shift = (now.left() - now.right()) / 2;
        int natural = c[0] - shift;
        int want = left - natural;
        Insets next = Insets.of(now.top(), now.bottom(), Math.max(0, 2 * want), Math.max(0, -2 * want));
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
                if (c instanceof EntityComponent<?> e && e.lookAtCursor()) e.lookAtCursor(false);
            });
        } catch (RuntimeException e) {
            WildsCore.LOG.debug("Wilds Core: equipment screen tidy-up skipped", e);
        }
    }
}
