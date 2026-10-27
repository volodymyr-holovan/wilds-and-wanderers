package ua.wilds.core.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.wilds.core.skills.branch.TraderSkills;

/**
 * The trader's discount: right after the villager has worked out its prices for this player (reputation, Hero of the
 * Village), every offer gets cheaper by the player's discount. The game never lets a price fall below one item.
 */
@Mixin(Villager.class)
public abstract class VillagerPricesMixin {
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void wilds$discount(Player player, CallbackInfo ci) {
        int off = TraderSkills.discount(player);
        if (off <= 0) return;
        for (MerchantOffer o : ((Villager) (Object) this).getOffers()) o.addToSpecialPriceDiff(-off);
    }
}
