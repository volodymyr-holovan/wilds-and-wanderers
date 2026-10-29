package ua.wilds.core.client.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import ua.wilds.core.WildsCore;

import java.io.Reader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides what the fight music should be, every few ticks, from what the client can see:
 * <ul>
 * <li>a <b>boss fight</b>: an entity of the boss tag (or of the pack's extra list) is near and engaged - it is aggressive,
 * the player hit it recently, or a boss bar is on screen. Bosses whose mod plays its own theme are left to it
 * (Reactive Music falls silent through its mute list); every other boss gets its own theme from assets/wilds_core/music/bosses.json,
 * and a boss without one gets Reactive Music's boss songs;</li>
 * <li><b>combat</b>: the player hit a living thing, or got hurt with an enemy or a projectile near, in the last 8 seconds,
 * or an enemy close by is attacking and no other player is closer to it. Reactive Music plays its combat entry then.</li>
 * </ul>
 * Each player's music is their own: a fight of a friend standing near starts nothing for this player (a mob after the
 * friend is closer to the friend), a boss fighting someone else is not this player's boss fight.
 * <ul>
 * </ul>
 * All of it is client-side and works for any mod: no server support needed.
 */
public final class MusicDirector {
    private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final int COMBAT_TICKS = 160, BOSS_LINGER = 80, SCAN_EVERY = 5;
    private static final double BOSS_RANGE = 64, ENEMY_RANGE = 12;

    private static final Map<ResourceLocation, ResourceLocation> themes = new HashMap<>();
    private static final Set<String> ownTheme = new HashSet<>();
    private static final Set<ResourceLocation> extraBosses = new HashSet<>();

    private static long tick, lastCombat = -10000, lastBossSeen = -10000;
    private static int lastHurtTime;
    private static Entity lastTarget;
    private static long lastTargetHit = -10000;
    private static LivingEntity boss;
    private static boolean fighting, bossFight;
    private static SoundInstance playing;
    private static ResourceLocation playingTheme;
    private static long themeStarted;
    private static Field overlayEvents;

    private MusicDirector() {}

    public static boolean inCombat() {
        return fighting && !bossFight;
    }

    public static boolean bossFight() {
        return bossFight;
    }

    /** the player hit something (melee, or a shot that landed) */
    public static void playerAttacked(Entity target) {
        if (!(target instanceof LivingEntity) || target == Minecraft.getInstance().player) return;
        lastTarget = target;
        lastTargetHit = tick;
        lastCombat = tick;
    }

    public static void reload(ResourceManager rm) {
        themes.clear();
        ownTheme.clear();
        extraBosses.clear();
        rm.getResource(ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "music/bosses.json")).ifPresent(res -> {
            try (Reader r = res.openAsReader()) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                for (var e : o.getAsJsonObject("themes").entrySet()) {
                    ResourceLocation mob = ResourceLocation.tryParse(e.getKey()), sound = ResourceLocation.tryParse(e.getValue().getAsString());
                    if (mob != null && sound != null) themes.put(mob, sound);
                }
                for (JsonElement e : o.getAsJsonArray("own_theme")) ownTheme.add(e.getAsString());
                for (JsonElement e : o.getAsJsonArray("bosses")) {
                    ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                    if (id != null) extraBosses.add(id);
                }
            } catch (Exception ex) {
                WildsCore.LOG.warn("Wilds Core: bad music/bosses.json", ex);
            }
        });
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        tick++;
        if (player == null || mc.level == null) {
            stopTheme(mc);
            fighting = bossFight = false;
            boss = null;
            return;
        }
        // hurt: the hurt animation starts when the server reports damage
        if (player.hurtTime > lastHurtTime && player.hurtTime > 0 && threatNear(player)) lastCombat = tick;
        lastHurtTime = player.hurtTime;
        if (tick % SCAN_EVERY == 0) scan(mc, player);
        if (lastTarget != null && lastTarget.isAlive() && lastTarget.distanceToSqr(player) < 24 * 24 && tick - lastTargetHit < 600
                && lastTarget instanceof Mob m && m.isAggressive()) lastCombat = tick;
        fighting = tick - lastCombat < COMBAT_TICKS;
        bossFight = tick - lastBossSeen < BOSS_LINGER;
        updateTheme(mc);
    }

    private static boolean threatNear(LocalPlayer p) {
        AABB box = p.getBoundingBox().inflate(16);
        return !p.level().getEntities(p, box, e -> e instanceof Enemy || e instanceof Projectile).isEmpty();
    }

    private static void scan(Minecraft mc, LocalPlayer player) {
        List<LivingEntity> near = mc.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BOSS_RANGE), LivingEntity::isAlive);
        boolean bars = bossBarsShown(mc);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : near) {
            if (e == player) continue;
            double d = e.distanceToSqr(player);
            if (isBoss(e) && engaged(e, player, bars)) {
                if (d < bestDist) {
                    best = e;
                    bestDist = d;
                }
            } else if (e instanceof Enemy && e instanceof Mob m && m.isAggressive() && d < ENEMY_RANGE * ENEMY_RANGE && huntsMe(m, player)) {
                lastCombat = tick;
            }
        }
        if (best != null) {
            boss = best;
            lastBossSeen = tick;
            lastCombat = tick;
        } else if (tick - lastBossSeen >= BOSS_LINGER) {
            boss = null;
        }
    }

    /**
     * this player's fight with the boss: they hit it lately, or it is after them (aggressive or showing its bar, and no
     * other player nearer to it). A boss fighting a friend farther off is the friend's fight, not this player's.
     */
    private static boolean engaged(LivingEntity e, LocalPlayer me, boolean bars) {
        if (e == lastTarget && tick - lastTargetHit < 600) return true;
        boolean active = bars || (e instanceof Mob m && m.isAggressive());
        return active && huntsMe(e, me);
    }

    /**
     * the client cannot see a mob's target; "after me" is read as: no other player is closer to it. So a monster attacking
     * a friend next to this player plays the fight music for the friend, not for everyone around.
     */
    private static boolean huntsMe(LivingEntity mob, LocalPlayer me) {
        double mine = mob.distanceToSqr(me);
        for (var other : me.clientLevel.players()) {
            if (other != me && !other.isSpectator() && other.isAlive() && other.distanceToSqr(mob) + 4 < mine) return false;
        }
        return true;
    }

    private static boolean isBoss(LivingEntity e) {
        EntityType<?> t = e.getType();
        return t.is(BOSS_TAG) || extraBosses.contains(BuiltInRegistries.ENTITY_TYPE.getKey(t));
    }

    private static boolean bossBarsShown(Minecraft mc) {
        try {
            if (overlayEvents == null) {
                overlayEvents = BossHealthOverlay.class.getDeclaredField("events");
                overlayEvents.setAccessible(true);
            }
            return !((Map<?, ?>) overlayEvents.get(mc.gui.getBossOverlay())).isEmpty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** a theme of ours for the boss, unless its mod plays its own or the sound is not there */
    private static ResourceLocation themeFor(LivingEntity e, Minecraft mc) {
        if (e == null) return null;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (ownTheme.contains(id.toString()) || ownTheme.contains(id.getNamespace() + ":*")) return null;
        ResourceLocation s = themes.get(id);
        return s != null && mc.getSoundManager().getSoundEvent(s) != null ? s : null;
    }

    private static void updateTheme(Minecraft mc) {
        ResourceLocation want = bossFight ? themeFor(boss, mc) : null;
        if (want == null) {
            stopTheme(mc);
            return;
        }
        // a new theme, or the old one ended (themes do not loop by themselves); the sound engine needs a moment to start one
        boolean ended = playing != null && tick - themeStarted > 40 && !mc.getSoundManager().isActive(playing);
        if (!want.equals(playingTheme) || playing == null || ended) {
            stopTheme(mc);
            playing = new SimpleSoundInstance(want, SoundSource.MUSIC, 1f, 1f, SoundInstance.createUnseededRandom(), false, 0,
                    SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            playingTheme = want;
            themeStarted = tick;
            mc.getSoundManager().play(playing);
            ReactiveMusicBridge.muteWhile(playing);
        }
        mc.getMusicManager().stopPlaying(); // vanilla music, when Reactive Music is not there
    }

    private static void stopTheme(Minecraft mc) {
        if (playing != null) mc.getSoundManager().stop(playing);
        playing = null;
        playingTheme = null;
    }
}
