package ua.wilds.core.skills;

import java.util.HashMap;
import java.util.Map;

/** The local player's abilities as the server last sent them (only the client fills this). */
public final class ClientAbilities {
    private static Map<String, Integer> abilities = new HashMap<>();

    private ClientAbilities() {}

    public static void set(Map<String, Integer> a) {
        abilities = new HashMap<>(a);
    }

    public static boolean has(String id) {
        return abilities.getOrDefault(id, 0) > 0;
    }

    public static int level(String id) {
        return abilities.getOrDefault(id, 0);
    }

    public static void clear() {
        abilities = new HashMap<>();
    }
}
