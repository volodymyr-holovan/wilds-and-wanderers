package ua.wilds.core.client.gear;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Field;

/**
 * The vanilla recipe book on the equipment screen. That screen's menu is a recipe-book menu with the crafting grid (the
 * server places recipes into it like into the vanilla inventory's), it only lacks the book: this is the vanilla book,
 * fed the screen's events. Whether it is open is the player's own recipe-book setting, so it stays as the player left it,
 * across openings of the inventory and across sessions, until the player closes it with its button.
 * <p>
 * The book (with its tabs) stands left of the screen's content, its middle level with the middle of the content and the
 * attributes under it ({@link GearScreens#layout}); its button sits left of the hotbar.
 */
final class RecipeBookPanel {
    static final int W = RecipeBookComponent.IMAGE_WIDTH, H = RecipeBookComponent.IMAGE_HEIGHT;
    /** the category tabs stick out left of the book */
    static final int TABS = 30;
    private static RecipeBookComponent book;
    private static ImageButton button;
    private static Field width;
    private static int placedAt = Integer.MIN_VALUE, ticks;

    private RecipeBookPanel() {}

    static void onInit(ScreenEvent.Init.Post e) {
        book = null;
        button = null;
        placedAt = placedTop = Integer.MIN_VALUE;
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> c) || !(c.getMenu() instanceof RecipeBookMenu<?, ?> menu)) return;
        Minecraft mc = Minecraft.getInstance();
        book = new RecipeBookComponent();
        book.init(c.width, c.height, mc, false, menu);
        button = new ImageButton(0, -100, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, b -> toggle());
        button.setTooltip(Tooltip.create(Component.literal("Книга рецептів: що можна змайструвати з того, що у вас є. Клацніть рецепт — "
                + "інгредієнти самі ляжуть у сітку майстрування.")));
        e.addListener(button);
    }

    static boolean open() {
        return book != null && book.isVisible();
    }

    static void toggle() {
        if (book == null) return;
        book.toggleVisibility();
        placedAt = placedTop = Integer.MIN_VALUE;
    }

    static void placeButton(int x, int y) {
        if (button == null) return;
        button.visible = x > Integer.MIN_VALUE;
        button.setX(x);
        button.setY(y);
    }

    private static Field height;
    private static int placedTop = Integer.MIN_VALUE;

    /**
     * the book's top left corner on the screen. The vanilla book centres itself in the size it was given and steps 86 to
     * the left (initVisuals sets that step anew each time): so it is given the size that puts it where the layout wants it.
     */
    static void place(Screen s, int left, int top) {
        if (book == null || (left == placedAt && top == placedTop)) return;
        placedAt = left;
        placedTop = top;
        try {
            if (width == null) {
                width = RecipeBookComponent.class.getDeclaredField("width");
                width.setAccessible(true);
                height = RecipeBookComponent.class.getDeclaredField("height");
                height.setAccessible(true);
            }
            width.setInt(book, 2 * (left + 86) + W);
            height.setInt(book, 2 * top + H);
            if (book.isVisible()) book.initVisuals();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            WildsCore.LOG.warn("Wilds Core: cannot place the recipe book", ex);
        }
    }

    static int top(Screen s) {
        return placedTop == Integer.MIN_VALUE ? (s.height - H) / 2 : placedTop;
    }

    static void render(ScreenEvent.Render.Post e) {
        if (!open() || !(e.getScreen() instanceof AbstractContainerScreen<?> c)) return;
        var g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);   // over the screen's own parts, under the tooltips
        book.render(g, e.getMouseX(), e.getMouseY(), e.getPartialTick());
        g.pose().popPose();
        book.renderGhostRecipe(g, c.getGuiLeft(), c.getGuiTop(), false, e.getPartialTick());
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        book.renderTooltip(g, c.getGuiLeft(), c.getGuiTop(), e.getMouseX(), e.getMouseY());
        g.pose().popPose();
    }

    static boolean inside(Screen s, double x, double y) {
        return open() && placedAt > Integer.MIN_VALUE && x >= placedAt - TABS && x < placedAt + W && y >= top(s) && y < top(s) + H;
    }

    static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!open()) return;
        // a click on the book is the book's, also where nothing reacts: it must not drop the carried item "outside the screen"
        if (book.mouseClicked(e.getMouseX(), e.getMouseY(), e.getButton()) || inside(e.getScreen(), e.getMouseX(), e.getMouseY())) {
            e.setCanceled(true);
        }
    }

    static void afterClick(ScreenEvent.MouseButtonPressed.Post e) {
        if (open() && e.getScreen() instanceof AbstractContainerScreen<?> c) {
            Slot slot = c.getSlotUnderMouse();
            if (slot != null) book.slotClicked(slot);
        }
    }

    static void onKey(ScreenEvent.KeyPressed.Pre e) {
        // Escape closes the screen (the book would take it for itself, it thinks it stands alone)
        if (open() && e.getKeyCode() != 256 && book.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers())) e.setCanceled(true);
    }

    static void onKeyUp(ScreenEvent.KeyReleased.Pre e) {
        if (open() && book.keyReleased(e.getKeyCode(), e.getScanCode(), e.getModifiers())) e.setCanceled(true);
    }

    static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        if (open() && book.charTyped(e.getCodePoint(), e.getModifiers())) e.setCanceled(true);
    }

    /** every tick while the screen is open: the book follows the inventory, and now and then learns new recipes */
    static void tick() {
        if (!open()) return;
        book.tick();
        if (++ticks % 40 == 0) book.recipesUpdated();
    }
}
