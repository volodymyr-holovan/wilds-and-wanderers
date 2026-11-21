package ua.wilds.core.mobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import ua.wilds.core.WildsCore;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** data/wilds_core/wilds_mobs/variants.json (written by pack/tools/gen_mob_balance.py), reloaded with /reload */
public final class MobData {
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(WildsCore.ID, "wilds_mobs/variants.json");

    public record Effect(ResourceLocation id, int ticks, int amp) {}
    public record Family(String id, List<ResourceLocation> bases, Effect hit) {}
    public record Variant(String id, String family, int tier, ResourceLocation entity, List<String> abilities, List<Effect> hits,
                          float scale, Map<String, ResourceLocation> equip, String loot) {}
    public record LevelAbility(String ability, int from, double chance) {}

    static volatile boolean loaded;
    static double hpRate = 0.008, dmgRate = 0.006, bossHpRate = 0.02, bossDmgRate = 0.012;
    static double tierHits = 3.7, tierPlayerHits = 10, bossHits = 60, bossPlayerHits = 5;
    static double radius = 64, stepBase = 1, stepRate = 0.01, spreadLo = 0.9, spreadHi = 1.05;
    static double replaceBase = 0.2, replaceRate = 0.002, replaceMax = 0.7, plainLootBase = 0.12, plainLootRate = 0.001;
    static int updateTicks = 200;
    static double[][] defence = {{0, 7, 0, 0, 20}, {400, 30, 20, 20, 48}};
    static Map<Integer, Integer> unlock = new HashMap<>();
    static Map<String, Double> worldTier = new HashMap<>();
    static List<LevelAbility> levelAbilities = List.of();
    static List<String> bossAbilities = List.of();
    static Map<String, Family> families = new LinkedHashMap<>();
    /** the family of each vanilla base type */
    static Map<ResourceLocation, Family> byBase = new HashMap<>();
    static Map<String, Variant> variants = new LinkedHashMap<>();
    static Map<String, List<Variant>> byFamily = new HashMap<>();

    private MobData() {}

    public static int unlock(int tier) {
        return unlock.getOrDefault(tier, 1000);
    }

    public static Optional<Variant> variant(String id) {
        return Optional.ofNullable(variants.get(id));
    }

    public static void reload(ResourceManager rm) {
        Optional<Resource> res = rm.getResource(FILE);
        if (res.isEmpty()) {
            WildsCore.LOG.warn("Wilds Core: {} is missing, mob levels are off", FILE);
            loaded = false;
            return;
        }
        try (Reader r = res.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject s = root.getAsJsonObject("settings");
            hpRate = s.get("hp_rate").getAsDouble();
            dmgRate = s.get("dmg_rate").getAsDouble();
            bossHpRate = s.get("boss_hp_rate").getAsDouble();
            bossDmgRate = s.get("boss_dmg_rate").getAsDouble();
            tierHits = s.get("tier_hits").getAsDouble();
            tierPlayerHits = s.get("tier_player_hits").getAsDouble();
            bossHits = s.get("boss_hits").getAsDouble();
            bossPlayerHits = s.get("boss_player_hits").getAsDouble();
            radius = s.get("radius").getAsDouble();
            updateTicks = s.get("update_ticks").getAsInt();
            JsonArray spread = s.getAsJsonArray("spawn_spread");
            spreadLo = spread.get(0).getAsDouble();
            spreadHi = spread.get(1).getAsDouble();
            stepBase = s.get("step_base").getAsDouble();
            stepRate = s.get("step_rate").getAsDouble();
            replaceBase = s.get("replace_base").getAsDouble();
            replaceRate = s.get("replace_rate").getAsDouble();
            replaceMax = s.get("replace_max").getAsDouble();
            plainLootBase = s.get("plain_loot_base").getAsDouble();
            plainLootRate = s.get("plain_loot_rate").getAsDouble();
            JsonArray def = s.getAsJsonArray("defence");
            double[][] d = new double[def.size()][];
            for (int i = 0; i < d.length; i++) {
                JsonArray row = def.get(i).getAsJsonArray();
                d[i] = new double[row.size()];
                for (int j = 0; j < row.size(); j++) d[i][j] = row.get(j).getAsDouble();
            }
            defence = d;
            Map<Integer, Integer> un = new HashMap<>();
            for (JsonElement e : s.getAsJsonArray("tiers")) un.put(e.getAsJsonObject().get("tier").getAsInt(), e.getAsJsonObject().get("unlock").getAsInt());
            unlock = un;
            Map<String, Double> wt = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : s.getAsJsonObject("world_tier").entrySet()) wt.put(e.getKey(), e.getValue().getAsDouble());
            worldTier = wt;
            List<LevelAbility> la = new ArrayList<>();
            for (JsonElement e : s.getAsJsonArray("level_abilities")) {
                JsonArray a = e.getAsJsonArray();
                la.add(new LevelAbility(a.get(0).getAsString(), a.get(1).getAsInt(), a.get(2).getAsDouble()));
            }
            levelAbilities = la;
            List<String> ba = new ArrayList<>();
            for (JsonElement e : s.getAsJsonArray("boss_abilities")) ba.add(e.getAsString());
            bossAbilities = ba;

            Map<String, Family> fam = new LinkedHashMap<>();
            Map<ResourceLocation, Family> bb = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("families").entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                List<ResourceLocation> bases = new ArrayList<>();
                for (JsonElement b : o.getAsJsonArray("bases")) bases.add(ResourceLocation.parse(b.getAsString()));
                Family f = new Family(e.getKey(), bases, effect(o.getAsJsonArray("hit")));
                fam.put(f.id(), f);
                for (ResourceLocation b : bases) bb.put(b, f);
            }
            Map<String, Variant> vs = new LinkedHashMap<>();
            Map<String, List<Variant>> bf = new HashMap<>();
            for (JsonElement e : root.getAsJsonArray("variants")) {
                JsonObject o = e.getAsJsonObject();
                List<String> ab = new ArrayList<>();
                for (JsonElement a : o.getAsJsonArray("abilities")) ab.add(a.getAsString());
                List<Effect> hits = new ArrayList<>();
                for (JsonElement h : o.getAsJsonArray("hits")) hits.add(effect(h.getAsJsonArray()));
                Map<String, ResourceLocation> eq = new HashMap<>();
                for (Map.Entry<String, JsonElement> q : o.getAsJsonObject("equip").entrySet()) eq.put(q.getKey(), ResourceLocation.parse(q.getValue().getAsString()));
                JsonElement ent = o.get("entity");
                Variant v = new Variant(o.get("id").getAsString(), o.get("family").getAsString(), o.get("tier").getAsInt(),
                        ent == null || ent.isJsonNull() ? null : ResourceLocation.parse(ent.getAsString()), List.copyOf(ab), List.copyOf(hits),
                        o.get("scale").getAsFloat(), Map.copyOf(eq), o.get("loot").getAsString());
                vs.put(v.id(), v);
                bf.computeIfAbsent(v.family(), k -> new ArrayList<>()).add(v);
            }
            bf.values().forEach(l -> l.sort((a, b) -> Integer.compare(a.tier(), b.tier())));
            families = fam;
            byBase = bb;
            variants = vs;
            byFamily = bf;
            loaded = true;
            WildsCore.LOG.info("Wilds Core: mob levels loaded, {} families, {} new kinds of mobs", fam.size(), vs.size());
        } catch (Exception ex) {
            loaded = false;
            WildsCore.LOG.error("Wilds Core: cannot read {}, mob levels are off", FILE, ex);
        }
    }

    private static Effect effect(JsonArray a) {
        return new Effect(ResourceLocation.parse(a.get(0).getAsString()), a.get(1).getAsInt(), a.get(2).getAsInt());
    }
}
