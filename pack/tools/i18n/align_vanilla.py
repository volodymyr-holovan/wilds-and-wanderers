"""Rewrites batch translations so shared vanilla terms match Minecraft's own uk_ua (i18n/ref/minecraft.json)."""
import glob, json, os, re
B = os.path.join(os.path.dirname(__file__), "..", "..", "i18n", "batches")
SUBS = [
    (r"пухк(ий|ого|ому|им) сніг", lambda m: {"ий": "сипкий", "ого": "сипкого", "ому": "сипкому", "им": "сипким"}[m.group(1)] + " сніг"),
    (r"бирк[ау] з іменем", "ярлик"),
    (r"у торбинці", "у мішечку"), (r"в торбинці", "в мішечку"),
    (r"спавнер(ів|и|а)", lambda m: "породжувач" + m.group(1)),
    (r"ефект візера", "висушування"),
    (r"Іфрити", "Пломені"), (r"вогняні стрижні", "стрижні пломеня"),
    (r"візер-скелети", "візери-скелети"),
    (r"\bголем(а|ів|и)?\b", lambda m: "ґолем" + (m.group(1) or "")),
    (r"\bГолем\b", "Ґолем"),
    (r"тризубом", "тризубцем"),
    (r'"Штани"', '"Наголінники"'),
]
for fn in sorted(glob.glob(os.path.join(B, "*.json"))):
    data = json.load(open(fn, encoding="utf-8"))
    n = 0
    for ns, entries in data.items():
        for k, v in entries.items():
            new = v
            for pat, rep in SUBS:
                new = re.sub(pat, rep, new)
            if new != v:
                entries[k] = new; n += 1
    if n:
        json.dump(data, open(fn, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print(os.path.basename(fn), n)
