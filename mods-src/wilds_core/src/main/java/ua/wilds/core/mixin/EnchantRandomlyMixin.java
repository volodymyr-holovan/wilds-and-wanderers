package ua.wilds.core.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.wilds.core.world.Enchants;

import java.util.List;
import java.util.Optional;

/** "enchant_randomly" without a list of enchantments: never a technical one (world.Enchants). */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyMixin {
    @Shadow @Final private Optional<HolderSet<Enchantment>> options;

    @ModifyVariable(method = "run", at = @At("STORE"), ordinal = 0)
    private List<Holder<Enchantment>> wilds$noTechnical(List<Holder<Enchantment>> list) {
        return options.isPresent() ? list : Enchants.givenOut(list);
    }
}
