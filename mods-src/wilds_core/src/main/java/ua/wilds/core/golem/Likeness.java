package ua.wilds.core.golem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How alike two items are, for the copper golem's sorting by likeness: 0 is the same item, bigger is less alike,
 * {@link #NONE} not alike at all. In order:
 * <ol>
 * <li>a tag both items are in, the smaller the tag the closer (spruce and birch logs: minecraft:logs; leggings: the leg
 * armour tag, then all armour; a modded gem and a diamond: c:gems);</li>
 * <li>a word of their ids both share (spruce_planks and spruce_log: "spruce");</li>
 * <li>the same kind of id (..._ingot, ..._seeds, ..._sapling);</li>
 * <li>the same broad kind of thing: armour, weapons and tools, food, potions, spawn eggs (not "blocks": too broad).</li>
 * </ol>
 * Tags are looked at once per item and kept until the tags reload ({@link #clear}).
 */
final class Likeness {
    static final int NONE = Integer.MAX_VALUE;
    private static final int BIG_TAG = 128;
    private static final Set<String> COMMON_WORDS = Set.of("block", "item", "stairs", "slab", "wall", "fence", "gate", "door", "trapdoor",
            "button", "pressure", "plate", "with", "from", "small", "large", "big", "raw", "cooked", "smooth", "polished", "chiseled",
            "cracked", "mossy", "waxed", "exposed", "weathered", "oxidized", "stripped", "pane", "glass", "wool", "carpet", "spawn", "egg");

    private record Tag(TagKey<Item> key, int size) {}

    private static final Map<Item, List<Tag>> tags = new HashMap<>();
    private static final Map<TagKey<Item>, Integer> sizes = new HashMap<>();

    private Likeness() {}

    static void clear() {
        tags.clear();
        sizes.clear();
    }

    static int distance(Item a, Item b) {
        if (a == b) return 0;
        for (Tag t : tagsOf(a)) {
            if (b.builtInRegistryHolder().is(t.key)) return t.size <= 8 ? 10 : t.size <= 32 ? 20 : 30;
        }
        String pa = BuiltInRegistries.ITEM.getKey(a).getPath(), pb = BuiltInRegistries.ITEM.getKey(b).getPath();
        Set<String> wa = words(pa);
        for (String w : words(pb)) if (wa.contains(w)) return 35;
        int ia = pa.lastIndexOf('_'), ib = pb.lastIndexOf('_');
        if (ia > 0 && ib > 0 && pa.substring(ia).equals(pb.substring(ib))) return 50;
        String ka = kind(a);
        return ka != null && ka.equals(kind(b)) ? 60 : NONE;
    }

    /** the item's tags that say something about it, smallest first */
    private static List<Tag> tagsOf(Item item) {
        return tags.computeIfAbsent(item, it -> {
            List<Tag> out = new ArrayList<>();
            it.builtInRegistryHolder().tags().forEach(key -> {
                if (key.location().getPath().contains("hidden")) return;
                int size = sizes.computeIfAbsent(key, k -> BuiltInRegistries.ITEM.getTag(k).map(s -> s.size()).orElse(0));
                if (size > 1 && size <= BIG_TAG) out.add(new Tag(key, size));
            });
            out.sort(Comparator.comparingInt(Tag::size));
            return out;
        });
    }

    private static Set<String> words(String path) {
        Set<String> out = new HashSet<>();
        for (String w : path.split("_")) if (w.length() >= 4 && !COMMON_WORDS.contains(w)) out.add(w);
        return out;
    }

    private static String kind(Item item) {
        if (item instanceof ArmorItem) return "armor";
        if (item instanceof SwordItem || item instanceof DiggerItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem)
            return "tool";
        if (item.components().has(net.minecraft.core.component.DataComponents.FOOD)) return "food";
        if (item instanceof PotionItem) return "potion";
        if (item instanceof SpawnEggItem) return "egg";
        return null;
    }
}
