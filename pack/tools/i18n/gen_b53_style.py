"""Style fixes over every Ukrainian string the pack shows (the mods' own uk_ua and ours), 03.11: the few Russian calques
found by a scan (в залежності від, співпадає, в даний момент, на протязі, в якості) and the gender of Nomadic Tents'
yurt doors (юрта is feminine: «Вхід крихітної юрти», not «крихітного юрти»)."""
import json, os, re, sys, zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import i18n  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
SUBS = [
    (r"\bв залежності від\b", "залежно від"), (r"\bВ залежності від\b", "Залежно від"),
    (r"\bне співпадає\b", "не збігається"), (r"\bспівпадає\b", "збігається"),
    (r"\bв даний момент\b", "наразі"), (r"\bна протязі\b", "протягом"),
    (r"\bв якості\b", "як"),
    (r"(крихітн|мал|середнь|велик|величезн)(ого|ий) юрти", lambda m: m.group(1) + {"крихітн": "ої", "мал": "ої", "середнь": "ої", "велик": "ої", "величезн": "ої"}[m.group(1)] + " юрти"),
]


def effective():
    found, seen = {}, set()
    for d in ("client/mods", "server/mods"):
        for fn in os.listdir(os.path.join(ROOT, d)):
            if fn.endswith(".jar") and fn not in seen:
                seen.add(fn)
                i18n.scan(zipfile.ZipFile(os.path.join(ROOT, d, fn)), found)
    for ns, (en, uk) in found.items():
        p = os.path.join(ROOT, "pack", "overrides", "kubejs", "assets", ns, "lang", "uk_ua.json")
        eff = dict(uk)
        if os.path.exists(p):
            eff.update(json.load(open(p, encoding="utf-8")))
        yield ns, eff


if __name__ == "__main__":
    total = 0
    for ns, eff in effective():
        out = {}
        for k, v in eff.items():
            if not isinstance(v, str):
                continue
            new = v
            for pat, rep in SUBS:
                new = re.sub(pat, rep, new)
            if new != v:
                out[k] = new
        if out:
            unknown = i18n.store(ns, out)
            total += len(out) - len(unknown)
            print(ns, len(out), "unknown" if unknown else "", unknown[:3])
    print("fixed", total)
