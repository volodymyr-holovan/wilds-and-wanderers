package ua.wilds.core.skills.branch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Майстер. Big abilities:
 * <ul>
 * <li>prospector "Рудознатець": sneak and use a pickaxe on stone - ores around (radius 6, +2 per level) glint for you;
 * once a minute;</li>
 * <li>smelters_touch "Дотик горна": a mined ore's raw drop sometimes comes out already smelted (20% per level);</li>
 * <li>thrifty_crafter "Ощадливий майстер": crafting sometimes gives one ingredient back (8% per level);</li>
 * <li>vein_miner "Жила": breaking an ore while sneaking also breaks the same ore touching it (up to 6, +4 per level).</li>
 * </ul>
 * Perks: extra_ore (an ore drops one more), harvest (a ripe crop drops one more), tool_saver (see the durability mixin).
 */
public final class CraftSkills {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<net.minecraft.world.item.Item> RAW = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials"));
    private static boolean veining;

    private CraftSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CraftSkills::prospect);
        NeoForge.EVENT_BUS.addListener(CraftSkills::drops);
        NeoForge.EVENT_BUS.addListener(CraftSkills::crafted);
        NeoForge.EVENT_BUS.addListener(CraftSkills::vein);
    }

    private static void prospect(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !SkillState.has(p, "prospector")) return;
        if (!e.getItemStack().is(ItemTags.PICKAXES)) return;
        BlockState clicked = p.level().getBlockState(e.getPos());
        if (!clicked.is(BlockTags.BASE_STONE_OVERWORLD) && !clicked.is(BlockTags.BASE_STONE_NETHER) && !clicked.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) return;
        e.setCanceled(true);
        if (!SkillState.use(p, "prospector", 1200)) {
            p.displayClientMessage(Component.translatable("wilds_core.skill.cooldown", SkillState.remaining(p, "prospector") / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int r = 6 + 2 * SkillState.level(p, "prospector");
        ServerLevel lvl = p.serverLevel();
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(e.getPos().offset(-r, -r, -r), e.getPos().offset(r, r, r))) {
            if (!lvl.getBlockState(pos).is(ORES)) continue;
            lvl.sendParticles(p, ParticleTypes.ELECTRIC_SPARK, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            if (++found >= 64) break;
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1f);
        p.displayClientMessage(Component.translatable("wilds_core.skill.prospector.found", found).withStyle(ChatFormatting.AQUA), true);
    }

    private static void drops(BlockDropsEvent e) {
        if (!(e.getBreaker() instanceof ServerPlayer p) || e.getDrops().isEmpty()) return;
        BlockState state = e.getState();
        if (state.is(ORES)) {
            double extra = SkillState.perk(p, Perks.EXTRA_ORE);
            if (extra > 0 && p.getRandom().nextDouble() < extra) {
                ItemEntity first = e.getDrops().get(0);
                ItemStack s = first.getItem().copy();
                s.setCount(1);
                e.getDrops().add(new ItemEntity(e.getLevel(), first.getX(), first.getY(), first.getZ(), s));
            }
            int touch = SkillState.level(p, "smelters_touch");
            if (touch > 0) {
                for (ItemEntity ie : e.getDrops()) {
                    ItemStack s = ie.getItem();
                    if (!s.is(RAW) || p.getRandom().nextFloat() >= 0.2f * touch) continue;
                    smelted(e.getLevel(), s).ifPresent(r -> ie.setItem(r.copyWithCount(s.getCount())));
                }
            }
        } else if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            double h = SkillState.perk(p, Perks.HARVEST);
            if (h > 0 && p.getRandom().nextDouble() < h) {
                ItemEntity first = e.getDrops().get(0);
                e.getDrops().add(new ItemEntity(e.getLevel(), first.getX(), first.getY(), first.getZ(), first.getItem().copyWithCount(1)));
            }
        }
    }

    private static Optional<ItemStack> smelted(ServerLevel lvl, ItemStack raw) {
        return lvl.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(raw), lvl)
                .map(h -> h.value().getResultItem(lvl.registryAccess())).filter(s -> !s.isEmpty());
    }

    private static void crafted(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        int lv = SkillState.level(p, "thrifty_crafter");
        if (lv <= 0 || p.getRandom().nextFloat() >= 0.08f * lv) return;
        Container grid = e.getInventory();
        List<ItemStack> candidates = new ArrayList<>();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            // nothing that stays in the grid anyway (buckets, tools) and nothing unique
            if (!s.isEmpty() && s.getMaxStackSize() > 1 && !s.hasCraftingRemainingItem()) candidates.add(s);
        }
        if (candidates.isEmpty()) return;
        ItemStack back = candidates.get(p.getRandom().nextInt(candidates.size())).copyWithCount(1);
        if (!p.getInventory().add(back)) p.drop(back, false);
        p.displayClientMessage(Component.translatable("wilds_core.skill.thrifty_crafter.saved", back.getHoverName()).withStyle(ChatFormatting.GREEN), true);
    }

    private static void vein(BlockEvent.BreakEvent e) {
        if (veining || !(e.getPlayer() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !e.getState().is(ORES)) return;
        int lv = SkillState.level(p, "vein_miner");
        if (lv <= 0) return;
        int limit = 6 + 4 * (lv - 1);
        Block ore = e.getState().getBlock();
        ServerLevel lvl = p.serverLevel();
        List<BlockPos> vein = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(e.getPos());
        seen.add(e.getPos());
        while (!queue.isEmpty() && vein.size() < limit) {
            BlockPos at = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (seen.contains(n)) continue;
                BlockPos im = n.immutable();
                seen.add(im);
                if (lvl.getBlockState(im).is(ore)) {
                    vein.add(im);
                    queue.add(im);
                    if (vein.size() >= limit) break;
                }
            }
        }
        veining = true;
        try {
            for (BlockPos pos : vein) {
                if (p.getMainHandItem().isEmpty()) break;
                p.gameMode.destroyBlock(pos);
            }
        } finally {
            veining = false;
        }
    }
}
