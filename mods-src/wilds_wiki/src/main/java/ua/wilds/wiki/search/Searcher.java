package ua.wilds.wiki.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Ranked fuzzy search over documents with weighted fields.
 *
 * A query is tried as typed, with the keyboard layout swapped and transliterated, so "drakon", "lhfrjy" and "дракн"
 * all find "Дракон". Every query word has to match some word of the document (short words are optional), by exact
 * word, prefix, inflected form ("дракона" -> "дракон"), substring or within an edit distance that grows with length.
 */
public final class Searcher<T> {
    public static final int NAME = 0, NAME_EN = 1, TRANSLIT = 2, KEYWORDS = 3, ID = 4, MOD = 5, TAGS = 6, FIELDS = 7;
    private static final float[] WEIGHT = {1f, 0.97f, 0.85f, 0.9f, 0.8f, 0.7f, 0.66f};

    public static final class Doc<T> {
        final T value;
        final String[][] fields = new String[FIELDS][];
        final String name, nameEn;
        final float boost;

        public Doc(T value, String name, String nameEn, float boost) {
            this.value = value;
            this.name = Text.fold(name);
            this.nameEn = Text.fold(nameEn);
            this.boost = boost;
            Arrays.fill(fields, new String[0]);
        }

        public Doc<T> field(int f, List<String> words) {
            fields[f] = words.stream().distinct().toArray(String[]::new);
            return this;
        }
    }

    public record Hit<T>(T value, float score) {}

    private final List<Doc<T>> docs = new ArrayList<>();

    public void add(Doc<T> d) {
        docs.add(d);
    }

    public int size() {
        return docs.size();
    }

    public List<Hit<T>> search(String raw, int limit) {
        String q = Text.fold(raw);
        if (q.isEmpty()) return List.of();
        List<String[]> variants = new ArrayList<>();
        List<Float> factors = new ArrayList<>();
        variants.add(Text.tokens(q));
        factors.add(1f);
        if (Text.hasLetters(q)) {
            String swapped = Text.fold(Text.swapLayout(raw));
            if (!swapped.equals(q)) {
                variants.add(Text.tokens(swapped));
                factors.add(0.92f);
            }
            String other = Text.isCyrillic(q) ? Text.toLatin(q) : Text.toCyrillic(q);
            if (!other.equals(q)) {
                variants.add(Text.tokens(other));
                factors.add(0.85f);
            }
        }
        List<Hit<T>> hits = new ArrayList<>();
        // the same words repeat across thousands of documents: score each distinct word once per query word
        List<List<java.util.Map<String, Float>>> memo = new ArrayList<>();
        for (String[] v : variants) {
            List<java.util.Map<String, Float>> m = new ArrayList<>();
            for (int i = 0; i < v.length; i++) m.add(new java.util.HashMap<>());
            memo.add(m);
        }
        for (Doc<T> d : docs) {
            float best = 0;
            for (int v = 0; v < variants.size(); v++) {
                float s = score(d, variants.get(v), memo.get(v)) * factors.get(v);
                if (v == 0 || s > best) best = Math.max(best, s);
            }
            if (best >= 0.5f) hits.add(new Hit<>(d.value, best * d.boost));
        }
        hits.sort(Comparator.comparingDouble((Hit<T> h) -> -h.score()));
        return hits.size() > limit ? new ArrayList<>(hits.subList(0, limit)) : hits;
    }

    private static float score(Doc<?> d, String[] q, List<java.util.Map<String, Float>> memo) {
        float sum = 0;
        int required = 0, missed = 0;
        for (int qi = 0; qi < q.length; qi++) {
            String t = q[qi];
            java.util.Map<String, Float> m = memo.get(qi);
            boolean optional = t.length() <= 2 && q.length > 1;
            float best = 0;
            for (int f = 0; f < FIELDS; f++) {
                float w = WEIGHT[f];
                if (w <= best) continue;
                for (String word : d.fields[f]) {
                    Float cached = m.get(word);
                    if (cached == null) {
                        cached = word(t, word);
                        m.put(word, cached);
                    }
                    float s = cached * w;
                    if (s > best) best = s;
                    if (best >= w) break;
                }
            }
            if (optional) {
                sum += best * 0.3f;
                continue;
            }
            required++;
            if (best == 0) missed++;
            sum += best;
        }
        if (required == 0) return 0;
        if (missed > 0 && (required < 3 || missed > 1)) return 0;
        float s = sum / required * (missed > 0 ? 0.6f : 1f);
        // whole-phrase bonuses: exact name, name starts with the query, name contains it
        String phrase = String.join(" ", q);
        if (d.name.equals(phrase) || d.nameEn.equals(phrase)) s += 0.5f;
        else if (d.name.startsWith(phrase) || d.nameEn.startsWith(phrase)) s += 0.3f;
        else if (phrase.length() >= 3 && (d.name.contains(phrase) || d.nameEn.contains(phrase))) s += 0.12f;
        // among equal matches the shorter name is the more likely target ("Меч" before "Меч із драконячої кістки")
        return s - Math.min(0.1f, d.name.length() * 0.002f);
    }

    /** How well one query word matches one document word, 0..1. */
    public static float word(String q, String w) {
        int ql = q.length(), wl = w.length();
        if (q.equals(w)) return 1f;
        if (w.startsWith(q)) return Math.max(0.72f, 0.92f - (wl - ql) * 0.015f) * (ql == 1 ? 0.6f : 1f);
        if (wl >= 4 && q.startsWith(w) && ql - wl <= 3) return 0.8f; // inflected form of the word: "дракона", "swords"
        if (ql >= 3 && w.contains(q)) return 0.5f; // inside a compound word: "dragonbone" has "bone"
        if (ql < 3) return 0;
        // a typo rarely hits both of the first two letters: skip the edit distance for words that share neither
        char a0 = q.charAt(0), a1 = q.charAt(1), b0 = w.charAt(0), b1 = wl > 1 ? w.charAt(1) : 0;
        if (a0 != b0 && a0 != b1 && a1 != b0 && a1 != b1) return 0;
        int max = ql <= 4 ? 1 : ql <= 7 ? 2 : 3;
        int d = Integer.MAX_VALUE;
        if (Math.abs(wl - ql) <= max) d = distance(q, w, max);
        // the user is still typing: compare with the beginning of the word
        if (wl > ql) d = Math.min(d, distance(q, w.substring(0, ql), max) + 0);
        if (d > max) return 0;
        float s = 0.78f - 0.14f * d;
        if (ql <= 4) s -= 0.12f; // one typo in a short word is a weaker signal
        return s;
    }

    /** Damerau-Levenshtein (optimal string alignment) distance, stops early once it exceeds max. */
    static int distance(String a, String b, int max) {
        int n = a.length(), m = b.length();
        if (Math.abs(n - m) > max) return max + 1;
        int[] prev2 = new int[m + 1], prev = new int[m + 1], cur = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            int rowMin = cur[0];
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char cb = b.charAt(j - 1);
                int cost = ca == cb ? 0 : 1;
                int v = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost);
                if (i > 1 && j > 1 && ca == b.charAt(j - 2) && a.charAt(i - 2) == cb) v = Math.min(v, prev2[j - 2] + 1);
                cur[j] = v;
                if (v < rowMin) rowMin = v;
            }
            if (rowMin > max) return max + 1;
            int[] t = prev2;
            prev2 = prev;
            prev = cur;
            cur = t;
        }
        return prev[m];
    }
}
