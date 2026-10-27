package ua.wilds.core.skills;

import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every big ability the skill tree can unlock. An ability that needs other mods names them; when one of them is
 * missing, the node of the tree gives its fallback ability instead (written in the tree data next to it), so a server
 * without Iron's Spells or Goblins still has a working tree.
 */
public final class Ability {
    private static final Map<String, List<String>> NEEDS = new LinkedHashMap<>();

    static {
        // warrior
        NEEDS.put("weapon_throw", List.of());
        NEEDS.put("heavy_strike", List.of());
        NEEDS.put("retribution", List.of());
        NEEDS.put("last_stand", List.of());
        // hunter
        NEEDS.put("hunters_mark", List.of());
        NEEDS.put("volley", List.of());
        NEEDS.put("beast_master", List.of());
        NEEDS.put("twin_births", List.of());
        // magic
        NEEDS.put("spell_reflect", List.of());
        NEEDS.put("free_spell", List.of("irons_spellbooks"));
        NEEDS.put("mana_harvest", List.of("irons_spellbooks"));
        NEEDS.put("mana_shield", List.of("irons_spellbooks"));
        NEEDS.put("arcane_ward", List.of());
        NEEDS.put("soul_siphon", List.of());
        // craft
        NEEDS.put("prospector", List.of());
        NEEDS.put("smelters_touch", List.of());
        NEEDS.put("thrifty_crafter", List.of());
        NEEDS.put("vein_miner", List.of());
        // sea
        NEEDS.put("sea_gifts", List.of());
        NEEDS.put("deep_shadow", List.of());
        NEEDS.put("battle_breath", List.of());
        NEEDS.put("deep_sight", List.of());
        // explorer
        NEEDS.put("danger_sense", List.of());
        NEEDS.put("rider", List.of());
        NEEDS.put("well_rested", List.of());
        NEEDS.put("pathfinder", List.of());
        // diplomat
        NEEDS.put("quiet_step", List.of());
        NEEDS.put("goblin_friend", List.of("goblins_tyranny"));
        NEEDS.put("peacemaker", List.of());
        NEEDS.put("silver_tongue", List.of());
        NEEDS.put("piglin_friend", List.of());
        // trader
        NEEDS.put("generous_deal", List.of());
        NEEDS.put("village_gifts", List.of());
        NEEDS.put("mercenaries", List.of());
        NEEDS.put("haggler", List.of());
    }

    private Ability() {}

    public static boolean known(String id) {
        return NEEDS.containsKey(id);
    }

    /** the ability works on this installation (its mods are there) */
    public static boolean available(String id) {
        List<String> mods = NEEDS.get(id);
        if (mods == null) return false;
        for (String m : mods) if (!ModList.get().isLoaded(m)) return false;
        return true;
    }

    public static Iterable<String> all() {
        return NEEDS.keySet();
    }
}
