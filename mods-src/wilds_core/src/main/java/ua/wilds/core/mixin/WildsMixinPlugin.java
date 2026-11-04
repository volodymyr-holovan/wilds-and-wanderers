package ua.wilds.core.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mixins into other mods apply only when that mod is installed: "client.RMSongPickerMixin" needs Reactive Music. */
public final class WildsMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> NEEDS = Map.ofEntries(Map.entry("ua.wilds.core.mixin.client.RMSongPickerMixin", "reactivemusic"),
            Map.entry("ua.wilds.core.mixin.AccessoriesResultSlotMixin", "accessories"),
            Map.entry("ua.wilds.core.mixin.client.DhClientWorldMixin", "distanthorizons"),
            Map.entry("ua.wilds.core.mixin.AccessoriesRowsMenuMixin", "accessories"),
            Map.entry("ua.wilds.core.mixin.client.AccessoriesPlayerInvMixin", "accessories"),
            Map.entry("ua.wilds.core.mixin.client.AccessoriesRowsScreenMixin", "accessories"),
            Map.entry("ua.wilds.core.mixin.client.FtbTeamsSelfMixin", "ftbteams"),
            Map.entry("ua.wilds.core.mixin.FtbQuestsTeamMixin", "ftbquests"),
            Map.entry("ua.wilds.core.mixin.FtbQuestsPartyMixin", "ftbquests"),
            Map.entry("ua.wilds.core.mixin.client.ApothTooltipMixin", "apotheosis"),
            Map.entry("ua.wilds.core.mixin.client.AccessoriesInvBackgroundMixin", "accessories"));

    public void onLoad(String mixinPackage) {}

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String mod = NEEDS.get(mixinClassName);
        return mod == null || LoadingModList.get().getModFileById(mod) != null;
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
