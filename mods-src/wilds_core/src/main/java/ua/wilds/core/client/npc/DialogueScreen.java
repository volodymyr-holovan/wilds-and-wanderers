package ua.wilds.core.client.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import ua.wilds.core.net.Net;

import java.util.ArrayList;
import java.util.List;

/**
 * A conversation page: the NPC's name and words in a dark panel across the lower part of the screen (the world and
 * the NPC's face stay visible above it), the text appearing letter by letter, and numbered answers below it.
 * Click an answer or press its number; Space shows the whole text at once; Esc ends the conversation.
 */
public class DialogueScreen extends Screen {
    private static DialogueScreen current;

    private String name = "", title = "";
    /** the words (a text component: item and creature names come translated) and their length in characters */
    private Component text = Component.empty();
    private int textLength;
    private final List<String> options = new ArrayList<>();
    private List<FormattedCharSequence> lines = List.of();
    private float shown;
    private int panelTop, optTop, hovered = -1;
    private boolean answered;

    private DialogueScreen() {
        super(Component.literal("Розмова"));
    }

    /** a page arrived from the server (npc 0: the conversation is over) */
    public static void page(int npcId, String json) {
        Minecraft mc = Minecraft.getInstance();
        if (npcId == 0 || json.isEmpty()) {
            DialogueCamera.stop();
            if (mc.screen instanceof DialogueScreen) mc.setScreen(null);
            current = null;
            return;
        }
        Entity npc = mc.level == null ? null : mc.level.getEntity(npcId);
        if (npc != null) DialogueCamera.start(npc);
        if (!(mc.screen instanceof DialogueScreen)) {
            current = new DialogueScreen();
            mc.setScreen(current);
        }
        current.show(JsonParser.parseString(json).getAsJsonObject());
    }

    private void show(JsonObject o) {
        name = o.get("name").getAsString();
        title = o.get("title").getAsString();
        String raw = o.get("text").getAsString();
        Component parsed = null;
        try {
            var mc = Minecraft.getInstance();
            if (mc.level != null) parsed = Component.Serializer.fromJson(raw, mc.level.registryAccess());
        } catch (RuntimeException ignored) {
        }
        text = parsed != null ? parsed : Component.literal(raw);
        options.clear();
        for (JsonElement e : o.getAsJsonArray("options")) options.add(e.getAsString());
        shown = 0;
        answered = false;
        layout();
    }

    protected void init() {
        layout();
    }

    private int panelWidth() {
        return Math.min(width - 24, 520);
    }

    private void layout() {
        if (font == null) return;
        int w = panelWidth() - 24;
        lines = font.split(text, w);
        textLength = 0;
        for (FormattedCharSequence l : lines) textLength += lengthOf(l);
        int textH = Math.min(lines.size(), 12) * 10;
        int optH = options.size() * 13;
        int h = 28 + textH + 10 + optH + 10;
        panelTop = height - h - 8;
        optTop = panelTop + 28 + textH + 10;
    }

    public void tick() {
        shown += 3.5f;
    }

    private boolean revealed() {
        return font == null || shown >= textLength;
    }

    public void render(GuiGraphics g, int mx, int my, float partial) {
        int pw = panelWidth(), x = (width - pw) / 2;
        // the panel: dark, with a thin gold line on top
        g.fillGradient(x, panelTop, x + pw, height - 8, 0xD0101014, 0xE8080809);
        g.fill(x, panelTop, x + pw, panelTop + 1, 0xFFC8A050);
        g.drawString(font, Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x + 12, panelTop + 8, 0xFFFFFF, false);
        if (!title.isEmpty()) {
            int nw = font.width(Component.literal(name).withStyle(ChatFormatting.BOLD));
            g.drawString(font, Component.literal(" — " + title).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), x + 12 + nw, panelTop + 8, 0xFFFFFF, false);
        }
        // the words, letter by letter
        int budget = (int) shown, y = panelTop + 26;
        int maxLines = Math.min(lines.size(), 12);
        for (int i = 0; i < maxLines && budget > 0; i++) {
            FormattedCharSequence line = lines.get(i);
            int len = lengthOf(line);
            if (len <= budget) g.drawString(font, line, x + 12, y, 0xE8E2D4, false);
            else g.drawString(font, prefix(line, budget), x + 12, y, 0xE8E2D4, false);
            budget -= len;
            y += 10;
        }
        // the answers
        hovered = -1;
        if (revealed()) {
            for (int i = 0; i < options.size(); i++) {
                int oy = optTop + i * 13;
                boolean over = mx >= x + 8 && mx < x + pw - 8 && my >= oy - 2 && my < oy + 11;
                if (over) {
                    hovered = i;
                    g.fill(x + 8, oy - 2, x + pw - 8, oy + 11, 0x40C8A050);
                }
                g.drawString(font, Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GOLD).append(Component.literal(options.get(i))),
                        x + 14, oy, over ? 0xFFFFFF : 0xC8C2B4, false);
            }
        } else {
            g.drawString(font, Component.literal("Пробіл — показати все").withStyle(ChatFormatting.DARK_GRAY), x + pw - 12 - font.width("Пробіл — показати все"), height - 20, 0xFFFFFF, false);
        }
    }

    private static int lengthOf(FormattedCharSequence s) {
        int[] n = {0};
        s.accept((i, style, cp) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    private static FormattedCharSequence prefix(FormattedCharSequence s, int count) {
        return sink -> {
            int[] n = {0};
            return s.accept((i, style, cp) -> n[0]++ >= count || sink.accept(i, style, cp));
        };
    }

    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // no blur and no darkening: the NPC's face is the point of this screen
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!revealed()) {
            shown = textLength;
            return true;
        }
        if (hovered >= 0) {
            answer(hovered);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (!revealed()) shown = textLength;
            else if (options.size() == 1) answer(0);
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && revealed()) {
            int i = key - GLFW.GLFW_KEY_1;
            if (i < options.size()) answer(i);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void answer(int i) {
        if (answered) return;
        answered = true;
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        PacketDistributor.sendToServer(new Net.DialogueChoice(i));
    }

    public void onClose() {
        PacketDistributor.sendToServer(new Net.DialogueChoice(-1));
        DialogueCamera.stop();
        current = null;
        super.onClose();
    }

    public boolean isPauseScreen() {
        return false;
    }
}
