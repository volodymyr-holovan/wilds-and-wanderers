package ua.wilds.wiki.search;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Search for the video settings screen (Reese's Sodium Options): whole words instead of letter n-grams, so "дощ"
 * finds rain settings and not lava or sculk sensors. Typos and the wrong keyboard layout still work, and a small
 * dictionary links Ukrainian and English words for the same setting.
 */
public final class SettingsSearch {
    private SettingsSearch() {}

    /** words that mean the same thing in settings, Ukrainian and English */
    private static final String[][] SYNONYMS = {
            {"дощ", "опади", "погода", "rain", "weather", "precipitation", "splash"},
            {"сніг", "снігопад", "сніжинки", "snow", "snowflake", "snowflakes"},
            {"частинки", "частинок", "particles", "particle"},
            {"хмари", "хмар", "clouds", "cloud"},
            {"туман", "fog"},
            {"тіні", "тінь", "shadows", "shadow"},
            {"дальність", "відстань", "прорисовка", "прорисовки", "distance", "render", "lod"},
            {"кадри", "кадрів", "fps", "framerate", "fpc", "частота"},
            {"яскравість", "гамма", "brightness", "gamma"},
            {"згладжування", "antialiasing", "anti", "aliasing", "msaa", "fxaa"},
            {"синхронізація", "vsync", "v-sync"},
            {"листя", "leaves", "foliage"},
            {"трава", "grass"},
            {"вода", "water"},
            {"небо", "sky"},
            {"зірки", "stars"},
            {"сонце", "sun"},
            {"місяць", "moon"},
            {"анімація", "анімації", "animation", "animations", "animated"},
            {"текстури", "текстура", "texture", "textures"},
            {"біоми", "біом", "biome", "biomes", "blend"},
            {"сутності", "мобів", "моби", "entity", "entities", "mobs"},
            {"інтерфейс", "gui", "hud", "interface"},
            {"світло", "освітлення", "light", "lighting"},
            {"вибухи", "explosion", "explosions"},
            {"вогонь", "fire"},
            {"лава", "lava"},
            {"маяк", "beacon"},
            {"пам'ять", "памяти", "memory", "ram"},
            {"продуктивність", "performance", "швидкодія"},
            {"екран", "screen", "fullscreen", "window", "вікно"},
    };

    /** Called instead of the n-gram search; returns null to let the original search run (unexpected data). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List search(List<?> items, List<String> texts, String query) {
        if (items == null || texts == null || items.size() != texts.size()) return null;
        String[] q = Text.tokens(Text.fold(query));
        if (q.length == 0) return null;
        List<Set<String>> alts = new ArrayList<>();
        for (String t : q) alts.add(alternatives(t));
        record Hit(int index, double score) {}
        List<Hit> strict = new ArrayList<>(), loose = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            List<String> words = Text.words(texts.get(i));
            double sum = 0, min = 1;
            for (Set<String> alt : alts) {
                float best = 0;
                for (String a : alt) for (String w : words) best = Math.max(best, Searcher.word(a, w));
                sum += best;
                min = Math.min(min, best);
            }
            double score = sum / alts.size();
            if (min >= 0.72) strict.add(new Hit(i, score));
            else if (min >= 0.55) loose.add(new Hit(i, score));
        }
        List<Hit> hits = strict.isEmpty() ? loose : strict;
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        try {
            Class<?> resultClass = Class.forName("me.flashyreese.mods.reeses_sodium_options.client.search.SearchResult");
            Constructor<?> c = resultClass.getDeclaredConstructor(Object.class, double.class, int.class);
            c.setAccessible(true);
            List out = new ArrayList<>();
            for (Hit h : hits) out.add(c.newInstance(items.get(h.index()), h.score(), h.index()));
            return out;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** the query word itself, the same keys typed in the other layout, and dictionary synonyms */
    private static Set<String> alternatives(String token) {
        Set<String> out = new HashSet<>();
        out.add(token);
        String swapped = Text.fold(Text.swapLayout(token));
        if (!swapped.isEmpty()) out.add(swapped);
        for (String[] group : SYNONYMS) {
            boolean hit = false;
            for (String g : group) {
                String fg = Text.fold(g);
                if (fg.equals(token) || fg.equals(swapped) || (token.length() >= 4 && fg.startsWith(token))) {
                    hit = true;
                    break;
                }
            }
            if (hit) for (String g : group) out.add(Text.fold(g));
        }
        return out;
    }
}
