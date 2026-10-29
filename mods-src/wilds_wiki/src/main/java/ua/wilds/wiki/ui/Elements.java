package ua.wilds.wiki.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import ua.wilds.wiki.WildsWiki;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.jei.RecipeView;

import java.util.ArrayList;
import java.util.List;

/** Building blocks of a wiki page. Each one lays itself out for a width and draws at a position. */
public final class Elements {
    private Elements() {}

    public abstract static class Element {
        int height;

        abstract int layout(Page p, int w);

        abstract void render(Page p, GuiGraphics g, int x, int y, int w);

        boolean click(Page p, int x, int y, int w, double mx, double my) {
            return false;
        }

        boolean scroll(double mx, double my, double sx, double sy) {
            return false;
        }

        boolean drag(double mx, double my, int button, double dx, double dy) {
            return false;
        }

        void tick() {}
    }

    static boolean over(Page p, int x, int y, int w, int h) {
        return p.mouseX() >= x && p.mouseX() < x + w && p.mouseY() >= y && p.mouseY() < y + h;
    }

    static String ellipsize(Font f, String s, int w) {
        if (f.width(s) <= w) return s;
        while (!s.isEmpty() && f.width(s + "…") > w) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    // ------------------------------------------------------------------ header

    public static final class Header extends Element {
        final Entry e;
        LivingEntity model;
        boolean modelTried;
        List<FormattedCharSequence> title;
        int scale, box;

        public Header(Entry e) {
            this.e = e;
        }

        LivingEntity model() {
            if (!modelTried && e.entityType != null) {
                modelTried = true;
                try {
                    Entity ent = e.entityType.create(Minecraft.getInstance().level);
                    if (ent instanceof LivingEntity le) model = le;
                } catch (Throwable t) {
                    WildsWiki.LOG.debug("Wilds Wiki: no model for {}", e.id, t);
                }
            }
            return model;
        }

        int layout(Page p, int w) {
            box = model() != null ? 76 : 40;
            int avail = w - box - 18;
            Component name = e.name.copy().withStyle(ChatFormatting.BOLD);
            scale = p.font().width(name) * 2 <= avail ? 2 : 1;
            title = p.font().split(name, scale == 2 ? avail / 2 : avail);
            int textH = title.size() * 9 * scale + 4 + (showEn() ? 10 : 0) + 14;
            return height = Math.max(box + 8, textH + 10);
        }

        boolean showEn() {
            return !e.nameEn.isEmpty() && !e.nameEn.equalsIgnoreCase(e.name.getString());
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            Theme.card(g, x, y, w, height, Theme.CARD);
            g.fill(x, y + 2, x + 2, y + height - 2, Theme.ACCENT);
            int bx = x + 8, by = y + (height - box) / 2;
            Theme.card(g, bx, by, box, box, 0xFF141A20);
            if (model != null) {
                try {
                    float h = Math.max(0.4f, model.getBbHeight()), wd = Math.max(0.4f, model.getBbWidth());
                    int s = (int) Math.max(4, Math.min((box - 12) / h, (box - 12) / wd));
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx + 2, by + 2, bx + box - 2, by + box - 2, s, 0.0625f,
                            p.mouseX(), p.mouseY(), model);
                } catch (Throwable t) {
                    model = null; // a mob that cannot render outside the world keeps its spawn egg icon
                }
            } else {
                g.pose().pushPose();
                g.pose().translate(bx + 4, by + 4, 0);
                g.pose().scale(2, 2, 1);
                e.renderIcon(g, 0, 0);
                g.pose().popPose();
            }
            int tx = bx + box + 10, ty = y + 8;
            for (FormattedCharSequence line : title) {
                g.pose().pushPose();
                g.pose().translate(tx, ty, 0);
                g.pose().scale(scale, scale, 1);
                g.drawString(p.font(), line, 0, 0, Theme.ACCENT, true);
                g.pose().popPose();
                ty += 9 * scale;
            }
            ty += 3;
            if (showEn()) {
                g.drawString(p.font(), Component.literal(e.nameEn).withStyle(ChatFormatting.ITALIC), tx, ty, Theme.MUTED, false);
                ty += 10;
            }
            int right = x + w - 6;
            int bx2 = badge(p, g, tx, ty + 1, e.kind.label().getString(), Theme.ACCENT_DIM, right);
            if (!e.modName.isEmpty()) badge(p, g, bx2 + 4, ty + 1, e.modName, 0xFF34404D, right);
        }

        static int badge(Page p, GuiGraphics g, int x, int y, String s, int color, int right) {
            if (right - x < 24) return x;
            String text = ellipsize(p.font(), s, right - x - 8);
            int w = p.font().width(text) + 8;
            Theme.card(g, x, y, w, 11, color);
            g.drawString(p.font(), text, x + 4, y + 2, Theme.TEXT, false);
            return x + w;
        }
    }

    // ------------------------------------------------------------------ text

    public static final class Heading extends Element {
        final Component text;
        final int level;

        public Heading(Component text, int level) {
            this.text = text;
            this.level = level;
        }

        int layout(Page p, int w) {
            return height = level == 1 ? 20 : 15;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            if (level == 1) {
                g.drawString(p.font(), text.copy().withStyle(ChatFormatting.BOLD), x, y + 7, Theme.ACCENT, true);
                Theme.hLine(g, x, y + 18, w, 0x60E8B04A);
            } else {
                g.drawString(p.font(), text.copy().withStyle(ChatFormatting.BOLD), x, y + 5, Theme.TEXT, true);
            }
        }
    }

    /** Wrapped rich text; style BULLET, TIP (green box), WARN (red box), MUTED. */
    public static final class Paragraph extends Element {
        public enum Kind { NORMAL, BULLET, TIP, WARN, MUTED }

        final FormattedText text;
        final Kind kind;
        List<FormattedCharSequence> lines = List.of();

        public Paragraph(FormattedText text, Kind kind) {
            this.text = text;
            this.kind = kind;
        }

        int inset() {
            return switch (kind) {
                case BULLET -> 10;
                case TIP, WARN -> 9;
                default -> 0;
            };
        }

        int layout(Page p, int w) {
            lines = p.font().split(text, w - inset() - (kind == Kind.TIP || kind == Kind.WARN ? 6 : 0));
            int h = lines.size() * 10;
            return height = kind == Kind.TIP || kind == Kind.WARN ? h + 10 : h + 3;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            int tx = x + inset(), ty = y;
            if (kind == Kind.TIP || kind == Kind.WARN) {
                int c = kind == Kind.TIP ? Theme.TIP : Theme.WARN;
                Theme.card(g, x, y, w, height - 2, (c & 0xFFFFFF) | 0x30000000);
                g.fill(x, y, x + 2, y + height - 2, c);
                ty += 4;
            } else if (kind == Kind.BULLET) {
                g.fill(x + 3, y + 3, x + 6, y + 6, Theme.ACCENT);
            }
            int color = kind == Kind.MUTED ? Theme.MUTED : Theme.TEXT;
            for (FormattedCharSequence line : lines) {
                String link = linkAt(p, line, tx, ty);
                if (link != null) {
                    // the link under the mouse lights up and the pointer turns into a hand
                    int[] r = range(p, line, link);
                    if (r != null) g.fill(tx + r[0] - 1, ty - 1, tx + r[1] + 1, ty + 9, 0x50E8B04A);
                    p.hoverLink();
                }
                g.drawString(p.font(), line, tx, ty, color, false);
                if (link != null) {
                    var e = p.data().entries.get(link.substring(RichText.LINK.length()));
                    if (e != null) {
                        List<Component> tip = new ArrayList<>();
                        tip.add(e.name.copy().withStyle(s -> s.withColor(Theme.ACCENT & 0xFFFFFF)));
                        tip.add(e.kind.label().copy().append(" · " + e.modName).withStyle(ChatFormatting.GRAY));
                        tip.add(Component.translatable("wilds_wiki.link.open").withStyle(ChatFormatting.DARK_GRAY));
                        p.tooltip(tip);
                    }
                }
                ty += 10;
            }
        }

        /** where on the line (from, to in pixels) the text of this link is */
        static int[] range(Page p, FormattedCharSequence line, String link) {
            int[] r = {-1, -1};
            int[] x = {0};
            line.accept((index, style, cp) -> {
                int w = p.font().width(FormattedCharSequence.forward(new String(Character.toChars(cp)), style));
                if (link.equals(style.getInsertion())) {
                    if (r[0] < 0) r[0] = x[0];
                    r[1] = x[0] + w;
                }
                x[0] += w;
                return true;
            });
            return r[0] < 0 ? null : r;
        }

        static String linkAt(Page p, FormattedCharSequence line, int x, int y) {
            if (p.mouseY() < y - 1 || p.mouseY() >= y + 9 || p.mouseX() < x) return null;
            Style s = p.font().getSplitter().componentStyleAtWidth(line, p.mouseX() - x);
            return s != null && s.getInsertion() != null && s.getInsertion().startsWith(RichText.LINK) ? s.getInsertion() : null;
        }

        boolean click(Page p, int x, int y, int w, double mx, double my) {
            int tx = x + inset(), ty = y + (kind == Kind.TIP || kind == Kind.WARN ? 4 : 0);
            for (FormattedCharSequence line : lines) {
                String link = linkAt(p, line, tx, ty);
                if (link != null) {
                    var e = p.data().entries.get(link.substring(RichText.LINK.length()));
                    if (e != null) {
                        p.open(e);
                        return true;
                    }
                }
                ty += 10;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ chips of items / mobs

    public static final class Chips extends Element {
        final List<Entry> entries;
        final boolean compact;
        final List<int[]> boxes = new ArrayList<>();
        final int max;

        public Chips(List<Entry> entries, boolean compact) {
            this.entries = entries;
            this.compact = compact;
            this.max = compact ? 200 : 60;
        }

        int layout(Page p, int w) {
            boxes.clear();
            int cx = 0, cy = 0, rowH = compact ? 20 : 20;
            for (int i = 0; i < Math.min(entries.size(), max); i++) {
                Entry e = entries.get(i);
                // two chips fit side by side; a long name is shortened rather than taking a whole row
                int cw = compact ? 20 : Math.min(26 + p.font().width(e.name), Math.max(60, (w - 3) / 2));
                if (cx > 0 && cx + cw > w) {
                    cx = 0;
                    cy += rowH + 2;
                }
                boxes.add(new int[]{cx, cy, cw});
                cx += cw + 3;
            }
            return height = entries.isEmpty() ? 0 : cy + rowH + 4;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            for (int i = 0; i < boxes.size(); i++) {
                int[] b = boxes.get(i);
                Entry e = entries.get(i);
                int bx = x + b[0], by = y + b[1];
                boolean hover = over(p, bx, by, b[2], 20);
                Theme.card(g, bx, by, b[2], 20, hover ? Theme.CARD_HOVER : Theme.CARD);
                e.renderIcon(g, bx + 2, by + 2);
                if (!compact) {
                    g.drawString(p.font(), ellipsize(p.font(), e.name.getString(), b[2] - 26), bx + 21, by + 6,
                            hover ? Theme.ACCENT : Theme.TEXT, false);
                }
                if (hover) {
                    if (!e.stack.isEmpty()) p.tooltip(e.stack);
                    else p.tooltip(List.of(e.name.copy().withStyle(s -> s.withColor(Theme.ACCENT & 0xFFFFFF)),
                            e.kind.label().copy().append(" · " + e.modName).withStyle(ChatFormatting.GRAY)));
                }
            }
        }

        boolean click(Page p, int x, int y, int w, double mx, double my) {
            for (int i = 0; i < boxes.size(); i++) {
                int[] b = boxes.get(i);
                if (mx >= x + b[0] && mx < x + b[0] + b[2] && my >= y + b[1] && my < y + b[1] + 20) {
                    p.open(entries.get(i));
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ facts table

    public static final class Facts extends Element {
        final List<Component[]> rows = new ArrayList<>();
        int labelW;
        final List<List<FormattedCharSequence>> wrapped = new ArrayList<>();

        public Facts add(Component label, Component value) {
            rows.add(new Component[]{label, value});
            return this;
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }

        int layout(Page p, int w) {
            labelW = 0;
            for (Component[] r : rows) labelW = Math.max(labelW, p.font().width(r[0]));
            labelW = Math.min(labelW + 12, w / 2);
            wrapped.clear();
            int h = 0;
            for (Component[] r : rows) {
                List<FormattedCharSequence> v = p.font().split(r[1], w - labelW - 8);
                wrapped.add(v);
                h += Math.max(1, v.size()) * 10 + 3;
            }
            return height = h + 4;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            int ry = y;
            for (int i = 0; i < rows.size(); i++) {
                int rh = Math.max(1, wrapped.get(i).size()) * 10 + 3;
                Theme.card(g, x, ry, w, rh, i % 2 == 0 ? Theme.CARD : 0xFF1A2129);
                g.drawString(p.font(), rows.get(i)[0], x + 6, ry + 2, Theme.MUTED, false);
                int ly = ry + 2;
                for (FormattedCharSequence line : wrapped.get(i)) {
                    g.drawString(p.font(), line, x + labelW, ly, Theme.TEXT, false);
                    ly += 10;
                }
                ry += rh;
            }
        }
    }

    // ------------------------------------------------------------------ pictures

    public static final class Image extends Element {
        final ResourceLocation tex;
        final int srcW, srcH, dispW, dispH;
        final Component caption;

        public Image(ResourceLocation tex, int srcW, int srcH, int dispW, int dispH, Component caption) {
            this.tex = tex;
            this.srcW = srcW;
            this.srcH = srcH;
            this.dispW = dispW;
            this.dispH = dispH;
            this.caption = caption;
        }

        int layout(Page p, int w) {
            return height = Math.min(dispH, dispH * w / Math.max(1, dispW)) + (caption != null ? 13 : 0) + 6;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            int dw = Math.min(dispW, w), dh = dispH * dw / Math.max(1, dispW);
            int ix = x + (w - dw) / 2;
            Theme.card(g, ix - 2, y, dw + 4, dh + 4, Theme.FRAME);
            g.blit(tex, ix, y + 2, dw, dh, 0, 0, srcW, srcH, srcW, srcH);
            if (caption != null) g.drawCenteredString(p.font(), caption, x + w / 2, y + dh + 7, Theme.MUTED);
        }
    }

    public static final class Model extends Element {
        final EntityType<?> type;
        final int size;
        LivingEntity entity;
        boolean tried;

        public Model(EntityType<?> type, int size) {
            this.type = type;
            this.size = size;
        }

        int layout(Page p, int w) {
            if (!tried) {
                tried = true;
                try {
                    if (type.create(Minecraft.getInstance().level) instanceof LivingEntity le) entity = le;
                } catch (Throwable ignored) {
                }
            }
            return height = entity == null ? 0 : size + 6;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            if (entity == null) return;
            int bx = x + (w - size) / 2;
            Theme.card(g, bx, y, size, size, 0xFF141A20);
            try {
                float h = Math.max(0.4f, entity.getBbHeight()), wd = Math.max(0.4f, entity.getBbWidth());
                int s = (int) Math.max(4, Math.min((size - 14) / h, (size - 14) / wd));
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx + 2, y + 2, bx + size - 2, y + size - 2, s, 0.0625f,
                        p.mouseX(), p.mouseY(), entity);
            } catch (Throwable t) {
                entity = null;
            }
        }
    }

    // ------------------------------------------------------------------ buttons and recipes

    public record Button(Component label, Runnable action) {}

    public static final class Buttons extends Element {
        final List<Button> buttons;
        final List<int[]> boxes = new ArrayList<>();

        public Buttons(List<Button> buttons) {
            this.buttons = buttons;
        }

        int layout(Page p, int w) {
            boxes.clear();
            int cx = 0, cy = 0;
            for (Button b : buttons) {
                int bw = p.font().width(b.label()) + 16;
                if (cx > 0 && cx + bw > w) {
                    cx = 0;
                    cy += 20;
                }
                boxes.add(new int[]{cx, cy, bw});
                cx += bw + 4;
            }
            return height = buttons.isEmpty() ? 0 : cy + 22;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            for (int i = 0; i < boxes.size(); i++) {
                int[] b = boxes.get(i);
                boolean hover = over(p, x + b[0], y + b[1], b[2], 17);
                Theme.cardOutline(g, x + b[0], y + b[1], b[2], 17, hover ? 0xFF3A2F1A : Theme.CARD, hover ? Theme.ACCENT : Theme.ACCENT_DIM);
                g.drawString(p.font(), buttons.get(i).label(), x + b[0] + 8, y + b[1] + 5, hover ? Theme.ACCENT : Theme.TEXT, false);
            }
        }

        boolean click(Page p, int x, int y, int w, double mx, double my) {
            for (int i = 0; i < boxes.size(); i++) {
                int[] b = boxes.get(i);
                if (mx >= x + b[0] && mx < x + b[0] + b[2] && my >= y + b[1] && my < y + b[1] + 17) {
                    buttons.get(i).action().run();
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Recipe extends Element {
        final RecipeView view;
        final Component caption;
        int vx, vy;

        public Recipe(RecipeView view, Component caption) {
            this.view = view;
            this.caption = caption;
        }

        int layout(Page p, int w) {
            return height = 13 + view.height() + 6;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {
            g.drawString(p.font(), caption, x, y + 2, Theme.MUTED, false);
            vx = x + Math.max(0, (w - view.width()) / 2);
            vy = y + 13;
            boolean inside = over(p, vx, vy, view.width(), view.height());
            int mx = inside ? p.mouseX() : -1000, my = inside ? p.mouseY() : -1000;
            view.render(g, vx, vy, mx, my);
            if (inside) view.moved(mx, my);
            if (inside) {
                p.overlay(() -> view.renderOverlays(g, mx, my));
            }
        }

        boolean click(Page p, int x, int y, int w, double mx, double my) {
            ItemStack s = view.stackAt((int) mx, (int) my);
            if (s.isEmpty()) return false;
            Entry e = p.data().byItem.get(s.getItem());
            if (e != null) p.open(e);
            return e != null;
        }

        boolean scroll(double mx, double my, double sx, double sy) {
            return view.scrolled(mx, my, sx, sy);
        }

        boolean drag(double mx, double my, int button, double dx, double dy) {
            return view.dragged(mx, my, button, dx, dy);
        }

        void tick() {
            view.tick();
        }
    }

    public static final class Spacer extends Element {
        final int h;

        public Spacer(int h) {
            this.h = h;
        }

        int layout(Page p, int w) {
            return height = h;
        }

        void render(Page p, GuiGraphics g, int x, int y, int w) {}
    }
}
