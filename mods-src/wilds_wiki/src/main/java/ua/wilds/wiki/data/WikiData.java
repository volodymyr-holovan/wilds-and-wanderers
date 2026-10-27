package ua.wilds.wiki.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import ua.wilds.wiki.WildsWiki;
import ua.wilds.wiki.search.Searcher;
import ua.wilds.wiki.search.Text;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/** Everything the wiki knows, built once per world and language: entries, articles, loot sources, the search index. */
public final class WikiData {
    private static WikiData instance;
    private static Object builtFor;

    public final Map<String, Entry> entries = new LinkedHashMap<>();
    public final List<Article> topics = new ArrayList<>();
    public final Map<Item, Entry> byItem = new HashMap<>();
    public final Map<Block, Entry> byBlock = new HashMap<>();
    public final Map<EntityType<?>, Entry> byEntity = new HashMap<>();
    public final Map<EntityType<?>, Item> spawnEggs = new HashMap<>();
    /** item id -> [kind, source id]; "entity|ns:mob" or "block|ns:block" -> item ids */
    public final Map<String, List<String[]>> lootSources = new HashMap<>();
    public final Map<String, List<String>> lootDrops = new HashMap<>();
    /** recipe id -> "added" or "modified:&lt;note&gt;" */
    public final Map<ResourceLocation, String> changedRecipes = new HashMap<>();
    /** "item:ns:id" / "entity:ns:id" -> pages of the mods' own guide books: {book, title, text: {uk|en: [paragraphs]}} */
    public final Map<String, List<JsonObject>> books = new HashMap<>();
    /**
     * generated/world.json, made from what the test server saw: "biomes" and "structures" (what lives, lies and drops there),
     * and the reverse lookups "mob_where", "block_where", "loot_where", "trades" (see tools/gen_world.py)
     */
    public JsonObject world = new JsonObject();
    /** item id -> [title key, description key] of the advancements it is the icon of */
    public final Map<String, List<String[]>> advancements = new HashMap<>();
    /** "ns:id" -> lang keys that describe it (lore lines, journal entries, documentation pages) */
    public final Map<String, List<String>> descKeys = new HashMap<>();
    public final Searcher<Entry> searcher = new Searcher<>();
    public ClientLanguage english;
    private final Map<String, String> modNames = new HashMap<>();

    public static void invalidate() {
        instance = null;
        builtFor = null;
    }

    /** The data for the current world; rebuilt after a language or resource pack change and in another world. */
    public static WikiData get() {
        Minecraft mc = Minecraft.getInstance();
        Object world = mc.getConnection() != null ? mc.getConnection().registryAccess() : null;
        if (instance == null || builtFor != world) {
            long t = System.currentTimeMillis();
            instance = new WikiData(mc.getResourceManager(), mc.getConnection() != null ? mc.getConnection().registryAccess() : null);
            builtFor = world;
            WildsWiki.LOG.info("Wilds Wiki: {} entries, {} topics in {} ms", instance.entries.size(), instance.topics.size(),
                    System.currentTimeMillis() - t);
        }
        return instance;
    }

    /** Recipe marks for JEI work without a world-bound index: the file is read on its own. */
    public static Map<ResourceLocation, String> readChangedRecipes(ResourceManager rm) {
        Map<ResourceLocation, String> out = new HashMap<>();
        JsonObject o = json(rm, "generated/changed_recipes.json");
        if (o != null) {
            for (var e : o.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                if (id != null) out.put(id, e.getValue().getAsString());
            }
        }
        return out;
    }

    private WikiData(ResourceManager rm, RegistryAccess access) {
        english = ClientLanguage.loadFrom(rm, List.of("en_us"), false);
        changedRecipes.putAll(readChangedRecipes(rm));
        readLoot(rm);
        readBooks(rm);
        JsonObject w = json(rm, "generated/world.json");
        if (w != null) world = w;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpawnEggItem egg) spawnEggs.putIfAbsent(egg.getType(new ItemStack(egg)), item);
        }
        addItems();
        addBlocks();
        addEntities();
        addEffects();
        if (access != null) {
            addEnchantments(access);
            addBiomes(access);
        }
        addStructures();
        addTagNames();
        readArticles(rm);
        for (Entry e : entries.values()) index(e);
    }

    // ---------------------------------------------------------------- entries

    private Entry put(Entry e, Component name, String enKey) {
        e.name = name;
        e.nameEn = english.getOrDefault(enKey, name.getString());
        e.modName = modName(e.id.getNamespace());
        entries.put(e.key, e);
        return e;
    }

    private void addItems() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            Entry e = new Entry(Entry.Kind.ITEM, id);
            e.stack = stack;
            e.icon = stack;
            Block b = Block.byItem(item);
            if (b != Blocks.AIR) {
                e.block = b;
                byBlock.putIfAbsent(b, e);
            }
            try {
                put(e, stack.getHoverName(), stack.getDescriptionId());
            } catch (Exception ex) {
                put(e, Component.literal(id.toString()), item.getDescriptionId());
            }
            byItem.put(item, e);
        }
    }

    private void addBlocks() {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (byBlock.containsKey(block) || block.defaultBlockState().isAir()) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            Entry e = new Entry(Entry.Kind.BLOCK, id);
            e.block = block;
            Item icon = block.asItem();
            if (icon == Items.AIR && block instanceof LiquidBlock) {
                Fluid f = block.defaultBlockState().getFluidState().getType();
                icon = f.getBucket();
            }
            e.icon = new ItemStack(icon == Items.AIR ? Items.BARRIER : icon);
            put(e, block.getName(), block.getDescriptionId());
            byBlock.put(block, e);
        }
    }

    private void addEntities() {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Item egg = spawnEggs.get(type);
            if (egg == null && type.getCategory() == MobCategory.MISC) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            Entry e = new Entry(Entry.Kind.ENTITY, id);
            e.entityType = type;
            e.icon = new ItemStack(egg != null ? egg : Items.EGG);
            put(e, type.getDescription(), type.getDescriptionId());
            byEntity.put(type, e);
        }
    }

    private void addEffects() {
        BuiltInRegistries.MOB_EFFECT.holders().forEach(h -> {
            MobEffect effect = h.value();
            Entry e = new Entry(Entry.Kind.EFFECT, h.key().location());
            e.effect = h;
            put(e, effect.getDisplayName(), effect.getDescriptionId());
        });
    }

    private void addEnchantments(RegistryAccess access) {
        access.registry(Registries.ENCHANTMENT).ifPresent(reg -> reg.holders().forEach(h -> {
            ResourceLocation id = h.key().location();
            Entry e = new Entry(Entry.Kind.ENCHANTMENT, id);
            e.icon = new ItemStack(Items.ENCHANTED_BOOK);
            put(e, h.value().description(), Util.makeDescriptionId("enchantment", id));
        }));
    }

    private static final String[][] BIOME_ICONS = {{"frozen", "ice"}, {"ice", "ice"}, {"snow", "snow_block"}, {"ocean", "kelp"},
            {"river", "water_bucket"}, {"beach", "sand"}, {"desert", "sand"}, {"badlands", "terracotta"}, {"mesa", "terracotta"},
            {"jungle", "jungle_sapling"}, {"swamp", "lily_pad"}, {"mangrove", "mangrove_propagule"}, {"cherry", "cherry_sapling"},
            {"birch", "birch_sapling"}, {"dark_forest", "dark_oak_sapling"}, {"taiga", "spruce_sapling"}, {"savanna", "acacia_sapling"},
            {"mushroom", "red_mushroom"}, {"cave", "pointed_dripstone"}, {"deep_dark", "sculk"}, {"peak", "stone"}, {"mountain", "stone"},
            {"hill", "stone"}, {"nether", "netherrack"}, {"crimson", "crimson_fungus"}, {"warped", "warped_fungus"},
            {"basalt", "basalt"}, {"soul", "soul_sand"}, {"end", "end_stone"}, {"forest", "oak_sapling"}, {"meadow", "poppy"},
            {"plains", "short_grass"}, {"grove", "spruce_sapling"}, {"void", "black_concrete"}};

    private void addBiomes(RegistryAccess access) {
        Registry<Biome> reg = access.registry(Registries.BIOME).orElse(null);
        if (reg == null) return;
        for (ResourceLocation id : reg.keySet()) {
            String key = "biome." + id.getNamespace() + "." + id.getPath();
            if (!I18n.exists(key) && !english.has(key)) continue; // technical biomes without a name
            Entry e = new Entry(Entry.Kind.BIOME, id);
            String icon = "grass_block";
            for (String[] m : BIOME_ICONS) {
                if (id.getPath().contains(m[0])) {
                    icon = m[1];
                    break;
                }
            }
            e.icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(icon)));
            put(e, Component.translatable(key), key);
        }
    }

    private void addStructures() {
        if (!world.has("structures")) return;
        for (var s : world.getAsJsonObject("structures").entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(s.getKey());
            if (id == null) continue;
            Entry e = new Entry(Entry.Kind.STRUCTURE, id);
            JsonObject o = s.getValue().getAsJsonObject();
            Item icon = o.has("icon") ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.get("icon").getAsString())) : Items.AIR;
            e.icon = new ItemStack(icon == Items.AIR ? Items.FILLED_MAP : icon);
            String key = "wilds_wiki.structure." + id.getNamespace() + "." + id.getPath().replace('/', '.');
            put(e, Component.translatable(key), key);
        }
    }

    /** world.json section as a map of lists, empty when missing */
    public List<String> where(String section, String key) {
        if (!world.has(section)) return List.of();
        JsonObject o = world.getAsJsonObject(section);
        if (!o.has(key)) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement el : o.getAsJsonArray(key)) out.add(el.isJsonArray() ? el.getAsJsonArray().get(0).getAsString() : el.getAsString());
        return out;
    }

    public JsonObject info(String section, String key) {
        if (!world.has(section)) return null;
        JsonObject o = world.getAsJsonObject(section);
        return o.has(key) ? o.getAsJsonObject(key) : null;
    }

    /** Translated item tag names ("Руди", "Мечі") make whole groups findable by what they are. */
    private void addTagNames() {
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            ResourceLocation t = pair.getFirst().location();
            String key = "tag.item." + t.getNamespace() + "." + t.getPath().replace('/', '.');
            if (!I18n.exists(key)) return;
            String name = I18n.get(key), en = english.getOrDefault(key, "");
            for (Holder<Item> h : pair.getSecond()) {
                Entry e = byItem.get(h.value());
                if (e == null) continue;
                e.tagNames.add(name);
                if (!en.isEmpty()) e.tagNamesEn.add(en);
            }
        });
    }

    // ---------------------------------------------------------------- articles, loot

    private void readArticles(ResourceManager rm) {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        // one article per file name: the current language wins, then Ukrainian (the pack's main language), then English
        Map<String, Article> chosen = new TreeMap<>();
        Map<String, Integer> rank = new HashMap<>();
        List<String> pref = List.of(lang, "uk_ua", "en_us");
        for (var res : rm.listResources("wilds_wiki", rl -> rl.getPath().endsWith(".md")).entrySet()) {
            String[] p = res.getKey().getPath().split("/");
            if (p.length < 3) continue;
            int r = pref.indexOf(p[1]);
            if (r < 0) continue;
            String name = res.getKey().getNamespace() + ":" + String.join("/", Arrays.copyOfRange(p, 2, p.length));
            if (rank.containsKey(name) && rank.get(name) <= r) continue;
            try (Reader reader = res.getValue().openAsReader()) {
                String text = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
                chosen.put(name, Article.parse(name, p[1], text));
                rank.put(name, r);
            } catch (Exception ex) {
                WildsWiki.LOG.warn("Wilds Wiki: cannot read article {}", res.getKey(), ex);
            }
        }
        for (Article a : chosen.values()) {
            if (a.topic != null) {
                ResourceLocation id = ResourceLocation.tryParse(a.topic.contains(":") ? a.topic : WildsWiki.ID + ":" + a.topic);
                if (id == null) continue;
                Entry e = new Entry(Entry.Kind.TOPIC, id);
                Item icon = a.icon != null ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(a.icon)) : Items.BOOK;
                e.icon = new ItemStack(icon == Items.AIR ? Items.BOOK : icon);
                e.name = Component.literal(a.title != null ? a.title : id.getPath());
                e.nameEn = "";
                e.modName = "Wilds & Wanderers";
                e.articles.add(a);
                e.keywords.addAll(a.keywords);
                entries.put(e.key, e);
                topics.add(a);
            }
            for (String target : a.targets) {
                if (target.contains("*")) { // "smallships:*_cog": every item of that pattern (ship woods, dye colours)
                    java.util.regex.Pattern pat = java.util.regex.Pattern.compile("item:" + target.replace(".", "\\.").replace("*", "[a-z0-9_/]*"));
                    for (Entry e : entries.values()) {
                        if (e.kind == Entry.Kind.ITEM && pat.matcher(e.key).matches() && !e.articles.contains(a)) {
                            e.articles.add(a);
                            e.keywords.addAll(a.keywords);
                        }
                    }
                    continue;
                }
                Entry e = resolve(target);
                if (e == null) {
                    WildsWiki.LOG.warn("Wilds Wiki: article {} is for unknown {}", a.source, target);
                    continue;
                }
                e.articles.add(a);
                e.keywords.addAll(a.keywords);
            }
        }
        topics.sort(Comparator.comparingInt((Article a) -> a.order).thenComparing(a -> a.title == null ? "" : a.title));
    }

    private void readLoot(ResourceManager rm) {
        JsonObject o = json(rm, "generated/loot.json");
        if (o == null) return;
        for (var e : o.getAsJsonObject("sources").entrySet()) {
            List<String[]> list = new ArrayList<>();
            for (JsonElement s : e.getValue().getAsJsonArray()) {
                JsonArray a = s.getAsJsonArray();
                list.add(new String[]{a.get(0).getAsString(), a.get(1).getAsString()});
            }
            lootSources.put(e.getKey(), list);
        }
        for (var e : o.getAsJsonObject("drops").entrySet()) {
            List<String> list = new ArrayList<>();
            for (JsonElement s : e.getValue().getAsJsonArray()) list.add(s.getAsString());
            lootDrops.put(e.getKey(), list);
        }
    }

    private void readBooks(ResourceManager rm) {
        JsonObject o = json(rm, "generated/books.json");
        if (o != null) {
            for (var e : o.entrySet()) {
                List<JsonObject> list = new ArrayList<>();
                for (JsonElement b : e.getValue().getAsJsonArray()) list.add(b.getAsJsonObject());
                books.put(e.getKey(), list);
            }
        }
        o = json(rm, "generated/advancements.json");
        if (o != null) {
            for (var e : o.entrySet()) {
                List<String[]> list = new ArrayList<>();
                for (JsonElement a : e.getValue().getAsJsonArray()) list.add(new String[]{a.getAsJsonArray().get(0).getAsString(), a.getAsJsonArray().get(1).getAsString()});
                advancements.put(e.getKey(), list);
            }
        }
        o = json(rm, "generated/desc_keys.json");
        if (o != null) {
            for (var e : o.entrySet()) {
                List<String> list = new ArrayList<>();
                for (JsonElement k : e.getValue().getAsJsonArray()) list.add(k.getAsString());
                descKeys.put(e.getKey(), list);
            }
        }
    }

    private static JsonObject json(ResourceManager rm, String path) {
        Optional<Resource> r = rm.getResource(ResourceLocation.fromNamespaceAndPath(WildsWiki.ID, path));
        if (r.isEmpty()) return null;
        try (Reader reader = r.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            WildsWiki.LOG.warn("Wilds Wiki: cannot read {}", path, ex);
            return null;
        }
    }

    // ---------------------------------------------------------------- lookups

    /** "ns:path" (item first, then mob, block, effect, enchantment, biome, topic) or "kind:ns:path". */
    public Entry resolve(String ref) {
        ref = ref.trim();
        int first = ref.indexOf(':'), second = ref.indexOf(':', first + 1);
        if (second > 0) {
            Entry e = entries.get(ref);
            if (e != null) return e;
        }
        ResourceLocation id = ResourceLocation.tryParse(ref);
        if (id == null) return null;
        for (Entry.Kind k : new Entry.Kind[]{Entry.Kind.ITEM, Entry.Kind.ENTITY, Entry.Kind.BLOCK, Entry.Kind.EFFECT, Entry.Kind.ENCHANTMENT,
                Entry.Kind.BIOME, Entry.Kind.STRUCTURE, Entry.Kind.TOPIC}) {
            Entry e = entries.get(Entry.key(k, id));
            if (e != null) return e;
        }
        return null;
    }

    /** topic pages in the order their articles ask for */
    public List<Entry> topicEntries() {
        List<Entry> out = new ArrayList<>();
        for (Article a : topics) {
            ResourceLocation id = ResourceLocation.tryParse(a.topic.contains(":") ? a.topic : WildsWiki.ID + ":" + a.topic);
            Entry e = id == null ? null : entries.get(Entry.key(Entry.Kind.TOPIC, id));
            if (e != null && !out.contains(e)) out.add(e);
        }
        return out;
    }

    /** Ponder scenes (Create and its addons) whose key is the item or starts with it: "mechanical_press", "mechanical_press_compacting" */
    public List<String> ponderScenes(ResourceLocation id) {
        if (ponder == null) {
            ponder = new HashMap<>();
            if (!(net.minecraft.locale.Language.getInstance() instanceof ClientLanguage lang)) return List.of();
            for (String k : lang.getLanguageData().keySet()) {
                int p = k.indexOf(".ponder.");
                if (p < 0 || !k.endsWith(".header")) continue;
                String ns = k.substring(0, p), scene = k.substring(p + 8, k.length() - 7);
                ponder.computeIfAbsent(ns, n -> new ArrayList<>()).add(scene);
            }
        }
        List<String> out = new ArrayList<>();
        for (String scene : ponder.getOrDefault(id.getNamespace(), List.of())) {
            if (scene.equals(id.getPath()) || scene.startsWith(id.getPath() + "_")) out.add(id.getNamespace() + ".ponder." + scene);
        }
        out.sort(Comparator.comparingInt(String::length).thenComparing(s -> s));
        return out;
    }

    private Map<String, List<String>> ponder;

    public Entry forBlock(Block b) {
        return byBlock.get(b);
    }

    public String modName(String ns) {
        return modNames.computeIfAbsent(ns, n -> ModList.get().getModContainerById(n)
                .map(c -> c.getModInfo().getDisplayName()).orElse(n.equals("minecraft") ? "Minecraft" : n));
    }

    // ---------------------------------------------------------------- search index

    private void index(Entry e) {
        String name = e.name.getString();
        float boost = switch (e.kind) {
            case TOPIC -> 1.15f;
            case ITEM, ENTITY -> 1f;
            case BLOCK, EFFECT, ENCHANTMENT -> 0.97f;
            case BIOME -> 0.95f;
            case STRUCTURE -> 0.96f;
        };
        if (!e.articles.isEmpty() && e.kind != Entry.Kind.TOPIC) boost += 0.03f;
        Searcher.Doc<Entry> d = new Searcher.Doc<>(e, name, e.nameEn, boost);
        d.field(Searcher.NAME, Text.words(name));
        d.field(Searcher.NAME_EN, Text.words(e.nameEn));
        d.field(Searcher.TRANSLIT, Text.words(Text.toLatin(Text.fold(name))));
        List<String> kw = new ArrayList<>();
        for (String k : e.keywords) kw.addAll(Text.words(k));
        d.field(Searcher.KEYWORDS, kw);
        d.field(Searcher.ID, Text.words(e.id.getPath()));
        d.field(Searcher.MOD, Text.words(e.modName + " " + e.id.getNamespace()));
        List<String> tags = new ArrayList<>();
        for (String t : e.tagNames) tags.addAll(Text.words(t));
        for (String t : e.tagNamesEn) tags.addAll(Text.words(t));
        d.field(Searcher.TAGS, tags);
        searcher.add(d);
    }
}
