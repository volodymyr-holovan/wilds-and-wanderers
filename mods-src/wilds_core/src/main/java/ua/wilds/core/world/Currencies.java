package ua.wilds.core.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import ua.wilds.core.WildsCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The pack's money: emeralds (villagers, wandering traders, quest givers, bounty boards) and goblin coins (the goblins
 * of Goblin's Tyranny). Every wandering trader, besides their usual goods, always changes any of them into any other,
 * at a fixed rate with a small spread, and never runs out. A currency whose mod is missing is simply left out.
 */
public final class Currencies {
    /** a currency and how many of it one emerald is worth */
    private record Money(String id, int perEmerald) {}

    private static final List<Money> ALL = List.of(new Money("minecraft:emerald", 1), new Money("goblins_tyranny:coin", 4));
    private static final String TAG = WildsCore.ID + ":exchange";

    private Currencies() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof WanderingTrader t)) return;
            if (t.getPersistentData().getBoolean(TAG)) return;
            List<MerchantOffer> add = offers();
            if (add.isEmpty()) return;
            MerchantOffers offers = t.getOffers();   // makes the trader's usual goods first
            offers.addAll(add);
            t.getPersistentData().putBoolean(TAG, true);
        });
    }

    /**
     * every pair both ways. Buying a currency costs its worth plus a little: 1 emerald -> 4 coins, 5 coins -> 1 emerald
     */
    private static List<MerchantOffer> offers() {
        List<Money> present = new ArrayList<>();
        for (Money m : ALL) if (item(m) != Items.AIR) present.add(m);
        List<MerchantOffer> out = new ArrayList<>();
        for (Money a : present) {
            for (Money b : present) {
                if (a == b) continue;
                // pay `give` of a, get `get` of b; the side worth less pays one more unit
                int give, get;
                if (a.perEmerald <= b.perEmerald) {          // a is the dearer one: one unit of value buys b's worth
                    give = 1;
                    get = b.perEmerald / a.perEmerald;
                } else {
                    give = a.perEmerald / b.perEmerald + 1;
                    get = 1;
                }
                out.add(new MerchantOffer(new ItemCost(item(a), give), Optional.empty(), new ItemStack(item(b), get), 99999, 0, 0f));
            }
        }
        return out;
    }

    private static Item item(Money m) {
        ResourceLocation rl = ResourceLocation.tryParse(m.id);
        return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
    }
}
