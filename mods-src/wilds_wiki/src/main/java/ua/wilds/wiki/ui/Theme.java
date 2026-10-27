package ua.wilds.wiki.ui;

import net.minecraft.client.gui.GuiGraphics;

/** Colours and small drawing helpers of the wiki: dark slate panels, amber accent, soft blue links. */
public final class Theme {
    private Theme() {}

    public static final int BACKDROP = 0xF0141920;
    public static final int FRAME = 0xFF2C3540;
    public static final int TOP_BAR = 0xFF1A2129;
    public static final int LIST_BG = 0xFF10151B;
    public static final int PAGE_BG = 0xFF161C23;
    public static final int CARD = 0xFF1E262F;
    public static final int CARD_HOVER = 0xFF283340;
    public static final int ROW_HOVER = 0x40FFFFFF;
    public static final int ROW_SELECTED = 0x50E8B04A;
    public static final int ACCENT = 0xFFE8B04A;
    public static final int ACCENT_DIM = 0xFF9C7632;
    public static final int TEXT = 0xFFE8E6E1;
    public static final int MUTED = 0xFF8D99A6;
    public static final int FAINT = 0xFF5B6672;
    public static final int LINK = 0xFF7CC4FF;
    public static final int TIP = 0xFF3B8F6A;
    public static final int WARN = 0xFFC2554B;
    public static final int SCROLL = 0xFF3A4654;
    public static final int SCROLL_THUMB = 0xFF6B7A8A;

    /** Panel with a 1px rounded look: the corner pixels stay empty. */
    public static void card(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void cardOutline(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        card(g, x, y, w, h, border);
        card(g, x + 1, y + 1, w - 2, h - 2, fill);
    }

    public static void hLine(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }

    /** a 16x16 magnifier drawn with fills, so the wiki needs no texture of its own for it */
    public static void magnifier(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 3, y + 1, x + 8, y + 2, color);
        g.fill(x + 2, y + 2, x + 3, y + 3, color);
        g.fill(x + 8, y + 2, x + 9, y + 3, color);
        g.fill(x + 1, y + 3, x + 2, y + 8, color);
        g.fill(x + 9, y + 3, x + 10, y + 8, color);
        g.fill(x + 2, y + 8, x + 3, y + 9, color);
        g.fill(x + 8, y + 8, x + 9, y + 9, color);
        g.fill(x + 3, y + 9, x + 8, y + 10, color);
        for (int i = 0; i < 4; i++) g.fill(x + 9 + i, y + 9 + i, x + 11 + i, y + 11 + i, color);
    }
}
