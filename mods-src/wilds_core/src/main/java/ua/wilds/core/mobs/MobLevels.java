package ua.wilds.core.mobs;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mob levels (docs/balance/mob-levels.md): every hostile mob and every boss gets the level of the strongest player
 * near it (skill tree level times the Apotheosis world tier), which then drifts slowly; the level raises health, armor,
 * damage and a few resistances and unlocks attacks. Natural spawns of the vanilla families are partly replaced by new
 * kinds (MobData variants), each stronger with better loot. All the extra strength acts against players only: colony
 * citizens, villagers and pets fight these mobs as if they were vanilla.
 */
public final class MobLevels {
    static final String TAG = "wilds_mob";
    private static final ResourceLocation HP = rl("lvl_health"), ARMOR = rl("lvl_armor"), TOUGH = rl("lvl_toughness"),
            SPEED = rl("lvl_speed"), KB = rl("lvl_knockback"), SCALE = rl("kind_scale"), ABSORB = rl("lvl_shield");

    /** what the game knows about a leveled mob while it is loaded (the NBT under TAG keeps it between loads) */
    public static final class Info {
        public float level, applied;
        public String variant;
        public boolean boss, minion;
        public List<String> abilities = new ArrayList<>();
        /** damage multiplier against players; health multiplier (non-player damage is scaled by it, so to them the mob is vanilla) */
        public double dmgMult = 1, hpMult = 1;
        /** health above the game's cap of 1024: the rest is a damage divisor */
        public double extra = 1;
        // ability state
        final Map<String, Long> cooldown = new HashMap<>();
        long lastHurt;
        boolean enraged, summoned, guard66, guard33;

        public MobData.Variant kind() { return variant == null ? null : MobData.variants.get(variant); }
        public int tier() { MobData.Variant v = kind(); return v == null ? 0 : v.tier(); }
    }

    static final Map<Mob, Info> TRACKED = new java.util.WeakHashMap<>();
    private static final Map<EntityType<?>, Boolean> BOSS_TYPES = new ConcurrentHashMap<>();
    private static final Set<String> BOSS_IDS = Set.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian",
            "twilightforest:naga", "twilightforest:lich", "twilightforest:minoshroom", "twilightforest:hydra", "twilightforest:knight_phantom",
            "twilightforest:ur_ghast", "twilightforest:alpha_yeti", "twilightforest:snow_queen", "twilightforest:plateau_boss",
            "cataclysm:ignis", "cataclysm:netherite_monstrosity", "cataclysm:the_harbinger", "cataclysm:the_leviathan", "cataclysm:ender_guardian",
            "cataclysm:maledictus", "cataclysm:ancient_remnant", "cataclysm:scylla", "mowziesmobs:ferrous_wroughtnaut", "mowziesmobs:frostmaw",
            "mowziesmobs:umvuthi", "mowziesmobs:sculptor", "irons_spellbooks:dead_king", "irons_spellbooks:fire_boss", "aether:slider",
            "aether:valkyrie_queen", "aether:sun_spirit", "iceandfire:cyclops", "iceandfire:hydra", "iceandfire:sea_serpent", "iceandfire:dread_lich",
            "alexsmobs:void_worm", "alexscaves:luxtructosaurus");
    /** forced effective levels for tests (/wildsmobs forcelevel) */
    static final Map<UUID, Integer> FORCED = new ConcurrentHashMap<>();
    private static final Map<UUID, double[]> PLAYER_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> SWAPPING = ThreadLocal.withInitial(() -> false);
    private static Method worldTierGet;
    private static boolean worldTierTried;
    private static long tick;
    /** what happened to natural spawns of the families (/wildsmobs stats) */
    static final Map<String, Integer> STATS = new ConcurrentHashMap<>();
    private static void stat(String k) { STATS.merge(k, 1, Integer::sum); }

    private MobLevels() {}

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(WildsCore.ID, p); }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) ->
                e.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) MobData::reload));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, MobLevels::onFinalize);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, MobLevels::onJoin);
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> { if (e.getEntity() instanceof Mob m) TRACKED.remove(m); });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, MobLevels::onDamage);
        NeoForge.EVENT_BUS.addListener(MobLevels::onDrops);
        NeoForge.EVENT_BUS.addListener(MobLevels::onXp);
        NeoForge.EVENT_BUS.addListener(MobLevels::onDeath);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> onTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> PLAYER_CACHE.remove(e.getEntity().getUUID()));
        MobCommand.register();
    }

    public static Info info(Mob m) {
        return TRACKED.get(m);
    }

    // ------------------------------------------------------------------ player level
    /** {skill level, world tier factor} of a player, refreshed every 5 s */
    static double[] playerLevel(ServerPlayer p) {
        Integer forced = FORCED.get(p.getUUID());
        if (forced != null) return new double[]{forced, 1};
        double[] c = PLAYER_CACHE.get(p.getUUID());
        if (c != null && tick - (long) c[2] < 100) return c;
        double lvl = 0;
        if (ModList.get().isLoaded("puffish_skills")) lvl = SkillLevel.of(p);
        double wt = 1;
        try {
            if (!worldTierTried) {
                worldTierTried = true;
                if (ModList.get().isLoaded("apotheosis"))
                    worldTierGet = Class.forName("dev.shadowsoffire.apotheosis.tiers.WorldTier").getMethod("getTier", Player.class);
            }
            if (worldTierGet != null) {
                Object tier = worldTierGet.invoke(null, p);
                if (tier instanceof Enum<?> en) wt = MobData.worldTier.getOrDefault(en.name().toLowerCase(java.util.Locale.ROOT), 1.0);
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            worldTierGet = null;
        }
        c = new double[]{lvl, wt, tick};
        PLAYER_CACHE.put(p.getUUID(), c);
        return c;
    }

    /** effective level of the strongest player within the radius, or -1 when none is near */
    static double targetLevel(ServerLevel level, Entity at) {
        double best = -1, r2 = MobData.radius * MobData.radius;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || p.distanceToSqr(at) > r2) continue;
            double[] l = playerLevel(p);
            best = Math.max(best, l[0] * l[1]);
        }
        return best;
    }

    // ------------------------------------------------------------------ who gets levels
    static boolean isBoss(Mob m) {
        return BOSS_TYPES.computeIfAbsent(m.getType(), t -> {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
            if (BOSS_IDS.contains(id) || t.is(Tags.EntityTypes.BOSSES)) return true;
            for (Class<?> c = m.getClass(); c != null && c != Mob.class && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) if (BossEvent.class.isAssignableFrom(f.getType())) return true;
            }
            return false;
        });
    }

    static boolean eligible(Mob m) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType());
        String ns = id.getNamespace(), path = id.getPath();
        if (ns.equals("minecolonies") || ns.equals(WildsCore.ID) || ns.equals("corpse") || ns.equals("guardvillagers")) return false;
        if (ns.equals("iceandfire") && path.endsWith("_dragon")) return false;
        if (path.startsWith("summoned_") || m.isNoAi()) return false;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;
        return m instanceof Enemy || isBoss(m);
    }

    // ------------------------------------------------------------------ spawning: new kinds replace vanilla ones
    private static void onFinalize(FinalizeSpawnEvent e) {
        if (!MobData.loaded || SWAPPING.get() || e.isSpawnCancelled()) return;
        MobSpawnType type = e.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.PATROL) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        Mob mob = e.getEntity();
        MobData.Family fam = MobData.byBase.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        if (fam == null) return;
        stat("family_spawn");
        mob.setPos(e.getX(), e.getY(), e.getZ());
        double t = targetLevel(level, mob);
        if (t < 0) { stat("no_player"); return; }
        MobData.Variant v = pick(fam, t, ThreadLocalRandom.current());
        if (v == null) { stat("kept_vanilla"); return; }
        ResourceLocation own = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (v.entity() == null || v.entity().equals(own)) {
            mob.getPersistentData().putString(TAG + "_pending", v.id());
            stat("in_place");
            return;
        }
        EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getOptional(v.entity()).orElse(null);
        if (et == null) return;
        Entity made = et.create(level);
        if (!(made instanceof Mob nm)) {
            if (made != null) made.discard();
            return;
        }
        nm.moveTo(e.getX(), e.getY(), e.getZ(), mob.getYRot(), 0);
        if (!level.noCollision(nm) || !level.getWorldBorder().isWithinBounds(nm.blockPosition())) {
            stat("swap_no_room");
            nm.discard();
            return;
        }
        nm.getPersistentData().putString(TAG + "_pending", v.id());
        SWAPPING.set(true);
        try {
            EventHooks.finalizeMobSpawn(nm, level, e.getDifficulty(), type, null);
        } catch (RuntimeException ex) {
            WildsCore.LOG.debug("Wilds Core: {} could not be set up as a new kind: {}", v.entity(), ex.toString());
            nm.discard();
            return;
        } finally {
            SWAPPING.set(false);
        }
        if (level.addFreshEntity(nm)) { e.setSpawnCancelled(true); stat("swapped"); } else stat("swap_refused");
    }

    /** among the unlocked kinds of the family the newest is the most likely */
    static MobData.Variant pick(MobData.Family fam, double level, java.util.Random rnd) {
        List<MobData.Variant> all = MobData.byFamily.getOrDefault(fam.id(), List.of());
        List<MobData.Variant> open = new ArrayList<>();
        for (MobData.Variant v : all) if (MobData.unlock(v.tier()) <= level) open.add(v);
        if (open.isEmpty()) return null;
        double chance = Math.min(MobData.replaceMax, MobData.replaceBase + level * MobData.replaceRate);
        if (rnd.nextDouble() >= chance) return null;
        double total = 0, w = 1;
        double[] ws = new double[open.size()];
        for (int i = open.size() - 1; i >= 0; i--) { ws[i] = w; total += w; w /= 2; }
        double r = rnd.nextDouble() * total;
        for (int i = open.size() - 1; i >= 0; i--) { r -= ws[i]; if (r <= 0) return open.get(i); }
        return open.get(open.size() - 1);
    }

    // ------------------------------------------------------------------ joining the world
    private static void onJoin(EntityJoinLevelEvent e) {
        if (!MobData.loaded || !(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof Mob mob)) return;
        CompoundTag pd = mob.getPersistentData();
        if (pd.contains(TAG, Tag.TAG_COMPOUND)) {
            Info in = read(pd.getCompound(TAG));
            TRACKED.put(mob, in);
            if (in.variant != null) hostile(mob);
            return;
        }
        if (!eligible(mob)) return;
        double t = targetLevel(level, mob);
        String pending = pd.getString(TAG + "_pending");
        pd.remove(TAG + "_pending");
        Info in = new Info();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        in.level = (float) Math.max(0, t) * (float) (MobData.spreadLo + rnd.nextDouble() * (MobData.spreadHi - MobData.spreadLo));
        in.boss = isBoss(mob);
        MobData.Variant v = pending.isEmpty() ? null : MobData.variants.get(pending);
        if (v != null) {
            in.variant = v.id();
            in.level = Math.max(in.level, MobData.unlock(v.tier()));
            in.abilities.addAll(v.abilities());
            dress(mob, v);
        }
        if (in.boss) in.abilities.addAll(MobData.bossAbilities);
        for (MobData.LevelAbility la : MobData.levelAbilities) {
            int from = la.from() + (v != null ? MobData.unlock(v.tier()) : 0);
            if (in.level >= from && rnd.nextDouble() < la.chance() && !in.abilities.contains(la.ability())) in.abilities.add(la.ability());
        }
        apply(mob, in, true);
        TRACKED.put(mob, in);
        if (v != null) hostile(mob);
    }

    /** a new kind attacks players even when its base mob is neutral (endermen, piglins, spiders by day, some mod mobs) */
    private static void hostile(Mob mob) {
        if (mob instanceof net.minecraft.world.entity.monster.Slime) return; // slimes hunt on their own
        ((ua.wilds.core.mixin.MobAccessor) mob).wilds$targets().addGoal(1,
                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob, Player.class, true));
    }

    /** a new kind's look: name, size, equipment, the creeper's charge and invisibility */
    private static void dress(Mob mob, MobData.Variant v) {
        if (v.scale() != 1f) set(mob, Attributes.SCALE, SCALE, v.scale() - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        for (Map.Entry<String, ResourceLocation> q : v.equip().entrySet()) {
            try {
                EquipmentSlot slot = EquipmentSlot.byName(q.getKey());
                mob.setItemSlot(slot, new ItemStack(BuiltInRegistries.ITEM.get(q.getValue())));
                mob.setDropChance(slot, 0.03f);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (v.abilities().contains("powered") && mob instanceof Creeper c) {
            CompoundTag t = new CompoundTag();
            c.addAdditionalSaveData(t);
            t.putBoolean("powered", true);
            c.readAdditionalSaveData(t);
        }
        if (v.abilities().contains("invisible")) mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
    }

    // ------------------------------------------------------------------ stats from the level
    static void apply(Mob mob, Info in, boolean fresh) {
        double l = in.level;
        int tier = in.tier();
        float ratio = mob.getMaxHealth() > 0 ? mob.getHealth() / mob.getMaxHealth() : 1;
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        double baseHp = hp == null ? 20 : Math.max(1, hp.getBaseValue());
        double targetHp;
        if (in.boss) targetHp = MobMath.bossHp(baseHp, l);
        else if (tier > 0) targetHp = MobMath.tierHp(tier, l);
        else targetHp = baseHp * MobMath.hpMult(l);
        in.hpMult = targetHp / baseHp;
        double cap = Attributes.MAX_HEALTH.value() instanceof net.minecraft.world.entity.ai.attributes.RangedAttribute ra ? ra.getMaxValue() : 1024;
        in.extra = targetHp > cap ? targetHp / cap : 1;
        set(mob, Attributes.MAX_HEALTH, HP, Math.min(targetHp, cap) / baseHp - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(mob, Attributes.ARMOR, ARMOR, MobMath.armorAdd(l, tier), AttributeModifier.Operation.ADD_VALUE);
        set(mob, Attributes.ARMOR_TOUGHNESS, TOUGH, MobMath.toughAdd(l), AttributeModifier.Operation.ADD_VALUE);
        set(mob, Attributes.MOVEMENT_SPEED, SPEED, MobMath.speedAdd(l), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, KB, in.boss ? Math.max(0.6, MobMath.kbAdd(l)) : MobMath.kbAdd(l), AttributeModifier.Operation.ADD_VALUE);
        if (in.abilities.contains("shield")) set(mob, Attributes.MAX_ABSORPTION, ABSORB, targetHp * 0.15, AttributeModifier.Operation.ADD_VALUE);

        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double nativeDmg = atk != null && atk.getBaseValue() > 0 ? atk.getBaseValue() : 4;
        if (in.boss) in.dmgMult = MobMath.bossDmg(nativeDmg, l) / nativeDmg;
        else if (tier > 0) in.dmgMult = MobMath.tierDmg(tier, l) / nativeDmg;
        else in.dmgMult = MobMath.dmgMult(l);
        if (in.minion) in.dmgMult *= 0.6;

        mob.setHealth(fresh ? mob.getMaxHealth() : ratio * mob.getMaxHealth());
        in.applied = in.level;
        MobData.Variant v = in.kind();
        if (v != null) mob.setCustomName(Component.translatable("wilds_core.mob.named", Component.translatable("wilds_core.mob." + v.id()), (int) l));
        write(mob, in);
    }

    private static void set(Mob mob, Holder<Attribute> attr, ResourceLocation id, double value, AttributeModifier.Operation op) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a == null) return;
        a.removeModifier(id);
        if (Math.abs(value) > 1e-6) a.addPermanentModifier(new AttributeModifier(id, value, op));
    }

    static void write(Mob mob, Info in) {
        CompoundTag t = new CompoundTag();
        t.putFloat("l", in.level);
        t.putFloat("a", in.applied);
        if (in.variant != null) t.putString("v", in.variant);
        if (in.boss) t.putBoolean("b", true);
        if (in.minion) t.putBoolean("m", true);
        t.putDouble("dm", in.dmgMult);
        t.putDouble("hm", in.hpMult);
        t.putDouble("ex", in.extra);
        ListTag ab = new ListTag();
        for (String s : in.abilities) ab.add(StringTag.valueOf(s));
        t.put("ab", ab);
        t.putBoolean("e", in.enraged);
        t.putBoolean("s", in.summoned);
        t.putBoolean("g6", in.guard66);
        t.putBoolean("g3", in.guard33);
        mob.getPersistentData().put(TAG, t);
    }

    static Info read(CompoundTag t) {
        Info in = new Info();
        in.level = t.getFloat("l");
        in.applied = t.getFloat("a");
        in.variant = t.contains("v") ? t.getString("v") : null;
        in.boss = t.getBoolean("b");
        in.minion = t.getBoolean("m");
        in.dmgMult = t.contains("dm") ? t.getDouble("dm") : 1;
        in.hpMult = t.contains("hm") ? t.getDouble("hm") : 1;
        in.extra = t.contains("ex") ? t.getDouble("ex") : 1;
        ListTag ab = t.getList("ab", Tag.TAG_STRING);
        for (int i = 0; i < ab.size(); i++) in.abilities.add(ab.getString(i));
        in.enraged = t.getBoolean("e");
        in.summoned = t.getBoolean("s");
        in.guard66 = t.getBoolean("g6");
        in.guard33 = t.getBoolean("g3");
        return in;
    }

    /** a helper mob (summoned reinforcements, split pieces): weaker, no loot */
    static Mob spawnHelper(ServerLevel level, Mob owner, EntityType<?> type, double x, double y, double z, float levelFactor, float scale) {
        Entity made = type.create(level);
        if (!(made instanceof Mob m)) { if (made != null) made.discard(); return null; }
        m.moveTo(x, y, z, owner.getYRot(), 0);
        if (!level.noCollision(m)) { m.discard(); return null; }
        Info src = TRACKED.get(owner);
        Info in = new Info();
        in.level = (src == null ? 0 : src.level) * levelFactor;
        in.minion = true;
        m.getPersistentData().put(TAG, new CompoundTag()); // marks it as handled before it joins
        SWAPPING.set(true);
        try {
            EventHooks.finalizeMobSpawn(m, level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        } catch (RuntimeException ignored) {
        } finally {
            SWAPPING.set(false);
        }
        if (scale != 1f) set(m, Attributes.SCALE, SCALE, scale - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(m, in, true);
        if (owner.getTarget() != null) m.setTarget(owner.getTarget());
        if (!level.addFreshEntity(m)) return null;
        TRACKED.put(m, in);
        return m;
    }

    // ------------------------------------------------------------------ slow drift of the level
    private static void onTick(MinecraftServer server) {
        tick++;
        if (!MobData.loaded || TRACKED.isEmpty()) return;
        List<Map.Entry<Mob, Info>> list = new ArrayList<>(TRACKED.entrySet());
        for (Map.Entry<Mob, Info> en : list) {
            Mob mob = en.getKey();
            Info in = en.getValue();
            if (mob == null || !mob.isAlive() || !(mob.level() instanceof ServerLevel level)) continue;
            int phase = (int) ((tick + mob.getId()) % MobData.updateTicks);
            if (phase == 0 && !in.minion) drift(level, mob, in);
            if (!in.abilities.isEmpty() && (tick + mob.getId()) % 10 == 0) MobAbilities.tick(level, mob, in, tick);
        }
        MobAbilities.expire(server, tick);
    }

    private static void drift(ServerLevel level, Mob mob, Info in) {
        double t = targetLevel(level, mob);
        if (t < 0) return;
        double step = MobData.stepBase + MobData.stepRate * in.level;
        double d = Math.max(-step, Math.min(step, t - in.level));
        if (in.boss && d < 0) return; // a boss never gets weaker
        in.level = (float) Math.max(0, in.level + d);
        if (Math.abs(in.level - in.applied) >= 1) apply(mob, in, false);
    }

    // ------------------------------------------------------------------ damage both ways
    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!MobData.loaded) return;
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        Entity attacker = src.getEntity();
        // a leveled mob takes damage
        if (victim instanceof Mob vm) {
            Info in = TRACKED.get(vm);
            if (in != null) {
                float amount = e.getAmount();
                if (!(attacker instanceof Player)) amount *= (float) in.hpMult; // to everything but players it is the plain mob
                amount /= (float) in.extra;
                if (src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_LIGHTNING) || src.is(DamageTypeTags.IS_EXPLOSION)
                        || src.is(DamageTypeTags.IS_FREEZING) || src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC))
                    amount *= (float) (1 - MobMath.resist(in.level));
                e.setAmount(amount);
                in.lastHurt = tick;
                if (attacker instanceof ServerPlayer p) MobAbilities.onHurtByPlayer((ServerLevel) vm.level(), vm, in, p, src, amount, tick);
            }
        }
        // a leveled mob hits a player
        if (victim instanceof Player p && attacker instanceof Mob am) {
            Info in = TRACKED.get(am);
            if (in != null) {
                double mult = src.is(DamageTypeTags.IS_EXPLOSION) ? Math.sqrt(in.dmgMult) : in.dmgMult;
                float amount = (float) (e.getAmount() * mult);
                e.setAmount(amount);
                MobAbilities.onHitPlayer((ServerLevel) am.level(), am, in, p, src, amount);
            }
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Mob m && m.level() instanceof ServerLevel level) {
            Info in = TRACKED.get(m);
            if (in != null) MobAbilities.onDeath(level, m, in);
        }
    }

    // ------------------------------------------------------------------ loot and experience
    private static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !(m.level() instanceof ServerLevel level)) return;
        Info in = TRACKED.get(m);
        if (in == null || in.minion || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<String> tables = new ArrayList<>();
        MobData.Variant v = in.kind();
        if (v != null) {
            tables.add("mobs/family_" + v.loot());
            tables.add("mobs/tier_" + Math.min(6, (v.tier() + 1) / 2));
        } else if (in.boss) {
            int band = (int) Math.max(1, Math.min(6, in.level / 70 + 1));
            tables.add("mobs/tier_" + band);
            tables.add("mobs/tier_" + band);
        } else {
            MobData.Family f = MobData.byBase.get(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()));
            if (f != null && rnd.nextDouble() < MobData.plainLootBase + in.level * MobData.plainLootRate) tables.add("mobs/family_" + f.id());
        }
        for (String t : tables) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rl(t)));
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, m)
                    .withParameter(LootContextParams.ORIGIN, m.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, e.getSource())
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, p)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, e.getSource().getDirectEntity())
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p)
                    .withLuck(p.getLuck())
                    .create(LootContextParamSets.ENTITY);
            for (ItemStack s : table.getRandomItems(params)) {
                ItemEntity ie = new ItemEntity(level, m.getX(), m.getY() + 0.5, m.getZ(), s);
                ie.setDefaultPickUpDelay();
                e.getDrops().add(ie);
            }
        }
    }

    private static void onXp(LivingExperienceDropEvent e) {
        if (!(e.getEntity() instanceof Mob m)) return;
        Info in = TRACKED.get(m);
        if (in == null) return;
        if (in.minion) { e.setDroppedExperience(0); return; }
        double f = in.boss ? 1 + in.level / 100.0 : in.tier() > 0 ? 1 + 0.3 * in.tier() : 1 + in.level / 200.0;
        e.setDroppedExperience((int) Math.round(e.getDroppedExperience() * f));
    }

    /** the skill tree's level (Pufferfish's Skills), kept apart so the class loads without the mod */
    static final class SkillLevel {
        private static final ResourceLocation CATEGORY = ResourceLocation.fromNamespaceAndPath("wilds", "path");

        static int of(ServerPlayer p) {
            try {
                return net.puffish.skillsmod.api.SkillsAPI.getCategory(CATEGORY)
                        .flatMap(net.puffish.skillsmod.api.Category::getExperience).map(x -> x.getLevel(p)).orElse(0);
            } catch (RuntimeException ex) {
                return 0;
            }
        }
    }
}
