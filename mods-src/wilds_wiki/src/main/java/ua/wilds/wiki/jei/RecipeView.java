package ua.wilds.wiki.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A JEI recipe drawn inside a wiki page. Only MC types here, so pages compile and run without JEI. */
public interface RecipeView {
    int width();

    int height();

    void render(GuiGraphics g, int x, int y, int mouseX, int mouseY);

    /** hover highlight and the ingredient tooltip, drawn after the page's clipping is off */
    void renderOverlays(GuiGraphics g, int mouseX, int mouseY);

    ItemStack stackAt(int mouseX, int mouseY);

    /** the wheel over the recipe: scrolls its own lists (long tag or loot grids) instead of the page */
    boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY);

    boolean dragged(double mouseX, double mouseY, int button, double dx, double dy);

    void moved(double mouseX, double mouseY);

    ResourceLocation recipeId();

    String categoryTitle();

    void tick();
}
