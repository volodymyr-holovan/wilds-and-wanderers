package ua.wilds.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import ua.wilds.core.WildsCore;

/**
 * The pack's defaults the game's own settings file does not keep on its own, applied once per game folder: the game
 * in Ukrainian, and the quest book on J (FTB Quests ships its key unbound, and Mekanism's legs mode takes J). After
 * that the player's choices are theirs: the flag file says it was done.
 */
public final class FirstRunDefaults {
    private static final String FLAG = "wilds_core-defaults.txt";
    private static final String LANGUAGE = "uk_ua";
    private static final String QUEST_KEY = "key.ftbquests.quests";
    private static boolean pending;

    private FirstRunDefaults() {}

    /** From the mod constructor: the options are loaded by now, the language manager is not yet made from them. */
    public static void init() {
        pending = !Files.exists(flag());
        if (!pending) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) mc.options.languageCode = LANGUAGE;
    }

    /** The first client tick: every mod's key is registered and read from the settings by now. */
    public static void tick(ClientTickEvent.Post e) {
        if (!pending) return;
        pending = false;
        Minecraft mc = Minecraft.getInstance();
        try {
            InputConstants.Key j = InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_J);
            KeyMapping quests = null;
            for (KeyMapping k : mc.options.keyMappings) if (QUEST_KEY.equals(k.getName())) quests = k;
            if (quests != null && quests.isUnbound()) {
                for (KeyMapping k : mc.options.keyMappings)
                    if (k != quests && k.getKey().equals(j) && k.getKeyModifier() == KeyModifier.NONE)
                        k.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
                quests.setKeyModifierAndCode(KeyModifier.NONE, j);
                KeyMapping.resetMapping();
            }
            mc.options.save();
            Files.writeString(flag(), "Wilds & Wanderers: the language and the quest book key were set on the first launch.\n"
                + "Delete this file to set them again.\n");
        } catch (Exception ex) {
            WildsCore.LOG.warn("Could not apply the pack's first-launch settings", ex);
        }
    }

    private static Path flag() {
        return FMLPaths.CONFIGDIR.get().resolve(FLAG);
    }
}
