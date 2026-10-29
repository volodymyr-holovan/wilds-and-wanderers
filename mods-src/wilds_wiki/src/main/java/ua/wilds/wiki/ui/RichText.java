package ua.wilds.wiki.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.data.WikiData;

/**
 * Inline markup of articles: **bold**, *italic*, `highlight`, [[ref]] and [[ref|text]] links to other pages.
 * A link keeps its target in the style's insertion ("wiki:&lt;entry key&gt;"); the page finds it under the mouse.
 */
public final class RichText {
    public static final String LINK = "wiki:";
    /**
     * Names of other pages found in plain text become links by themselves, the first time on a page (later ones stay
     * plain, a page full of links reads badly). {@link #beginPage} starts a page: its own name is never a link.
     */
    private static WikiData autoData;
    private static final java.util.Set<String> autoSeen = new java.util.HashSet<>();

    private RichText() {}

    public static void beginPage(WikiData data, Entry current) {
        autoData = data;
        autoSeen.clear();
        if (current != null) autoSeen.add(current.key);
    }

    /** plain text with the names of other pages turned into links, in the given style */
    static void autolink(MutableComponent out, String text, Style st) {
        WikiData data = autoData;
        if (data == null || text.length() < 5) {
            out.append(Component.literal(text).withStyle(st));
            return;
        }
        // word boundaries: letters (any alphabet), digits, apostrophes and hyphens inside a word
        java.util.List<int[]> words = new java.util.ArrayList<>();
        int i = 0, n = text.length();
        while (i < n) {
            while (i < n && !isWord(text.charAt(i))) i++;
            int st0 = i;
            while (i < n && (isWord(text.charAt(i)) || (joins(text.charAt(i)) && i + 1 < n && isWord(text.charAt(i + 1))))) i++;
            if (i > st0) words.add(new int[]{st0, i});
        }
        int done = 0, max = data.longestName();
        for (int w = 0; w < words.size(); w++) {
            for (int len = Math.min(max, words.size() - w); len >= 1; len--) {
                int a = words.get(w)[0], b = words.get(w + len - 1)[1];
                String cand = text.substring(a, b).toLowerCase(java.util.Locale.ROOT).replaceAll("\s+", " ");
                Entry e = data.byName(cand);
                if (e == null || autoSeen.contains(e.key)) continue;
                autoSeen.add(e.key);
                if (a > done) out.append(Component.literal(text.substring(done, a)).withStyle(st));
                out.append(Component.literal(text.substring(a, b)).withStyle(st.withColor(TextColor.fromRgb(Theme.LINK & 0xFFFFFF))
                        .withUnderlined(true).withInsertion(LINK + e.key)));
                done = b;
                w += len - 1;
                break;
            }
        }
        if (done < n) out.append(Component.literal(text.substring(done)).withStyle(st));
    }

    private static boolean isWord(char c) {
        return Character.isLetterOrDigit(c);
    }

    /** an apostrophe or a hyphen inside a word («м’ясо», «Ар-Нуво») keeps it one word */
    private static boolean joins(char c) {
        return c == '\'' || c == '’' || c == '-';
    }

    public static MutableComponent parse(String s, WikiData data, int baseColor) {
        MutableComponent out = Component.empty().withStyle(Style.EMPTY.withColor(baseColor));
        StringBuilder buf = new StringBuilder();
        boolean bold = false, italic = false, mark = false;
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("[[", i)) {
                int end = s.indexOf("]]", i + 2);
                if (end > 0) {
                    flush(out, buf, bold, italic, mark);
                    String inner = s.substring(i + 2, end);
                    int bar = inner.indexOf('|');
                    String ref = bar >= 0 ? inner.substring(0, bar) : inner;
                    Entry e = data.resolve(ref);
                    String text = bar >= 0 ? inner.substring(bar + 1) : e != null ? e.name.getString() : ref;
                    out.append(link(text, e, bold));
                    i = end + 2;
                    continue;
                }
            }
            if (s.startsWith("**", i)) {
                flush(out, buf, bold, italic, mark);
                bold = !bold;
                i += 2;
                continue;
            }
            char c = s.charAt(i);
            if (c == '*' || c == '`') {
                flush(out, buf, bold, italic, mark);
                if (c == '*') italic = !italic;
                else mark = !mark;
                i++;
                continue;
            }
            buf.append(c);
            i++;
        }
        flush(out, buf, bold, italic, mark);
        return out;
    }

    public static Component link(String text, Entry e, boolean bold) {
        if (e == null) return Component.literal(text).withStyle(s -> s.withColor(Theme.MUTED).withBold(bold));
        return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(Theme.LINK & 0xFFFFFF)).withUnderlined(true)
                .withBold(bold).withInsertion(LINK + e.key));
    }

    private static void flush(MutableComponent out, StringBuilder buf, boolean bold, boolean italic, boolean mark) {
        if (buf.isEmpty()) return;
        Style st = Style.EMPTY.withBold(bold).withItalic(italic);
        if (mark) st = st.withColor(Theme.ACCENT & 0xFFFFFF);
        autolink(out, buf.toString(), st);
        buf.setLength(0);
    }

    /** Create writes highlights of its item descriptions as _word_: show them in the accent colour. */
    public static MutableComponent createStyle(String s, int baseColor) {
        MutableComponent out = Component.empty().withStyle(Style.EMPTY.withColor(baseColor));
        String[] parts = s.split("_", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            Style st = i % 2 == 1 && parts.length > 2 ? Style.EMPTY.withColor(Theme.ACCENT & 0xFFFFFF) : Style.EMPTY;
            autolink(out, parts[i], st);
        }
        return out;
    }
}
