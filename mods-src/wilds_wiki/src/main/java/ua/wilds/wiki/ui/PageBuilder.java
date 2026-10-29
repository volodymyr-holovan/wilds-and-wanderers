package ua.wilds.wiki.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import ua.wilds.wiki.WildsWiki;
import ua.wilds.wiki.data.Article;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.data.History;
import ua.wilds.wiki.data.WikiData;
import ua.wilds.wiki.jei.Jei;
import ua.wilds.wiki.jei.RecipeView;
import ua.wilds.wiki.ui.Elements.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/** Turns an entry into page elements: article first, then everything the game itself can tell about the thing. */
public final class PageBuilder {
    private static final DecimalFormat NUM = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("uk")));
    private final WikiData data;
    private final List<Element> out = new ArrayList<>();

    private PageBuilder(WikiData data) {
        this.data = data;
    }

    static MutableComponent t(String key, Object... args) {
        return Component.translatable("wilds_wiki." + key, args);
    }

    static String num(double v) {
        return NUM.format(v);
    }

    // ================================================================== entry pages

    public static List<Element> build(WikiData data, Entry e) {
        RichText.beginPage(data, e);
        PageBuilder b = new PageBuilder(data);
        b.out.add(new Header(e));
        b.out.add(new Spacer(4));
        for (Article a : e.articles) b.article(a);
        try {
            switch (e.kind) {
                case ITEM -> b.item(e);
                case BLOCK -> b.blockOnly(e);
                case ENTITY -> b.entity(e);
                case EFFECT -> b.effect(e);
                case ENCHANTMENT -> b.enchantment(e);
                case BIOME -> b.biome(e);
                case STRUCTURE -> b.structure(e);
                case TOPIC -> {}
            }
        } catch (Exception ex) {
            WildsWiki.LOG.warn("Wilds Wiki: page {} is incomplete", e.key, ex);
        }
        if (e.kind != Entry.Kind.TOPIC) {
            b.out.add(new Spacer(8));
            b.out.add(new Paragraph(t("footer", e.id.toString(), e.modName), Paragraph.Kind.MUTED));
        }
        return b.out;
    }

    private void heading(String key) {
        out.add(new Heading(t("section." + key), 1));
    }

    private void item(Entry e) {
        ItemStack stack = e.stack;
        description(stack.getDescriptionId(), e.id, stack);
        books("item:" + e.id);
        advancements(e);
        obtain(e);
        uses(e);
        Facts f = new Facts();
        itemFacts(stack, f);
        if (e.block != null) blockFacts(e.block, f);
        if (!f.isEmpty()) {
            heading("facts");
            out.add(f);
        }
        if (e.block != null) drops("block|" + BuiltInRegistries.BLOCK.getKey(e.block), e);
        tags(e);
    }

    private void blockOnly(Entry e) {
        description(e.block.getDescriptionId(), e.id, ItemStack.EMPTY);
        Facts f = new Facts();
        blockFacts(e.block, f);
        if (!f.isEmpty()) {
            heading("facts");
            out.add(f);
        }
        drops("block|" + e.id, null);
    }

    // ------------------------------------------------------------------ description from the mod itself

    /** Descriptions mods ship in their lang files under well-known keys, JEI information pages and the item tooltip. */
    private void description(String descId, ResourceLocation id, ItemStack stack) {
        List<Element> parts = new ArrayList<>();
        String ns = id.getNamespace(), path = id.getPath();
        // Create (and its addons): summary, then condition -> behaviour pairs
        if (I18n.exists(descId + ".tooltip.summary")) {
            parts.add(new Paragraph(RichText.createStyle(I18n.get(descId + ".tooltip.summary"), Theme.TEXT), Paragraph.Kind.NORMAL));
            for (int i = 1; i <= 6; i++) {
                String cond = descId + ".tooltip.condition" + i, beh = descId + ".tooltip.behaviour" + i;
                if (!I18n.exists(cond) || !I18n.exists(beh)) continue;
                MutableComponent c = Component.literal(I18n.get(cond) + ": ").withStyle(ChatFormatting.GRAY)
                        .append(RichText.createStyle(I18n.get(beh), Theme.TEXT));
                parts.add(new Paragraph(c, Paragraph.Kind.BULLET));
            }
            for (int i = 1; i <= 4; i++) {
                String ctrl = descId + ".tooltip.control" + i, act = descId + ".tooltip.action" + i;
                if (!I18n.exists(ctrl) || !I18n.exists(act)) continue;
                parts.add(new Paragraph(Component.literal(I18n.get(ctrl) + ": ").withStyle(ChatFormatting.GRAY)
                        .append(RichText.createStyle(I18n.get(act), Theme.TEXT)), Paragraph.Kind.BULLET));
            }
        }
        // Ponder scenes: Create explains its machines step by step, the same texts make a good description
        List<String> scenes = data.ponderScenes(id);
        for (int s = 0; s < Math.min(3, scenes.size()); s++) {
            String k = scenes.get(s);
            parts.add(new Heading(Component.literal(I18n.get(k + ".header")), 2));
            for (int i = 1; i <= 24 && I18n.exists(k + ".text_" + i); i++) {
                parts.add(new Paragraph(RichText.createStyle(I18n.get(k + ".text_" + i), Theme.TEXT), Paragraph.Kind.BULLET));
            }
        }
        if (!scenes.isEmpty()) parts.add(new Paragraph(t("ponder.hint"), Paragraph.Kind.MUTED));
        Set<String> seen = new HashSet<>();
        String[] keys = {descId + ".desc", descId + ".description", descId + ".tooltip", descId + ".lore", descId + ".info",
                descId + ".tooltip.0", descId + ".tooltip.1", descId + ".tooltip.2", descId + ".tooltip.3", descId + ".desc.0",
                descId + ".desc.1", descId + ".desc.2", descId + ".tooltip1", descId + ".tooltip2", descId + ".tooltip3",
                "tooltip." + ns + "." + path, "description." + ns + "." + path, "desc." + ns + "." + path, ns + "." + path + ".desc",
                "jei." + ns + "." + path, "jei." + ns + "." + path + ".desc", "info." + ns + "." + path};
        List<String> all = new ArrayList<>(Arrays.asList(keys));
        all.addAll(data.descKeys.getOrDefault(id.toString(), List.of()));
        for (String k : all) {
            if (!I18n.exists(k)) continue;
            String v = I18n.get(k);
            if (v.isBlank() || !seen.add(v) || v.contains("%s") || v.contains("%1$s")) continue;
            parts.add(new Paragraph(Component.literal(v), Paragraph.Kind.NORMAL));
        }
        if (!stack.isEmpty() && WildsWiki.jei && Jei.ready()) {
            for (FormattedText ft : Jei.info(stack)) {
                if (seen.add(ft.getString())) parts.add(new Paragraph(ft, Paragraph.Kind.NORMAL));
            }
        }
        if (!stack.isEmpty()) {
            List<Component> tip = tooltip(stack);
            if (!tip.isEmpty()) {
                parts.add(new Heading(t("section.tooltip"), 2));
                for (Component c : tip) if (seen.add(c.getString())) parts.add(new Paragraph(c, Paragraph.Kind.NORMAL));
            }
        }
        if (!parts.isEmpty()) {
            heading("description");
            out.addAll(parts);
        }
    }

    // ------------------------------------------------------------------ the mods' own guide books

    /** Pages of the Animal Dictionary, Cave Codex, Bestiary, Patchouli books and the AE2 guide about this thing, in full. */
    private void books(String key) {
        List<JsonObject> list = data.books.get(key);
        if (list == null || list.isEmpty()) return;
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        String want = lang.startsWith("uk") ? "uk" : "en";
        heading("books");
        for (JsonObject b : list) {
            JsonObject texts = b.getAsJsonObject("text");
            String got = texts.has(want) ? want : texts.has("en") ? "en" : texts.keySet().iterator().next();
            String book = local(b.get("book").getAsString());
            JsonElement te = b.get("title");
            String title = te.isJsonObject() ? (te.getAsJsonObject().has(got) ? te.getAsJsonObject().get(got).getAsString() : "") : local(te.getAsString());
            out.add(new Heading(Component.literal(title.isBlank() || title.equals(book) ? book : book + " — " + title).withStyle(ChatFormatting.GOLD), 2));
            if (!got.equals(want)) out.add(new Paragraph(t("books.other_language"), Paragraph.Kind.MUTED));
            for (JsonElement pe : texts.getAsJsonArray(got)) {
                String p = pe.getAsString();
                if (p.startsWith("## ")) out.add(new Heading(Component.literal(p.substring(3)), 3));
                else if (p.startsWith("• ")) out.add(new Paragraph(RichText.createStyle(p.substring(2), Theme.TEXT), Paragraph.Kind.BULLET));
                else out.add(new Paragraph(RichText.createStyle(p, Theme.TEXT), Paragraph.Kind.NORMAL));
            }
        }
    }

    /** advancements with this item as their icon: what the game itself hints the item is for */
    private void advancements(Entry e) {
        List<String[]> list = data.advancements.get(e.id.toString());
        if (list == null) return;
        List<Element> parts = new ArrayList<>();
        for (String[] a : list) {
            if (!I18n.exists(a[0]) || !I18n.exists(a[1])) continue;
            parts.add(new Paragraph(Component.literal(I18n.get(a[0])).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" — " + I18n.get(a[1])).withStyle(ChatFormatting.GRAY)), Paragraph.Kind.BULLET));
        }
        if (parts.isEmpty()) return;
        heading("advancements");
        out.addAll(parts);
    }

    private static String local(String s) {
        return I18n.exists(s) ? I18n.get(s) : s;
    }

    private List<Component> tooltip(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        List<Component> lines;
        try {
            lines = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
        } catch (Exception ex) {
            return List.of();
        }
        List<Component> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // the first line is the name
            Component c = lines.get(i);
            String s = c.getString().trim();
            if (s.isEmpty() || s.equals(data.modName(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()))) continue;
            out.add(c);
        }
        return out;
    }

    // ------------------------------------------------------------------ how to get it, what it is for

    private void obtain(Entry e) {
        List<Element> parts = new ArrayList<>();
        boolean jei = WildsWiki.jei && Jei.ready();
        int recipes = jei ? Jei.count(e.stack, Jei.Role.RECIPES) : 0;
        if (recipes > 0) {
            for (RecipeView v : Jei.recipes(e.stack, Jei.Role.RECIPES, 2)) parts.add(recipe(v));
            parts.add(new Buttons(List.of(new Button(t("button.recipes", recipes), () -> Jei.show(e.stack, Jei.Role.RECIPES)))));
        }
        List<String[]> sources = data.lootSources.getOrDefault(e.id.toString(), List.of());
        List<Entry> mobs = new ArrayList<>(), blocks = new ArrayList<>(), skeletons = new ArrayList<>();
        Map<String, List<Component>> places = new LinkedHashMap<>();
        for (String[] s : sources) {
            ResourceLocation id = ResourceLocation.tryParse(s[1]);
            if (id == null) continue;
            switch (s[0]) {
                case "entity" -> {
                    Entry m = data.byEntity.get(BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null));
                    if (m != null && !mobs.contains(m)) mobs.add(m);
                }
                case "skeleton" -> {
                    Entry m = data.byEntity.get(BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null));
                    if (m != null && !skeletons.contains(m)) skeletons.add(m);
                }
                case "block" -> {
                    Block b = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                    Entry be = b == null ? null : data.forBlock(b);
                    if (be != null && be != e && !blocks.contains(be)) blocks.add(be);
                }
                default -> places.computeIfAbsent(s[0], k -> new ArrayList<>()).add(lootName(id));
            }
        }
        if (!mobs.isEmpty()) {
            parts.add(new Heading(t("obtain.mobs"), 2));
            parts.add(new Chips(mobs, mobs.size() > 12));
        }
        if (!skeletons.isEmpty()) {
            parts.add(new Heading(t("obtain.skeleton"), 2));
            parts.add(new Paragraph(t("obtain.skeleton.how"), Paragraph.Kind.MUTED));
            parts.add(new Chips(skeletons, false));
        }
        if (!blocks.isEmpty()) {
            parts.add(new Heading(t("obtain.blocks"), 2));
            parts.add(new Chips(blocks, blocks.size() > 12));
        }
        for (var p : places.entrySet()) {
            parts.add(new Heading(t("obtain." + p.getKey()), 2));
            List<Component> names = p.getValue();
            int shown = Math.min(names.size(), 12);
            for (int i = 0; i < shown; i++) parts.add(new Paragraph(names.get(i), Paragraph.Kind.BULLET));
            if (names.size() > shown) parts.add(new Paragraph(t("obtain.more", names.size() - shown), Paragraph.Kind.MUTED));
        }
        itemWorld(e, parts);
        heading("obtain");
        if (parts.isEmpty()) {
            out.add(new Paragraph(t(jei ? "obtain.none" : "obtain.no_jei"), Paragraph.Kind.MUTED));
        } else {
            out.addAll(parts);
        }
    }

    private Element recipe(RecipeView v) {
        String change = Jei.changeOf(v.recipeId());
        MutableComponent cap = Component.literal(v.categoryTitle());
        if (change != null) {
            cap.append(Component.literal("  ✦ ").withStyle(s -> s.withColor(Theme.ACCENT & 0xFFFFFF)))
                    .append(t(change.equals("added") ? "recipe.added" : "recipe.modified").withStyle(s -> s.withColor(Theme.ACCENT & 0xFFFFFF)));
        }
        return new Recipe(v, cap);
    }

    private Component lootName(ResourceLocation table) {
        String key = "wilds_wiki.loot." + table.getNamespace() + "." + table.getPath().replace('/', '.');
        if (I18n.exists(key)) return Component.translatable(key);
        String path = table.getPath();
        int slash = path.lastIndexOf('/');
        String last = (slash >= 0 ? path.substring(slash + 1) : path).replace('_', ' ');
        return t("loot.fallback", data.modName(table.getNamespace()), last);
    }

    private void uses(Entry e) {
        if (!WildsWiki.jei || !Jei.ready()) return;
        int uses = Jei.count(e.stack, Jei.Role.USES), station = Jei.count(e.stack, Jei.Role.STATION);
        if (uses == 0 && station == 0) return;
        heading("uses");
        if (station > 0) {
            out.add(new Paragraph(t("uses.station"), Paragraph.Kind.TIP));
            for (RecipeView v : Jei.recipes(e.stack, Jei.Role.STATION, 1)) out.add(recipe(v));
        }
        if (uses > 0) for (RecipeView v : Jei.recipes(e.stack, Jei.Role.USES, 1)) out.add(recipe(v));
        List<Button> buttons = new ArrayList<>();
        if (uses > 0) buttons.add(new Button(t("button.uses", uses), () -> Jei.show(e.stack, Jei.Role.USES)));
        if (station > 0) buttons.add(new Button(t("button.station", station), () -> Jei.show(e.stack, Jei.Role.STATION)));
        out.add(new Buttons(buttons));
    }

    private void drops(String key, Entry self) {
        List<String> drops = data.lootDrops.get(key);
        if (drops == null) return;
        List<Entry> list = new ArrayList<>();
        for (String d : drops) {
            Entry de = data.resolve("item:" + d);
            if (de != null && de != self && !list.contains(de)) list.add(de);
        }
        if (list.isEmpty()) return;
        heading("drops");
        out.add(new Chips(list, list.size() > 12));
    }

    private void tags(Entry e) {
        if (e.tagNames.isEmpty()) return;
        Set<String> uniq = new LinkedHashSet<>(e.tagNames);
        heading("tags");
        out.add(new Paragraph(Component.literal(String.join(" · ", uniq)), Paragraph.Kind.MUTED));
    }

    // ------------------------------------------------------------------ facts

    private void itemFacts(ItemStack s, Facts f) {
        f.add(t("fact.stack"), Component.literal(String.valueOf(s.getMaxStackSize())));
        if (s.isDamageableItem()) f.add(t("fact.durability"), Component.literal(String.valueOf(s.getMaxDamage())));
        FoodProperties food = s.get(DataComponents.FOOD);
        if (food != null) {
            f.add(t("fact.food"), t("fact.food.value", food.nutrition(), num(food.nutrition() / 2.0), num(food.saturation())));
        }
        f.add(t("fact.rarity"), t("rarity." + s.getRarity().name().toLowerCase()));
        if (s.has(DataComponents.FIRE_RESISTANT)) f.add(t("fact.fire_resistant"), t("yes"));
        if (s.isEnchantable()) f.add(t("fact.enchantable"), t("yes"));
        int burn = 0;
        try {
            burn = s.getBurnTime(RecipeType.SMELTING);
        } catch (Exception ignored) {
        }
        if (burn > 0) f.add(t("fact.fuel"), t("fact.fuel.value", num(burn / 200.0)));
        float compost = ComposterBlock.COMPOSTABLES.getFloat(s.getItem());
        if (compost > 0) f.add(t("fact.compost"), Component.literal(num(compost * 100) + "%"));
    }

    private void blockFacts(Block b, Facts f) {
        BlockState st = b.defaultBlockState();
        float hard;
        try {
            hard = st.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (Exception ex) {
            hard = b.defaultDestroyTime();
        }
        f.add(t("fact.hardness"), hard < 0 ? t("fact.unbreakable") : Component.literal(num(hard)));
        f.add(t("fact.blast"), Component.literal(num(b.getExplosionResistance())));
        List<Component> tools = new ArrayList<>();
        if (st.is(BlockTags.MINEABLE_WITH_PICKAXE)) tools.add(t("tool.pickaxe"));
        if (st.is(BlockTags.MINEABLE_WITH_AXE)) tools.add(t("tool.axe"));
        if (st.is(BlockTags.MINEABLE_WITH_SHOVEL)) tools.add(t("tool.shovel"));
        if (st.is(BlockTags.MINEABLE_WITH_HOE)) tools.add(t("tool.hoe"));
        if (!tools.isEmpty()) {
            MutableComponent c = Component.empty();
            for (int i = 0; i < tools.size(); i++) c.append(i == 0 ? Component.empty() : Component.literal(", ")).append(tools.get(i));
            String tier = st.is(Tags.Blocks.NEEDS_NETHERITE_TOOL) ? "netherite" : st.is(BlockTags.NEEDS_DIAMOND_TOOL) ? "diamond"
                    : st.is(BlockTags.NEEDS_IRON_TOOL) ? "iron" : st.is(BlockTags.NEEDS_STONE_TOOL) ? "stone" : null;
            if (tier != null) c.append(Component.literal(" — ")).append(t("tier." + tier));
            f.add(t("fact.tool"), c);
        }
        if (st.requiresCorrectToolForDrops()) f.add(t("fact.needs_tool"), t("fact.needs_tool.value"));
        if (st.getLightEmission() > 0) f.add(t("fact.light"), Component.literal(String.valueOf(st.getLightEmission())));
        if (b instanceof FallingBlock) f.add(t("fact.gravity"), t("yes"));
        if (b instanceof EntityBlock) f.add(t("fact.block_entity"), t("fact.block_entity.value"));
    }

    // ------------------------------------------------------------------ mobs

    @SuppressWarnings("unchecked")
    private void entity(Entry e) {
        EntityType<?> type = e.entityType;
        description(type.getDescriptionId(), e.id, ItemStack.EMPTY);
        books("entity:" + e.id);
        mobWorld(e);
        drops("entity|" + e.id, null);
        Facts f = new Facts();
        f.add(t("fact.category"), t("category." + type.getCategory().getName()));
        if (DefaultAttributes.hasSupplier(type)) {
            AttributeSupplier a = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
            attr(f, a, Attributes.MAX_HEALTH, "health", v -> t("fact.health.value", num(v), num(v / 2)));
            attr(f, a, Attributes.ARMOR, "armor", v -> Component.literal(num(v)));
            attr(f, a, Attributes.ATTACK_DAMAGE, "attack", v -> t("fact.health.value", num(v), num(v / 2)));
            attr(f, a, Attributes.MOVEMENT_SPEED, "speed", v -> Component.literal(num(v)));
            attr(f, a, Attributes.FLYING_SPEED, "flying_speed", v -> Component.literal(num(v)));
            attr(f, a, Attributes.KNOCKBACK_RESISTANCE, "knockback", v -> Component.literal(num(v * 100) + "%"));
        }
        f.add(t("fact.size"), t("fact.size.value", num(type.getWidth()), num(type.getHeight())));
        if (type.fireImmune()) f.add(t("fact.fire_immune"), t("yes"));
        heading("facts");
        out.add(f);
        Item egg = data.spawnEggs.get(type);
        if (egg != null) {
            Entry ee = data.byItem.get(egg);
            if (ee != null) {
                out.add(new Heading(t("section.egg"), 2));
                out.add(new Chips(List.of(ee), false));
            }
        }
    }

    private static void attr(Facts f, AttributeSupplier a, Holder<Attribute> h, String key, java.util.function.DoubleFunction<Component> fmt) {
        if (!a.hasAttribute(h)) return;
        double v = a.getBaseValue(h);
        if (v > 0) f.add(t("fact." + key), fmt.apply(v));
    }

    // ------------------------------------------------------------------ effects, enchantments, biomes

    private void effect(Entry e) {
        MobEffect m = e.effect.value();
        description(m.getDescriptionId(), e.id, ItemStack.EMPTY);
        Facts f = new Facts();
        f.add(t("fact.effect_kind"), t("effect." + m.getCategory().name().toLowerCase()));
        f.add(t("fact.color"), Component.literal("■■■").withStyle(s -> s.withColor(m.getColor())));
        if (m.isInstantenous()) f.add(t("fact.instant"), t("yes"));
        heading("facts");
        out.add(f);
    }

    private void enchantment(Entry e) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Optional<Holder.Reference<Enchantment>> h = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(e.id);
        description("enchantment." + e.id.getNamespace() + "." + e.id.getPath(), e.id, ItemStack.EMPTY);
        if (h.isEmpty()) return;
        Enchantment.EnchantmentDefinition d = h.get().value().definition();
        Facts f = new Facts();
        f.add(t("fact.max_level"), Component.literal(String.valueOf(d.maxLevel())));
        f.add(t("fact.weight"), Component.literal(String.valueOf(d.weight())));
        f.add(t("fact.anvil_cost"), Component.literal(String.valueOf(d.anvilCost())));
        heading("facts");
        out.add(f);
        List<Entry> items = new ArrayList<>();
        for (Holder<Item> i : d.supportedItems()) {
            Entry ie = data.byItem.get(i.value());
            if (ie != null) items.add(ie);
            if (items.size() >= 80) break;
        }
        if (!items.isEmpty()) {
            heading("enchant_items");
            out.add(new Chips(items, true));
        }
    }

    private void biome(Entry e) {
        var level = Minecraft.getInstance().level;
        description("biome." + e.id.getNamespace() + "." + e.id.getPath(), e.id, ItemStack.EMPTY);
        if (level == null) return;
        Optional<Biome> b = level.registryAccess().registryOrThrow(Registries.BIOME).getOptional(e.id);
        if (b.isEmpty()) return;
        photo("biome", e.id);
        biomeWorld(e);
        Facts f = new Facts();
        f.add(t("fact.temperature"), Component.literal(num(b.get().getBaseTemperature())));
        Biome.Precipitation p = b.get().hasPrecipitation() ? b.get().getPrecipitationAt(new BlockPos(0, 64, 0)) : Biome.Precipitation.NONE;
        f.add(t("fact.precipitation"), t("precipitation." + p.name().toLowerCase()));
        heading("facts");
        out.add(f);
    }

    // ------------------------------------------------------------------ the world: biomes, structures, where things are

    private List<Entry> resolveAll(String kind, List<String> ids) {
        List<Entry> list = new ArrayList<>();
        for (String id : ids) {
            Entry x = data.resolve(kind == null ? id : kind + ":" + id);
            if (x != null && !list.contains(x)) list.add(x);
        }
        return list;
    }

    private void chipSection(String key, String kind, List<String> ids) {
        List<Entry> list = resolveAll(kind, ids);
        if (list.isEmpty()) return;
        heading(key);
        out.add(new Chips(list, list.size() > 16));
    }

    private void photo(String kind, ResourceLocation id) {
        ResourceLocation tex = Photos.get(kind, id);
        if (tex != null) out.add(new Image(tex, Photos.W, Photos.H, Photos.W, Photos.H, null));
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o != null && o.has(key)) o.getAsJsonArray(key).forEach(x -> out.add(x.isJsonArray() ? x.getAsJsonArray().get(0).getAsString() : x.getAsString()));
        return out;
    }

    private static final String[] MOB_GROUPS = {"monster", "creature", "ambient", "water_creature", "water_ambient", "underground_water_creature", "axolotls"};

    /** what the dump saw in the biome: structures, mobs by kind, the blocks it is made of */
    private void biomeWorld(Entry e) {
        JsonObject o = data.info("biomes", e.id.toString());
        if (o == null) return;
        List<Entry> structs = resolveAll("structure", strings(o, "structures"));
        heading("structures");
        if (structs.isEmpty()) out.add(new Paragraph(t("biome.no_structures"), Paragraph.Kind.MUTED));
        else out.add(new Chips(structs, false));
        if (o.has("mobs")) {
            JsonObject mobs = o.getAsJsonObject("mobs");
            boolean any = false;
            for (String g : MOB_GROUPS) {
                List<Entry> list = resolveAll("entity", strings(mobs, g));
                if (list.isEmpty()) continue;
                if (!any) heading("mobs");
                any = true;
                out.add(new Heading(t("mobs." + g), 2));
                out.add(new Chips(list, false));
            }
        }
        chipSection("biome_blocks", null, strings(o, "blocks"));
    }

    private void structure(Entry e) {
        JsonObject o = data.info("structures", e.id.toString());
        photo("structure", e.id);
        Facts f = new Facts();
        if (o != null) {
            if (o.has("size")) {
                var sz = o.getAsJsonArray("size");
                f.add(t("fact.structure_size"), t("fact.structure_size.value", sz.get(0).getAsInt(), sz.get(2).getAsInt(), sz.get(1).getAsInt()));
            }
            List<String> d = new ArrayList<>();
            for (String dim : strings(o, "dims")) d.add(dimName(dim).getString());
            if (!d.isEmpty()) f.add(t("fact.dimension"), Component.literal(String.join(", ", d)));
            if (o.has("spacing")) f.add(t("fact.rarity"), t("fact.rarity.value", o.get("spacing").getAsInt() * 16));
        }
        f.add(t("fact.mod"), Component.literal(e.modName));
        heading("facts");
        out.add(f);
        if (o == null) return;
        chipSection("structure.bosses", "entity", strings(o, "bosses"));
        chipSection("structure.mobs", "entity", strings(o, "mobs"));
        List<String> loot = strings(o, "loot");
        List<Entry> items = resolveAll(null, strings(o, "items"));
        if (!loot.isEmpty() || !items.isEmpty()) {
            heading("structure.loot");
            for (String l : loot) {
                ResourceLocation id = ResourceLocation.tryParse(l);
                if (id != null) out.add(new Paragraph(lootName(id), Paragraph.Kind.BULLET));
            }
            if (!items.isEmpty()) out.add(new Chips(items, true));
        }
        List<Entry> blocks = resolveAll(null, strings(o, "blocks"));
        if (!blocks.isEmpty()) {
            heading("structure.blocks");
            out.add(new Chips(blocks, true));
        }
        List<Entry> biomes = resolveAll("biome", strings(o, "biomes"));
        if (!biomes.isEmpty()) {
            heading("structure.biomes");
            out.add(new Chips(biomes, biomes.size() > 24));
        }
    }

    private static Component dimName(String id) {
        String key = "wilds_wiki.dimension." + id.replace(':', '.');
        return Component.literal(I18n.exists(key) ? I18n.get(key) : id);
    }

    /** on a mob page: the biomes it spawns in and the structures it guards */
    private void mobWorld(Entry e) {
        List<Entry> biomes = resolveAll("biome", data.where("mob_biomes", e.id.toString()));
        List<Entry> structs = resolveAll("structure", data.where("mob_structures", e.id.toString()));
        if (biomes.isEmpty() && structs.isEmpty()) return;
        heading("where");
        if (!biomes.isEmpty()) {
            out.add(new Heading(t("where.biomes"), 2));
            out.add(new Chips(biomes, biomes.size() > 24));
        }
        if (!structs.isEmpty()) {
            out.add(new Heading(t("where.structures"), 2));
            out.add(new Chips(structs, false));
        }
    }

    /** on an item page: structures whose chests hold it, villagers selling it, biomes and structures made of it */
    private void itemWorld(Entry e, List<Element> parts) {
        String id = e.id.toString();
        List<Entry> structs = resolveAll("structure", data.where("item_structures", id));
        if (!structs.isEmpty()) {
            parts.add(new Heading(t("obtain.structures"), 2));
            parts.add(new Chips(structs, structs.size() > 16));
        }
        if (data.world.has("trades") && data.world.getAsJsonObject("trades").has(id)) {
            parts.add(new Heading(t("obtain.trades"), 2));
            int n = 0;
            for (var tr : data.world.getAsJsonObject("trades").getAsJsonArray(id)) {
                if (n++ >= 8) break;
                var a = tr.getAsJsonArray();
                String who = a.get(0).getAsString();
                String prof;
                if (who.equals("wandering_trader")) prof = I18n.get("entity.minecraft.wandering_trader");
                else {
                    String k = "entity.minecraft.villager." + who.substring(who.indexOf(':') + 1);
                    prof = I18n.exists(k) ? I18n.get(k) : I18n.get("entity." + who.replace(':', '.'));
                }
                MutableComponent c = Component.literal(prof).withStyle(ChatFormatting.GOLD);
                if (!who.equals("wandering_trader"))
                    c.append(Component.literal(" (" + t("trade.level." + a.get(1).getAsInt()).getString() + ")").withStyle(ChatFormatting.GRAY));
                c.append(Component.literal(": " + a.get(3).getAsInt() + " × " + itemName(a.get(2).getAsString())));
                String b = a.get(4).getAsString();
                if (!b.isEmpty() && !b.equals("minecraft:air")) c.append(Component.literal(" + " + a.get(5).getAsInt() + " × " + itemName(b)));
                c.append(Component.literal(" → " + a.get(6).getAsInt()));
                parts.add(new Paragraph(c, Paragraph.Kind.BULLET));
            }
        }
        List<Entry> inBiomes = resolveAll("biome", data.where("block_biomes", id));
        if (!inBiomes.isEmpty()) {
            parts.add(new Heading(t("obtain.natural"), 2));
            parts.add(new Chips(inBiomes, inBiomes.size() > 24));
        }
        List<Entry> inStructs = resolveAll("structure", data.where("block_structures", id));
        if (!inStructs.isEmpty()) {
            parts.add(new Heading(t("obtain.built"), 2));
            parts.add(new Chips(inStructs, inStructs.size() > 16));
        }
    }

    private static String itemName(String id) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return new ItemStack(it).getHoverName().getString();
    }

    // ================================================================== articles

    private void article(Article a) {
        StringBuilder para = new StringBuilder();
        Runnable flush = () -> {
            if (!para.isEmpty()) {
                out.add(new Paragraph(RichText.parse(para.toString(), data, Theme.TEXT), Paragraph.Kind.NORMAL));
                para.setLength(0);
            }
        };
        for (String raw : a.body) {
            String l = raw.trim();
            if (l.isEmpty()) {
                flush.run();
                continue;
            }
            if (l.startsWith("## ")) {
                flush.run();
                out.add(new Heading(RichText.parse(l.substring(3), data, Theme.TEXT), 2));
            } else if (l.startsWith("# ")) {
                flush.run();
                out.add(new Heading(RichText.parse(l.substring(2), data, Theme.ACCENT), 1));
            } else if (l.startsWith("- ")) {
                flush.run();
                out.add(new Paragraph(RichText.parse(l.substring(2), data, Theme.TEXT), Paragraph.Kind.BULLET));
            } else if (l.startsWith(">! ")) {
                flush.run();
                out.add(new Paragraph(RichText.parse(l.substring(3), data, Theme.TEXT), Paragraph.Kind.WARN));
            } else if (l.startsWith("> ")) {
                flush.run();
                out.add(new Paragraph(RichText.parse(l.substring(2), data, Theme.TEXT), Paragraph.Kind.TIP));
            } else if (l.startsWith("!")) {
                flush.run();
                directive(a, l);
            } else {
                if (!para.isEmpty()) para.append(' ');
                para.append(l);
            }
        }
        flush.run();
    }

    private void directive(Article a, String l) {
        String[] p = l.substring(1).split("\\s+");
        try {
            switch (p[0]) {
                case "items", "icons" -> {
                    List<Entry> list = new ArrayList<>();
                    for (int i = 1; i < p.length; i++) {
                        Entry e = data.resolve(p[i]);
                        if (e != null) list.add(e);
                        else WildsWiki.LOG.warn("Wilds Wiki: {} links to unknown {}", a.source, p[i]);
                    }
                    out.add(new Chips(list, p[0].equals("icons")));
                }
                case "image" -> {
                    int sw = Integer.parseInt(p[2]), sh = Integer.parseInt(p[3]);
                    float scale = p.length > 4 ? Float.parseFloat(p[4]) : 1f;
                    String cap = p.length > 5 ? String.join(" ", Arrays.copyOfRange(p, 5, p.length)) : null;
                    out.add(new Image(ResourceLocation.parse(p[1]), sw, sh, (int) (sw * scale), (int) (sh * scale),
                            cap == null ? null : Component.literal(cap)));
                }
                case "entity" -> {
                    Entry e = data.resolve(p[1].contains("entity:") ? p[1] : "entity:" + p[1]);
                    if (e != null && e.entityType != null) out.add(new Model(e.entityType, p.length > 2 ? Integer.parseInt(p[2]) : 90));
                }
                case "recipe", "uses" -> {
                    Entry e = data.resolve(p[1]);
                    if (e != null && !e.stack.isEmpty() && WildsWiki.jei && Jei.ready()) {
                        Jei.Role r = p[0].equals("recipe") ? Jei.Role.RECIPES : Jei.Role.USES;
                        for (RecipeView v : Jei.recipes(e.stack, r, 1)) out.add(recipe(v));
                    }
                }
                default -> WildsWiki.LOG.warn("Wilds Wiki: {}: unknown directive {}", a.source, p[0]);
            }
        } catch (RuntimeException ex) {
            WildsWiki.LOG.warn("Wilds Wiki: {}: bad line {}", a.source, l);
        }
    }

    // ================================================================== home page

    public static List<Element> home(WikiData data, Runnable clearHistory) {
        RichText.beginPage(data, null);
        PageBuilder b = new PageBuilder(data);
        Entry home = data.entries.get(Entry.key(Entry.Kind.TOPIC, ResourceLocation.fromNamespaceAndPath(WildsWiki.ID, "home")));
        if (home != null) {
            b.out.add(new Header(home));
            b.out.add(new Spacer(4));
            for (Article a : home.articles) b.article(a);
        } else {
            b.out.add(new Heading(t("title"), 1));
        }
        b.out.add(new Paragraph(t("home.stats", data.entries.size()), Paragraph.Kind.MUTED));
        List<Entry> topics = new ArrayList<>();
        for (Article a : data.topics) {
            if (a.topic == null || a.topic.equals("home")) continue;
            ResourceLocation id = ResourceLocation.tryParse(a.topic.contains(":") ? a.topic : WildsWiki.ID + ":" + a.topic);
            Entry e = id == null ? null : data.entries.get(Entry.key(Entry.Kind.TOPIC, id));
            if (e != null) topics.add(e);
        }
        if (!topics.isEmpty()) {
            b.heading("topics");
            b.out.add(new Chips(topics, false));
        }
        List<Entry> viewed = new ArrayList<>();
        for (String k : History.get().viewed) {
            Entry e = data.entries.get(k);
            if (e != null) viewed.add(e);
        }
        if (!viewed.isEmpty()) {
            b.heading("recent");
            b.out.add(new Chips(viewed, false));
            b.out.add(new Buttons(List.of(new Button(t("button.clear_history"), clearHistory))));
        }
        return b.out;
    }
}
