"""Ukrainian localization pipeline for the pack.

  i18n.py extract <mods_dir>   -> pack/i18n/src/<ns>.json   (English text of keys missing in uk_ua)
                                  pack/i18n/ref/<ns>.json   (Ukrainian text the mod already ships, for terminology)
  i18n.py build                -> validates pack/i18n/uk/<ns>.json and writes
                                  pack/overrides/kubejs/assets/<ns>/lang/uk_ua.json
  i18n.py status               -> progress per namespace

pack/i18n/fixes/<ns>.json corrects or extends the Ukrainian a mod ships itself (keys from ref), merged into the same output.

Validation: every translated key must exist in src, placeholders (%s, %d, %1$s, {0}, <tags>) and
formatting codes (§x) must match the English text exactly, so tooltips cannot break.
"""
import io, json, os, re, sys, zipfile

PACK = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
I18N = os.path.join(PACK, "i18n")
SRC, REF, UK, FIX = (os.path.join(I18N, d) for d in ("src", "ref", "uk", "fixes"))
OUT = os.path.join(PACK, "overrides", "kubejs", "assets")


def load_lang(raw):
    txt = raw.decode("utf-8-sig", "replace")
    try:
        return json.loads(txt, strict=False)
    except Exception:
        txt = re.sub(r"^\s*//.*$", "", txt, flags=re.M)
        txt = re.sub(r",\s*([}\]])", r"\1", txt)
        try:
            return json.loads(txt, strict=False)
        except Exception:
            return {}


def scan(zf, found):
    names = set(zf.namelist())
    for n in names:
        m = re.match(r"assets/([^/]+)/lang/en_us\.json$", n)
        if m:
            ns = m.group(1)
            en = load_lang(zf.read(n))
            uk_name = f"assets/{ns}/lang/uk_ua.json"
            uk = load_lang(zf.read(uk_name)) if uk_name in names else {}
            cur = found.setdefault(ns, ({}, {}))
            cur[0].update(en)
            cur[1].update(uk)
    for n in names:
        if n.startswith("META-INF/jarjar/") and n.endswith(".jar"):
            try:
                with zipfile.ZipFile(io.BytesIO(zf.read(n))) as inner:
                    scan(inner, found)
            except zipfile.BadZipFile:
                pass


def extract(mods_dir):
    found = {}
    for fn in sorted(os.listdir(mods_dir)):
        if fn.endswith(".jar"):
            with zipfile.ZipFile(os.path.join(mods_dir, fn)) as z:
                scan(z, found)
    os.makedirs(SRC, exist_ok=True); os.makedirs(REF, exist_ok=True)
    total = 0
    for ns, (en, uk) in sorted(found.items()):
        missing = {k: v for k, v in en.items() if k not in uk and isinstance(v, str) and v.strip()}
        if missing:
            json.dump(missing, open(os.path.join(SRC, ns + ".json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
            total += len(missing)
        if uk:
            json.dump(uk, open(os.path.join(REF, ns + ".json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"namespaces with gaps: {len(os.listdir(SRC))}, missing keys: {total}")


TOKEN = re.compile(r"%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[sdfxXcbeo%]|\{\d+\}|§[0-9a-fk-or]|<[a-z_/#!][^<>\s]{0,29}>|\\n|\n")


def tokens(s):
    # "%2$s" and "%s" are the same argument slot for comparison: translations may reorder arguments
    return sorted(re.sub(r"^%\d+\$", "%", t) for t in TOKEN.findall(s))


def build():
    problems, written = [], 0
    names = set(os.listdir(UK)) if os.path.isdir(UK) else set()
    if os.path.isdir(FIX):
        names |= set(os.listdir(FIX))
    for fn in sorted(names):
        if not fn.endswith(".json"):
            continue
        ns = fn[:-5]
        src_path = os.path.join(SRC, fn)
        src = json.load(open(src_path, encoding="utf-8")) if os.path.exists(src_path) else {}
        uk = json.load(open(os.path.join(UK, fn), encoding="utf-8")) if os.path.exists(os.path.join(UK, fn)) else {}
        good = {}
        fix_path, ref_path = os.path.join(FIX, fn), os.path.join(REF, fn)
        if os.path.exists(fix_path):
            ref = json.load(open(ref_path, encoding="utf-8")) if os.path.exists(ref_path) else {}
            for k, v in json.load(open(fix_path, encoding="utf-8")).items():
                base = ref.get(k, src.get(k))
                if base is None:
                    problems.append(f"{ns}: fix for unknown key {k}")
                elif tokens(base) != tokens(v):
                    problems.append(f"{ns}: placeholder mismatch in fix {k}")
                else:
                    good[k] = v
        for k, v in uk.items():
            if k not in src:
                problems.append(f"{ns}: unknown key {k}")
                continue
            if tokens(src[k]) != tokens(v):
                problems.append(f"{ns}: placeholder mismatch in {k}: en={tokens(src[k])} uk={tokens(v)}")
                continue
            good[k] = v
        d = os.path.join(OUT, ns, "lang")
        os.makedirs(d, exist_ok=True)
        json.dump(good, open(os.path.join(d, "uk_ua.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)
        written += len(good)
    for p in problems:
        print("  PROBLEM", p)
    print(f"written {written} keys, problems {len(problems)}")
    return not problems


def status():
    rows = []
    for fn in sorted(os.listdir(SRC)):
        ns = fn[:-5]
        src = json.load(open(os.path.join(SRC, fn), encoding="utf-8"))
        ukp = os.path.join(UK, fn)
        done = len(json.load(open(ukp, encoding="utf-8"))) if os.path.exists(ukp) else 0
        rows.append((ns, len(src), done))
    for ns, total, done in sorted(rows, key=lambda r: r[1] - r[2], reverse=True):
        if done < total:
            print(f"{ns:34} {done:5}/{total:5}")
    t = sum(r[1] for r in rows); d = sum(min(r[2], r[1]) for r in rows)
    print(f"TOTAL {d}/{t} ({100 * d // max(t, 1)}%)")


def merge():
    """Split translation batches (i18n/batches/*.json, {namespace: {key: text}}) into uk/<ns>.json."""
    bdir = os.path.join(I18N, "batches")
    os.makedirs(UK, exist_ok=True)
    for fn in sorted(os.listdir(bdir)):
        if fn.endswith(".json"):
            for ns, entries in json.load(open(os.path.join(bdir, fn), encoding="utf-8")).items():
                p = os.path.join(UK, ns + ".json")
                cur = json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {}
                cur.update(entries)
                json.dump(cur, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)
    print("merged batches")


def todo(ns, limit=400, offset=0):
    """Print untranslated keys of a namespace as compact JSON (input for the next batch)."""
    src = json.load(open(os.path.join(SRC, ns + ".json"), encoding="utf-8"))
    ukp = os.path.join(UK, ns + ".json")
    done = json.load(open(ukp, encoding="utf-8")) if os.path.exists(ukp) else {}
    left = [(k, v) for k, v in src.items() if k not in done][offset:offset + limit]
    sys.stdout.reconfigure(encoding="utf-8")
    print(json.dumps(dict(left), ensure_ascii=False, indent=0))


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "todo":
        todo(sys.argv[2], *(int(a) for a in sys.argv[3:]))
    elif cmd == "merge":
        merge()
    elif cmd == "extract":
        extract(sys.argv[2])
    elif cmd == "build":
        sys.exit(0 if build() else 1)
    elif cmd == "status":
        status()
