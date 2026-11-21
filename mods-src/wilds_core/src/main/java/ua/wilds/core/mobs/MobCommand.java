package ua.wilds.core.mobs;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ua.wilds.core.WildsCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * /wildspower (everyone): your power level, which the mobs near you follow.
 * /wildsmobs (operators): look at and measure the balance with the real game.
 *   level | info [r] | stats | forcelevel <n>|off
 *   spawn <kind> <level>        a new creature at that level in front of you
 *   test <level>                every new creature at that level around you; after 15 s: alive, attacking you
 *   hits <kind|entity> <level>  how many hits of YOUR weapon (with its enchantments) kill it, measured on the mob
 *   hit <kind|entity> <level>   it hits YOU once through the real damage path; reports the damage and your health share
 *   loot <n> <level>            n new creatures of that level killed by you: the extra loot they dropped
 *   boss <entity> <level>       a boss at that level, with its numbers
 */
final class MobCommand {
    private record Pending(ServerLevel level, ServerPlayer player, List<Mob> mobs, double lvl, long at) {}
    private static final List<Pending> PENDING = new ArrayList<>();
    /** loot test: drops of the killed creatures are counted here instead of falling */
    static Map<String, Integer> lootCapture;

    private MobCommand() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> {
            e.getDispatcher().register(Commands.literal("wildspower").executes(MobCommand::power));
            e.getDispatcher().register(Commands.literal("wildsmobs").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("level").executes(MobCommand::power))
                    .then(Commands.literal("stats").executes(c -> ok(c, "natural spawns: " + new TreeMap<>(MobLevels.STATS))))
                    .then(Commands.literal("info").executes(c -> info(c, 16))
                            .then(Commands.argument("r", IntegerArgumentType.integer(1, 256)).executes(c -> info(c, IntegerArgumentType.getInteger(c, "r")))))
                    .then(Commands.literal("forcelevel").then(Commands.literal("off").executes(c -> { MobLevels.FORCED.remove(c.getSource().getPlayerOrException().getUUID()); return ok(c, "forced level off"); }))
                            .then(Commands.argument("n", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                                MobLevels.FORCED.put(c.getSource().getPlayerOrException().getUUID(), IntegerArgumentType.getInteger(c, "n"));
                                return ok(c, "forced level " + IntegerArgumentType.getInteger(c, "n"));
                            })))
                    .then(Commands.literal("spawn").then(kindLevel((c, k, l) -> spawn(c, k, l))))
                    .then(Commands.literal("hits").then(kindLevel(MobCommand::hits)))
                    .then(Commands.literal("hit").then(kindLevel(MobCommand::hit)))
                    .then(Commands.literal("boss").then(kindLevel((c, k, l) -> spawn(c, k, l))))
                    .then(Commands.literal("test").then(Commands.argument("lvl", IntegerArgumentType.integer(0, 1000)).executes(c -> test(c, IntegerArgumentType.getInteger(c, "lvl")))))
                    .then(Commands.literal("loot").then(Commands.argument("n", IntegerArgumentType.integer(1, 2000)).then(Commands.argument("lvl", IntegerArgumentType.integer(0, 1000))
                            .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "n"), IntegerArgumentType.getInteger(c, "lvl")))))));
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> report());
    }

    private interface KL { int run(CommandContext<CommandSourceStack> c, String kind, int lvl) throws CommandSyntaxException; }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> kindLevel(KL k) {
        // "kind level" in one greedy string: entity ids with ':' need no quotes
        return Commands.argument("kind_level", StringArgumentType.greedyString()).executes(c -> {
            String[] a = StringArgumentType.getString(c, "kind_level").trim().split("\s+");
            return k.run(c, a[0], a.length > 1 ? Integer.parseInt(a[1]) : 0);
        });
    }

    private static int ok(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int power(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        double[] pw = MobLevels.power(p);
        double[] l = MobLevels.playerLevel(p);
        c.getSource().sendSuccess(() -> Component.translatable("wilds_core.power", (int) Math.round(l[0] * l[1]), (int) Math.round(pw[1]), (int) Math.round(pw[2])), false);
        if (c.getSource().hasPermission(2))
            ok(c, String.format("hit %.1f armor %.1f tough %.1f epf %.1f hp %.0f; world tier x%.2f", pw[3], pw[4], pw[5], pw[6], p.getMaxHealth(), l[1]));
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c, int r) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int all = 0, kinds = 0;
        Map<String, Integer> seen = new TreeMap<>();
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r))) {
            MobLevels.Info in = MobLevels.info(m);
            if (in == null) continue;
            all++;
            if (in.variant != null) { kinds++; seen.merge(in.variant, 1, Integer::sum); }
            if (r <= 16) ok(c, describe(m, in));
        }
        return ok(c, "leveled mobs within " + r + ": " + all + ", new creatures: " + kinds + " " + seen);
    }

    static String describe(Mob m, MobLevels.Info in) {
        AttributeInstance ar = m.getAttribute(Attributes.ARMOR), to = m.getAttribute(Attributes.ARMOR_TOUGHNESS);
        double armor = ar == null ? 0 : ar.getValue(), tough = to == null ? 0 : to.getValue();
        double hits = m.getMaxHealth() * in.extra / MobMath.afterArmor(MobMath.gearDamage(in.level), armor, tough);
        return String.format("%s %s lvl %.1f%s%s hp %.0f armor %.1f/%.1f ref-hits %.2f dmg x%.2f ab %s",
                BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()), in.variant == null ? "" : "[" + in.variant + "]", in.level,
                in.boss ? " BOSS" : "", in.minion ? " minion" : "", m.getMaxHealth() * in.extra, armor, tough, hits, in.dmgMult, in.abilities);
    }

    /** a kind id ("draugr") or an entity id ("minecraft:wither") */
    private static Mob make(ServerLevel level, ServerPlayer p, String kindOrId, double lvl, double dx, double dz) {
        MobData.Variant v = MobData.variants.get(kindOrId);
        ResourceLocation type = v != null ? v.entity() : ResourceLocation.tryParse(kindOrId);
        EntityType<?> et = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
        if (et == null) return null;
        Entity e = et.create(level);
        if (!(e instanceof Mob m)) { if (e != null) e.discard(); return null; }
        m.moveTo(p.getX() + dx, p.getY(), p.getZ() + dz, 0, 0);
        if (v != null) m.getPersistentData().putString(MobLevels.TAG + "_pending", v.id());
        EventHooks.finalizeMobSpawn(m, level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        Integer prev = MobLevels.FORCED.put(p.getUUID(), (int) Math.round(lvl));
        try {
            level.addFreshEntity(m);
        } finally {
            if (prev == null) MobLevels.FORCED.remove(p.getUUID()); else MobLevels.FORCED.put(p.getUUID(), prev);
        }
        MobLevels.Info in = MobLevels.info(m);
        if (in != null && Math.abs(in.level - lvl) > 0.01) {
            in.level = (float) lvl;
            MobLevels.apply(m, in, true);
        }
        return m;
    }

    private static int spawn(CommandContext<CommandSourceStack> c, String kind, int lvl) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Mob m = make(p.serverLevel(), p, kind, lvl, p.getLookAngle().x * 5, p.getLookAngle().z * 5);
        if (m == null) return ok(c, "could not spawn " + kind);
        MobLevels.Info in = MobLevels.info(m);
        return ok(c, in == null ? "spawned without level: " + kind : describe(m, in));
    }

    /** the player's real hit (weapon attribute + enchantments) applied to the mob until it dies */
    private static int hits(CommandContext<CommandSourceStack> c, String kind, int lvl) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Mob m = make(p.serverLevel(), p, kind, lvl, 3, 0);
        if (m == null) return ok(c, "could not spawn " + kind);
        MobLevels.Info in = MobLevels.info(m);
        m.setNoAi(true);
        double[] pw = MobLevels.power(p);
        int n = 0;
        float before = m.getHealth() * (float) (in == null ? 1 : in.extra);
        while (m.isAlive() && n < 500) {
            m.invulnerableTime = 0;
            m.hurt(p.damageSources().playerAttack(p), (float) pw[3]);
            n++;
        }
        boolean dead = !m.isAlive();
        m.discard();
        String line = String.format("HITS %s lvl %d: %d hits of %.1f (health %.0f)%s", kind, lvl, n, pw[3], before, dead ? "" : " NOT DEAD");
        WildsCore.LOG.info("[wildsmobs] {}", line);
        return ok(c, line);
    }

    /** the mob hits the player once through the real path (its weapon, the player's armor and enchantments, the cap) */
    private static int hit(CommandContext<CommandSourceStack> c, String kind, int lvl) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Mob m = make(p.serverLevel(), p, kind, lvl, 1.5, 0);
        if (m == null) return ok(c, "could not spawn " + kind);
        m.setNoAi(true);
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        float before = p.getHealth();
        AttributeInstance atk = m.getAttribute(Attributes.ATTACK_DAMAGE);
        boolean landed;
        if (atk != null && atk.getValue() > 0) landed = m.doHurtTarget(p);
        else landed = p.hurt(m.damageSources().mobAttack(m), 4f);
        float taken = before - p.getHealth();
        MobLevels.Info in = MobLevels.info(m);
        m.discard();
        p.setHealth(p.getMaxHealth());
        String line = String.format("HIT %s lvl %d: %s %.2f of %.0f health (%.0f%%) -> %d hits to die; dmg x%.2f", kind, lvl, landed ? "took" : "missed",
                taken, p.getMaxHealth(), 100 * taken / p.getMaxHealth(), taken > 0 ? (int) Math.ceil(p.getMaxHealth() / taken) : 0, in == null ? 1 : in.dmgMult);
        WildsCore.LOG.info("[wildsmobs] {}", line);
        return ok(c, line);
    }

    private static int loot(CommandContext<CommandSourceStack> c, int n, int lvl) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<MobData.Variant> open = new ArrayList<>();
        for (MobData.Variant v : MobData.variants.values()) if (MobData.unlock(v.tier()) <= Math.max(lvl, 8)) open.add(v);
        lootCapture = new TreeMap<>();
        int killed = 0;
        try {
            for (int i = 0; i < n; i++) {
                MobData.Variant v = open.get(p.getRandom().nextInt(open.size()));
                Mob m = make(p.serverLevel(), p, v.id(), lvl, 2, 2);
                if (m == null) continue;
                m.setNoAi(true);
                int guard = 0;
                while (m.isAlive() && guard++ < 200) { m.invulnerableTime = 0; m.hurt(p.damageSources().playerAttack(p), 1000f); }
                if (!m.isAlive()) killed++;
                m.discard();
            }
        } finally {
            for (ItemEntity ie : p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8))) ie.discard();
        }
        Map<String, Integer> got = lootCapture;
        lootCapture = null;
        String line = "LOOT " + killed + " creatures at level " + lvl + " (" + open.size() + " kinds open): " + got;
        WildsCore.LOG.info("[wildsmobs] {}", line);
        return ok(c, line);
    }

    /** called from MobLevels.onDrops for the extra loot while a loot test runs */
    static void capture(LivingDropsEvent e, List<ItemEntity> extra) {
        if (lootCapture == null) return;
        for (ItemEntity ie : extra) lootCapture.merge(BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).toString(), ie.getItem().getCount(), Integer::sum);
    }

    private static int test(CommandContext<CommandSourceStack> c, int lvl) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<Mob> mobs = new ArrayList<>();
        int i = 0, n = MobData.variants.size();
        for (MobData.Variant v : MobData.variants.values()) {
            double a = Math.PI * 2 * i++ / n, r = 6 + (i % 3) * 2;
            Mob m = make(p.serverLevel(), p, v.id(), Math.max(lvl, MobData.unlock(v.tier())), Math.cos(a) * r, Math.sin(a) * r);
            if (m == null) ok(c, "FAIL spawn " + v.id() + " " + v.entity());
            else mobs.add(m);
        }
        synchronized (PENDING) {
            PENDING.add(new Pending(p.serverLevel(), p, mobs, lvl, p.serverLevel().getGameTime() + 300));
        }
        return ok(c, "spawned " + mobs.size() + " creatures, report in 15 s");
    }

    private static void report() {
        synchronized (PENDING) {
            PENDING.removeIf(t -> {
                if (t.level().getGameTime() < t.at()) return false;
                int alive = 0, targeting = 0;
                for (Mob m : t.mobs()) {
                    MobLevels.Info in = MobLevels.info(m);
                    boolean tg = m.getTarget() instanceof Player;
                    if (m.isAlive()) alive++;
                    if (tg) targeting++;
                    WildsCore.LOG.info("[wildsmobs test] {}{}{}", m.isAlive() ? "" : "DEAD ", tg ? "" : "NO-TARGET ",
                            in == null ? BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()) + " no level" : describe(m, in));
                    m.discard();
                }
                String sum = "[wildsmobs test] level " + t.lvl() + ": " + t.mobs().size() + " creatures, alive " + alive + ", targeting the player " + targeting;
                WildsCore.LOG.info(sum);
                t.player().sendSystemMessage(Component.literal(sum));
                return true;
            });
        }
    }
}
