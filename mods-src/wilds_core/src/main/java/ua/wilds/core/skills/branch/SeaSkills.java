package ua.wilds.core.skills.branch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ua.wilds.core.WildsCore;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

/**
 * Мореплавець. Big abilities:
 * <ul>
 * <li>sea_gifts "Дари моря": the first opening of a chest that was generated in a structure under or by the water
 * (shipwrecks, ocean ruins, sunken cities, pirate ships...) often adds a nautilus shell, emeralds, prismarine
 * or, rarely, a heart of the sea right into the inventory;</li>
 * <li>deep_shadow "Тінь глибин": water creatures see the player in water worse (25% per level) and lose sight of them sooner;</li>
 * <li>battle_breath "Подих бою": every hit under water gives back three bubbles of air (per level);</li>
 * <li>deep_sight "Очі глибин": clear sight under water.</li>
 * </ul>
 * Perk aqua_guard: less damage from water creatures.
 */
public final class SeaSkills {
    /** structures of the sea (data/wilds_core/tags/worldgen/structure/sea_treasure.json, mods add their own) */
    private static final TagKey<Structure> SEA = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "sea_treasure"));
    private static final String[] SEA_WORDS = {"shipwreck", "ocean", "underwater", "sunken", "buried_treasure", "pirate", "sea", "ship", "boat",
            "reef", "coral", "lighthouse", "atlantis", "galleon", "frigate", "corsair", "whale", "trench", "lily_pad", "abyss"};

    private SeaSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SeaSkills::openChest);
        NeoForge.EVENT_BUS.addListener(SeaSkills::visibility);
        NeoForge.EVENT_BUS.addListener(SeaSkills::onIncoming);
        NeoForge.EVENT_BUS.addListener(SeaSkills::tick);
    }

    static boolean aquatic(Entity e) {
        return e instanceof WaterAnimal || e instanceof Drowned || e instanceof Guardian || e.getType().is(EntityTypeTags.AQUATIC)
                || (e instanceof LivingEntity le && le.isInWater() && e.getType().getDescriptionId().matches(".*(shark|serpent|siren|leviathan|deepling|thrasher|piranha|squid|crab|eel|jelly|angler).*"));
    }

    private static void openChest(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !SkillState.has(p, "sea_gifts")) return;
        BlockEntity be = p.level().getBlockEntity(e.getPos());
        if (!(be instanceof RandomizableContainerBlockEntity chest)) return;
        ResourceKey<LootTable> table = chest.getLootTable();
        if (table == null) return; // already opened (or placed by a player)
        String path = table.location().getPath();
        boolean sea = p.serverLevel().structureManager().getStructureWithPieceAt(e.getPos(), SEA).isValid();
        if (!sea) for (String w : SEA_WORDS) if (path.contains(w)) sea = true;
        if (!sea) return;
        int lv = SkillState.level(p, "sea_gifts");
        if (p.getRandom().nextFloat() >= 0.35f + 0.15f * lv) return;
        float r = p.getRandom().nextFloat();
        ItemStack gift = r < 0.05f * lv ? new ItemStack(Items.HEART_OF_THE_SEA)
                : r < 0.40f ? new ItemStack(Items.NAUTILUS_SHELL)
                : r < 0.75f ? new ItemStack(Items.EMERALD, 1 + p.getRandom().nextInt(3))
                : new ItemStack(Items.PRISMARINE_CRYSTALS, 2 + p.getRandom().nextInt(3));
        p.displayClientMessage(Component.translatable("wilds_core.skill.sea_gifts.found", gift.getHoverName()).withStyle(ChatFormatting.AQUA), true);
        if (!p.getInventory().add(gift)) p.drop(gift, false);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    private static void visibility(LivingEvent.LivingVisibilityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isInWater() || e.getLookingEntity() == null || !aquatic(e.getLookingEntity())) return;
        int lv = SkillState.level(p, "deep_shadow");
        if (lv > 0) e.modifyVisibility(Math.max(0.25, 1 - 0.25 * lv));
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        Entity src = e.getSource().getEntity();
        if (e.getEntity() instanceof ServerPlayer p && src != null && aquatic(src)) {
            double g = Math.min(0.7, SkillState.perk(p, Perks.AQUA_GUARD));
            if (g > 0) e.setAmount((float) (e.getAmount() * (1 - g)));
        }
        if (src instanceof ServerPlayer p && p.isUnderWater()) {
            int lv = SkillState.level(p, "battle_breath");
            if (lv > 0) p.setAirSupply(Math.min(p.getMaxAirSupply(), p.getAirSupply() + 90 * lv));
        }
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 11) return;
        if (SkillState.has(p, "deep_sight")) {
            if (p.isEyeInFluid(FluidTags.WATER)) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            } else {
                MobEffectInstance nv = p.getEffect(MobEffects.NIGHT_VISION);
                if (nv != null && nv.isAmbient() && !nv.isVisible()) p.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
        int shadow = SkillState.level(p, "deep_shadow");
        if (shadow > 0 && p.isInWater()) {
            double keep = 24 - 6 * shadow;
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(40), m -> m.getTarget() == p && aquatic(m))) {
                if (m.distanceTo(p) > keep) m.setTarget(null);
            }
        }
    }
}
