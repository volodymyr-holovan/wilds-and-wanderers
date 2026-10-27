package ua.wilds.core.skills.branch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.wilds.core.mixin.MobAccessor;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Торговець. Big abilities:
 * <ul>
 * <li>generous_deal "Щедра угода": once a day, a villager trade costs nothing (what was paid comes back);</li>
 * <li>village_gifts "Улюбленець села": villagers near the player now and then toss a gift of their trade (the same
 * gifts as for the Hero of the Village);</li>
 * <li>mercenaries "Найманці": sneak and use 8 emeralds on an iron golem or a village guard - it follows the player and
 * fights for them for a day (two at level 2);</li>
 * <li>haggler "Торгівля з розумом": every trade one emerald cheaper per level (+ the discount perk), see VillagerPricesMixin.</li>
 * </ul>
 */
public final class TraderSkills {
    static final String HIRED_BY = "wilds_core:hired_by", HIRED_UNTIL = "wilds_core:hired_until";
    private static final int HIRE_PRICE = 8;

    private TraderSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TraderSkills::trade);
        NeoForge.EVENT_BUS.addListener(TraderSkills::gifts);
        NeoForge.EVENT_BUS.addListener(TraderSkills::hire);
        NeoForge.EVENT_BUS.addListener(TraderSkills::rejoin);
    }

    /** emeralds off every trade, used by VillagerPricesMixin */
    public static int discount(Player p) {
        return (int) Math.round(SkillState.perk(p, Perks.DISCOUNT)) + SkillState.level(p, "haggler");
    }

    private static void trade(TradeWithVillagerEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !SkillState.has(p, "generous_deal")) return;
        if (!SkillState.useDaily(p, "generous_deal")) return;
        MerchantOffer o = e.getMerchantOffer();
        for (ItemStack paid : new ItemStack[]{o.getCostA(), o.getCostB()}) {
            if (paid.isEmpty()) continue;
            ItemStack back = paid.copy();
            if (!p.getInventory().add(back)) p.drop(back, false);
        }
        p.displayClientMessage(Component.translatable("wilds_core.skill.generous_deal.used").withStyle(ChatFormatting.GOLD), true);
    }

    private static void gifts(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 600 != 301) return;
        int lv = SkillState.level(p, "village_gifts");
        if (lv <= 0 || p.getRandom().nextFloat() >= 0.25f * lv) return;
        List<Villager> near = p.level().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(6), v -> !v.isBaby() && !v.isSleeping());
        if (near.isEmpty()) return;
        Villager v = near.get(p.getRandom().nextInt(near.size()));
        VillagerProfession prof = v.getVillagerData().getProfession();
        if (prof == VillagerProfession.NONE || prof == VillagerProfession.NITWIT) return;
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("gameplay/hero_of_the_village/" + id.getPath() + "_gift"));
        ServerLevel lvl = p.serverLevel();
        LootTable table = lvl.getServer().reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) return;
        LootParams params = new LootParams.Builder(lvl).withParameter(LootContextParams.ORIGIN, v.position())
                .withParameter(LootContextParams.THIS_ENTITY, v).create(LootContextParamSets.GIFT);
        for (ItemStack s : table.getRandomItems(params)) {
            ItemEntity ie = new ItemEntity(lvl, v.getX(), v.getEyeY() - 0.3, v.getZ(), s);
            ie.setDeltaMovement(p.position().subtract(v.position()).normalize().scale(0.3).add(0, 0.2, 0));
            lvl.addFreshEntity(ie);
        }
        lvl.playSound(null, v.getX(), v.getY(), v.getZ(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
    }

    private static boolean hireable(Entity e) {
        return e instanceof IronGolem || BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("guardvillagers:guard");
    }

    private static void hire(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !hireable(e.getTarget())) return;
        int lv = SkillState.level(p, "mercenaries");
        ItemStack hand = e.getItemStack();
        if (lv <= 0 || !hand.is(Items.EMERALD)) return;
        e.setCanceled(true);
        PathfinderMob mob = (PathfinderMob) e.getTarget();
        long now = p.serverLevel().getGameTime();
        long hired = p.level().getEntitiesOfClass(PathfinderMob.class, p.getBoundingBox().inflate(64),
                m -> p.getUUID().equals(owner(m)) && m.getPersistentData().getLong(HIRED_UNTIL) > now).size();
        if (hired >= lv && !p.getUUID().equals(owner(mob))) {
            p.displayClientMessage(Component.translatable("wilds_core.skill.mercenaries.limit", lv).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (hand.getCount() < HIRE_PRICE) {
            p.displayClientMessage(Component.translatable("wilds_core.skill.mercenaries.price", HIRE_PRICE).withStyle(ChatFormatting.RED), true);
            return;
        }
        hand.shrink(HIRE_PRICE);
        CompoundTag d = mob.getPersistentData();
        d.putUUID(HIRED_BY, p.getUUID());
        d.putLong(HIRED_UNTIL, now + 24000);
        addGoals(mob);
        p.displayClientMessage(Component.translatable("wilds_core.skill.mercenaries.hired").withStyle(ChatFormatting.GREEN), true);
        p.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1f, 0.8f);
    }

    static UUID owner(Entity e) {
        CompoundTag d = e.getPersistentData();
        return d.hasUUID(HIRED_BY) ? d.getUUID(HIRED_BY) : null;
    }

    /** goals are not saved with the entity: give them back when a hired mob is loaded again */
    private static void rejoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof PathfinderMob m && owner(m) != null) addGoals(m);
    }

    private static void addGoals(PathfinderMob m) {
        MobAccessor acc = (MobAccessor) m;
        GoalSelector goals = acc.wilds$goals(), targets = acc.wilds$targets();
        if (goals.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof FollowHirer)) return;
        goals.addGoal(2, new FollowHirer(m));
        targets.addGoal(0, new DefendHirer(m));
    }

    private static ServerPlayer hirer(Mob m) {
        UUID u = owner(m);
        if (u == null || m.getPersistentData().getLong(HIRED_UNTIL) < m.level().getGameTime()) return null;
        return m.level().getServer() == null ? null : m.level().getServer().getPlayerList().getPlayer(u);
    }

    /** walks after the player who paid, until the contract runs out */
    static final class FollowHirer extends Goal {
        private final PathfinderMob mob;
        private ServerPlayer boss;

        FollowHirer(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {
            boss = hirer(mob);
            return boss != null && boss.level() == mob.level() && mob.distanceToSqr(boss) > 36 && mob.getTarget() == null;
        }

        public boolean canContinueToUse() {
            return boss != null && boss.isAlive() && mob.distanceToSqr(boss) > 9 && mob.getTarget() == null;
        }

        public void tick() {
            mob.getLookControl().setLookAt(boss, 10, mob.getMaxHeadXRot());
            if (mob.distanceToSqr(boss) > 1024) mob.teleportTo(boss.getX(), boss.getY(), boss.getZ());
            else mob.getNavigation().moveTo(boss, 1.1);
        }
    }

    /** attacks whoever attacks the player, and whatever the player attacks */
    static final class DefendHirer extends Goal {
        private final PathfinderMob mob;

        DefendHirer(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        public boolean canUse() {
            ServerPlayer boss = hirer(mob);
            if (boss == null) return false;
            LivingEntity foe = boss.getLastHurtByMob() != null && boss.tickCount - boss.getLastHurtByMobTimestamp() < 200 ? boss.getLastHurtByMob() : null;
            if (foe == null && boss.getLastHurtMob() != null && boss.tickCount - boss.getLastHurtMobTimestamp() < 200) foe = boss.getLastHurtMob();
            if (foe == null || foe == mob || !foe.isAlive() || foe instanceof Player) return false;
            if (!TargetingConditions.forCombat().test(mob, foe)) return false;
            mob.setTarget(foe);
            return true;
        }

        public boolean canContinueToUse() {
            return false;
        }
    }
}
