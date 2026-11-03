package ua.wilds.core.finder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import ua.wilds.core.WildsCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Item finder, the server half. The client turns what the player typed (in any language the client knows the item
 * names in, or the item's id) into a set of item ids; the server looks for them around the player, within
 * {@link #RADIUS} blocks, and answers with the places:
 * <ul>
 * <li>every block with an item storage: NeoForge's item handler capability first (so chests, barrels, shulker boxes,
 * Iron/Sophisticated chests, drawers, machines and storages of other mods all count, whatever their size), the vanilla
 * Container otherwise; an item inside a shulker box or a bundle stored there counts too;</li>
 * <li>minecarts and boats with chests, items lying on the ground, items in item frames.</li>
 * </ul>
 * Never blocks themselves (so no ore finding), never storages in a claim of another party (Open Parties and Claims)
 * and never Lootr's containers (their loot is made for each player when opened: asking them would roll it).
 */
public final class Finder {
    public static final int RADIUS = 24;
    private static final int MAX_HITS = 128, MAX_IDS = 512;
    private static final Map<UUID, Long> lastAsk = new HashMap<>();

    private Finder() {}

    public static List<BlockPos> find(ServerPlayer p, List<String> ids) {
        long now = p.serverLevel().getGameTime();
        Long last = lastAsk.get(p.getUUID());
        if (last != null && now - last < 5) return null;   // at most four searches a second
        lastAsk.put(p.getUUID(), now);
        Set<Item> want = new HashSet<>();
        for (int i = 0; i < ids.size() && i < MAX_IDS; i++) {
            ResourceLocation rl = ResourceLocation.tryParse(ids.get(i));
            if (rl != null) BuiltInRegistries.ITEM.getOptional(rl).ifPresent(want::add);
        }
        List<BlockPos> out = new ArrayList<>();
        if (want.isEmpty()) return out;
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition();
        double r2 = (double) RADIUS * RADIUS;
        ChunkPos c0 = new ChunkPos(at);
        int cr = RADIUS / 16 + 1;
        for (int cx = c0.x - cr; cx <= c0.x + cr && out.size() < MAX_HITS; cx++) {
            for (int cz = c0.z - cr; cz <= c0.z + cr && out.size() < MAX_HITS; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null || !Claims.mayLook(p, level, cx, cz)) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(at) > r2 || lootr(be)) continue;
                    if (holds(level, pos, be, want)) {
                        out.add(pos.immutable());
                        if (out.size() >= MAX_HITS) break;
                    }
                }
            }
        }
        AABB box = new AABB(at).inflate(RADIUS);
        for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof ItemEntity || e instanceof ContainerEntity || e instanceof ItemFrame)) {
            if (out.size() >= MAX_HITS) break;
            if (e.distanceToSqr(p) > r2 || !Claims.mayLook(p, level, e.chunkPosition().x, e.chunkPosition().z)) continue;
            boolean hit = switch (e) {
                case ItemEntity ie -> matches(ie.getItem(), want);
                case ItemFrame f -> matches(f.getItem(), want);
                case ContainerEntity ce -> e.getType().builtInRegistryHolder().key().location().getNamespace().equals("lootr") ? false : contains(ce, want);
                default -> false;
            };
            if (hit) out.add(e.blockPosition());
        }
        return out;
    }

    private static boolean lootr(BlockEntity be) {
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        return id != null && id.getNamespace().equals("lootr");
    }

    private static boolean holds(ServerLevel level, BlockPos pos, BlockEntity be, Set<Item> want) {
        try {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, be.getBlockState(), be, null);
            if (h != null) {
                for (int i = 0; i < h.getSlots(); i++) if (matches(h.getStackInSlot(i), want)) return true;
                return false;
            }
            if (be instanceof Container c) return contains(c, want);
        } catch (RuntimeException ex) {
            // a storage of some mod that fails when asked from outside: skip it, never break the search
            WildsCore.LOG.debug("Item finder: storage at {} failed", pos, ex);
        }
        return false;
    }

    private static boolean contains(Container c, Set<Item> want) {
        for (int i = 0; i < c.getContainerSize(); i++) if (matches(c.getItem(i), want)) return true;
        return false;
    }

    /** the stack, or anything inside it (a shulker box's contents, a bundle) */
    static boolean matches(ItemStack s, Set<Item> want) {
        if (s.isEmpty()) return false;
        if (want.contains(s.getItem())) return true;
        var inner = s.get(DataComponents.CONTAINER);
        if (inner != null) for (ItemStack i : inner.nonEmptyItems()) if (want.contains(i.getItem())) return true;
        var bundle = s.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) for (ItemStack i : bundle.items()) if (want.contains(i.getItem())) return true;
        return false;
    }

    /** Open Parties and Claims, when installed: a claim of someone outside the player's party is not looked into */
    static final class Claims {
        private static final boolean OPAC = ModList.get().isLoaded("openpartiesandclaims");

        static boolean mayLook(ServerPlayer p, ServerLevel level, int cx, int cz) {
            if (!OPAC) return true;
            try {
                var api = xaero.pac.common.server.api.OpenPACServerAPI.get(p.server);
                var claim = api.getServerClaimsManager().get(level.dimension().location(), cx, cz);
                if (claim == null) return true;
                UUID owner = claim.getPlayerId();
                if (owner == null || owner.equals(p.getUUID())) return true;
                var parties = api.getPartyManager();
                var a = parties.getPartyByMember(p.getUUID());
                var b = parties.getPartyByMember(owner);
                return a != null && b != null && a.getId().equals(b.getId());
            } catch (RuntimeException | LinkageError ex) {
                return true;
            }
        }
    }
}
