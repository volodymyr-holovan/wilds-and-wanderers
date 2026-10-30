package ua.wilds.core.skills;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import ua.wilds.core.WildsCore;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reward types of the skill tree (registered with Pufferfish's Skills):
 * <ul>
 * <li><b>wilds_core:stat</b> {attribute, value, operation, fallback?: {attribute, value, operation}} - an attribute bonus;
 * when the attribute's mod is missing the fallback is used, so the node still gives something;</li>
 * <li><b>wilds_core:perk</b> {perk, value} - a number the pack's own mechanics read (fire damage taken, extra ore...);</li>
 * <li><b>wilds_core:ability</b> {ability, fallback?} - a big ability; with its mod missing, the fallback ability.</li>
 * </ul>
 * Values add up per unlocked node (Puffish passes the count).
 */
final class SkillRewards {
    private static final AtomicInteger IDS = new AtomicInteger();

    private SkillRewards() {}

    static void register() {
        SkillsAPI.registerReward(id("stat"), SkillRewards::stat);
        SkillsAPI.registerReward(id("perk"), SkillRewards::perk);
        SkillsAPI.registerReward(id("ability"), SkillRewards::ability);
        // Respawning (after death, and on the way back from the End) makes a NEW player object: transient attribute
        // modifiers stay on the old one, and Puffish re-applies rewards only on login, so every stat node of the tree
        // was gone until the player rejoined. Re-apply the pack's rewards to the new player; a dimension change keeps the
        // player object, it is refreshed too in case another mod rebuilds the attributes there.
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            refresh(p);
            // a death respawn starts at 20 health: with the tree's extra hearts it should start full
            if (!e.isEndConquered()) p.setHealth(p.getMaxHealth());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) refresh(p);
        });
    }

    private static void refresh(ServerPlayer p) {
        for (String type : new String[]{"stat", "perk", "ability"}) SkillsAPI.updateRewards(p, id(type));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WildsCore.ID, path);
    }

    private static Result<JsonObject, Problem> data(RewardConfigContext ctx) {
        return ctx.getData().andThen(JsonElement::getAsObject);
    }

    // ---------------------------------------------------------------- stat

    private record StatSpec(Holder<Attribute> attribute, double value, AttributeModifier.Operation op) {}

    private static Optional<StatSpec> parseStat(JsonObject o) {
        String name = o.getString("attribute").getSuccess().orElse(null);
        if (name == null) return Optional.empty();
        ResourceLocation rl = ResourceLocation.tryParse(name.contains(":") ? name : "minecraft:" + name);
        if (rl == null) return Optional.empty();
        Optional<Holder.Reference<Attribute>> h = BuiltInRegistries.ATTRIBUTE.getHolder(rl);
        if (h.isEmpty()) return Optional.empty();
        double v = o.getDouble("value").getSuccess().orElse(0.0);
        String op = o.getString("operation").getSuccess().orElse("add_value");
        AttributeModifier.Operation operation = switch (op) {
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
        return Optional.of(new StatSpec(h.get(), v, operation));
    }

    private static Result<? extends Reward, Problem> stat(RewardConfigContext ctx) {
        return data(ctx).andThen(o -> {
            Optional<StatSpec> spec = parseStat(o);
            if (spec.isEmpty()) {
                spec = o.getObject("fallback").getSuccess().flatMap(SkillRewards::parseStat);
                if (spec.isEmpty()) {
                    ctx.emitWarning("wilds_core:stat: neither the attribute nor its fallback exist here, the node gives nothing");
                    return Result.success(new Reward() {
                        public void update(RewardUpdateContext c) {}

                        public void dispose(RewardDisposeContext c) {}
                    });
                }
            }
            return Result.success(new StatReward(spec.get(), id("skill_" + IDS.incrementAndGet())));
        });
    }

    private static final class StatReward implements Reward {
        private final StatSpec spec;
        private final ResourceLocation modifier;
        private final Set<UUID> applied = new HashSet<>();

        StatReward(StatSpec spec, ResourceLocation modifier) {
            this.spec = spec;
            this.modifier = modifier;
        }

        public void update(RewardUpdateContext c) {
            ServerPlayer p = c.getPlayer();
            AttributeInstance inst = p.getAttribute(spec.attribute);
            if (inst == null) return;
            inst.removeModifier(modifier);
            if (c.getCount() > 0) {
                inst.addTransientModifier(new AttributeModifier(modifier, spec.value * c.getCount(), spec.op));
                applied.add(p.getUUID());
            } else {
                applied.remove(p.getUUID());
            }
        }

        public void dispose(RewardDisposeContext c) {
            for (UUID u : applied) {
                ServerPlayer p = c.getServer().getPlayerList().getPlayer(u);
                AttributeInstance inst = p == null ? null : p.getAttribute(spec.attribute);
                if (inst != null) inst.removeModifier(modifier);
            }
            applied.clear();
        }
    }

    // ---------------------------------------------------------------- perk

    private static Result<? extends Reward, Problem> perk(RewardConfigContext ctx) {
        return data(ctx).andThen(o -> {
            String perk = o.getString("perk").getSuccess().orElse(null);
            if (perk == null || !Perks.known(perk)) return Result.failure(Problem.message("wilds_core:perk: unknown perk " + perk));
            double value = o.getDouble("value").getSuccess().orElse(0.0);
            return Result.success(new PerkReward(perk, value));
        });
    }

    private static final class PerkReward implements Reward {
        private final String perk;
        private final double value;
        private final Set<UUID> applied = new HashSet<>();

        PerkReward(String perk, double value) {
            this.perk = perk;
            this.value = value;
        }

        public void update(RewardUpdateContext c) {
            SkillState.of(c.getPlayer()).setPerkPart(this, perk, value * c.getCount());
            if (c.getCount() > 0) applied.add(c.getPlayer().getUUID());
            else applied.remove(c.getPlayer().getUUID());
            Skills.changed(c.getPlayer());
        }

        public void dispose(RewardDisposeContext c) {
            for (UUID u : applied) {
                ServerPlayer p = c.getServer().getPlayerList().getPlayer(u);
                if (p != null) SkillState.of(p).setPerkPart(this, perk, 0);
            }
            applied.clear();
        }
    }

    // ---------------------------------------------------------------- ability

    private static Result<? extends Reward, Problem> ability(RewardConfigContext ctx) {
        return data(ctx).andThen(o -> {
            String want = o.getString("ability").getSuccess().orElse(null);
            if (want == null || !Ability.known(want)) return Result.failure(Problem.message("wilds_core:ability: unknown ability " + want));
            String use = want;
            if (!Ability.available(want)) {
                String fb = o.getString("fallback").getSuccess().orElse(null);
                if (fb != null && Ability.available(fb)) {
                    ctx.emitWarning("wilds_core:ability: " + want + " needs a mod that is missing, the node gives " + fb);
                    use = fb;
                } else {
                    ctx.emitWarning("wilds_core:ability: " + want + " needs a mod that is missing and has no fallback");
                }
            }
            return Result.success(new AbilityReward(use));
        });
    }

    private static final class AbilityReward implements Reward {
        private final String ability;
        private final Set<UUID> applied = new HashSet<>();

        AbilityReward(String ability) {
            this.ability = ability;
        }

        public void update(RewardUpdateContext c) {
            SkillState s = SkillState.of(c.getPlayer());
            int before = s.abilities.getOrDefault(ability, 0);
            s.abilityParts.put(this, c.getCount());
            int sum = 0;
            for (var e : s.abilityParts.entrySet()) if (e.getKey() instanceof AbilityReward r && r.ability.equals(ability)) sum += e.getValue();
            if (sum > 0) s.abilities.put(ability, sum);
            else s.abilities.remove(ability);
            if (c.getCount() > 0) applied.add(c.getPlayer().getUUID());
            else applied.remove(c.getPlayer().getUUID());
            if (before != sum) Skills.changed(c.getPlayer());
        }

        public void dispose(RewardDisposeContext c) {
            for (UUID u : applied) {
                ServerPlayer p = c.getServer().getPlayerList().getPlayer(u);
                if (p == null) continue;
                SkillState s = SkillState.of(p);
                s.abilityParts.remove(this);
                s.abilities.remove(ability);
                Skills.changed(p);
            }
            applied.clear();
        }
    }
}
