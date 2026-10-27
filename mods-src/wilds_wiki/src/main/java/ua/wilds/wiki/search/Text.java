package ua.wilds.wiki.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Text folding for search: one spelling for letters people mix up, transliteration and keyboard layout swaps. */
public final class Text {
    private Text() {}

    /**
     * Lower case, letters that are easy to confuse folded together (ґ/г, ї/і/и, є/е, й/и, Russian ы/э/ё),
     * soft sign and apostrophes dropped, accents removed, everything else becomes a space.
     */
    public static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder b = new StringBuilder(n.length());
        boolean space = true;
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                // NFD splits й and ї into и/і + mark: the mark goes, the base letter stays (й -> и, ї -> і)
                continue;
            }
            switch (c) {
                case 'ґ' -> c = 'г';
                case 'є', 'э', 'ё' -> c = 'е';
                case 'ы' -> c = 'и';
                case 'ъ', 'ь', '\'', '’', 'ʼ', '`', '´' -> { continue; }
                default -> {}
            }
            if (Character.isLetterOrDigit(c)) {
                b.append(c);
                space = false;
            } else if (!space) {
                b.append(' ');
                space = true;
            }
        }
        int len = b.length();
        if (len > 0 && b.charAt(len - 1) == ' ') b.setLength(len - 1);
        return b.toString();
    }

    public static String[] tokens(String folded) {
        if (folded.isEmpty()) return new String[0];
        return folded.split(" ");
    }

    public static boolean isCyrillic(String s) {
        int cyr = 0, lat = 0;
        for (int i = 0; i < s.length(); i++) {
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(s.charAt(i));
            if (ub == Character.UnicodeBlock.CYRILLIC) cyr++;
            else if (s.charAt(i) >= 'a' && s.charAt(i) <= 'z') lat++;
        }
        return cyr > lat;
    }

    public static boolean hasLetters(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) return true;
        return false;
    }

    // Ukrainian national transliteration (2010), simplified for folded text (є/ї/й are already е/і/и)
    private static final String CYR = "абвгдежзиіклмнопрстуфхцчшщюя";
    private static final String[] LAT = {"a", "b", "v", "h", "d", "e", "zh", "z", "y", "i", "k", "l", "m", "n", "o", "p", "r", "s", "t",
            "u", "f", "kh", "ts", "ch", "sh", "shch", "iu", "ia"};

    /** Cyrillic to Latin letters: "дракон" -> "drakon", so a Latin query finds Ukrainian names and the other way round. */
    public static String toLatin(String folded) {
        StringBuilder b = new StringBuilder(folded.length() + 8);
        for (int i = 0; i < folded.length(); i++) {
            char c = folded.charAt(i);
            int k = CYR.indexOf(c);
            b.append(k >= 0 ? LAT[k] : String.valueOf(c));
        }
        return b.toString();
    }

    /** Latin spelled phonetically to Cyrillic: "drakon" -> "дракон", "zhaba" -> "жаба". Greedy longest match. */
    public static String toCyrillic(String folded) {
        String[][] map = {{"shch", "щ"}, {"sch", "щ"}, {"zh", "ж"}, {"kh", "х"}, {"ts", "ц"}, {"ch", "ч"}, {"sh", "ш"}, {"yu", "ю"},
                {"iu", "ю"}, {"ya", "я"}, {"ia", "я"}, {"ye", "е"}, {"yi", "і"}, {"a", "а"}, {"b", "б"}, {"v", "в"}, {"w", "в"}, {"h", "г"},
                {"g", "г"}, {"d", "д"}, {"e", "е"}, {"z", "з"}, {"y", "и"}, {"i", "і"}, {"j", "и"}, {"k", "к"}, {"c", "к"}, {"q", "к"},
                {"l", "л"}, {"m", "м"}, {"n", "н"}, {"o", "о"}, {"p", "п"}, {"r", "р"}, {"s", "с"}, {"t", "т"}, {"u", "у"}, {"f", "ф"},
                {"x", "кс"}};
        StringBuilder b = new StringBuilder(folded.length());
        int i = 0;
        outer:
        while (i < folded.length()) {
            for (String[] m : map) {
                if (folded.startsWith(m[0], i)) {
                    b.append(m[1]);
                    i += m[0].length();
                    continue outer;
                }
            }
            b.append(folded.charAt(i++));
        }
        return b.toString();
    }

    // The same physical keys on the QWERTY and the Ukrainian ЙЦУКЕН layouts
    private static final String QWERTY = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";
    private static final String UKR = "йцукенгшщзхїфівапролджєячсмитьбю'";

    /** Text typed with the wrong keyboard layout: "lhfrjy" -> "дракон", "ckbpy" -> "слизн". Works both ways. */
    public static String swapLayout(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        boolean toUkr = !isCyrillic(s);
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int k = toUkr ? QWERTY.indexOf(c) : UKR.indexOf(c);
            if (!toUkr && k < 0) {
                // ы, э, ъ, ё of the Russian layout sit on the keys of і, є, ї, `
                k = "ыэъё".indexOf(c);
                if (k >= 0) k = UKR.indexOf("ієї'".charAt(k));
            }
            b.append(k >= 0 ? (toUkr ? UKR.charAt(k) : QWERTY.charAt(k)) : c);
        }
        return b.toString();
    }

    /** Words of the text for indexing: folded tokens, ids split on _ / . : */
    public static List<String> words(String s) {
        List<String> out = new ArrayList<>();
        for (String t : tokens(fold(s.replace('_', ' ').replace('/', ' ').replace(':', ' ').replace('.', ' ')))) {
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
