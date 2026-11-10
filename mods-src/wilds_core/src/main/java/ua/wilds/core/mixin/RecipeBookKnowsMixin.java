package ua.wilds.core.mixin;

import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The recipe book shows only recipes the player has "unlocked", and unlocking is an advancement every recipe must bring
 * with it: many mods' recipes bring none, so the book never showed them and a click could not lay them out. In this pack
 * every recipe is known from the start: the server accepts any recipe for the book's click (here), and the client fills
 * its book with all of them (client.AllRecipes). Nothing is saved or sent for it.
 */
@Mixin(RecipeBook.class)
public abstract class RecipeBookKnowsMixin {
    @Inject(method = "contains(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z", at = @At("HEAD"), cancellable = true)
    private void wilds$all(RecipeHolder<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerRecipeBook && recipe != null) cir.setReturnValue(true);
    }
}
