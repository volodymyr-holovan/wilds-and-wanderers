package ua.wilds.core.client;

import net.neoforged.fml.loading.FMLPaths;
import ua.wilds.core.WildsCore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Changes to other mods' client settings that also reach players who installed the pack before them (pack/overrides-initial
 * is copied only on the first install). Each patch runs once: config/wilds_core-patches.txt keeps the last version done.
 * Only the named values are replaced in place, so everything else the player set stays.
 *  1 (22.11): pinned quests in the middle of the right edge (under the minimap they covered its coordinates at GUI
 *             scales 1-2), Distant Horizons' start-up notes in chat off (garbage collector, memory, mod lists)
 *  2 (22.11): Jade's look-at tooltip in the journal theme (dark and gold), the mod's name in gold
 */
public final class ConfigPatches {
    private static final int VERSION = 2;
    private static final String FLAG = "wilds_core-patches.txt";

    private ConfigPatches() {}

    public static void run() {
        Path dir = FMLPaths.CONFIGDIR.get();
        int done = 0;
        try {
            Path flag = dir.resolve(FLAG);
            if (Files.exists(flag)) done = Integer.parseInt(Files.readString(flag).trim().split("\\s+")[0]);
        } catch (Exception ignored) {
        }
        if (done >= VERSION) return;
        try {
            if (done < 1) {
                set(dir.resolve("ftbquests-client.snbt"), "pinned_quests_pos", "\"right\"");
                set(dir.resolve("ftbquests-client.snbt"), "pinned_quests_inset_x", "6");
                set(dir.resolve("ftbquests-client.snbt"), "pinned_quests_inset_y", "0");
                Path dh = dir.resolve("DistantHorizons.toml");
                for (String k : new String[]{"showLowMemoryWarningOnStartup", "showReplayWarningOnStartup", "showModCompatibilityWarningsOnStartup",
                        "showHighVanillaRenderDistanceWarning", "showPoolInsufficientMemoryWarning", "showGarbageCollectorWarning",
                        "showSlowWorldGenSettingWarnings"})
                    set(dh, k, "false");
            }
            if (done < 2) {
                // Jade's look-at tooltip in the journal's dark and gold, the mod name in gold instead of blue
                Path jade = dir.resolve("jade").resolve("jade.json");
                set(jade, "\"activeTheme\"", "\"wilds:journal\",");
                set(jade, "\"color\"", "\"gold\"");
            }
            Files.writeString(dir.resolve(FLAG), VERSION + "\nWilds & Wanderers: client setting patches applied up to this version.\n");
        } catch (Exception ex) {
            WildsCore.LOG.warn("Wilds Core: could not patch client settings", ex);
        }
    }

    /** key: value (SNBT) or key = value (TOML) on its own line; a file that does not exist yet is left to its mod */
    private static void set(Path file, String key, String value) throws java.io.IOException {
        if (!Files.exists(file)) return;
        String s = Files.readString(file, StandardCharsets.UTF_8);
        String t = Pattern.compile("(?m)^(\\s*" + Pattern.quote(key) + "\\s*[:=]\\s*)\\S.*$").matcher(s)
                .replaceAll(m -> java.util.regex.Matcher.quoteReplacement(m.group(1) + value));
        if (!t.equals(s)) Files.writeString(file, t, StandardCharsets.UTF_8);
    }
}
