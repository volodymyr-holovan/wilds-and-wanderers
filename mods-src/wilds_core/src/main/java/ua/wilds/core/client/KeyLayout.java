package ua.wilds.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import ua.wilds.core.WildsCore;

/**
 * The pack's key layout. With 280 key mappings from 60 mods, several mods ship the same default key for things that
 * happen in the same moment (Iris reloads its shaders on R, which also opens Iron's spell wheel; Ars Nouveau's spell
 * radial and Iron's cast share V; Xaero's new waypoint and the backpack share B...). Vanilla keys and every mod's main
 * key stay where the mods' own guides say they are; the secondary one of each clashing pair moves to Alt + a key that
 * nothing else uses. Applied once per layout version, and only to keys the player has not rebound: their own choices
 * are kept. Situational keys that only work in one moment (armour abilities, riding a dragon, steering a ship) keep
 * their letters.
 * <p>
 * Tooling: -Dwilds_core.keydump=&lt;file&gt; writes every key mapping (name, category, current and default key, modifier,
 * conflict context) on the first client tick, so overlapping keys can be found per context.
 */
public final class KeyLayout {
    private static final int VERSION = 1;
    private static final String FLAG = "wilds_core-keys.txt";
    private static final String DUMP = System.getProperty("wilds_core.keydump");
    /** mapping -> {key it moves from (its default), key it moves to, Alt or none} */
    private static final Map<String, Object[]> MOVES = Map.ofEntries(
            Map.entry("iris.keybind.reload", new Object[]{InputConstants.KEY_R, InputConstants.KEY_I, KeyModifier.ALT}),
            Map.entry("iris.keybind.toggleShaders", new Object[]{InputConstants.KEY_K, InputConstants.KEY_K, KeyModifier.ALT}),
            Map.entry("iris.keybind.shaderPackSelection", new Object[]{InputConstants.KEY_O, InputConstants.KEY_O, KeyModifier.ALT}),
            Map.entry("key.kubejs.kubedex", new Object[]{InputConstants.KEY_K, -1, KeyModifier.NONE}),
            Map.entry("gui.xaero_new_waypoint", new Object[]{InputConstants.KEY_B, InputConstants.KEY_B, KeyModifier.ALT}),
            Map.entry("key.corpse.death_history", new Object[]{InputConstants.KEY_U, InputConstants.KEY_U, KeyModifier.ALT}),
            Map.entry("gui.xaero_minimap_settings", new Object[]{InputConstants.KEY_Y, InputConstants.KEY_Y, KeyModifier.ALT}),
            Map.entry("gui.xaero_enlarge_map", new Object[]{InputConstants.KEY_Z, InputConstants.KEY_E, KeyModifier.ALT}),
            Map.entry("key.mekanism.key_hud", new Object[]{InputConstants.KEY_H, InputConstants.KEY_H, KeyModifier.ALT}),
            Map.entry("key.ars_nouveau.selection_hud", new Object[]{InputConstants.KEY_V, InputConstants.KEY_V, KeyModifier.ALT}),
            // the Aether's own accessories screen fails on the server (the pack has one equipment screen, H)
            Map.entry("key.aether.open_accessories.desc", new Object[]{InputConstants.KEY_I, -1, KeyModifier.NONE}));
    private static boolean done, dumped;
    private static final List<String> moved = new ArrayList<>();

    private KeyLayout() {}

    public static void tick(ClientTickEvent.Post e) {
        if (!done) {
            done = true;
            apply();
        }
        if (DUMP != null && !dumped) {
            dumped = true;
            dump();
        }
    }

    private static void apply() {
        Path flag = FMLPaths.CONFIGDIR.get().resolve(FLAG);
        try {
            if (Files.exists(flag) && Files.readString(flag).trim().startsWith("version=" + VERSION)) return;
            Minecraft mc = Minecraft.getInstance();
            for (KeyMapping k : mc.options.keyMappings) {
                Object[] m = MOVES.get(k.getName());
                if (m == null) continue;
                InputConstants.Key from = InputConstants.Type.KEYSYM.getOrCreate((int) m[0]);
                // only a key still on the mod's own default: anything the player chose stays
                if (!k.getKey().equals(from) || k.getKeyModifier() != KeyModifier.NONE) continue;
                int to = (int) m[1];
                k.setKeyModifierAndCode((KeyModifier) m[2], to < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(to));
                moved.add(k.getName());
            }
            KeyMapping.resetMapping();
            mc.options.save();
            hideAetherAccessoriesButton();
            Files.writeString(flag, "version=" + VERSION + "\nWilds & Wanderers moved the keys that clashed between mods (" + String.join(", ", moved)
                    + ").\nDelete this file to apply the pack's key layout again (only keys still on their defaults change).\n");
        } catch (Exception ex) {
            WildsCore.LOG.warn("Could not apply the pack's key layout", ex);
        }
    }

    /**
     * The Aether draws its own accessories button in inventory screens; its screen fails on the server in this pack
     * (NullPointerException in Accessories' slot generator, the slots are the shared ones of the one equipment screen),
     * so the button only threw errors. The Aether's own client setting hides it.
     */
    private static void hideAetherAccessoriesButton() {
        try {
            if (!net.neoforged.fml.ModList.get().isLoaded("aether")) return;
            Object client = Class.forName("com.aetherteam.aether.AetherConfig").getField("CLIENT").get(null);
            Object v = client.getClass().getField("disable_accessory_button").get(client);
            if (v instanceof net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?> cv && Boolean.FALSE.equals(cv.get())) {
                @SuppressWarnings("unchecked")
                var b = (net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Boolean>) cv;
                b.set(true);
                b.save();
                moved.add("aether_button");
            }
        } catch (Exception | LinkageError ex) {
            WildsCore.LOG.warn("Could not hide the Aether's accessories button", ex);
        }
    }

    /** once, in the first world after the keys moved: tell the player where they went */
    public static void onJoin(ClientPlayerNetworkEvent.LoggingIn e) {
        if (moved.isEmpty() || e.getPlayer() == null) return;
        Minecraft mc = Minecraft.getInstance();
        List<String> names = new ArrayList<>();
        for (KeyMapping k : mc.options.keyMappings)
            if (moved.contains(k.getName())) names.add(Component.translatable(k.getName()).getString() + " — " + k.getTranslatedKeyMessage().getString());
        if (moved.contains("aether_button")) names.add("Кнопку аксесуарів Aether прибрано: усе спорядження — у вікні інвентаря (E)");
        moved.clear();
        e.getPlayer().displayClientMessage(Component.literal("Клавіші, що збігалися між модами, розведено (змінити: Налаштування → Керування):")
                .withStyle(ChatFormatting.GOLD), false);
        for (String n : names) e.getPlayer().displayClientMessage(Component.literal("  " + n).withStyle(ChatFormatting.GRAY), false);
    }

    private static void dump() {
        List<String> out = new ArrayList<>();
        for (KeyMapping k : Minecraft.getInstance().options.keyMappings) {
            out.add(String.join("\t", k.getName(), k.getCategory(), k.getKey().getName(), k.getKeyModifier().name(),
                    k.getDefaultKey().getName(), k.getDefaultKeyModifier().name(), String.valueOf(k.getKeyConflictContext())));
        }
        try {
            Files.write(Path.of(DUMP), out, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            WildsCore.LOG.warn("Could not write the key dump", ex);
        }
    }
}
