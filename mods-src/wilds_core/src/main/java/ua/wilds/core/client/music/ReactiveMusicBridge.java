package ua.wilds.core.client.music;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.fml.ModList;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Talks to Reactive Music by reflection, so the pack runs the same without it: sets its BOSS / NEARBY_MOBS flags and asks
 * it to fall silent while a boss theme of ours plays. Every lookup is done once; a failure turns the bridge off for good.
 */
public final class ReactiveMusicBridge {
    private static boolean tried, ok;
    private static Map<Object, Boolean> events;
    private static Object boss, combat;
    private static Method muteFor;

    private ReactiveMusicBridge() {}

    @SuppressWarnings("unchecked")
    private static boolean init() {
        if (tried) return ok;
        tried = true;
        if (!ModList.get().isLoaded("reactivemusic")) return false;
        try {
            Class<?> picker = Class.forName("circuitlord.reactivemusic.SongPicker");
            Class<?> type = Class.forName("circuitlord.reactivemusic.SongpackEventType");
            Field map = picker.getField("songpackEventMap");
            events = (Map<Object, Boolean>) map.get(null);
            boss = enumValue(type, "BOSS");
            combat = enumValue(type, "NEARBY_MOBS");
            muteFor = Class.forName("circuitlord.reactivemusic.ReactiveMusic").getMethod("trackSoundMuteMusic", SoundInstance.class, boolean.class);
            ok = events != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            WildsCore.LOG.warn("Wilds Core: Reactive Music changed, combat and boss music fall back to its own rules", e);
        }
        return ok;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    /** called by the mixin right after Reactive Music computed its flags */
    public static void applyFlags() {
        if (!init()) return;
        try {
            events.put(boss, MusicDirector.bossFight());
            events.put(combat, MusicDirector.inCombat());
        } catch (RuntimeException e) {
            ok = false;
        }
    }

    /** Reactive Music fades out while this sound plays and comes back after it */
    public static void muteWhile(SoundInstance sound) {
        if (!init() || muteFor == null) return;
        try {
            muteFor.invoke(null, sound, true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            muteFor = null;
        }
    }
}
