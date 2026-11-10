package ua.wilds.core.client;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import ua.wilds.core.WildsCore;

/**
 * Every recipe in the recipe book from the start (see mixin.RecipeBookKnowsMixin): once the recipes of the server have
 * come and the player exists, all that are not "special" (maps, fireworks: they have no fixed ingredients) are put into
 * the client's book. Checked every second: a reload of the server's data brings the recipes anew.
 */
public final class AllRecipes {
    private static Object filledFor;
    private static int ticks;

    private AllRecipes() {}

    public static void tick() {
        if (++ticks % 20 != 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            filledFor = null;
            return;
        }
        var manager = mc.getConnection().getRecipeManager();
        var all = manager.getRecipes();
        if (all.isEmpty()) return;
        ClientRecipeBook book = mc.player.getRecipeBook();
        Object key = java.util.List.of(book, all.size(), all.iterator().next());
        if (key.equals(filledFor) || book.getCollections().isEmpty()) return;
        filledFor = key;
        int n = 0;
        for (RecipeHolder<?> r : all) {
            if (r.value().isSpecial() || book.contains(r)) continue;
            book.add(r);
            n++;
        }
        if (n > 0) for (var c : book.getCollections()) c.updateKnownRecipes(book);
        WildsCore.LOG.info("Wilds Core: {} more recipes in the recipe book", n);
    }
}
