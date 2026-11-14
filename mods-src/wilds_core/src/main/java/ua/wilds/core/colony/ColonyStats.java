package ua.wilds.core.colony;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Builder placement timing for the colony test (-Dwilds_core.colonytest=true only): the interval between two
 * placements of one builder, next to the interval MineColonies' own delay would have given.
 */
public final class ColonyStats {
    public static final boolean ON = Boolean.getBoolean("wilds_core.colonytest");

    private static final Map<Object, Long> LAST = new WeakHashMap<>();
    private static long count, sumTicks, sumVanilla;

    private ColonyStats() {}

    public static synchronized void placement(Object ai, int vanillaDelay) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long now = server.getTickCount();
        Long last = LAST.put(ai, now);
        if (last == null || now - last > 100) return;  // walking, fetching material: not a placement cycle
        count++;
        sumTicks += now - last;
        sumVanilla += 5L * (1 + (vanillaDelay > 0 ? (vanillaDelay + 4) / 5 : 0));
    }

    public static synchronized String report() {
        if (count == 0) return "placements: none timed yet";
        return String.format("placements timed: %d, average cycle %.2f ticks, MineColonies' own delay would give %.2f ticks (x%.2f)",
                count, sumTicks / (double) count, sumVanilla / (double) count, (sumVanilla / (double) count) / (sumTicks / (double) count));
    }
}
