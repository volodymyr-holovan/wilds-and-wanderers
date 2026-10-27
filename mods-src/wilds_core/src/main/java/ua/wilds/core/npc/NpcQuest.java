package ua.wilds.core.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import ua.wilds.core.WildsCore;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One quest a wandering NPC can give, read from data/wilds_core/npc_quests/*.json (made by pack/tools/npc/gen_npc.py from
 * the stories in pack/tools/npc/quests_*.py). The same key names the hidden quest of the "Доручення" chapter.
 * A giver (persona) has one quest or a chain of up to five, given one after another to a player who finished the
 * previous one; the persona's key is the key of its own first-written quest and is what the NPC entity remembers.
 * <pre>
 * {key, persona, stage, stages, valuable, npc: {name, title, skin, slim, item}, where: {dims: [...], words: [...], under: bool, min_day},
 *  task: {type: item|kill|other, id, count}, reward: {items: ["id*n"], xp, skill_xp, loot},
 *  lines: {greet, ask, accept, decline, remind, done, thanks, lie_ok, lie_fail, haggle_ok, haggle_fail, dead},
 *  story: ["..."], questions: [{q, a}], valuable: bool (lying is possible), haggle: bool}
 * </pre>
 */
public final class NpcQuest {
    private static final Map<String, NpcQuest> ALL = new LinkedHashMap<>();
    private static final Map<String, List<NpcQuest>> CHAINS = new LinkedHashMap<>();
    /** what any giver can tell about the land (data/wilds_core/npc_rumors/*.json: a list of lines) */
    private static final List<String> RUMORS = new ArrayList<>();

    public static List<String> rumors() {
        return RUMORS;
    }

    public String key, persona, name, title, skin, item;
    public int stage = 1, stages = 1;
    public boolean slim, valuable, canHaggle, under;
    public List<String> dims = new ArrayList<>(), words = new ArrayList<>(), story = new ArrayList<>();
    public List<String[]> questions = new ArrayList<>();
    public String taskType = "other", taskId = "";
    public int taskCount = 1, xp, skillXp, minDay;
    public List<String> rewardItems = new ArrayList<>();
    public String loot;
    public final Map<String, String> lines = new LinkedHashMap<>();

    public String line(String k) {
        return lines.getOrDefault(k, "");
    }

    public static NpcQuest get(String key) {
        return ALL.get(key);
    }

    public static Collection<NpcQuest> all() {
        return ALL.values();
    }

    /** the givers: persona key -> its quests by stage */
    public static Map<String, List<NpcQuest>> chains() {
        return CHAINS;
    }

    public static List<NpcQuest> chain(String persona) {
        return CHAINS.getOrDefault(persona, List.of());
    }

    public static void reload(ResourceManager rm) {
        ALL.clear();
        CHAINS.clear();
        for (var res : rm.listResources("npc_quests", rl -> rl.getNamespace().equals(WildsCore.ID) && rl.getPath().endsWith(".json")).entrySet()) {
            try (Reader r = res.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(r);
                JsonArray list = root.isJsonArray() ? root.getAsJsonArray() : new JsonArray();
                if (root.isJsonObject()) list.add(root);
                for (JsonElement e : list) {
                    NpcQuest q = parse(e.getAsJsonObject());
                    ALL.put(q.key, q);
                }
            } catch (Exception ex) {
                WildsCore.LOG.warn("Wilds Core: bad NPC quest file {}", res.getKey(), ex);
            }
        }
        RUMORS.clear();
        for (var res : rm.listResources("npc_rumors", rl -> rl.getNamespace().equals(WildsCore.ID) && rl.getPath().endsWith(".json")).entrySet()) {
            try (Reader r = res.getValue().openAsReader()) {
                for (JsonElement e : JsonParser.parseReader(r).getAsJsonArray()) RUMORS.add(e.getAsString());
            } catch (Exception ex) {
                WildsCore.LOG.warn("Wilds Core: bad rumour file {}", res.getKey(), ex);
            }
        }
        for (NpcQuest q : ALL.values()) CHAINS.computeIfAbsent(q.persona, k -> new ArrayList<>()).add(q);
        for (List<NpcQuest> c : CHAINS.values()) c.sort(java.util.Comparator.comparingInt(q -> q.stage));
        WildsCore.LOG.info("Wilds Core: {} NPC quests from {} quest givers", ALL.size(), CHAINS.size());
    }

    private static String s(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }

    private static List<String> strings(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k)) for (JsonElement e : o.getAsJsonArray(k)) out.add(e.getAsString());
        return out;
    }

    private static NpcQuest parse(JsonObject o) {
        NpcQuest q = new NpcQuest();
        q.key = o.get("key").getAsString();
        q.persona = s(o, "persona", q.key);
        q.stage = o.has("stage") ? o.get("stage").getAsInt() : 1;
        q.stages = o.has("stages") ? o.get("stages").getAsInt() : 1;
        JsonObject npc = o.getAsJsonObject("npc");
        q.name = s(npc, "name", "Мандрівник");
        q.title = s(npc, "title", "");
        q.skin = s(npc, "skin", "minecraft:textures/entity/player/wide/steve.png");
        q.slim = npc.has("slim") && npc.get("slim").getAsBoolean();
        q.item = s(npc, "item", "minecraft:air");
        JsonObject where = o.getAsJsonObject("where");
        if (where != null) {
            q.dims = strings(where, "dims");
            q.words = strings(where, "words");
            q.under = where.has("under") && where.get("under").getAsBoolean();
            q.minDay = where.has("min_day") ? where.get("min_day").getAsInt() : 0;
        }
        JsonObject task = o.getAsJsonObject("task");
        if (task != null) {
            q.taskType = s(task, "type", "other");
            q.taskId = s(task, "id", "");
            q.taskCount = task.has("count") ? task.get("count").getAsInt() : 1;
        }
        JsonObject rw = o.getAsJsonObject("reward");
        if (rw != null) {
            q.rewardItems = strings(rw, "items");
            q.xp = rw.has("xp") ? rw.get("xp").getAsInt() : 0;
            q.skillXp = rw.has("skill_xp") ? rw.get("skill_xp").getAsInt() : 0;
            q.loot = s(rw, "loot", null);
        }
        JsonObject lines = o.getAsJsonObject("lines");
        if (lines != null) for (var e : lines.entrySet()) q.lines.put(e.getKey(), e.getValue().getAsString());
        q.story = strings(o, "story");
        if (o.has("questions")) {
            for (JsonElement e : o.getAsJsonArray("questions")) {
                JsonObject qa = e.getAsJsonObject();
                q.questions.add(new String[]{qa.get("q").getAsString(), qa.get("a").getAsString()});
            }
        }
        q.valuable = o.has("valuable") && o.get("valuable").getAsBoolean();
        q.canHaggle = !o.has("haggle") || o.get("haggle").getAsBoolean();
        return q;
    }
}
