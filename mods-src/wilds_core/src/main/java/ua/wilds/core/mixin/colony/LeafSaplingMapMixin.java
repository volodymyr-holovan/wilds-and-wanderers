package ua.wilds.core.mixin.colony;

import com.minecolonies.api.compatibility.CompatibilityManager;
import com.minecolonies.api.crafting.ItemStorage;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The lumberjack's tree search runs on the pathfinding threads and asks which sapling a leaf block drops, from a plain
 * HashMap that the server thread fills while forests are cut (the first leaf of every new kind is learned on the
 * spot). Reading a HashMap while it grows can throw or loop; the search then dies with an exception, the path comes back
 * empty and the lumberjack "finds no trees". MineColonies 1.1.1420 stopped reading it from the threads (#11863); here
 * the map becomes a ConcurrentHashMap, which is safe for both (no nulls are ever put in it).
 */
@Mixin(value = CompatibilityManager.class, remap = false)
public abstract class LeafSaplingMapMixin {
    @Shadow @Final @Mutable private Map<Block, ItemStorage> leavesToSaplingMap;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void wilds$threadSafeLeaves(CallbackInfo ci) {
        leavesToSaplingMap = new ConcurrentHashMap<>(leavesToSaplingMap);
    }
}
