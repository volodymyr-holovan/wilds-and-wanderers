package ua.wilds.core.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.wilds.core.world.Enchants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** "enchant_with_levels" without a list of enchantments: never a technical one (world.Enchants). */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantWithLevelsMixin {
    @ModifyVariable(method = "enchantItem(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/RegistryAccess;Ljava/util/Optional;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Optional<? extends HolderSet<Enchantment>> wilds$noTechnical(Optional<? extends HolderSet<Enchantment>> options, RandomSource random, ItemStack stack,
                                                                                int level, RegistryAccess access) {
        if (options.isPresent()) return options;
        List<Holder<Enchantment>> all = new ArrayList<>();
        access.registryOrThrow(Registries.ENCHANTMENT).holders().forEach(all::add);
        return Optional.of(HolderSet.direct(Enchants.givenOut(all)));
    }
}
