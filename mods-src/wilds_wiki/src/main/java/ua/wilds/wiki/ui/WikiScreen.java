package ua.wilds.wiki.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import ua.wilds.wiki.data.Entry;
import ua.wilds.wiki.data.History;
import ua.wilds.wiki.data.WikiData;
import ua.wilds.wiki.search.Searcher;
import ua.wilds.wiki.ui.Elements.Element;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** The wiki: search and results on the left, the page on the right, back/forward like a browser. */
public final class WikiScreen extends Screen implements Page {
    private static final int TOP = 22, ROW = 22;
    private static final ItemStack BOOK = new ItemStack(Items.KNOWLEDGE_BOOK);

    private final Screen parent;
    private final WikiData data;
    private EditBox search;
    private String query = "";
    private long queryChangedAt = -1;

    /** left column: results of the query, or recent searches and pages when the query is empty */
    private record Row(Entry entry, String query, Component label) {}

    private final List<Row> rows = new ArrayList<>();
    private int selected = -1, resultCount;
    private double listScroll;

    private Entry current;
    private List<Element> page = List.of();
    private int pageWidth = -1, pageHeight;
    private double pageScroll;
    private boolean draggingScroll;
    private final Deque<String> back = new ArrayDeque<>(), forward = new ArrayDeque<>();

    // geometry, computed in init()
    private int x0, y0, x1, y1, listX, listW, pageX, pageW, bodyY;
    private int mouseX, mouseY;
    private float partial;
    private List<Component> tooltipLines;
    private ItemStack tooltipStack = ItemStack.EMPTY;
    private final List<Runnable> overlays = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();

    public WikiScreen(Screen parent, Entry open, String initialQuery) {
        super(Component.translatable("wilds_wiki.title"));
        this.parent = parent;
        this.data = WikiData.get();
        if (initialQuery != null) query = initialQuery;
        if (open != null) show(open, false);
        else showHome();
        refreshRows();
    }

    // ------------------------------------------------------------------ Page

    public Font font() {
        return font;
    }

    public WikiData data() {
        return data;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public float partialTick() {
        return partial;
    }

    public void open(Entry e) {
        if (!query.isBlank()) History.get().addQuery(query);
        show(e, true);
    }

    public void tooltip(List<Component> lines) {
        tooltipLines = lines;
    }

    public void tooltip(ItemStack stack) {
        tooltipStack = stack;
    }

    public void overlay(Runnable r) {
        overlays.add(r);
    }

    // ------------------------------------------------------------------ navigation

    private void show(Entry e, boolean push) {
        if (push && current != e) {
            back.push(current == null ? "" : current.key);
            forward.clear();
        }
        current = e;
        page = PageBuilder.build(data, e);
        pageWidth = -1;
        pageScroll = 0;
        History.get().addViewed(e.key);
    }

    private void showHome() {
        current = null;
        page = PageBuilder.home(data, () -> {
            History.get().clear();
            showHome();
            refreshRows();
        });
        pageWidth = -1;
        pageScroll = 0;
    }

    private void goHome() {
        if (current != null) {
            back.push(current.key);
            forward.clear();
        }
        showHome();
    }

    private void navigate(Deque<String> from, Deque<String> to) {
        if (from.isEmpty()) return;
        to.push(current == null ? "" : current.key);
        String k = from.pop();
        Entry e = data.entries.get(k);
        if (e == null) showHome();
        else show(e, false);
    }

    // ------------------------------------------------------------------ search

    private void refreshRows() {
        rows.clear();
        selected = -1;
        listScroll = 0;
        if (query.isBlank()) {
            List<String> qs = History.get().queries;
            if (!qs.isEmpty()) {
                rows.add(new Row(null, null, Component.translatable("wilds_wiki.list.recent_queries")));
                for (String q : qs.subList(0, Math.min(8, qs.size()))) rows.add(new Row(null, q, Component.literal(q)));
            }
            List<String> viewed = History.get().viewed;
            if (!viewed.isEmpty()) {
                rows.add(new Row(null, null, Component.translatable("wilds_wiki.list.recent_pages")));
                for (String k : viewed) {
                    Entry e = data.entries.get(k);
                    if (e != null) rows.add(new Row(e, null, null));
                }
            }
            if (rows.isEmpty()) {
                rows.add(new Row(null, null, Component.translatable("wilds_wiki.list.topics")));
                for (Entry e : data.topicEntries()) rows.add(new Row(e, null, null));
            }
            resultCount = -1;
            return;
        }
        List<Searcher.Hit<Entry>> hits = data.searcher.search(query, 300);
        resultCount = hits.size();
        for (Searcher.Hit<Entry> h : hits) rows.add(new Row(h.value(), null, null));
        if (!rows.isEmpty()) selected = 0;
    }

    @Override
    public void tick() {
        if (queryChangedAt >= 0 && System.currentTimeMillis() - queryChangedAt > 120) {
            queryChangedAt = -1;
            refreshRows();
        }
        for (Element e : page) e.tick();
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        int w = Math.min(width - 16, 620), h = height - 16;
        x0 = (width - w) / 2;
        y0 = 8;
        x1 = x0 + w;
        y1 = y0 + h;
        listW = Math.max(130, Math.min(200, w * 34 / 100));
        listX = x0 + 4;
        pageX = listX + listW + 6;
        pageW = x1 - 4 - pageX;
        bodyY = y0 + TOP + 4;
        search = new EditBox(font, listX + 18, bodyY + 4, listW - 22, 12, Component.translatable("wilds_wiki.search"));
        search.setBordered(false);
        search.setMaxLength(80);
        search.setTextColor(Theme.TEXT);
        search.setHint(Component.translatable("wilds_wiki.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(q -> {
            if (!q.equals(query)) {
                query = q;
                queryChangedAt = System.currentTimeMillis();
            }
        });
        addRenderableWidget(search);
        setInitialFocus(search);
        pageWidth = -1;
    }

    private int listTop() {
        return bodyY + 24;
    }

    private int listBottom() {
        return y1 - 16;
    }

    private int pageTop() {
        return bodyY + 2;
    }

    private int pageBottom() {
        return y1 - 4;
    }

    private void layoutPage() {
        int w = pageW - 16;
        if (w == pageWidth) return;
        pageWidth = w;
        pageHeight = 0;
        for (Element e : page) {
            try {
                pageHeight += e.layout(this, w) + 2;
            } catch (Throwable t) {
                broken(e, t);
            }
        }
    }

    /** one odd item of some mod must not close the wiki or crash the game: its element is skipped */
    static int errors;

    private void broken(Element e, Throwable t) {
        errors++;
        e.height = 0;
        ua.wilds.wiki.WildsWiki.LOG.warn("Wilds Wiki: {} on page {} failed", e.getClass().getSimpleName(),
                current == null ? "home" : current.key, t);
    }

    /** for the self test: scroll the page as if the wheel was turned */
    void scrollPage(double by) {
        pageScroll += by;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        Theme.cardOutline(g, x0, y0, x1 - x0, y1 - y0, Theme.BACKDROP, Theme.FRAME);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + TOP, Theme.TOP_BAR);
        Theme.hLine(g, x0 + 1, y0 + TOP, x1 - x0 - 2, Theme.ACCENT_DIM);
        g.renderFakeItem(BOOK, x0 + 5, y0 + 3);
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), x0 + 25, y0 + 7, Theme.ACCENT, true);
        // list column and the search field
        Theme.card(g, listX, bodyY, listW, y1 - 4 - bodyY, Theme.LIST_BG);
        Theme.cardOutline(g, listX + 3, bodyY + 2, listW - 6, 16, 0xFF0B0F13, search.isFocused() ? Theme.ACCENT_DIM : Theme.FRAME);
        Theme.magnifier(g, listX + 6, bodyY + 5, Theme.MUTED);
        Theme.card(g, pageX, bodyY, pageW, y1 - 4 - bodyY, Theme.PAGE_BG);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        mouseX = mx;
        mouseY = my;
        partial = pt;
        tooltipLines = null;
        tooltipStack = ItemStack.EMPTY;
        overlays.clear();
        super.render(g, mx, my, pt); // background and the search field
        if (search.getValue().isEmpty()) {
            g.drawString(font, Elements.ellipsize(font, Component.translatable("wilds_wiki.search.hint").getString(), search.getWidth() - 8),
                    search.getX() + 6, search.getY(), Theme.FAINT, false);
        }
        renderTopButtons(g, mx, my);
        renderList(g, mx, my);
        renderPage(g, mx, my);
        for (Runnable r : overlays) r.run();
        if (!tooltipStack.isEmpty()) g.renderTooltip(font, tooltipStack, mx, my);
        else if (tooltipLines != null) g.renderComponentTooltip(font, tooltipLines, mx, my);
    }

    private static final String[] NAV = {"◀", "▶", "⌂"};

    private int navX(int i) {
        return x1 - 4 - (NAV.length - i) * 18;
    }

    private void renderTopButtons(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < NAV.length; i++) {
            int bx = navX(i), by = y0 + 3;
            boolean enabled = i == 0 ? !back.isEmpty() : i == 1 ? !forward.isEmpty() : current != null;
            boolean hover = enabled && mx >= bx && mx < bx + 16 && my >= by && my < by + 16;
            Theme.card(g, bx, by, 16, 16, hover ? Theme.CARD_HOVER : Theme.CARD);
            g.drawCenteredString(font, NAV[i], bx + 8, by + 4, enabled ? (hover ? Theme.ACCENT : Theme.TEXT) : Theme.FAINT);
            if (hover) tooltipLines = List.of(Component.translatable("wilds_wiki.nav." + i));
        }
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        int top = listTop(), bottom = listBottom();
        if (resultCount >= 0) {
            Component c = resultCount == 0 ? Component.translatable("wilds_wiki.list.nothing")
                    : Component.translatable("wilds_wiki.list.found", resultCount);
            g.drawString(font, c, listX + 5, bottom + 4, Theme.MUTED, false);
        }
        int total = rows.size() * ROW;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, total - (bottom - top))));
        g.enableScissor(listX, top, listX + listW, bottom);
        int y = top - (int) listScroll;
        boolean inside = mx >= listX && mx < listX + listW && my >= top && my < bottom;
        for (int i = 0; i < rows.size(); i++, y += ROW) {
            if (y + ROW < top || y > bottom) continue;
            Row r = rows.get(i);
            boolean hover = inside && my >= y && my < y + ROW && r.label == null;
            if (r.entry == null && r.query == null) {
                g.drawString(font, Component.literal(Elements.ellipsize(font, r.label.getString(), (listW - 12) * 8 / 9))
                        .withStyle(ChatFormatting.BOLD), listX + 6, y + 9, Theme.ACCENT_DIM, false);
                continue;
            }
            if (i == selected) g.fill(listX + 2, y, listX + listW - 2, y + ROW - 1, Theme.ROW_SELECTED);
            else if (hover) g.fill(listX + 2, y, listX + listW - 2, y + ROW - 1, Theme.ROW_HOVER);
            if (r.query != null) {
                g.drawString(font, "↺", listX + 8, y + 7, Theme.MUTED, false);
                g.drawString(font, Elements.ellipsize(font, r.query, listW - 30), listX + 22, y + 7, Theme.TEXT, false);
                continue;
            }
            Entry e = r.entry;
            e.renderIcon(g, listX + 4, y + 3);
            g.drawString(font, Elements.ellipsize(font, e.name.getString(), listW - 30), listX + 23, y + 3,
                    e == current ? Theme.ACCENT : Theme.TEXT, false);
            String sub = e.kind.label().getString() + " · " + e.modName;
            g.pose().pushPose();
            g.pose().translate(listX + 23, y + 13, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, Elements.ellipsize(font, sub, (int) ((listW - 30) / 0.75f)), 0, 0, Theme.MUTED, false);
            g.pose().popPose();
        }
        g.disableScissor();
        scrollbar(g, listX + listW - 3, top, bottom, total, listScroll);
    }

    private void renderPage(GuiGraphics g, int mx, int my) {
        layoutPage();
        int top = pageTop(), bottom = pageBottom();
        pageScroll = Math.max(0, Math.min(pageScroll, Math.max(0, pageHeight + 8 - (bottom - top))));
        boolean inside = mx >= pageX && mx < pageX + pageW && my >= top && my < bottom;
        int realX = mouseX, realY = mouseY;
        if (!inside) {
            mouseX = -1000;
            mouseY = -1000;
        }
        g.enableScissor(pageX, top, pageX + pageW, bottom);
        int y = top + 6 - (int) pageScroll;
        for (Element e : page) {
            if (e.height > 0 && y + e.height >= top && y <= bottom) {
                try {
                    e.render(this, g, pageX + 8, y, pageWidth);
                } catch (Throwable t) {
                    broken(e, t);
                }
            }
            y += e.height + 2;
        }
        g.disableScissor();
        mouseX = realX;
        mouseY = realY;
        scrollbar(g, pageX + pageW - 4, top, bottom, pageHeight + 8, pageScroll);
    }

    private static void scrollbar(GuiGraphics g, int x, int top, int bottom, int total, double scroll) {
        int view = bottom - top;
        if (total <= view) return;
        g.fill(x, top, x + 2, bottom, Theme.SCROLL);
        int thumb = Math.max(16, view * view / total);
        int ty = top + (int) ((view - thumb) * scroll / (total - view));
        g.fill(x, ty, x + 2, ty + thumb, Theme.SCROLL_THUMB);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 3) {
            navigate(back, forward);
            return true;
        }
        if (button == 4) {
            navigate(forward, back);
            return true;
        }
        if (button == 0) {
            for (int i = 0; i < NAV.length; i++) {
                int bx = navX(i), by = y0 + 3;
                if (mx >= bx && mx < bx + 16 && my >= by && my < by + 16) {
                    if (i == 0) navigate(back, forward);
                    else if (i == 1) navigate(forward, back);
                    else goHome();
                    return true;
                }
            }
            int top = listTop(), bottom = listBottom();
            if (mx >= listX && mx < listX + listW && my >= top && my < bottom) {
                int i = (int) ((my - top + listScroll) / ROW);
                if (i >= 0 && i < rows.size()) activate(rows.get(i));
                setFocused(search);
                return true;
            }
            if (mx >= pageX + pageW - 8 && mx < pageX + pageW && my >= pageTop() && my < pageBottom()) {
                draggingScroll = true;
                dragScroll(my);
                return true;
            }
            if (mx >= pageX && mx < pageX + pageW && my >= pageTop() && my < pageBottom()) {
                int y = pageTop() + 6 - (int) pageScroll;
                mouseX = (int) mx;
                mouseY = (int) my;
                for (Element e : new ArrayList<>(page)) {
                    if (my >= y && my < y + e.height && e.click(this, pageX + 8, y, pageWidth, mx, my)) {
                        setFocused(search);
                        return true;
                    }
                    y += e.height + 2;
                }
                setFocused(search);
                return true;
            }
        }
        boolean r = super.mouseClicked(mx, my, button);
        setFocused(search);
        return r;
    }

    private void dragScroll(double my) {
        int view = pageBottom() - pageTop(), total = pageHeight + 8;
        if (total <= view) return;
        double f = (my - pageTop()) / view;
        pageScroll = Math.max(0, Math.min(total - view, f * total - view / 2.0));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScroll) {
            dragScroll(my);
            return true;
        }
        if (elementAt(mx, my) instanceof Element e && e.drag(mx, my, button, dx, dy)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingScroll = false;
        return super.mouseReleased(mx, my, button);
    }

    private void activate(Row r) {
        if (r.query != null) {
            search.setValue(r.query);
            query = r.query;
            refreshRows();
        } else if (r.entry != null) {
            open(r.entry);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < pageX) listScroll -= sy * ROW * 2;
        else if (elementAt(mx, my) instanceof Element e && e.scroll(mx, my, sx, sy)) return true;
        else pageScroll -= sy * 24;
        return true;
    }

    /** the page element under the mouse, or null */
    private Element elementAt(double mx, double my) {
        if (mx < pageX || mx >= pageX + pageW || my < pageTop() || my >= pageBottom()) return null;
        int y = pageTop() + 6 - (int) pageScroll;
        for (Element e : page) {
            if (my >= y && my < y + e.height) return e;
            y += e.height + 2;
        }
        return null;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        boolean alt = (mods & GLFW.GLFW_MOD_ALT) != 0;
        if (alt && key == GLFW.GLFW_KEY_LEFT) {
            navigate(back, forward);
            return true;
        }
        if (alt && key == GLFW.GLFW_KEY_RIGHT) {
            navigate(forward, back);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP -> {
                moveSelection(key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (queryChangedAt >= 0) {
                    queryChangedAt = -1;
                    refreshRows();
                }
                if (selected >= 0 && selected < rows.size()) activate(rows.get(selected));
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                pageScroll += pageBottom() - pageTop() - 20;
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                pageScroll -= pageBottom() - pageTop() - 20;
                return true;
            }
            default -> {}
        }
        if (!search.isFocused()) setFocused(search);
        return super.keyPressed(key, scan, mods);
    }

    private void moveSelection(int d) {
        if (rows.isEmpty()) return;
        int i = selected;
        do {
            i = Math.floorMod(i + d, rows.size());
        } while (rows.get(i).label != null && i != selected);
        selected = i;
        int top = listTop(), bottom = listBottom(), y = i * ROW;
        if (y < listScroll) listScroll = y;
        if (y + ROW > listScroll + bottom - top) listScroll = y + ROW - (bottom - top);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!query.isBlank() && current != null) History.get().addQuery(query);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        // the key press that opened the wiki from another screen also arrives here as a typed letter
        if (System.currentTimeMillis() - openedAt < 150) return true;
        return super.charTyped(c, mods);
    }
}
