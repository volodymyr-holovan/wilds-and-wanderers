package ua.wilds.core.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.wilds.core.skills.branch.CraftSkills;

/** Skill smith_hands: work on the anvil costs fewer levels (CraftSkills#anvilCost), on both sides so the price shown is the price paid. */
@Mixin(AnvilMenu.class)
public abstract class AnvilSmithMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;

    protected AnvilSmithMixin(MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access) {
        super(type, id, inv, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void wilds$smith(CallbackInfo ci) {
        int c = cost.get();
        if (c > 1) cost.set(CraftSkills.anvilCost(player, c));
    }
}
