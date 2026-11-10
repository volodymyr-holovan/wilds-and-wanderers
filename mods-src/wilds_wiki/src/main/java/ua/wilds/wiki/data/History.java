package ua.wilds.wiki.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import ua.wilds.wiki.WildsWiki;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Recent searches, recently opened pages and bookmarks, kept in config/wilds_wiki_history.json of the game folder. */
public final class History {
    private static final int MAX = 20;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static History instance;

    public List<String> queries = new ArrayList<>();
    public List<String> viewed = new ArrayList<>();
    public List<String> bookmarks = new ArrayList<>();
    /** the page open when the wiki was closed ("" the home page), and how far it was scrolled */
    public String last = "";
    public double lastScroll;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("wilds_wiki_history.json");
    }

    public static History get() {
        if (instance == null) {
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
                instance = GSON.fromJson(r, History.class);
            } catch (Exception ignored) {
                // no history yet, or a broken file: start a new one
            }
            if (instance == null) instance = new History();
            if (instance.queries == null) instance.queries = new ArrayList<>();
            if (instance.viewed == null) instance.viewed = new ArrayList<>();
            if (instance.bookmarks == null) instance.bookmarks = new ArrayList<>();
        }
        return instance;
    }

    public void addQuery(String q) {
        q = q.trim();
        if (q.length() < 2) return;
        String fq = q;
        // typing "драк" then "дракон" leaves one entry, the longer one
        queries.removeIf(old -> old.equalsIgnoreCase(fq) || fq.toLowerCase().startsWith(old.toLowerCase()));
        queries.add(0, q);
        trim(queries);
        save();
    }

    public void addViewed(String key) {
        viewed.remove(key);
        viewed.add(0, key);
        trim(viewed);
        save();
    }

    /** adds the page to the bookmarks, or takes it away when it is there; true when it is bookmarked now */
    public boolean toggleBookmark(String key) {
        boolean add = !bookmarks.remove(key);
        if (add) bookmarks.add(0, key);
        save();
        return add;
    }

    public void setLast(String key, double scroll) {
        if (key.equals(last) && scroll == lastScroll) return;
        last = key;
        lastScroll = scroll;
        save();
    }

    public void clear() {
        queries.clear();
        viewed.clear();
        save();
    }

    private static void trim(List<String> l) {
        while (l.size() > MAX) l.remove(l.size() - 1);
    }

    private void save() {
        try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            WildsWiki.LOG.warn("Wilds Wiki: cannot save history", e);
        }
    }
}
