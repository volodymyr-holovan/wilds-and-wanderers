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

    private RichText() {}

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
        MutableComponent c = Component.literal(buf.toString());
        Style st = Style.EMPTY.withBold(bold).withItalic(italic);
        if (mark) st = st.withColor(Theme.ACCENT & 0xFFFFFF);
        out.append(c.withStyle(st));
        buf.setLength(0);
    }

    /** Create writes highlights of its item descriptions as _word_: show them in the accent colour. */
    public static MutableComponent createStyle(String s, int baseColor) {
        MutableComponent out = Component.empty().withStyle(Style.EMPTY.withColor(baseColor));
        String[] parts = s.split("_", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            MutableComponent c = Component.literal(parts[i]);
            if (i % 2 == 1 && parts.length > 2) c = c.withStyle(st -> st.withColor(Theme.ACCENT & 0xFFFFFF));
            out.append(c);
        }
        return out;
    }
}
