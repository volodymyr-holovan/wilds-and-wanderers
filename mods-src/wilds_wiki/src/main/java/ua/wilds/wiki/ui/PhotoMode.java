package ua.wilds.wiki.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import ua.wilds.wiki.WildsWiki;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Development only, started with -Dwilds_wiki.photos=&lt;shots.json&gt; on the local test server: flies the (spectator, op)
 * player to every camera point of the list, waits until the chunks around are drawn, hides the HUD and saves a small
 * screenshot as &lt;out&gt;/&lt;name&gt;.png (default out: wiki_photos/ in the game directory). Existing pictures are skipped,
 * so an interrupted run continues where it stopped. The game closes when the list is done.
 * shots.json: [{"name": "structure/cataclysm/sunken_city", "dim": "wilds_dump:photo", "x":..,"y":..,"z":..,"yaw":..,"pitch":..}]
 */
public final class PhotoMode {
    public static final String LIST = System.getProperty("wilds_wiki.photos");
    public static final boolean ON = LIST != null;
    // the wiki's pictures are 480x270; -Dwilds_wiki.photos.width=1280 takes bigger ones (16:9) for looking at a place
    static final int W = Integer.getInteger("wilds_wiki.photos.width", 480), H = W * 9 / 16;

    private static List<JsonObject> shots;
    private static Path out;
    private static int index = -1, ticks, waited, stable, lastSections, startDelay = 400;
    private static int oldDistance;
    private static net.minecraft.client.CloudStatus oldClouds;

    private PhotoMode() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        try {
            if (startDelay > 0) {
                startDelay--;
                if (startDelay == 0) load();
                return;
            }
            if (shots == null) return;
            if (index < 0 || waited < 0) next(mc);
            else waitAndShoot(mc);
        } catch (Throwable t) {
            WildsWiki.LOG.error("Wilds Wiki photo mode failed at {}", index, t);
            waited = -1;
        }
    }

    private static void load() throws Exception {
        try (Reader r = Files.newBufferedReader(Path.of(LIST), StandardCharsets.UTF_8)) {
            JsonArray a = JsonParser.parseReader(r).getAsJsonArray();
            shots = new ArrayList<>();
            for (JsonElement e : a) shots.add(e.getAsJsonObject());
        }
        out = Path.of(System.getProperty("wilds_wiki.photos.out", "wiki_photos"));
        Minecraft mc = Minecraft.getInstance();
        oldDistance = mc.options.renderDistance().get();
        oldClouds = mc.options.cloudStatus().get();
        mc.options.renderDistance().set(20);
        mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        Files.createDirectories(out);
        WildsWiki.LOG.info("Wilds Wiki photo mode: {} shots into {}", shots.size(), out.toAbsolutePath());
    }

    private static Path file(JsonObject s) {
        return out.resolve(s.get("name").getAsString() + ".png");
    }

    private static void next(Minecraft mc) {
        do {
            index++;
        } while (index < shots.size() && Files.exists(file(shots.get(index))));
        if (index >= shots.size()) {
            WildsWiki.LOG.info("Wilds Wiki photo mode: done");
            mc.options.hideGui = false;
            mc.options.renderDistance().set(oldDistance);
            mc.options.cloudStatus().set(oldClouds);
            mc.options.save();
            mc.stop();
            shots = null;
            return;
        }
        JsonObject s = shots.get(index);
        mc.options.hideGui = true;
        mc.player.connection.sendCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @s %.1f %.1f %.1f %.1f %.1f",
                s.get("dim").getAsString(), s.get("x").getAsDouble(), s.get("y").getAsDouble(), s.get("z").getAsDouble(),
                s.get("yaw").getAsDouble(), s.get("pitch").getAsDouble()));
        waited = 0;
        stable = 0;
        lastSections = -1;
    }

    /** at least 3 s, then until the number of drawn sections stops growing for 2 s (at most 40 s) */
    private static void waitAndShoot(Minecraft mc) throws Exception {
        waited++;
        int n = mc.levelRenderer.countRenderedSections();
        stable = n == lastSections ? stable + 1 : 0;
        lastSections = n;
        if (waited < 60 || (stable < 40 && waited < 800)) return;
        JsonObject s = shots.get(index);
        NativeImage full = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        try (NativeImage small = new NativeImage(W, H, false)) {
            // box filter over the source pixels each target pixel covers (crop to 16:9 first)
            int sw = full.getWidth(), sh = full.getHeight();
            int cw = Math.min(sw, sh * 16 / 9), ch = cw * 9 / 16, ox = (sw - cw) / 2, oy = (sh - ch) / 2;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int x0 = ox + x * cw / W, x1 = Math.max(x0 + 1, ox + (x + 1) * cw / W);
                    int y0 = oy + y * ch / H, y1 = Math.max(y0 + 1, oy + (y + 1) * ch / H);
                    long r = 0, g = 0, b = 0, c = 0;
                    for (int yy = y0; yy < y1; yy++) {
                        for (int xx = x0; xx < x1; xx++) {
                            int p = full.getPixelRGBA(xx, yy);
                            r += p & 255;
                            g += (p >> 8) & 255;
                            b += (p >> 16) & 255;
                            c++;
                        }
                    }
                    small.setPixelRGBA(x, y, 0xFF000000 | (int) (b / c) << 16 | (int) (g / c) << 8 | (int) (r / c));
                }
            }
            Path f = file(s);
            Files.createDirectories(f.getParent());
            small.writeToFile(f);
        } finally {
            full.close();
        }
        WildsWiki.LOG.info("Wilds Wiki photo {}/{}: {} ({} sections, {} ticks)", index + 1, shots.size(), s.get("name").getAsString(), n, waited);
        waited = -1;
    }
}
