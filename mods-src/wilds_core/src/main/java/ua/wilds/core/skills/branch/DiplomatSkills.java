package ua.wilds.core.skills.branch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import ua.wilds.core.skills.SkillState;

import java.util.Set;

/**
 * Дипломат. Big abilities:
 * <ul>
 * <li>quiet_step "Тиха хода": monsters notice the player 20% later per level (up to 60%);</li>
 * <li>goblin_friend "Друг гоблінів" (Goblin's Tyranny): goblin warriors do not attack first; the goblin bartender,
 * merchant and blacksmith give a pouch of goblin coins once a day. Fallback piglin_friend: piglins stay calm without
 * gold armour;</li>
 * <li>peacemaker "Миротворець": sneak four times quickly - every neutral creature that is angry at the player
 * (wolves, golems, bees, endermen, zombified piglins, llamas...) calms down. Every 10 minutes;</li>
 * <li>silver_tongue "Красномовство": quest givers believe the player more often (see the NPC dialogues).</li>
 * </ul>
 */
public final class DiplomatSkills {
    /** goblins that keep fighting whatever happens: the chieftains */
    private static final Set<String> GOBLIN_BOSSES = Set.of("goblins_tyranny:leader_goblin", "goblins_tyranny:champion_goblin");
    private static final Set<String> GOBLIN_TRADERS = Set.of("goblins_tyranny:bartender_goblin", "goblins_tyranny:merchant", "goblins_tyranny:blacksmith_goblin");

    private DiplomatSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DiplomatSkills::visibility);
        NeoForge.EVENT_BUS.addListener(DiplomatSkills::target);
        NeoForge.EVENT_BUS.addListener(DiplomatSkills::interact);
    }

    private static String id(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
    }

    private static void visibility(LivingEvent.LivingVisibilityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getLookingEntity() instanceof Enemy)) return;
        int lv = SkillState.level(p, "quiet_step");
        if (lv > 0) e.modifyVisibility(Math.max(0.4, 1 - 0.2 * lv));
    }

    private static void target(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p)) return;
        LivingEntity attacker = e.getEntity();
        String id = id(attacker);
        // a creature the player hurt first may fight back
        if (attacker.getLastHurtByMob() == p) return;
        if (id.startsWith("goblins_tyranny:") && !GOBLIN_BOSSES.contains(id) && SkillState.has(p, "goblin_friend")) e.setCanceled(true);
        else if (attacker instanceof Piglin && SkillState.has(p, "piglin_friend")) e.setCanceled(true);
    }

    private static void interact(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !SkillState.has(p, "goblin_friend")) return;
        if (!GOBLIN_TRADERS.contains(id(e.getTarget())) || !SkillState.useDaily(p, "goblin_gift")) return;
        ResourceLocation coin = ResourceLocation.fromNamespaceAndPath("goblins_tyranny", "coin");
        var item = BuiltInRegistries.ITEM.get(coin);
        ItemStack gift = item == Items.AIR ? new ItemStack(Items.GOLD_NUGGET, 6) : new ItemStack(item, 4 + p.getRandom().nextInt(5));
        if (!p.getInventory().add(gift)) p.drop(gift, false);
        p.displayClientMessage(Component.translatable("wilds_core.skill.goblin_friend.gift", gift.getHoverName()).withStyle(ChatFormatting.GOLD), true);
    }

    /** the client saw four quick sneaks */
    public static void pacify(ServerPlayer p) {
        if (!SkillState.has(p, "peacemaker")) return;
        if (!SkillState.use(p, "peacemaker", 12000)) {
            p.displayClientMessage(Component.translatable("wilds_core.skill.cooldown", SkillState.remaining(p, "peacemaker") / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int calmed = 0;
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), m -> m instanceof NeutralMob)) {
            NeutralMob n = (NeutralMob) m;
            if (m.getTarget() == p || p.getUUID().equals(n.getPersistentAngerTarget())) {
                n.stopBeingAngry();
                m.setTarget(null);
                m.setLastHurtByMob(null);
                p.serverLevel().sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 3, 0.3, 0.2, 0.3, 0);
                calmed++;
            }
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("wilds_core.skill.peacemaker.used", calmed).withStyle(ChatFormatting.GREEN), true);
    }
}
