package ua.wilds.wiki.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ua.wilds.wiki.WildsWiki;
import ua.wilds.wiki.data.WikiData;

import java.util.Map;

@JeiPlugin
public final class WikiJeiPlugin implements IModPlugin {
    static IJeiRuntime runtime;
    static Map<ResourceLocation, String> changed = Map.of();
    public static final int MARK = 0xFFE8B04A;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(WildsWiki.ID, "jei");
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        changed = WikiData.readChangedRecipes(Minecraft.getInstance().getResourceManager());
        ChangedRecipeMark mark = new ChangedRecipeMark();
        registration.getJeiHelpers().getAllRecipeTypes().forEach(t -> add(registration, t, mark));
        WildsWiki.LOG.info("Wilds Wiki: {} changed recipes marked in JEI", changed.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void add(IAdvancedRegistration r, RecipeType<?> type, ChangedRecipeMark mark) {
        r.addRecipeCategoryDecorator((RecipeType) type, (IRecipeCategoryDecorator) mark);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    static String change(IRecipeCategory<Object> category, Object recipe) {
        if (changed.isEmpty()) return null;
        ResourceLocation id;
        try {
            id = category.getRegistryName(recipe);
        } catch (Exception e) {
            return null;
        }
        return id == null ? null : changed.get(id);
    }

    /** Recipes the pack added or changed get an amber frame with a corner tab and an explanation on hover. */
    static final class ChangedRecipeMark implements IRecipeCategoryDecorator<Object> {
        @Override
        public void draw(Object recipe, IRecipeCategory<Object> category, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            if (change(category, recipe) == null) return;
            int w = category.getWidth(), h = category.getHeight();
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.renderOutline(-2, -2, w + 4, h + 4, 0xA0E8B04A);
            // corner tab: a small amber triangle in the top left corner
            for (int i = 0; i < 5; i++) g.fill(-2, -2 + i, 3 - i, -1 + i, MARK);
            g.pose().popPose();
        }

        @Override
        public void decorateTooltips(ITooltipBuilder tooltip, Object recipe, IRecipeCategory<Object> category, IRecipeSlotsView slots,
                                     double mouseX, double mouseY) {
            String c = change(category, recipe);
            if (c == null) return;
            // JEI asks for this tooltip when the mouse is over the recipe but not over one of its slots
            tooltip.add(Component.translatable("wilds_wiki.jei.title").withStyle(s -> s.withColor(MARK)));
            if (c.equals("added")) {
                tooltip.add(Component.translatable("wilds_wiki.jei.added").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("wilds_wiki.jei.modified").withStyle(ChatFormatting.GRAY));
                if (c.startsWith("modified:")) {
                    tooltip.add(Component.translatable("wilds_wiki.jei.note." + c.substring(9)).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
