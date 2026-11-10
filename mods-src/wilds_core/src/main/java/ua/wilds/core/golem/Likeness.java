package ua.wilds.core.golem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import ua.wilds.core.WildsCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a storage is "about", for the copper golem's sorting by likeness.
 * <p>
 * Every item has <b>traits</b>, found from the game's own data so that modded items get them like vanilla ones:
 * its tags, the game's own and the common ones (logs, stairs, c:gems, wool...), the words of its id, also inside longer words (sand in
 * sandstone, book in bookshelf), its dye colour, what it is (food, potion, melee or ranged weapon, armour and for which
 * slot, digging tool, ammunition, bucket, spawn egg, music disc), what the block is made of by the tool that mines it
 * (wood, earth, stone, plants), a falling block, the block's colour on a map (sand, sandstone and birch are all "sand
 * coloured"), its mod, and its <b>family</b>: what it is made of when a recipe makes it from one material only (purpur
 * stairs, slabs and pillars from the purpur block; planks, stairs and doors from their log).
 * <p>
 * A storage's <b>theme</b> is the traits its items share (three quarters of them, so one stray item does not blind it).
 * That is what makes sand among sandstone "sand coloured desert blocks" and not "falling blocks", and a bookshelf among
 * planks "wooden things" and not "books": only what the neighbours have too counts. A trait says more the fewer items
 * of the whole game have it, so a stack goes to the storage whose theme fits it by the rarest trait (purpur stairs: the
 * purpur storage before the stairs storage before the building blocks storage). A broad trait needs more witnesses: one
 * lone item never makes a "stone things" storage, three do. And when something narrow explains a storage, the much
 * broader things its items also share do not count (boots are all "armour that takes trims", the storage is about boots).
 * <p>
 * A storage with no theme at all (a jumble) still takes what is made of, or is the material of, something it holds.
 * The very same item already lying somewhere always wins over all of this (CopperSorter.Survey).
 */
final class Likeness {
    static final int NONE = Integer.MAX_VALUE;
    /** the same family as an item of a storage without a theme */
    private static final int FAMILY_ONLY = 200;
    /** tags, words and families bigger than this say too little; what a thing is, is made of or looks like may be bigger */
    private static final int WIDE = 350;
    private static final Set<String> NO_WORDS = Set.of("the", "and", "with", "from", "block", "item", "for", "raw");
    private static final String[] BROAD = {"cat:", "mat:", "color:", "mod:", "dye:"};

    private static Index index;

    private Likeness() {}

    static void clear() {
        index = null;
    }

    private static Index index() {
        if (index == null) {
            long t = System.nanoTime();
            index = new Index(ServerLifecycleHooks.getCurrentServer());
            WildsCore.LOG.info("Copper golem: {} items, {} traits, {} families learned in {} ms", index.traits.size(), index.names.size(), index.families,
                    (System.nanoTime() - t) / 1_000_000);
        }
        return index;
    }

    /** what the items of one storage share */
    static final class Theme {
        final Set<Item> items;
        /** trait ids, sorted */
        final int[] traits;
        private final Set<Item> roots = new HashSet<>();

        private Theme(Set<Item> items, int[] traits, Index ix) {
            this.items = items;
            this.traits = traits;
            for (Item i : items) roots.add(ix.root(i));
        }
    }

    static Theme theme(Set<Item> items) {
        Index ix = index();
        int n = items.size(), need = n <= 3 ? n : (n * 3 + 3) / 4;
        Map<Integer, Integer> count = new HashMap<>();
        for (Item i : items) for (int t : ix.of(i)) count.merge(t, 1, Integer::sum);
        List<Integer> shared = new ArrayList<>();
        int least = Integer.MAX_VALUE;
        for (var e : count.entrySet()) {
            int size = ix.sizes[e.getKey()];
            // a broad trait needs more witnesses, and so does a look or a material: one item is no "magenta things"
            if (e.getValue() < need || (n < 2 && (size > 64 || ix.broad[e.getKey()])) || (n < 3 && size > WIDE)) continue;
            shared.add(e.getKey());
            least = Math.min(least, size);
        }
        // when something narrow explains the storage, the much broader things its items also share are a coincidence
        // (boots are all "trimmable armour" too, but the storage is about boots)
        int most = Math.max(100, least * 3);
        shared.removeIf(t -> ix.sizes[t] > most);
        int[] traits = shared.stream().mapToInt(Integer::intValue).sorted().toArray();
        return new Theme(items, traits, ix);
    }

    /** how well the item fits the storage: 0 it is there already, smaller is better, NONE not at all */
    static int fit(Item item, Theme theme) {
        if (theme.items.contains(item)) return 0;
        Index ix = index();
        int best = NONE;
        for (int t : ix.of(item)) {
            if (Arrays.binarySearch(theme.traits, t) < 0) continue;
            best = Math.min(best, score(ix.sizes[t]));
        }
        if (best != NONE) return best;
        return theme.traits.length == 0 && theme.roots.contains(ix.root(item)) ? FAMILY_ONLY : NONE;
    }

    private static int score(int size) {
        return 10 + (int) Math.round(10 * Math.log(Math.max(2, size)) / Math.log(2));
    }

    /** for /wildssort: the theme's traits, the most telling first, and why an item fits */
    static String describe(Theme theme) {
        Index ix = index();
        List<Integer> t = new ArrayList<>();
        for (int i : theme.traits) t.add(i);
        t.sort((a, b) -> ix.sizes[a] - ix.sizes[b]);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(12, t.size()); i++) sb.append(i == 0 ? "" : ", ").append(ix.names.get(t.get(i))).append('(').append(ix.sizes[t.get(i)]).append(')');
        return sb.length() == 0 ? "no theme (takes the families of its items)" : sb.toString();
    }

    static String why(Item item, Theme theme) {
        if (theme.items.contains(item)) return "the same item is there";
        Index ix = index();
        int best = -1;
        for (int t : ix.of(item)) if (Arrays.binarySearch(theme.traits, t) >= 0 && (best < 0 || ix.sizes[t] < ix.sizes[best])) best = t;
        if (best >= 0) return ix.names.get(best) + " (" + ix.sizes[best] + " items), score " + score(ix.sizes[best]);
        return theme.traits.length == 0 && theme.roots.contains(ix.root(item)) ? "family of " + BuiltInRegistries.ITEM.getKey(ix.root(item)) : "does not fit";
    }

    // ---------------------------------------------------------------- what the game knows about every item

    private static final class Index {
        final Map<Item, int[]> traits = new HashMap<>();
        final List<String> names = new ArrayList<>();
        int[] sizes;
        boolean[] broad;
        int families;
        private final Map<Item, Item> parent = new HashMap<>();
        private final Set<Item> materials = new HashSet<>();

        Index(MinecraftServer server) {
            if (server != null) families(server);
            Map<Item, List<String>> raw = new HashMap<>();
            Map<String, Integer> words = new HashMap<>();
            Map<Item, List<String>> wordsOf = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                List<String> t = new ArrayList<>();
                describe(item, t);
                raw.put(item, t);
                List<String> w = words(BuiltInRegistries.ITEM.getKey(item).getPath(), t);
                wordsOf.put(item, w);
                for (String s : new HashSet<>(w)) words.merge(s, 1, Integer::sum);
            }
            // a word inside a longer word: sand in sandstone, book in bookshelf, stone in cobblestone
            for (var e : wordsOf.entrySet()) {
                Set<String> all = new HashSet<>(e.getValue());
                for (String w : e.getValue()) {
                    for (int cut = 4; cut <= w.length() - 3; cut++) {
                        String head = w.substring(0, cut), tail = w.substring(w.length() - cut);
                        if (words.getOrDefault(head, 0) >= 2) all.add(head);
                        if (words.getOrDefault(tail, 0) >= 2) all.add(tail);
                    }
                }
                for (String w : all) raw.get(e.getKey()).add("word:" + w);
            }
            Map<String, Integer> count = new HashMap<>();
            for (List<String> t : raw.values()) for (String s : new HashSet<>(t)) count.merge(s, 1, Integer::sum);
            Map<String, Integer> ids = new HashMap<>();
            List<Integer> sizeList = new ArrayList<>();
            for (var e : raw.entrySet()) {
                Set<Integer> out = new HashSet<>();
                for (String s : e.getValue()) {
                    int n = count.get(s);
                    if (n < 2 || (n > WIDE && !broad(s))) continue;
                    Integer id = ids.get(s);
                    if (id == null) {
                        id = names.size();
                        ids.put(s, id);
                        names.add(s);
                        sizeList.add(n);
                    }
                    out.add(id);
                }
                traits.put(e.getKey(), out.stream().mapToInt(Integer::intValue).sorted().toArray());
            }
            sizes = sizeList.stream().mapToInt(Integer::intValue).toArray();
            broad = new boolean[sizes.length];
            for (int i = 0; i < broad.length; i++) broad[i] = broad(names.get(i));
        }

        int[] of(Item item) {
            int[] t = traits.get(item);
            return t == null ? new int[0] : t;
        }

        private static boolean broad(String trait) {
            for (String p : BROAD) if (trait.startsWith(p)) return true;
            return false;
        }

        /** the material at the start of the item's family (itself when it is made of nothing single) */
        Item root(Item item) {
            Item at = item;
            Set<Item> seen = new HashSet<>();
            while (parent.containsKey(at) && seen.add(at)) at = parent.get(at);
            if (!parent.containsKey(at)) return at;
            // a circle (a diamond and its block make each other): the same member for all of it
            Item least = at;
            for (Item i : seen) if (BuiltInRegistries.ITEM.getKey(i).compareTo(BuiltInRegistries.ITEM.getKey(least)) < 0) least = i;
            return least;
        }

        /** crafting and stonecutting recipes that make a thing out of one material */
        private void families(MinecraftServer server) {
            Map<Item, Set<Item>> madeOf = new HashMap<>();
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                Recipe<?> r = holder.value();
                if (r.getType() != RecipeType.CRAFTING && r.getType() != RecipeType.STONECUTTING) continue;
                try {
                    ItemStack result = r.getResultItem(server.registryAccess());
                    if (result.isEmpty()) continue;
                    Item material = null;
                    boolean single = true;
                    for (Ingredient in : r.getIngredients()) {
                        if (in.isEmpty()) continue;
                        ItemStack[] options = in.getItems();
                        if (options.length != 1 || (material != null && material != options[0].getItem())) {
                            single = false;
                            break;
                        }
                        material = options[0].getItem();
                    }
                    if (single && material != null && material != result.getItem()) madeOf.computeIfAbsent(result.getItem(), k -> new HashSet<>()).add(material);
                } catch (RuntimeException ex) {
                    // a recipe of some mod that cannot say what it makes without a crafting grid
                }
            }
            for (var e : madeOf.entrySet()) {
                if (e.getValue().size() != 1) continue;
                Item m = e.getValue().iterator().next();
                parent.put(e.getKey(), m);
                materials.add(m);
            }
        }

        private void describe(Item item, List<String> t) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            boolean[] melee = {item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem};
            item.builtInRegistryHolder().tags().forEach(k -> {
                String ns = k.location().getNamespace(), p = k.location().getPath();
                if (p.equals("swords") || p.equals("axes") || p.startsWith("tools/melee_weapon")) melee[0] = true;
                // only the game's own and the common tags describe things; mods' own are mostly lists for their mechanics
                if (!ns.equals("minecraft") && !ns.equals("c") && !ns.equals("neoforge")) return;
                if (p.contains("hidden") || p.startsWith("enchantable/") || p.contains("piglin") || p.contains("decorated_pot") || p.contains("repair")) return;
                t.add("tag:" + k.location());
            });
            if (item instanceof BlockItem bi) {
                BlockState s = bi.getBlock().defaultBlockState();
                s.getTags().forEach(k -> {
                    String loc = k.location().toString();
                    switch (loc) {
                        case "minecraft:mineable/axe" -> t.add("mat:wood");
                        case "minecraft:mineable/shovel" -> t.add("mat:earth");
                        case "minecraft:mineable/pickaxe" -> t.add("mat:stone");
                        case "minecraft:mineable/hoe" -> t.add("mat:plants");
                        default -> {
                        }
                    }
                });
                if (bi.getBlock() instanceof FallingBlock) t.add("cat:falling");
                try {
                    MapColor c = bi.getBlock().defaultMapColor();
                    if (c != MapColor.NONE) t.add("color:" + c.id);
                } catch (RuntimeException ex) {
                    // a block whose colour depends on a state it does not have yet
                }
            }
            var c = item.components();
            if (c.has(DataComponents.FOOD)) t.add("cat:food");
            if (item instanceof PotionItem || c.has(DataComponents.POTION_CONTENTS)) t.add("cat:potion");
            if (item instanceof SpawnEggItem) t.add("cat:spawn_egg");
            if (c.has(DataComponents.JUKEBOX_PLAYABLE)) t.add("cat:music_disc");
            if (item instanceof ArmorItem a) {
                t.add("cat:armor");
                t.add("cat:armor_" + a.getEquipmentSlot().getName());
            }
            // a weapon of any mod: it hits harder in the hand and is not a digging tool
            if (!melee[0] && !(item instanceof DiggerItem) && !(item instanceof ProjectileWeaponItem)) {
                var mods = c.get(DataComponents.ATTRIBUTE_MODIFIERS);
                if (mods != null) for (var m : mods.modifiers()) {
                    if (m.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) && m.modifier().amount() >= 3) melee[0] = true;
                }
            }
            if (melee[0]) t.add("cat:melee_weapon");
            if (item instanceof ProjectileWeaponItem || item instanceof TridentItem) t.add("cat:ranged_weapon");
            if (item instanceof ArrowItem) t.add("cat:ammunition");
            if (item instanceof DiggerItem) t.add("cat:digging_tool");
            if (item instanceof ShieldItem) t.add("cat:shield");
            if (item instanceof BucketItem) t.add("cat:bucket");
            if (!id.getNamespace().equals("minecraft")) t.add("mod:" + id.getNamespace());
            Item root = root(item);
            if (root != item || materials.contains(item)) {
                t.add("fam:" + BuiltInRegistries.ITEM.getKey(root));
                if (root == item) families++;
            }
        }

        /** the words of an id; a dye colour in it becomes the trait dye:colour and is not a word */
        private static List<String> words(String path, List<String> traits) {
            String p = "_" + path.replace('/', '_') + "_";
            DyeColor[] colours = DyeColor.values();
            Arrays.sort(colours, (a, b) -> b.getName().length() - a.getName().length());   // light_blue before blue
            for (DyeColor d : colours) {
                String key = "_" + d.getName() + "_";
                if (p.contains(key)) {
                    traits.add("dye:" + d.getName());
                    p = p.replace(key, "_");
                    break;
                }
            }
            List<String> out = new ArrayList<>();
            for (String w : p.split("_")) {
                if (w.length() > 4 && w.endsWith("s") && !w.endsWith("ss")) w = w.substring(0, w.length() - 1);   // planks, stairs, boots
                if (w.length() >= 3 && !NO_WORDS.contains(w)) out.add(w);
            }
            return out;
        }
    }
}
