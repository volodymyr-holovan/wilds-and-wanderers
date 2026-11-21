package ua.wilds.core.mixin.colony;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The town hall's colony map draws the filled maps kept in the town hall, but only maps of scale 0 were sent: an
 * explorer's map, a zoomed-out map or a map from a cartography table was ignored without a word, and the window stayed
 * an empty parchment ("put a map in first"). The window itself handles any scale (it sizes and places each map by its
 * own scale), so every filled map is sent.
 */
@Mixin(value = BuildingTownHall.class, remap = false)
public abstract class TownHallMapMixin {
    @Redirect(method = "serializeToView", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;scale:B", opcode = org.objectweb.asm.Opcodes.GETFIELD), require = 1)
    private byte wilds$anyScale(MapItemSavedData data) {
        return 0;
    }
}
