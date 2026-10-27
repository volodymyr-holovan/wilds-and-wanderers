package ua.wilds.wiki.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import ua.wilds.wiki.WildsWiki;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.data.WikiData;
import ua.wilds.wiki.jei.Jei;
import ua.wilds.wiki.search.Searcher;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Started with -Dwilds_wiki.selftest=true: once in a world, searches, builds every page, shows a set of pages and saves
 * screenshots (screenshots/wiki_*.png), then writes logs/wilds_wiki_selftest.txt. Lets the wiki be checked unattended.
 */
public final class SelfTest {
    public static final boolean ON = Boolean.getBoolean("wilds_wiki.selftest");
    private static final String[] QUERIES = {"дракон", "дркон", "lhfrjy", "drakon", "dragon", "діамант", "daimond", "кайло", "руда",
            "меч", "жорна", "камера збагачення", "бурштин", "mekanism", "create", "слиз", "вогняна кров", "яйце дракона", "сезони",
            "home", "команди", "швидкість", "рівнини", "гострота", "zombie", "зомбі", "ящірка", "динозавр", "tyrannosaurus"};

    private static int ticks = -1, step;
    private static final List<Runnable[]> steps = new ArrayList<>();
    private static PrintWriter out;
    private static List<Entry> all;
    private static int built, buildErrors;
    private static long buildStart;
    private static WikiScreen probe;
    private static List<Entry> sweep;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        try {
            if (ticks == 300) begin(mc);
            else if (ticks > 300 && all != null && built < all.size()) buildSome(mc);
            else if (ticks > 300 && all != null && step < steps.size() * 40) shots(mc);
            else if (ticks > 300 && sweep != null && !sweep.isEmpty()) sweepOne(mc);
            else if (ticks > 300 && sweep != null) finish(mc);
        } catch (Throwable t) {
            WildsWiki.LOG.error("Wilds Wiki self test failed", t);
            if (out != null) {
                t.printStackTrace(out);
                out.flush();
            }
        }
    }

    private static void begin(Minecraft mc) throws Exception {
        out = new PrintWriter(Files.newBufferedWriter(FMLPaths.GAMEDIR.get().resolve("logs/wilds_wiki_selftest.txt"), StandardCharsets.UTF_8));
        long t = System.currentTimeMillis();
        WikiData.invalidate();
        WikiData d = WikiData.get();
        out.printf("index: %d entries, %d topics, %d changed recipes, %d loot items, built in %d ms, jei=%s%n", d.entries.size(), d.topics.size(),
                d.changedRecipes.size(), d.lootSources.size(), System.currentTimeMillis() - t, WildsWiki.jei && Jei.ready());
        int[] kinds = new int[Entry.Kind.values().length];
        for (Entry e : d.entries.values()) kinds[e.kind.ordinal()]++;
        for (Entry.Kind k : Entry.Kind.values()) out.printf("  %s: %d%n", k, kinds[k.ordinal()]);
        for (String q : QUERIES) {
            long s = System.nanoTime();
            List<Searcher.Hit<Entry>> hits = d.searcher.search(q, 5);
            long us = (System.nanoTime() - s) / 1000;
            StringBuilder b = new StringBuilder();
            for (Searcher.Hit<Entry> h : hits) b.append(h.value().name.getString()).append(" [").append(h.value().key).append(String.format("] %.2f; ", h.score()));
            out.printf("search '%s' %d us: %s%n", q, us, b);
        }
        out.flush();
        // every page was built without errors once (8765 pages, ~30 ms each): a sample keeps the run short
        all = new ArrayList<>(d.entries.values());
        Collections.shuffle(all, new Random(7));
        all = new ArrayList<>(all.subList(0, Math.min(1200, all.size())));
        probe = new WikiScreen(null, null, null);
        probe.init(mc, 854, 480);
        buildStart = System.currentTimeMillis();
        plan(d);
    }

    private static void buildSome(Minecraft mc) {
        WikiData d = WikiData.get();
        int end = Math.min(all.size(), built + 120);
        for (; built < end; built++) {
            Entry e = all.get(built);
            try {
                for (Elements.Element el : PageBuilder.build(d, e)) el.layout(probe, 300);
            } catch (Throwable t) {
                buildErrors++;
                if (buildErrors <= 30) {
                    out.println("page " + e.key + " failed: " + t);
                }
            }
        }
        if (built == all.size()) {
            out.printf("built %d pages in %d ms, %d failed%n", built, System.currentTimeMillis() - buildStart, buildErrors);
            out.flush();
        }
    }

    private static void plan(WikiData d) {
        shot("home", () -> WildsWiki.open(null, null, null));
        shot("search_drakon", () -> WildsWiki.open(null, null, "дракон"));
        shot("search_layout", () -> WildsWiki.open(null, null, "lhfrjy"));
        shot("search_ore", () -> WildsWiki.open(null, null, "руда"));
        page("fire_dragon", d, "entity:iceandfire:fire_dragon", 0);
        page("topic_dragons", d, "topic:wilds_wiki:dragons", 0);
        page("topic_dragons_2", d, "topic:wilds_wiki:dragons", 260);
        page("dragonbone", d, "item:iceandfire:dragonbone", 0);
        page("dragonbone_2", d, "item:iceandfire:dragonbone", 300);
        page("mana_orb", d, "item:irons_spellbooks:mana_upgrade_orb", 120);
        page("press", d, "item:create:mechanical_press", 0);
        page("press_2", d, "item:create:mechanical_press", 300);
        page("iron_ore", d, "item:minecraft:iron_ore", 0);
        page("iron_ore_2", d, "item:minecraft:iron_ore", 250);
        page("enrichment", d, "item:mekanism:enrichment_chamber", 0);
        page("speed", d, "effect:minecraft:speed", 0);
        page("plains", d, "biome:minecraft:plains", 0);
        page("sharpness", d, "enchantment:minecraft:sharpness", 0);
        page("heavy_bone", d, "item:alexscaves:heavy_bone", 0);
        page("water", d, "block:minecraft:water", 0);
        page("links", d, "topic:wilds_wiki:links", 0);
        page("links_2", d, "topic:wilds_wiki:links", 330);
        shot("jei_mana_orb", () -> {
            if (WildsWiki.jei && Jei.ready()) {
                Entry e = d.entries.get("item:irons_spellbooks:mana_upgrade_orb");
                if (e != null) Jei.show(e.stack, Jei.Role.RECIPES);
            }
        });
        shot("jei_bone_meal", () -> {
            if (WildsWiki.jei && Jei.ready()) Jei.show(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), Jei.Role.RECIPES);
        });
    }

    private static void page(String name, WikiData d, String key, int scroll) {
        shot(name, () -> {
            Entry e = d.entries.get(key);
            if (e == null) {
                out.println("missing entry " + key);
                return;
            }
            WikiScreen s = new WikiScreen(null, e, null);
            Minecraft.getInstance().setScreen(s);
            s.scrollPage(scroll);
        });
    }

    private static void shot(String name, Runnable setup) {
        steps.add(new Runnable[]{setup, () -> Screenshot.grab(FMLPaths.GAMEDIR.get().toFile(), "wiki_" + name + ".png",
                Minecraft.getInstance().getMainRenderTarget(), c -> {})});
    }

    private static void shots(Minecraft mc) {
        int i = step / 40, phase = step % 40;
        if (phase == 0) steps.get(i)[0].run();
        if (phase == 39) {
            steps.get(i)[1].run();
            out.println("screenshot " + i + " errors so far " + WikiScreen.errors);
            out.flush();
        }
        step++;
        if (step == steps.size() * 40) {
            List<Entry> list = new ArrayList<>(all);
            Collections.shuffle(list, new Random(42));
            sweep = new ArrayList<>(list.subList(0, Math.min(600, list.size())));
            for (Entry e : all) if (!e.articles.isEmpty()) sweep.add(e);
            out.println("render sweep over " + sweep.size() + " pages");
        }
    }

    private static int seenErrors;
    private static String shown;

    private static void sweepOne(Minecraft mc) {
        // errors of the page shown on the previous tick were counted while it was drawn
        if (WikiScreen.errors > seenErrors) out.println("render errors on " + shown + ": " + (WikiScreen.errors - seenErrors));
        seenErrors = WikiScreen.errors;
        Entry e = sweep.remove(sweep.size() - 1);
        shown = e.key;
        mc.setScreen(new WikiScreen(null, e, null));
    }

    private static void finish(Minecraft mc) {
        out.printf("done: render errors %d, page build errors %d%n", WikiScreen.errors, buildErrors);
        out.close();
        sweep = null;
        all = null;
        mc.setScreen(null);
        WildsWiki.LOG.info("Wilds Wiki self test finished");
    }

    static ResourceLocation id(String s) {
        return ResourceLocation.parse(s);
    }
}
