package ua.wilds.core.skills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the skill tree gives a player right now: ability levels and perk values. Rebuilt by the tree itself (Puffish
 * Skills re-applies every unlocked reward when the player joins), so nothing here is saved, except cooldowns, which
 * live in the player's persistent data and survive restarts.
 */
public final class SkillState {
    private static final Map<UUID, SkillState> STATES = new ConcurrentHashMap<>();
    private static final String COOLDOWNS = "wilds_core:cooldowns";

    public final Map<String, Integer> abilities = new HashMap<>();
    public final Map<String, Double> perks = new HashMap<>();
    /** rewards contribute separately (several nodes give the same perk): key = reward instance */
    final Map<Object, Double> perkParts = new HashMap<>();
    final Map<Object, String> perkOf = new HashMap<>();
    /** unlocked count per ability reward instance */
    final Map<Object, Integer> abilityParts = new HashMap<>();

    private SkillState() {}

    public static SkillState of(Player p) {
        return STATES.computeIfAbsent(p.getUUID(), u -> new SkillState());
    }

    public static void forget(UUID id) {
        STATES.remove(id);
    }

    public static boolean has(Player p, String ability) {
        SkillState s = STATES.get(p.getUUID());
        return s != null && s.abilities.getOrDefault(ability, 0) > 0;
    }

    public static int level(Player p, String ability) {
        SkillState s = STATES.get(p.getUUID());
        return s == null ? 0 : s.abilities.getOrDefault(ability, 0);
    }

    public static double perk(Player p, String perk) {
        SkillState s = STATES.get(p.getUUID());
        return s == null ? 0 : s.perks.getOrDefault(perk, 0.0);
    }

    void setPerkPart(Object reward, String perk, double value) {
        if (value == 0) {
            perkParts.remove(reward);
            perkOf.remove(reward);
        } else {
            perkParts.put(reward, value);
            perkOf.put(reward, perk);
        }
        double sum = 0;
        for (var e : perkParts.entrySet()) if (perk.equals(perkOf.get(e.getKey()))) sum += e.getValue();
        if (sum == 0) perks.remove(perk);
        else perks.put(perk, sum);
    }

    // ---------------------------------------------------------------- cooldowns (game time, persistent)

    private static CompoundTag cooldowns(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!kept.contains(COOLDOWNS)) kept.put(COOLDOWNS, new CompoundTag());
        return kept.getCompound(COOLDOWNS);
    }

    /** true when the ability is ready (and starts its cooldown of the given ticks) */
    public static boolean use(ServerPlayer p, String key, long cooldownTicks) {
        long now = p.serverLevel().getGameTime();
        CompoundTag c = cooldowns(p);
        if (c.getLong(key) > now) return false;
        c.putLong(key, now + cooldownTicks);
        return true;
    }

    public static boolean ready(ServerPlayer p, String key) {
        return cooldowns(p).getLong(key) <= p.serverLevel().getGameTime();
    }

    public static long remaining(ServerPlayer p, String key) {
        return Math.max(0, cooldowns(p).getLong(key) - p.serverLevel().getGameTime());
    }

    /** once per in-game day (the counter is the day number) */
    public static boolean useDaily(ServerPlayer p, String key) {
        long day = p.serverLevel().getDayTime() / 24000L;
        CompoundTag c = cooldowns(p);
        if (c.contains(key) && c.getLong(key) >= day) return false;
        c.putLong(key, day);
        return true;
    }
}
