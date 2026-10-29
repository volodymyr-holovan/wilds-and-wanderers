package ua.wilds.core.client.music;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ua.wilds.core.WildsCore;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * The pack's Reactive Music songpack, resourcepacks/WildsMusic: its rules (ReactiveMusic.yaml) and its own tracks come
 * with the pack; the songs it borrows from Reactive Music's built-in pack are copied here from the Reactive Music jar on
 * this computer (so the pack never ships them). The songpack is made the active one once per game folder - after that the
 * player's own choice in Reactive Music's settings stays.
 */
public final class MusicPack {
    static final String NAME = "Wilds & Wanderers";
    private static final Path DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks").resolve("WildsMusic");
    private static final Path FLAG = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("wilds_core-music.txt");
    private static final Pattern SONGS = Pattern.compile("songs:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL), QUOTED = Pattern.compile("\"([^\"]+)\"");
    private static boolean pending;

    private MusicPack() {}

    /** from the mod constructor: the borrowed songs in place before Reactive Music looks at the songpacks */
    public static void prepare() {
        if (!ModList.get().isLoaded("reactivemusic")) return;
        Path yaml = DIR.resolve("ReactiveMusic.yaml");
        if (!Files.exists(yaml)) return;
        try {
            Set<String> wanted = new LinkedHashSet<>();
            Matcher m = SONGS.matcher(Files.readString(yaml));
            while (m.find()) {
                Matcher q = QUOTED.matcher(m.group(1));
                while (q.find()) wanted.add(q.group(1));
            }
            Path jar = ModList.get().getModFileById("reactivemusic").getFile().getFilePath();
            Files.createDirectories(DIR.resolve("music"));
            int copied = 0;
            try (ZipFile z = new ZipFile(jar.toFile())) {
                for (String song : wanted) {
                    Path dst = DIR.resolve("music").resolve(song + ".mp3");
                    var entry = z.getEntry("musicpack/music/" + song + ".mp3");
                    if (entry == null || (Files.exists(dst) && Files.size(dst) == entry.getSize())) continue;
                    try (var in = z.getInputStream(entry)) {
                        Files.copy(in, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        copied++;
                    }
                }
            }
            if (copied > 0) WildsCore.LOG.info("Wilds Core: {} songs of Reactive Music's own pack put into the pack's songpack", copied);
            pending = !Files.exists(FLAG);
        } catch (Exception e) {
            WildsCore.LOG.warn("Wilds Core: cannot prepare the pack's songpack", e);
        }
    }

    /** Reactive Music writes its settings when the game closes normally: the choice goes into the file right away too */
    private static void remember() throws java.io.IOException {
        Path cfg = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("ReactiveMusic.json5");
        com.google.gson.JsonObject o = Files.exists(cfg)
                ? com.google.gson.JsonParser.parseString(Files.readString(cfg)).getAsJsonObject() : new com.google.gson.JsonObject();
        o.addProperty("loadedUserSongpack", NAME);
        o.addProperty("musicDelayLength2", "SONGPACK_DEFAULT");
        o.addProperty("musicSwitchSpeed2", "SONGPACK_DEFAULT");
        Files.writeString(cfg, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(o));
    }

    /** once Reactive Music runs, in a new game folder: the pack's songpack becomes Reactive Music's active one */
    public static void tick(ClientTickEvent.Post e) {
        if (!pending) return;
        try {
            // Reactive Music starts its player thread a little later than the first tick: wait for it
            Class<?> rmClass = Class.forName("circuitlord.reactivemusic.ReactiveMusic");
            if (rmClass.getField("thread").get(null) == null) return;
            pending = false;
            Class<?> loader = Class.forName("circuitlord.reactivemusic.RMSongpackLoader");
            loader.getMethod("fetchAvailableSongpacks").invoke(null);
            Object ours = null;
            for (Object zip : (List<?>) loader.getField("availableSongpacks").get(null)) {
                Object cfg = zip.getClass().getField("config").get(zip);
                if (cfg != null && NAME.equals(cfg.getClass().getField("name").get(cfg))) ours = zip;
            }
            if (ours == null) {
                WildsCore.LOG.warn("Wilds Core: the pack's songpack was not found by Reactive Music");
                return;
            }
            Class<?> rm = Class.forName("circuitlord.reactivemusic.ReactiveMusic");
            Field current = rm.getField("currentSongpack");
            if (current.get(null) != ours) rm.getMethod("setActiveSongpack", ours.getClass()).invoke(null, ours);
            remember();
            Files.writeString(FLAG, "Wilds & Wanderers: the pack's songpack was turned on once. Delete this file to do it again.\n");
            WildsCore.LOG.info("Wilds Core: the pack's songpack is on");
        } catch (Exception ex) {
            pending = false;
            WildsCore.LOG.warn("Wilds Core: cannot turn on the pack's songpack", ex);
        }
    }
}
