package ua.wilds.wiki.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the wiki asks JEI. Callers check {@code WildsWiki.jei} first: without JEI this class is never loaded.
 * Role: RECIPES = how to make it, USES = what it is used in, STATION = what is made in it (catalyst).
 */
public final class Jei {
    public enum Role { RECIPES, USES, STATION }

    private Jei() {}

    public static boolean ready() {
        return WikiJeiPlugin.runtime != null;
    }

    private static RecipeIngredientRole role(Role r) {
        return switch (r) {
            case RECIPES -> RecipeIngredientRole.OUTPUT;
            case USES -> RecipeIngredientRole.INPUT;
            case STATION -> RecipeIngredientRole.CATALYST;
        };
    }

    private static List<IFocus<?>> focus(ItemStack stack, Role r) {
        IFocusFactory ff = WikiJeiPlugin.runtime.getJeiHelpers().getFocusFactory();
        return List.of(ff.createFocus(role(r), VanillaTypes.ITEM_STACK, stack));
    }

    private static List<IRecipeCategory<?>> categories(List<IFocus<?>> f) {
        return WikiJeiPlugin.runtime.getRecipeManager().createRecipeCategoryLookup().limitFocus(f).get()
                .filter(c -> !c.getRecipeType().equals(RecipeTypes.INFORMATION) && !tagList(c)).toList();
    }

    /** JEI's own "items in this tag" pages: a tag is not something the player can do with the item */
    private static boolean tagList(IRecipeCategory<?> c) {
        return c.getRecipeType().getRecipeClass().getName().startsWith("mezz.jei.library.plugins.jei.tags.");
    }

    public static int count(ItemStack stack, Role r) {
        if (!ready() || stack.isEmpty()) return 0;
        try {
            List<IFocus<?>> f = focus(stack, r);
            IRecipeManager rm = WikiJeiPlugin.runtime.getRecipeManager();
            int n = 0;
            for (IRecipeCategory<?> c : categories(f)) n += countIn(rm, c, f);
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    private static <T> int countIn(IRecipeManager rm, IRecipeCategory<T> c, List<IFocus<?>> f) {
        return (int) rm.createRecipeLookup(c.getRecipeType()).limitFocus(f).get().count();
    }

    public static void show(ItemStack stack, Role r) {
        if (ready() && !stack.isEmpty()) WikiJeiPlugin.runtime.getRecipesGui().show(focus(stack, r));
    }

    /** First recipes of the item, one per category (crafting first, as JEI orders them), at most max. */
    public static List<RecipeView> recipes(ItemStack stack, Role r, int max) {
        List<RecipeView> out = new ArrayList<>();
        if (!ready() || stack.isEmpty()) return out;
        try {
            List<IFocus<?>> f = focus(stack, r);
            IRecipeManager rm = WikiJeiPlugin.runtime.getRecipeManager();
            for (IRecipeCategory<?> c : categories(f)) {
                if (out.size() >= max) break;
                layout(rm, c, f).ifPresent(out::add);
            }
        } catch (Exception ignored) {
            // a category of some mod failing to lay out must not take the page down
        }
        return out;
    }

    private static <T> Optional<RecipeView> layout(IRecipeManager rm, IRecipeCategory<T> c, List<IFocus<?>> f) {
        IFocusFactory ff = WikiJeiPlugin.runtime.getJeiHelpers().getFocusFactory();
        return rm.createRecipeLookup(c.getRecipeType()).limitFocus(f).get().findFirst()
                .flatMap(recipe -> rm.createRecipeLayoutDrawable(c, recipe, ff.createFocusGroup(f)))
                .map(Layout::new);
    }

    /** Text of the JEI information pages of the item (mods and our own KubeJS info pages). */
    public static List<FormattedText> info(ItemStack stack) {
        Set<String> seen = new LinkedHashSet<>();
        List<FormattedText> out = new ArrayList<>();
        if (!ready() || stack.isEmpty()) return out;
        try {
            IRecipeManager rm = WikiJeiPlugin.runtime.getRecipeManager();
            for (Role r : new Role[]{Role.RECIPES, Role.USES}) {
                rm.createRecipeLookup(RecipeTypes.INFORMATION).limitFocus(focus(stack, r)).get().forEach((IJeiIngredientInfoRecipe i) -> {
                    for (FormattedText t : i.getDescription()) if (seen.add(t.getString())) out.add(t);
                });
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** The item under the mouse in JEI's ingredient list, bookmarks or recipe screen. */
    public static ItemStack hovered() {
        IJeiRuntime rt = WikiJeiPlugin.runtime;
        if (rt == null) return ItemStack.EMPTY;
        try {
            if (Minecraft.getInstance().screen instanceof IRecipesGui gui) {
                Optional<ItemStack> s = gui.getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
                if (s.isPresent()) return s.get();
            }
            ItemStack s = rt.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (s != null && !s.isEmpty()) return s;
            s = rt.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (s != null && !s.isEmpty()) return s;
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    /** JEI's search field has the keyboard: the wiki key is a letter being typed there. */
    public static boolean typing() {
        IJeiRuntime rt = WikiJeiPlugin.runtime;
        return rt != null && rt.getIngredientListOverlay().hasKeyboardFocus();
    }

    public static String changeOf(ResourceLocation id) {
        return id == null ? null : WikiJeiPlugin.changed.get(id);
    }

    private static final class Layout<T> implements RecipeView {
        private final IRecipeLayoutDrawable<T> d;
        private final int offX, offY, w, h;

        Layout(IRecipeLayoutDrawable<T> d) {
            this.d = d;
            d.setPosition(0, 0);
            Rect2i inner = d.getRect(), outer = d.getRectWithBorder();
            offX = inner.getX() - outer.getX();
            offY = inner.getY() - outer.getY();
            w = outer.getWidth();
            h = outer.getHeight();
        }

        public int width() {
            return w;
        }

        public int height() {
            return h;
        }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            d.setPosition(x + offX, y + offY);
            d.drawRecipe(g, mouseX, mouseY);
        }

        public void renderOverlays(GuiGraphics g, int mouseX, int mouseY) {
            if (d.isMouseOver(mouseX, mouseY)) d.drawOverlays(g, mouseX, mouseY);
        }

        public ItemStack stackAt(int mouseX, int mouseY) {
            return d.getItemStackUnderMouse(mouseX, mouseY).orElse(ItemStack.EMPTY);
        }

        public boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            try {
                return d.isMouseOver(mouseX, mouseY) && d.getInputHandler().handleMouseScrolled(mouseX, mouseY, scrollX, scrollY);
            } catch (Exception e) {
                return false;
            }
        }

        public boolean dragged(double mouseX, double mouseY, int button, double dx, double dy) {
            try {
                return d.getInputHandler().handleMouseDragged(mouseX, mouseY, InputConstants.Type.MOUSE.getOrCreate(button), dx, dy);
            } catch (Exception e) {
                return false;
            }
        }

        public void moved(double mouseX, double mouseY) {
            try {
                d.getInputHandler().handleMouseMoved(mouseX, mouseY);
            } catch (Exception ignored) {
            }
        }

        public ResourceLocation recipeId() {
            try {
                return d.getRecipeCategory().getRegistryName(d.getRecipe());
            } catch (Exception e) {
                return null;
            }
        }

        public String categoryTitle() {
            return d.getRecipeCategory().getTitle().getString();
        }

        public void tick() {
            d.tick();
        }
    }
}
