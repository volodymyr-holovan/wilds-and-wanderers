package ua.wilds.core.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import ua.wilds.core.WildsCore;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the quest generator (pack/tools/quests/gen_quests.py) tells Wilds Core, from the server data
 * (kubejs/data/wilds_core/quests/*.json): which quests every new player gets pinned, which custom tasks a tutorial
 * event completes, and the invisible "gate" quest of each quest an NPC hands out.
 */
public final class QuestData {
    public static final List<Long> pins = new ArrayList<>();
    public static final Map<String, List<Long>> tutorial = new HashMap<>();
    /** NPC quest key -> {quest id, gate id} */
    public static final Map<String, long[]> gates = new HashMap<>();

    private QuestData() {}

    public static void reload(ResourceManager rm) {
        pins.clear();
        tutorial.clear();
        gates.clear();
        JsonObject o = read(rm, "onboarding");
        if (o != null) for (JsonElement e : o.getAsJsonArray("pins")) pins.add(id(e.getAsString()));
        o = read(rm, "tutorial");
        if (o != null) {
            for (var e : o.entrySet()) {
                List<Long> ids = new ArrayList<>();
                for (JsonElement t : e.getValue().getAsJsonArray()) ids.add(id(t.getAsString()));
                tutorial.put(e.getKey(), ids);
            }
        }
        o = read(rm, "gates");
        if (o != null) {
            for (var e : o.entrySet()) {
                JsonObject g = e.getValue().getAsJsonObject();
                gates.put(e.getKey(), new long[]{id(g.get("quest").getAsString()), id(g.get("gate").getAsString())});
            }
        }
        WildsCore.LOG.info("Wilds Core: {} pinned start quests, {} tutorial events, {} NPC quest gates", pins.size(), tutorial.size(), gates.size());
    }

    /** FTB Quests ids are 16 hex digits */
    public static long id(String hex) {
        return Long.parseUnsignedLong(hex, 16);
    }

    private static JsonObject read(ResourceManager rm, String name) {
        var res = rm.getResource(ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "quests/" + name + ".json"));
        if (res.isEmpty()) return null;
        try (Reader r = res.get().openAsReader()) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            WildsCore.LOG.warn("Wilds Core: bad quests/{}.json", name, e);
            return null;
        }
    }
}
