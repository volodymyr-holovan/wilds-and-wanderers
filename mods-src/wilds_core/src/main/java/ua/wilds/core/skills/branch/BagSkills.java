package ua.wilds.core.skills.branch;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ua.wilds.core.WildsCore;
import ua.wilds.core.skills.ClientAbilities;
import ua.wilds.core.skills.Perks;
import ua.wilds.core.skills.SkillState;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Торба. Big abilities:
 * <ul>
 * <li>extra_row "Ще один ряд": one more row of nine slots in the inventory per level (two levels). The rows are a storage
 * of their own (a player attachment), shown in the equipment screen between the inventory and the hotbar
 * ({@code AccessoriesRowsMenuMixin}), so the vanilla inventory keeps its 36 slots and no mod that counts on them breaks.
 * Picked up items go there when the inventory is full; on death they drop like the rest (unless keepInventory); rows
 * that are no longer unlocked (the tree was reset) give their items back;</li>
 * <li>pocket_crafting "Кишеньковий верстак": a button in the inventory opens a 3x3 crafting grid anywhere;</li>
 * <li>mender "Дбайливі руки": worn armour and the items in both hands mend one point every 10 s (5 s at level 2);</li>
 * <li>magnet "Мандрівна торба" (the keystone): items within 6 blocks fly to the player (not while sneaking).</li>
 * </ul>
 * Perk: pickup_range (blocks added to the reach of picking items up).
 */
public final class BagSkills {
    public static final int ROWS = 2;
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WildsCore.ID);
    public static final Supplier<AttachmentType<ItemStackHandler>> EXTRA_ROWS = ATTACHMENTS.register("extra_rows",
            () -> AttachmentType.serializable(() -> new ItemStackHandler(ROWS * 9)).copyOnDeath().build());

    /** crafting grids opened by the button: they stay open away from a crafting table */
    private static final Set<AbstractContainerMenu> pocket = Collections.newSetFromMap(new WeakHashMap<>());

    private BagSkills() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BagSkills::tick);
        NeoForge.EVENT_BUS.addListener(BagSkills::pickup);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, BagSkills::drops);
    }

    public static ItemStackHandler rows(Player p) {
        return p.getData(EXTRA_ROWS);
    }

    /** how many extra rows the player has unlocked; the client knows it from the abilities the server sent */
    public static int unlocked(Player p) {
        int lv = p.level().isClientSide ? ClientAbilities.level("extra_row") : SkillState.level(p, "extra_row");
        return Math.max(0, Math.min(ROWS, lv));
    }

    // ---------------------------------------------------------------- picking up into the extra rows

    private static void pickup(ItemEntityPickupEvent.Pre e) {
        Player p = e.getPlayer();
        if (p.level().isClientSide) return;
        int open = unlocked(p) * 9;
        ItemEntity ie = e.getItemEntity();
        if (open == 0 || ie.hasPickUpDelay() || ie.isRemoved()) return;
        ItemStack stack = ie.getItem();
        Inventory inv = p.getInventory();
        if (inv.getSlotWithRemainingSpace(stack) >= 0 || inv.getFreeSlot() >= 0) return;   // the inventory takes it as usual
        ItemStack left = insert(rows(p), open, stack.copy());
        int taken = stack.getCount() - left.getCount();
        if (taken <= 0) return;
        p.take(ie, taken);
        if (left.isEmpty()) ie.discard();
        else ie.setItem(left);
    }

    /** into the first slots of the rows: onto stacks of the same item first, then into empty slots */
    public static ItemStack insert(ItemStackHandler h, int slots, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < slots && !stack.isEmpty(); i++) {
                if ((pass == 0) == h.getStackInSlot(i).isEmpty()) continue;
                stack = h.insertItem(i, stack, false);
            }
        }
        return stack;
    }

    private static void drops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        ItemStackHandler h = rows(p);
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemEntity ie = new ItemEntity(p.level(), p.getX(), p.getEyeY() - 0.3, p.getZ(), s.copy());
            ie.setDefaultPickUpDelay();
            e.getDrops().add(ie);
            h.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ---------------------------------------------------------------- every few ticks

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int t = p.tickCount;
        ua.wilds.core.inventory.ExtraRows.sync(p);
        if (t % 4 == 0) reach(p);
        if (t % 40 == 0) giveBackLocked(p);
        int mender = SkillState.level(p, "mender");
        if (mender > 0 && t % (mender >= 2 ? 100 : 200) == 0) mend(p);
    }

    /** pickup_range: items that close are picked up; magnet: items within 6 blocks fly in */
    private static void reach(ServerPlayer p) {
        double extra = Math.min(4, SkillState.perk(p, Perks.PICKUP_RANGE));
        boolean magnet = SkillState.has(p, "magnet") && !p.isShiftKeyDown();
        if (extra <= 0 && !magnet) return;
        double r = Math.max(magnet ? 6 : 0, 1 + extra);
        Vec3 at = p.position().add(0, 0.6, 0);
        for (ItemEntity ie : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(r), ie -> !ie.hasPickUpDelay() && ie.isAlive())) {
            double d = ie.position().distanceTo(at);
            if (d <= 1 + extra) ie.playerTouch(p);
            else if (magnet) {
                Vec3 pull = at.subtract(ie.position()).normalize().scale(0.35);
                ie.setDeltaMovement(pull);
                ie.hasImpulse = true;
            }
        }
    }

    private static void giveBackLocked(ServerPlayer p) {
        ItemStackHandler h = rows(p);
        for (int i = unlocked(p) * 9; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            h.setStackInSlot(i, ItemStack.EMPTY);
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
    }

    private static void mend(ServerPlayer p) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
            ItemStack s = p.getItemBySlot(slot);
            if (s.isDamageableItem() && s.isDamaged()) s.setDamageValue(s.getDamageValue() - 1);
        }
    }

    // ---------------------------------------------------------------- the pocket crafting table

    public static void pocketCraft(ServerPlayer p) {
        if (!SkillState.has(p, "pocket_crafting")) return;
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> {
            CraftingMenu m = new CraftingMenu(id, inv, ContainerLevelAccess.create(pl.level(), pl.blockPosition()));
            pocket.add(m);
            return m;
        }, Component.translatable("container.crafting")));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 0.5f, 1.3f);
    }

    /** CraftingMenuMixin: a pocket grid stays open wherever the player goes */
    public static boolean isPocket(AbstractContainerMenu m) {
        return pocket.contains(m);
    }

    /**
     * a slot of the extra rows, in any menu (inventory.ExtraRows, AccessoriesRowsMenuMixin): inactive while its row is
     * not unlocked (not drawn, nothing goes in or out)
     */
    public static final class RowSlot extends net.neoforged.neoforge.items.SlotItemHandler {
        private final Player player;

        public RowSlot(net.neoforged.neoforge.items.IItemHandler h, int index, Player player, int x, int y) {
            super(h, index, x, y);
            this.player = player;
        }

        public boolean isActive() {
            return getSlotIndex() < unlocked(player) * 9;
        }

        public boolean mayPlace(ItemStack stack) {
            return isActive() && super.mayPlace(stack);
        }

        public boolean mayPickup(Player p) {
            return isActive() && super.mayPickup(p);
        }
    }
}
