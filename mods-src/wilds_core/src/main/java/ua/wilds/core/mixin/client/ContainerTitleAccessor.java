package ua.wilds.core.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** where a container screen draws its title, so the copper golem's status goes right after it (SortTitle) */
@Mixin(AbstractContainerScreen.class)
public interface ContainerTitleAccessor {
    @Accessor("titleLabelX")
    int wilds$titleX();

    @Accessor("titleLabelY")
    int wilds$titleY();
}
