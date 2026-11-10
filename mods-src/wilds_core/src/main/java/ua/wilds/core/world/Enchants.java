package ua.wilds.core.world;

import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * Loot that asks for "a random enchantment" without saying which (loot tables of mods written before 1.21) gets one out
 * of every enchantment of the game, also the technical ones data packs keep for their own mechanics: Dungeons and
 * Taverns' boss sticks carry nova_structures:shulker_boss, named "LOOT TABLE ERROR - REPORT TO MOD/DATAPACK AUTHOR", and
 * players found books of it. Such loot now chooses among the enchantments meant for players: those in any of vanilla's
 * tags for the enchanting table, random loot, trades, treasure, mob equipment or curses, and those with a translated
 * name (technical ones are named with plain text).
 */
public final class Enchants {
    private static final List<TagKey<Enchantment>> GIVEN_OUT = List.of(EnchantmentTags.IN_ENCHANTING_TABLE, EnchantmentTags.ON_RANDOM_LOOT,
            EnchantmentTags.TRADEABLE, EnchantmentTags.TREASURE, EnchantmentTags.NON_TREASURE, EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT,
            EnchantmentTags.ON_TRADED_EQUIPMENT, EnchantmentTags.CURSE);
    private static boolean logged;

    private Enchants() {}

    public static boolean givenOut(Holder<Enchantment> h) {
        for (TagKey<Enchantment> t : GIVEN_OUT) if (h.is(t)) return true;
        // a mod's enchantment with a real name that only its own loot gives stays; technical ones are named with plain text
        return h.value().description().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents;
    }

    /** the list without technical enchantments (the list itself when nothing would be left) */
    public static List<Holder<Enchantment>> givenOut(List<Holder<Enchantment>> all) {
        List<Holder<Enchantment>> out = new ArrayList<>(all.size());
        StringBuilder left = new StringBuilder();
        for (Holder<Enchantment> h : all) {
            if (givenOut(h)) out.add(h);
            else if (!logged) left.append(' ').append(h.unwrapKey().map(k -> k.location().toString()).orElse("?"));
        }
        if (!logged && all.size() > 20) {
            logged = true;
            ua.wilds.core.WildsCore.LOG.info("Wilds Core: enchantments never given by loot without a list:{}", left);
        }
        return out.isEmpty() ? all : out;
    }
}
