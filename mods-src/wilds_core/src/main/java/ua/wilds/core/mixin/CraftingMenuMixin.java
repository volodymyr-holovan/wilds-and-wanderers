package ua.wilds.core.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.wilds.core.skills.branch.BagSkills;

/** The pocket crafting table (skill pocket_crafting): its grid has no crafting table block to stay near. */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void wilds$pocket(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (BagSkills.isPocket((CraftingMenu) (Object) this)) cir.setReturnValue(player.isAlive());
    }
}
