package ua.wilds.core.skills;

import java.util.Set;

/**
 * Small numeric bonuses of the tree that are not attributes. Each is read by the mechanic it changes
 * (see the branch classes); values add up over the nodes a player took and are capped where noted.
 */
public final class Perks {
    /** fraction of fire, lava and burning damage ignored (max 0.8) */
    public static final String FIRE_GUARD = "fire_guard";
    /** fraction of damage from water creatures ignored (max 0.7) */
    public static final String AQUA_GUARD = "aqua_guard";
    /** fraction of magic damage ignored (max 0.6) */
    public static final String MAGIC_GUARD = "magic_guard";
    /** chance that a mined ore drops one more of its drop */
    public static final String EXTRA_ORE = "extra_ore";
    /** chance that a tool or weapon does not lose durability (max 0.75) */
    public static final String TOOL_SAVER = "tool_saver";
    /** extra speed (fraction) of arrows, tridents and spell projectiles the player launches: they fly further */
    public static final String FAR_SHOT = "far_shot";
    /** hearts healed every 5 seconds while not starving */
    public static final String REGEN = "regen";
    /** extra speed (fraction) of the animal the player rides */
    public static final String MOUNT = "mount";
    /** emeralds off every villager trade */
    public static final String DISCOUNT = "discount";
    /** chance that a harvested crop drops one more of its produce */
    public static final String HARVEST = "harvest";
    /** extra saturation (fraction) from food */
    public static final String HEARTY = "hearty";
    /** extra damage and health (fraction) of the player's tamed animals */
    public static final String TAMER = "tamer";
    /** better chance to convince quest givers (lie, haggle) */
    public static final String PERSUASION = "persuasion";

    private static final Set<String> ALL = Set.of(FIRE_GUARD, AQUA_GUARD, MAGIC_GUARD, EXTRA_ORE, TOOL_SAVER, FAR_SHOT, REGEN, MOUNT,
            DISCOUNT, HARVEST, HEARTY, TAMER, PERSUASION);

    private Perks() {}

    public static boolean known(String id) {
        return ALL.contains(id);
    }
}
