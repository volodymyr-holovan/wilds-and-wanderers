package ua.wilds.core.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.wilds.core.net.Net;
import ua.wilds.core.skills.branch.BagSkills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The extra inventory rows (skill extra_row) in every screen that shows the player's inventory: chests and storages of
 * any mod, backpacks, furnaces, crafting tables, machines.
 * <p>
 * <b>No slots.</b> The rows are a storage of their own (BagSkills.EXTRA_ROWS) and are <i>never</i> added to other
 * mods' menus as slots: every mod counts its menu's slots its own way (Sophisticated Backpacks counts "the storage" as
 * every slot after the inventory, others sync their slots by number, check sizes, rebuild their slot lists), and slots
 * appended by somebody else break them in ways no list of exceptions keeps up with. Instead the client draws the rows as
 * a panel under the screen (client.RowsPanel) from a copy the server keeps sending (RowsSync), and a click on it is a
 * message of its own (RowClick) that the server carries out here, with the menu's cursor item: the menu itself never
 * knows the rows exist, so no menu of any mod can be broken by them.
 * <p>
 * Only the equipment screen (Accessories, our own layout) has them as real slots (AccessoriesRowsMenuMixin).
 * <p>
 * <b>Shift-click</b>: from a row, into the screen's storage when it has one (nine slots or more: a chest, a backpack),
 * else into the inventory; from anything else, the menu's own rule first, and only what is left (the inventory, or
 * the hotbar, being full) goes into the rows.
 */
public final class ExtraRows {
    /** the last rows' contents sent to each player */
    private static final Map<ServerPlayer, List<ItemStack>> sent = new WeakHashMap<>();

    private ExtraRows() {}

    /** the screen of this menu may show the rows panel: it shows all 36 slots of the player's inventory */
    public static boolean fits(AbstractContainerMenu menu, Player player) {
        if (menu == null || menu instanceof InventoryMenu || menu == player.inventoryMenu || hasSlots(menu)) return false;
        boolean[] seen = new boolean[36];
        int n = 0;
        for (Slot s : menu.slots) {
            if (s.container == player.getInventory() && s.getContainerSlot() >= 0 && s.getContainerSlot() < 36 && !seen[s.getContainerSlot()]) {
                seen[s.getContainerSlot()] = true;
                n++;
            }
        }
        return n == 36;
    }

    /** the menu has the rows as slots of its own (the equipment screen) */
    public static boolean hasSlots(AbstractContainerMenu menu) {
        for (int i = menu.slots.size() - 1; i >= 0; i--) if (menu.slots.get(i) instanceof BagSkills.RowSlot) return true;
        return false;
    }

    /** the player's client has the rows (an older pack without them gets nothing that would confuse it) */
    public static boolean client(ServerPlayer p) {
        return p.connection != null && p.connection.hasChannel(Net.RowsV1.TYPE);
    }

    // ---------------------------------------------------------------- the client's copy

    /** every tick: the rows' contents to the client when they changed */
    public static void sync(ServerPlayer p) {
        if (!client(p)) return;
        ItemStackHandler h = BagSkills.rows(p);
        List<ItemStack> last = sent.get(p);
        boolean same = last != null && last.size() == h.getSlots();
        for (int i = 0; same && i < h.getSlots(); i++) same = ItemStack.matches(last.get(i), h.getStackInSlot(i));
        if (same) return;
        List<ItemStack> now = new ArrayList<>(h.getSlots());
        for (int i = 0; i < h.getSlots(); i++) now.add(h.getStackInSlot(i).copy());
        sent.put(p, now);
        PacketDistributor.sendToPlayer(p, new Net.RowsSync(now));
    }

    // ---------------------------------------------------------------- a click on the panel

    public static final int PICKUP = 0, QUICK = 1, SWAP = 2, THROW = 3;

    /**
     * a click on the K-th row slot of the panel under the screen of the menu containerId: PICKUP (button 0 all, 1 half
     * or one), QUICK (shift), SWAP (button: the hotbar slot 0-8 or 40 the off hand), THROW (button 0 one, 1 all)
     */
    public static void click(ServerPlayer p, int containerId, int index, int button, int mode) {
        AbstractContainerMenu menu = p.containerMenu;
        if (menu.containerId != containerId || p.isSpectator() || !fits(menu, p) || !menu.stillValid(p)) return;
        if (index < 0 || index >= BagSkills.unlocked(p) * 9) return;
        ItemStackHandler h = BagSkills.rows(p);
        ItemStack in = h.getStackInSlot(index).copy();
        switch (mode) {
            case PICKUP -> pickup(menu, h, index, in, button);
            case QUICK -> {
                if (in.isEmpty()) break;
                h.setStackInSlot(index, moveOut(menu, p, in));
            }
            case SWAP -> {
                if (button != 40 && (button < 0 || button > 8)) break;
                Inventory inv = p.getInventory();
                ItemStack other = inv.getItem(button).copy();
                if (in.isEmpty() && other.isEmpty()) break;
                // the menu's own rules for that inventory slot (a backpack opened from the hotbar is locked in its
                // slot while its screen is open: moving it would duplicate its contents)
                Slot own = null;
                for (Slot s : menu.slots) if (s.container == inv && s.getContainerSlot() == button) own = s;
                if (own != null && (!other.isEmpty() && !own.mayPickup(p) || !in.isEmpty() && !own.mayPlace(in))) break;
                // and never an item that holds items itself while some screen is open (the one it may be open in)
                if (holdsItems(other) || holdsItems(in)) break;
                inv.setItem(button, in);
                h.setStackInSlot(index, other);
            }
            case THROW -> {
                if (in.isEmpty() || !menu.getCarried().isEmpty()) break;
                int n = button == 1 ? in.getCount() : 1;
                h.setStackInSlot(index, in.copyWithCount(in.getCount() - n));
                p.drop(in.copyWithCount(n), true);
            }
            default -> {
            }
        }
        menu.broadcastChanges();   // the cursor item and the inventory to the client
        sync(p);
    }

    /** a backpack, a shulker box, a bundle: an item with items inside */
    private static boolean holdsItems(ItemStack s) {
        if (s.isEmpty()) return false;
        return s.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM) != null
                || s.has(net.minecraft.core.component.DataComponents.CONTAINER) || s.has(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
    }

    /** a left (button 0) or right (1) click with the cursor item, as on any vanilla slot */
    private static void pickup(AbstractContainerMenu menu, ItemStackHandler h, int index, ItemStack in, int button) {
        ItemStack carried = menu.getCarried().copy();
        if (carried.isEmpty()) {
            if (in.isEmpty()) return;
            int n = button == 0 ? in.getCount() : (in.getCount() + 1) / 2;
            menu.setCarried(in.copyWithCount(n));
            h.setStackInSlot(index, in.copyWithCount(in.getCount() - n));
        } else if (in.isEmpty()) {
            int n = Math.min(button == 0 ? carried.getCount() : 1, Math.min(carried.getMaxStackSize(), h.getSlotLimit(index)));
            h.setStackInSlot(index, carried.copyWithCount(n));
            menu.setCarried(carried.copyWithCount(carried.getCount() - n));
        } else if (ItemStack.isSameItemSameComponents(in, carried)) {
            int room = Math.min(in.getMaxStackSize(), h.getSlotLimit(index)) - in.getCount();
            int n = Math.min(room, button == 0 ? carried.getCount() : 1);
            if (n <= 0) return;
            h.setStackInSlot(index, in.copyWithCount(in.getCount() + n));
            menu.setCarried(carried.copyWithCount(carried.getCount() - n));
        } else if (carried.getCount() <= Math.min(carried.getMaxStackSize(), h.getSlotLimit(index))) {
            h.setStackInSlot(index, carried);
            menu.setCarried(in);
        }
    }

    // ---------------------------------------------------------------- shift-click

    /** shift-click on a row slot of the equipment screen (its rows are slots) */
    public static void quickMoveFromRow(AbstractContainerMenu menu, Player player, BagSkills.RowSlot from) {
        ItemStack stack = from.getItem();
        if (stack.isEmpty() || !from.mayPickup(player)) return;
        ItemStack left = moveOut(menu, player, stack.copy());
        if (left.getCount() == stack.getCount()) return;
        from.set(left);
        from.setChanged();
    }

    /**
     * out of the rows: into the screen's storage (a chest, a backpack, a barrel: nine slots or more), else into the
     * inventory; never into a machine's few slots (a diamond does not go into a furnace's input, as vanilla would not
     * put it there either). Returns what is left.
     */
    static ItemStack moveOut(AbstractContainerMenu menu, Player player, ItemStack stack) {
        List<Slot> storage = storageSlots(menu, player);
        return insert(storage.size() < 9 ? inventorySlots(menu, player) : storage, stack);
    }

    /** the server, after the menu's own shift-click: what it could not place goes into the rows */
    public static void quickMoveRest(AbstractContainerMenu menu, Player player, Slot from) {
        if (!(player instanceof ServerPlayer sp) || from instanceof BagSkills.RowSlot || from instanceof ResultSlot) return;
        if (!from.hasItem() || !from.mayPickup(player)) return;
        // an output (a furnace's, a machine's): taking from it has rules of its own (experience, recipes) that only the
        // menu's own moving follows
        if (!from.mayPlace(from.getItem())) return;
        if (!hasSlots(menu) && !(client(sp) && fits(menu, player))) return;
        int open = BagSkills.unlocked(player) * 9;
        if (open == 0) return;
        boolean ownInventory = from.container == player.getInventory();
        // from the inventory in a screen with a storage the menu tried that storage: leave it where it was
        if (ownInventory && storageSlots(menu, player).size() >= 9) return;
        // from somewhere else (a chest...) only if the inventory itself is full for this item
        if (!ownInventory && insert(inventorySlots(menu, player), from.getItem().copy(), true).getCount() < from.getItem().getCount()) return;
        ItemStack stack = from.getItem();
        ItemStack left = BagSkills.insert(BagSkills.rows(player), open, stack.copy());
        if (left.getCount() == stack.getCount()) return;
        from.set(left);
        from.setChanged();
    }

    /** the menu's storage that is not the player's: a chest's slots, a furnace's input and fuel... (not crafting grids) */
    static List<Slot> storageSlots(AbstractContainerMenu menu, Player player) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (s instanceof BagSkills.RowSlot || s instanceof ResultSlot || s.container == player.getInventory() || !s.isActive()) continue;
            Container c = s.container;
            if (c instanceof CraftingContainer) continue;
            String name = s.getClass().getName() + c.getClass().getName();
            if (name.contains("io.wispforest.accessories") || name.contains("Armor") || name.contains("Curio")) continue;
            out.add(s);
        }
        return out;
    }

    /** the player's 36 inventory slots in the menu: the main rows first, then the hotbar */
    static List<Slot> inventorySlots(AbstractContainerMenu menu, Player player) {
        List<Slot> main = new ArrayList<>(), bar = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (s.container != player.getInventory()) continue;
            int i = s.getContainerSlot();
            if (i >= 9 && i < 36) main.add(s);
            else if (i >= 0 && i < 9) bar.add(s);
        }
        main.addAll(bar);
        return main;
    }

    static ItemStack insert(List<Slot> slots, ItemStack stack) {
        return insert(slots, stack, false);
    }

    /** onto stacks of the same item first, then into empty slots; every slot's own rules (mayPlace, limits) apply */
    static ItemStack insert(List<Slot> slots, ItemStack stack, boolean simulate) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (Slot s : slots) {
                if (stack.isEmpty()) break;
                ItemStack in = s.getItem();
                if (pass == 0 ? in.isEmpty() || !ItemStack.isSameItemSameComponents(in, stack) : !in.isEmpty()) continue;
                if (!s.mayPlace(stack)) continue;
                int max = Math.min(s.getMaxStackSize(stack), stack.getMaxStackSize());
                int room = max - in.getCount();
                if (room <= 0) continue;
                int moved = Math.min(room, stack.getCount());
                if (!simulate) {
                    if (in.isEmpty()) s.set(stack.copyWithCount(moved));
                    else s.set(in.copyWithCount(in.getCount() + moved));
                    s.setChanged();
                }
                stack = stack.copyWithCount(stack.getCount() - moved);
            }
        }
        return stack;
    }
}
