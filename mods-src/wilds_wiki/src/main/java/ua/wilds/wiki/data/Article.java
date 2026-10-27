package ua.wilds.wiki.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written article: assets/&lt;ns&gt;/wilds_wiki/&lt;lang&gt;/&lt;name&gt;.md
 *
 * <pre>
 * ---
 * for: iceandfire:fire_dragon, entity:iceandfire:ice_dragon   (pages it is shown on; "kind:" picks item/block/entity...)
 * topic: dragons        (or: its own page wilds_wiki:dragons, found by title and keywords)
 * title: Дракони
 * icon: iceandfire:dragonegg_red
 * keywords: вивірна, dragon, приручення
 * order: 10             (topics on the home page are sorted by it)
 * ---
 * # Heading, ## Subheading, paragraphs, - bullets, > tip, >! warning
 * **bold**, *italic*, [[minecraft:diamond]] or [[entity:iceandfire:fire_dragon|text]] links
 * !items a:b c:d   !image ns:textures/x.png 64 32 [caption]   !entity ns:mob [size]   !recipe ns:item   !uses ns:item
 * </pre>
 */
public final class Article {
    public final String source;
    public final String lang;
    public final List<String> targets = new ArrayList<>();
    public String topic, title, icon;
    public int order = 100;
    public final List<String> keywords = new ArrayList<>();
    public final List<String> body = new ArrayList<>();

    public Article(String source, String lang) {
        this.source = source;
        this.lang = lang;
    }

    public static Article parse(String source, String lang, String text) {
        Article a = new Article(source, lang);
        String[] lines = text.replace("\r", "").replace("\uFEFF", "").split("\n", -1);
        int i = 0;
        if (lines.length > 0 && lines[0].trim().equals("---")) {
            for (i = 1; i < lines.length && !lines[i].trim().equals("---"); i++) {
                String l = lines[i];
                int c = l.indexOf(':');
                if (c < 0) continue;
                String k = l.substring(0, c).trim().toLowerCase(), v = l.substring(c + 1).trim();
                switch (k) {
                    case "for" -> split(v, a.targets);
                    case "topic" -> a.topic = v;
                    case "title" -> a.title = v;
                    case "icon" -> a.icon = v;
                    case "keywords" -> split(v, a.keywords);
                    case "order" -> {
                        try {
                            a.order = Integer.parseInt(v);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    default -> {}
                }
            }
            i++;
        }
        for (; i < lines.length; i++) a.body.add(lines[i]);
        return a;
    }

    private static void split(String v, List<String> out) {
        for (String s : v.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) out.add(s);
        }
    }
}
