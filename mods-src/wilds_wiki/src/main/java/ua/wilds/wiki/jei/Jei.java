package ua.wilds.wiki.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
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
                .filter(c -> !c.getRecipeType().equals(RecipeTypes.INFORMATION) && !tagList(c) && !mirror(c))
                .sorted(java.util.Comparator.comparingInt(Jei::rank)).toList();
    }

    /**
     * Create's copies of ordinary recipes: every crafting recipe again for the mechanical crafter and the basin, every 3x3
     * block again for the press, every stonecutter recipe again for the saw, every potion again for the basin. On a page
     * they only repeat the real recipe ("can also be made by Create machines"), so the wiki leaves them to JEI.
     */
    private static boolean mirror(IRecipeCategory<?> c) {
        var uid = c.getRecipeType().getUid();
        return uid.getNamespace().equals("create") && (uid.getPath().startsWith("automatic_") || uid.getPath().equals("block_cutting"));
    }

    /** a slot with this many items takes (or makes) nearly anything: "any item can get Reactive", "socket a gem into any gear" */
    private static final int ANY_ITEM = 200;

    /** the recipe is not about this item: the slot that holds it holds almost every other item too */
    private static boolean generic(IRecipeLayoutDrawable<?> d, ItemStack stack) {
        for (IRecipeSlotView s : d.getRecipeSlotsView().getSlotViews()) {
            List<ItemStack> items = s.getItemStacks().limit(ANY_ITEM + 1).toList();
            if (items.size() > ANY_ITEM && s.getItemStacks().anyMatch(i -> ItemStack.isSameItem(i, stack))) return true;
        }
        return false;
    }

    /** by-products of machines and bulk processing: ways nobody looks for first */
    private static final String[] SIDE = {"item_application", "deploying", "sandpaper", "polishing", "crushing", "milling", "pressing", "compacting",
            "cutting", "sawing", "haunting", "splashing", "washing", "fan_", "mystery", "packing", "spout", "draining", "recycling", "uncrafting",
            "grinding", "sieving", "composting", "fuel", "anvil", "grindstone", "decorated_pot", "loom", "enrich", "inscriber_", "entropy", "transform",
            "repair", "salvag"};

    /**
     * how soon a kind of recipe is shown: the crafting table first, then furnaces, smithing, the stonecutter and brewing,
     * then stations of mods; what machines make of the item in passing (pressed, crushed, washed, applied to a block) last
     */
    public static int rank(IRecipeCategory<?> c) {
        var uid = c.getRecipeType().getUid();
        String p = uid.getPath();
        if (uid.getNamespace().equals("minecraft")) {
            return switch (p) {
                case "crafting" -> 0;
                case "smithing" -> 1;
                case "smelting", "blasting", "smoking", "campfire_cooking" -> 2;
                case "stonecutting" -> 3;
                case "brewing" -> 4;
                default -> p.equals("anvil") || p.equals("fuel") || p.equals("compostable") || p.equals("grindstone") ? 60 : 10;
            };
        }
        for (String s : SIDE) if (p.contains(s)) return 50;
        // MineColonies shows what each colony worker can make: mostly ordinary recipes again (the blacksmith forges
        // what a crafting table makes); first only when a worker is the one way to get the item
        if (uid.getNamespace().equals("minecolonies")) return 50;
        return 20;
    }

    /** first recipes for the page: the main ways only, unless the item has no other */
    public static List<RecipeView> mainRecipes(ItemStack stack, Role r, int max) {
        List<RecipeView> out = new ArrayList<>();
        if (!ready() || stack.isEmpty()) return out;
        try {
            List<IFocus<?>> f = focus(stack, r);
            IRecipeManager rm = WikiJeiPlugin.runtime.getRecipeManager();
            List<IRecipeCategory<?>> cats = categories(f);
            for (IRecipeCategory<?> c : cats) {
                if (out.size() >= max || rank(c) >= 50) break;
                layout(rm, c, f, stack).ifPresent(out::add);
            }
            // by-products only when they are the sole way to get the item
            if (out.isEmpty() && r == Role.RECIPES) for (IRecipeCategory<?> c : cats) {
                if (out.size() >= max) break;
                if (rank(c) >= 50) layout(rm, c, f, stack).ifPresent(out::add);
            }
        } catch (Exception ignored) {
            // a category of some mod failing to lay out must not take the page down
        }
        return out;
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
            for (IRecipeCategory<?> c : categories(f)) n += countIn(rm, c, f, stack);
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    /** recipes of the category for the item; a small category is looked into, so a lone generic recipe is not counted */
    private static <T> int countIn(IRecipeManager rm, IRecipeCategory<T> c, List<IFocus<?>> f, ItemStack stack) {
        int n = (int) rm.createRecipeLookup(c.getRecipeType()).limitFocus(f).get().count();
        if (n == 0 || n > 3) return n;
        IFocusFactory ff = WikiJeiPlugin.runtime.getJeiHelpers().getFocusFactory();
        return (int) rm.createRecipeLookup(c.getRecipeType()).limitFocus(f).get()
                .map(recipe -> rm.createRecipeLayoutDrawable(c, recipe, ff.createFocusGroup(f)))
                .flatMap(Optional::stream)
                .filter(d -> !generic(d, stack)).count();
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
                layout(rm, c, f, stack).ifPresent(out::add);
            }
        } catch (Exception ignored) {
            // a category of some mod failing to lay out must not take the page down
        }
        return out;
    }

    /** the first recipe of the category that is about this item (generic ones skipped), laid out for a page */
    private static <T> Optional<RecipeView> layout(IRecipeManager rm, IRecipeCategory<T> c, List<IFocus<?>> f, ItemStack stack) {
        IFocusFactory ff = WikiJeiPlugin.runtime.getJeiHelpers().getFocusFactory();
        return rm.createRecipeLookup(c.getRecipeType()).limitFocus(f).get().limit(LOOK)
                .map(recipe -> rm.createRecipeLayoutDrawable(c, recipe, ff.createFocusGroup(f)))
                .flatMap(Optional::stream)
                .filter(d -> !generic(d, stack))
                .findFirst()
                .map(Layout::new);
    }

    /** how many recipes of a category are looked at for one that is about the item */
    private static final int LOOK = 8;

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

        public String categoryId() {
            return d.getRecipeCategory().getRecipeType().getUid().toString();
        }

        public void tick() {
            d.tick();
        }
    }
}
