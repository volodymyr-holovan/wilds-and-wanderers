package ua.wilds.core.skills;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ua.wilds.core.WildsCore;

import java.util.function.Supplier;

/**
 * The extra inventory rows of the removed skill branch Торба are gone, but players still have what they kept in them:
 * the storage stays registered (an unknown attachment would be thrown away with its items) and hands everything back
 * when its owner joins, into the inventory, what does not fit at the player's feet. The points spent on the branch come
 * back by themselves: Pufferfish's Skills counts spent points from the nodes that still exist in the tree.
 */
public final class LegacyRows {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WildsCore.ID);
    private static final Supplier<AttachmentType<ItemStackHandler>> EXTRA_ROWS = ATTACHMENTS.register("extra_rows",
            () -> AttachmentType.serializable(() -> new ItemStackHandler(18)).copyOnDeath().build());

    private LegacyRows() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) giveBack(p);
        });
    }

    private static void giveBack(ServerPlayer p) {
        if (!p.hasData(EXTRA_ROWS)) return;
        ItemStackHandler h = p.getData(EXTRA_ROWS);
        boolean any = false;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i).copy();
            if (s.isEmpty()) continue;
            h.setStackInSlot(i, ItemStack.EMPTY);
            any = true;
            if (!p.getInventory().add(s) || !s.isEmpty()) p.drop(s, false);
        }
        p.removeData(EXTRA_ROWS);
        if (any) p.sendSystemMessage(Component.literal("§eДодаткові ряди інвентаря прибрано: речі з них повернуто вам в інвентар (що не влізло — лежить поруч)."));
    }
}
