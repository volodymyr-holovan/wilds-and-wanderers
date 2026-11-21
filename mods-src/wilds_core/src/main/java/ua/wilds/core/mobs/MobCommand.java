package ua.wilds.core.mobs;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * /wildsmobs (operators): look at the levels in game and check the balance with the mobs themselves.
 *   level                       your level and world tier factor
 *   info                        the leveled mobs within 16 blocks
 *   forcelevel <n>|off          pretend your level is n (tests only, not saved)
 *   spawn <kind> <level>        a new kind at that level in front of you
 *   test <level>                every new kind at that level around you; after 8 s: alive, target, health, hits, damage
 *   boss <entity> <level>       a boss at that level, with its numbers
 */
final class MobCommand {
    private record Pending(ServerLevel level, ServerPlayer player, List<Mob> mobs, double lvl, long at) {}
    private static final List<Pending> PENDING = new ArrayList<>();

    private MobCommand() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(Commands.literal("wildsmobs").requires(s -> s.hasPermission(2))
                .then(Commands.literal("level").executes(MobCommand::level))
                .then(Commands.literal("stats").executes(c -> ok(c, "natural spawns of the families: " + new java.util.TreeMap<>(MobLevels.STATS))))
                .then(Commands.literal("info").executes(c -> info(c, 16)).then(Commands.argument("r", IntegerArgumentType.integer(1, 256)).executes(c -> info(c, IntegerArgumentType.getInteger(c, "r")))))
                .then(Commands.literal("forcelevel").then(Commands.literal("off").executes(c -> { MobLevels.FORCED.remove(c.getSource().getPlayerOrException().getUUID()); return ok(c, "forced level off"); }))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                            MobLevels.FORCED.put(c.getSource().getPlayerOrException().getUUID(), IntegerArgumentType.getInteger(c, "n"));
                            return ok(c, "forced level " + IntegerArgumentType.getInteger(c, "n"));
                        })))
                .then(Commands.literal("spawn").then(Commands.argument("kind", StringArgumentType.word()).then(Commands.argument("lvl", IntegerArgumentType.integer(0, 1000))
                        .executes(c -> spawn(c, StringArgumentType.getString(c, "kind"), IntegerArgumentType.getInteger(c, "lvl"))))))
                .then(Commands.literal("test").then(Commands.argument("lvl", IntegerArgumentType.integer(0, 1000)).executes(c -> test(c, IntegerArgumentType.getInteger(c, "lvl")))))
                .then(Commands.literal("boss").then(Commands.argument("entity", StringArgumentType.greedyString()).executes(c -> {
                    String[] a = StringArgumentType.getString(c, "entity").split(" ");
                    return boss(c, a[0], a.length > 1 ? Integer.parseInt(a[1]) : 100);
                })))));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> report());
    }

    private static int ok(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int level(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        double[] l = MobLevels.playerLevel(p);
        return ok(c, String.format("skill level %.0f x world tier %.2f = %.1f; reference hit D=%.1f", l[0], l[1], l[0] * l[1], MobMath.gearDamage(l[0] * l[1])));
    }

    private static int info(CommandContext<CommandSourceStack> c, int r) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int all = 0, kinds = 0;
        java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r))) {
            MobLevels.Info in = MobLevels.info(m);
            if (in == null) continue;
            all++;
            if (in.variant != null) { kinds++; seen.merge(in.variant, 1, Integer::sum); }
            if (r <= 16) ok(c, describe(m, in));
        }
        return ok(c, "leveled mobs within " + r + ": " + all + ", new kinds: " + kinds + " " + seen);
    }

    static String describe(Mob m, MobLevels.Info in) {
        AttributeInstance ar = m.getAttribute(Attributes.ARMOR), to = m.getAttribute(Attributes.ARMOR_TOUGHNESS);
        double armor = ar == null ? 0 : ar.getValue(), tough = to == null ? 0 : to.getValue();
        double d = MobMath.gearDamage(in.level);
        double hits = m.getMaxHealth() * in.extra / MobMath.afterArmor(d, armor, tough);
        AttributeInstance atk = m.getAttribute(Attributes.ATTACK_DAMAGE);
        double raw = (atk != null && atk.getBaseValue() > 0 ? atk.getBaseValue() : 4) * in.dmgMult;
        double toP = MobMath.toPlayer(raw, in.level);
        return String.format("%s %s lvl %.1f%s%s hp %.0f armor %.1f/%.1f hits %.2f raw %.1f -> player hits %.1f ab %s",
                BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()), in.variant == null ? "" : "[" + in.variant + "]", in.level,
                in.boss ? " BOSS" : "", in.minion ? " minion" : "", m.getMaxHealth() * in.extra, armor, tough, hits, raw,
                MobMath.defence(in.level)[3] / Math.max(0.01, toP), in.abilities);
    }

    private static Mob make(ServerLevel level, ServerPlayer p, ResourceLocation type, String variant, double lvl, double dx, double dz) {
        EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
        if (et == null) return null;
        Entity e = et.create(level);
        if (!(e instanceof Mob m)) { if (e != null) e.discard(); return null; }
        m.moveTo(p.getX() + dx, p.getY(), p.getZ() + dz, 0, 0);
        if (variant != null) m.getPersistentData().putString(MobLevels.TAG + "_pending", variant);
        EventHooks.finalizeMobSpawn(m, level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        Integer prev = MobLevels.FORCED.put(p.getUUID(), (int) Math.round(lvl / 1.0));
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

    private static int spawn(CommandContext<CommandSourceStack> c, String kind, int lvl) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        MobData.Variant v = MobData.variants.get(kind);
        if (v == null) return ok(c, "no kind " + kind);
        ResourceLocation type = v.entity() != null ? v.entity() : MobData.families.get(v.family()).bases().get(0);
        Mob m = make(p.serverLevel(), p, type, kind, lvl, p.getLookAngle().x * 4, p.getLookAngle().z * 4);
        if (m == null) return ok(c, "could not spawn " + type);
        MobLevels.Info in = MobLevels.info(m);
        return ok(c, in == null ? "spawned without level: " + type : describe(m, in));
    }

    private static int boss(CommandContext<CommandSourceStack> c, String id, int lvl) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Mob m = make(p.serverLevel(), p, ResourceLocation.parse(id), null, lvl, p.getLookAngle().x * 8, p.getLookAngle().z * 8);
        if (m == null) return ok(c, "could not spawn " + id);
        MobLevels.Info in = MobLevels.info(m);
        return ok(c, in == null ? "spawned without level: " + id : describe(m, in));
    }

    private static int test(CommandContext<CommandSourceStack> c, int lvl) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<Mob> mobs = new ArrayList<>();
        int i = 0, n = MobData.variants.size();
        for (MobData.Variant v : MobData.variants.values()) {
            double a = Math.PI * 2 * i++ / n, r = 5 + (i % 3) * 2;
            ResourceLocation type = v.entity() != null ? v.entity() : MobData.families.get(v.family()).bases().get(0);
            Mob m = make(p.serverLevel(), p, type, v.id(), Math.max(lvl, MobData.unlock(v.tier())), Math.cos(a) * r, Math.sin(a) * r);
            if (m == null) ok(c, "FAIL spawn " + v.id() + " " + type);
            else mobs.add(m);
        }
        synchronized (PENDING) {
            PENDING.add(new Pending(p.serverLevel(), p, mobs, lvl, p.serverLevel().getGameTime() + 300));
        }
        return ok(c, "spawned " + mobs.size() + " kinds, report in 8 s");
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
                    String line = (m.isAlive() ? "" : "DEAD ") + (tg ? "" : "NO-TARGET ") + (in == null ? BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()) + " no level" : describe(m, in));
                    ua.wilds.core.WildsCore.LOG.info("[wildsmobs test] {}", line);
                    m.discard();
                }
                String sum = "[wildsmobs test] level " + t.lvl() + ": " + t.mobs().size() + " kinds, alive " + alive + ", targeting the player " + targeting;
                ua.wilds.core.WildsCore.LOG.info(sum);
                t.player().sendSystemMessage(Component.literal(sum));
                return true;
            });
        }
    }
}
