"""Collect what the mods themselves write about their things, so a wiki page says at least what the in-game guide says.

  python gen_books.py

Writes into src/main/resources/assets/wilds_wiki/generated/:
  books.json     - "item:<id>" / "entity:<id>" -> list of {"book": lang key or name, "title": text or lang key,
                   "lang": "uk"|"en", "text": [paragraphs]} from the guide books inside the jars:
                   Alex's Mobs animal dictionary, Alex's Caves cave codex, the Ice and Fire bestiary, Patchouli books
                   (Apotheosis, Apothic Attributes, Iron's Spells, Ice and Fire) and the AE2 guide. Ukrainian when the
                   mod ships it, English otherwise.
  desc_keys.json - "<id>" -> lang keys that describe the thing (Ars Nouveau documentation pages, glyph and ritual
                   descriptions, Hybrid Aquatic journal, ".desc"/".tooltip" lines...); the game resolves them in the
                   player's language.
"""
import glob, json, os, re, zipfile
from collections import defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "wilds_wiki", "generated")
JARS = sorted(set(glob.glob(os.path.join(ROOT, "client", "mods", "*.jar"))), key=os.path.basename)
JARS.append(os.path.join(os.environ["APPDATA"], ".minecraft", "versions", "1.21.1", "1.21.1.jar"))  # vanilla advancements and names
REG = os.path.join(ROOT, "docs", "registries")
ITEMS = set(open(os.path.join(REG, "item.txt"), encoding="utf-8").read().split())
ENTITIES = set(open(os.path.join(REG, "entity_type.txt"), encoding="utf-8").read().split())

books = defaultdict(list)
recipe_out = {}


def add(target, book, title, lang, paras, key=None):
    """title: a lang key, or the page's title in this language; key: what makes two languages the same page"""
    paras = [p for p in (q.strip() for q in paras) if p]
    if not paras:
        return
    kind, _, rid = target.partition(":")
    if (kind == "item" and rid not in ITEMS) or (kind == "entity" and rid not in ENTITIES):
        return
    key = key or title
    for b in books[target]:  # the same page in another language joins the entry
        if b["book"] == book and b["_key"] == key:
            b["text"].setdefault(lang, paras)
            if isinstance(b["title"], dict):
                b["title"].setdefault(lang, title)
            return
    books[target].append({"book": book, "_key": key, "title": {lang: title} if key != title else title, "text": {lang: paras}})


def link_key(key):
    """'entity.alexsmobs.hummingbird' / 'item.x.y' / 'block.x.y' -> target."""
    m = re.match(r"(entity|item|block)\.([a-z0-9_]+)\.([a-z0-9_/]+)$", key or "")
    if not m:
        return None
    return ("entity:" if m[1] == "entity" else "item:") + f"{m[2]}:{m[3]}"


def read_text(z, path):
    try:
        return z.read(path).decode("utf-8-sig", "replace")
    except KeyError:
        return None


# ------------------------------------------------------------------ Citadel books (Alex's Mobs, Alex's Caves)
def citadel_paras(raw):
    raw = re.sub(r"<image>?<[^>]*>|<[^>]+\.png[^>]*>", " ", raw)
    parts = re.split(r"<NEWLINE>|\n\s*\n", raw)
    out = []
    for p in parts:
        p = re.sub(r"\s+", " ", p).strip()
        if p:
            out.append(p)
    return out


def citadel(z, base, book_name):
    names = set(z.namelist())
    for n in sorted(names):
        if not n.startswith(base) or not n.endswith(".json"):
            continue
        rel = n[len(base):]
        if re.match(r"[a-z]{2,3}_[a-z]{2}/", rel):
            continue
        try:
            d = json.loads(z.read(n))
        except ValueError:
            continue
        text_file = d.get("text")
        if not text_file:
            continue
        texts = [(l, citadel_paras(raw)) for l, raw in (("uk", read_text(z, f"{base}uk_ua/{text_file}")),
                                                        ("en", read_text(z, f"{base}en_us/{text_file}"))) if raw]
        title = d.get("title", "")
        targets = set()
        t = link_key(title)
        if t:
            targets.add(t)
        for e in d.get("entity_renders", []):
            targets.add("entity:" + e.get("entity", ""))
        for e in d.get("item_renders", []):
            targets.add("item:" + e.get("item", ""))
        for r in d.get("recipes", []):
            o = recipe_out.get(r.get("recipe"))
            if o:
                targets.add("item:" + o)
        for w in d.get("widgets", []):
            if w.get("type") == "item" and w.get("item"):
                targets.add("item:" + w["item"])
            if w.get("entity_id"):
                targets.add("entity:" + w["entity_id"])
            if w.get("recipe") and recipe_out.get(w["recipe"]):
                targets.add("item:" + recipe_out[w["recipe"]])
        for tg in targets:
            for lang, paras in texts:
                add(tg, book_name, title, lang, paras)


# ------------------------------------------------------------------ Ice and Fire bestiary
BESTIARY = {
    "firedragon": ["entity:iceandfire:fire_dragon", "item:iceandfire:dragonscales_red", "item:iceandfire:fire_dragon_blood", "item:iceandfire:fire_dragon_heart", "item:iceandfire:dragon_skull_fire"],
    "icedragon": ["entity:iceandfire:ice_dragon", "item:iceandfire:dragonscales_blue", "item:iceandfire:ice_dragon_blood", "item:iceandfire:ice_dragon_heart", "item:iceandfire:dragon_skull_ice"],
    "lightningdragon": ["entity:iceandfire:lightning_dragon", "item:iceandfire:dragonscales_electric", "item:iceandfire:lightning_dragon_blood", "item:iceandfire:lightning_dragon_heart", "item:iceandfire:dragon_skull_lightning"],
    "firedragonegg": ["item:iceandfire:dragonegg_red", "item:iceandfire:dragonegg_green", "item:iceandfire:dragonegg_bronze", "item:iceandfire:dragonegg_gray"],
    "icedragonegg": ["item:iceandfire:dragonegg_blue", "item:iceandfire:dragonegg_white", "item:iceandfire:dragonegg_sapphire", "item:iceandfire:dragonegg_silver"],
    "lightningdragonegg": ["item:iceandfire:dragonegg_electric", "item:iceandfire:dragonegg_amethyst", "item:iceandfire:dragonegg_copper", "item:iceandfire:dragonegg_black"],
    "tameddragons": ["item:iceandfire:dragon_horn", "item:iceandfire:dragon_stick", "item:iceandfire:dragon_flute", "item:iceandfire:dragon_meal"],
    "dragonforge": ["item:iceandfire:dragonforge_fire_core", "item:iceandfire:dragonforge_ice_core", "item:iceandfire:dragonforge_lightning_core",
                    "item:iceandfire:dragonforge_fire_brick", "item:iceandfire:dragonforge_ice_brick", "item:iceandfire:dragonforge_lightning_brick",
                    "item:iceandfire:dragonforge_fire_input", "item:iceandfire:dragonforge_ice_input", "item:iceandfire:dragonforge_lightning_input",
                    "item:iceandfire:dragonsteel_fire_ingot", "item:iceandfire:dragonsteel_ice_ingot", "item:iceandfire:dragonsteel_lightning_ingot"],
    "amphithere": ["entity:iceandfire:amphithere", "item:iceandfire:amphithere_feather"],
    "cockatrice": ["entity:iceandfire:cockatrice", "item:iceandfire:cockatrice_eye", "item:iceandfire:cockatrice_scepter"],
    "cyclops": ["entity:iceandfire:cyclops", "item:iceandfire:cyclops_eye"],
    "deathworm": ["entity:iceandfire:death_worm", "item:iceandfire:deathworm_chitin_yellow", "item:iceandfire:deathworm_gauntlet_yellow"],
    "dread_mobs": ["entity:iceandfire:dread_thrall", "entity:iceandfire:dread_lich", "entity:iceandfire:dread_knight", "entity:iceandfire:dread_beast", "entity:iceandfire:dread_ghoul", "entity:iceandfire:dread_horse", "entity:iceandfire:dread_scuttler"],
    "ghost": ["entity:iceandfire:ghost", "item:iceandfire:ghost_ingot"],
    "gorgon": ["entity:iceandfire:gorgon", "item:iceandfire:gorgon_head"],
    "hippocampus": ["entity:iceandfire:hippocampus", "item:iceandfire:hippocampus_fin", "item:iceandfire:hippocampus_slapper"],
    "hippogryph": ["entity:iceandfire:hippogryph", "item:iceandfire:hippogryph_talon", "item:iceandfire:hippogryph_egg"],
    "hydra": ["entity:iceandfire:hydra", "item:iceandfire:hydra_fang", "item:iceandfire:hydra_heart", "item:iceandfire:hydra_skull"],
    "pixie": ["entity:iceandfire:pixie", "item:iceandfire:pixie_dust", "item:iceandfire:pixie_wings", "item:iceandfire:jar_empty", "item:iceandfire:jar_pixie_0"],
    "seaserpent": ["entity:iceandfire:sea_serpent", "item:iceandfire:sea_serpent_scales_blue", "item:iceandfire:sea_serpent_fang"],
    "siren": ["entity:iceandfire:siren", "item:iceandfire:siren_tear", "item:iceandfire:siren_flute", "item:iceandfire:earplugs"],
    "stymphalianbird": ["entity:iceandfire:stymphalian_bird", "item:iceandfire:stymphalian_bird_feather"],
    "troll": ["entity:iceandfire:troll", "item:iceandfire:troll_tusk", "item:iceandfire:troll_leather_forest"],
    "materials": ["item:iceandfire:silver_ingot", "item:iceandfire:sapphire_gem", "item:iceandfire:silver_ore", "item:iceandfire:sapphire_ore"],
    "alchemy": ["item:iceandfire:fire_stew", "item:iceandfire:frost_stew", "item:iceandfire:lightning_stew"],
    "villagers": ["item:iceandfire:manuscript"],
    "introduction": ["item:iceandfire:bestiary"],
}


def bestiary(z):
    names = z.namelist()
    for stem, targets in BESTIARY.items():
        for lang, folder in (("uk", "uk_ua_0"), ("en", "en_us_0")):
            files = sorted((n for n in names if re.match(rf"assets/iceandfire/lang/bestiary/{folder}/{stem}_\d+\.txt$", n)),
                           key=lambda n: int(re.search(r"_(\d+)\.txt$", n)[1]))
            if files:
                bestiary_add(z, files, lang, targets)


def bestiary_add(z, files, lang, targets):
    if True:
        raw = "\n".join(z.read(f).decode("utf-8-sig", "replace") for f in files)
        raw = re.sub(r"<[^>]*>", " ", raw)
        paras = []
        for block in re.split(r"\n\s*\n", raw):
            p = re.sub(r"\s+", " ", block).strip()
            if len(p) > 1:
                paras.append(p)
        merged = []  # the bestiary wraps lines by hand; join fragments that do not end a sentence
        for p in paras:
            if merged and not re.search(r"[.!?:»\")]$", merged[-1]):
                merged[-1] += " " + p
            else:
                merged.append(p)
        for t in targets:
            add(t, "item.iceandfire.bestiary", "", lang, merged)


# ------------------------------------------------------------------ Patchouli
def patchouli_text(s):
    s = s.replace("$(br2)", "\n\n").replace("$(p)", "\n\n").replace("$(br)", "\n")
    s = re.sub(r"\$\(li\)", "\n• ", s)
    s = re.sub(r"\$\(k:([a-z_.]+)\)", lambda m: "[" + m[1].split(".")[-1] + "]", s)
    s = re.sub(r"\$\([^)]*\)", "", s)
    return [re.sub(r"[ \t]+", " ", p).strip() for p in s.split("\n\n")]


def patchouli(z, title_of):
    names = set(z.namelist())
    entries = defaultdict(dict)  # (book, relpath) -> lang -> path
    for n in names:
        m = re.match(r"(?:assets|data)/([^/]+)/patchouli_books/([^/]+)/([a-z]{2}_[a-z]{2})/entries/(.+\.json)$", n)
        if m:
            entries[(m[1], m[2], m[4])][m[3]] = n
    for (ns, book, rel), langs in entries.items():
        for lang in ("uk", "en"):
            patchouli_entry(z, langs.get("uk_ua" if lang == "uk" else "en_us"), lang, title_of.get(book, book))


def patchouli_entry(z, path, lang, book_title):
    if path:
        try:
            d = json.loads(z.read(path).decode("utf-8-sig"))
        except ValueError:
            return
        paras, targets = [], set()
        icon = d.get("icon", "")
        if isinstance(icon, str) and re.match(r"^[a-z0-9_]+:[a-z0-9_/]+$", icon.split("{")[0]):
            targets.add("item:" + icon.split("{")[0])
        for p in d.get("pages", []):
            if p.get("title"):
                paras.append("## " + re.sub(r"\$\([^)]*\)", "", p["title"]))
            if p.get("text"):
                paras.extend(patchouli_text(p["text"]))
            for k in ("item",):
                v = p.get(k)
                if isinstance(v, str):
                    for one in v.split(","):
                        one = one.strip().split("{")[0]
                        if ":" in one:
                            targets.add("item:" + one)
            if p.get("entity"):
                targets.add("entity:" + str(p["entity"]).split("{")[0])
            for k in ("recipe", "recipe2"):
                if p.get(k) and recipe_out.get(p[k]):
                    targets.add("item:" + recipe_out[p[k]])
        title = re.sub(r"\$\([^)]*\)", "", d.get("name", ""))
        for t in targets:
            add(t, book_title, title, lang, paras, key=re.sub(r"/[a-z]{2}_[a-z]{2}/", "/", path))


# ------------------------------------------------------------------ AE2 guide (markdown, English)
def ae2_guide(z):
    for n in z.namelist():
        if not (n.startswith("assets/ae2/ae2guide/") and n.endswith(".md")):
            continue
        raw = z.read(n).decode("utf-8", "replace")
        m = re.match(r"---\n(.*?)\n---\n(.*)$", raw, re.S)
        if not m:
            continue
        ids = re.findall(r"^-\s*(ae2:[a-z0-9_]+)\s*$", m[1], re.M)
        if not ids:
            continue
        title = (re.search(r"title:\s*(.+)", m[1]) or [None, ""])[1].strip()
        body = m[2]
        body = re.sub(r"```.*?```", "", body, flags=re.S)
        body = re.sub(r"<(\w+)[^>]*>.*?</\1>", "", body, flags=re.S)
        body = re.sub(r"<[^>]+/?>", "", body)
        body = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", body)
        body = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", body)
        paras = []
        for block in re.split(r"\n\s*\n", body):
            block = block.strip()
            if not block:
                continue
            if block.startswith("#"):
                paras.append("## " + block.lstrip("#").strip())
            else:
                paras.append(re.sub(r"\s*\n\s*", " ", block))
        for i in ids:
            add("item:" + i, "AE2 Guide", title, "en", paras)


# ------------------------------------------------------------------ lang keys that describe a thing
DESC_TOKENS = re.compile(r"(^|[._])(desc\d*|description|tooltip\d*|info|lore|summary|hint|flavor|page\d+|journal)([._]|$)")


def desc_keys(lang):
    ids = {}
    for i in ITEMS | ENTITIES:
        ns, path = i.split(":", 1)
        ids.setdefault(path, []).append((ns, i))
    out = defaultdict(list)
    for key, val in lang.items():
        if not isinstance(val, str) or "%" in val or len(val) < 12 or not DESC_TOKENS.search(key):
            continue
        parts = key.split(".")
        if parts[0] in ("advancements", "advancement", "config", "configuration", "gui", "key", "subtitles", "death", "commands", "jei", "emi") \
                or "config" in key or "advancement" in key:
            continue
        for seg in parts:
            for cand in (seg, re.sub(r"\d+$", "", seg)):
                for ns, full in ids.get(cand, []):
                    if ns in key:
                        if key not in out[full]:
                            out[full].append(key)
    return out


def main():
    lang = {}
    title_of = {"apoth_chronicle": "book.apotheosis.name", "iss_guide_book": "Iron's Spells 'n Spellbooks Guide", "iceandfire": "item.iceandfire.bestiary"}
    for jar in JARS:
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                m = re.match(r"data/([^/]+)/(?:recipe|recipes)/(.+)\.json$", n)
                if m:
                    try:
                        r = json.loads(z.read(n))
                        res = r.get("result")
                        rid = res.get("id") or res.get("item") if isinstance(res, dict) else res if isinstance(res, str) else None
                        if rid:
                            recipe_out[f"{m[1]}:{m[2]}"] = rid
                    except Exception:
                        pass
                elif re.match(r"assets/[^/]+/lang/en_us\.json$", n):
                    try:
                        lang.update(json.loads(z.read(n).decode("utf-8-sig"), strict=False))
                    except ValueError:
                        pass
    for jar in JARS:
        with zipfile.ZipFile(jar) as z:
            names = z.namelist()
            if any(n.startswith("assets/alexsmobs/book/animal_dictionary/") for n in names):
                citadel(z, "assets/alexsmobs/book/animal_dictionary/", "item.alexsmobs.animal_dictionary")
            if any(n.startswith("assets/alexscaves/books/") for n in names):
                citadel(z, "assets/alexscaves/books/", "item.alexscaves.cave_book")
            if any(n.startswith("assets/iceandfire/lang/bestiary/") for n in names):
                bestiary(z)
            if any("patchouli_books/" in n for n in names):
                patchouli(z, title_of)
            if any(n.startswith("assets/ae2/ae2guide/") for n in names):
                ae2_guide(z)
    keys = desc_keys(lang)
    # advancements: their icon item gets "title: description" (progression hints in mods without a book)
    adv = defaultdict(list)
    for jar in JARS:
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if not re.match(r"data/[^/]+/advancements?/.+\.json$", n) or "/recipes/" in n:
                    continue
                try:
                    d = json.loads(z.read(n))
                except ValueError:
                    continue
                disp = d.get("display") or {}
                icon = disp.get("icon") or {}
                iid = icon.get("id") or icon.get("item") if isinstance(icon, dict) else None
                t, de = disp.get("title"), disp.get("description")
                tk = t.get("translate") if isinstance(t, dict) else None
                dk = de.get("translate") if isinstance(de, dict) else None
                if iid in ITEMS and tk and dk and dk in lang and [tk, dk] not in adv[iid]:
                    adv[iid].append([tk, dk])
    json.dump(adv, open(os.path.join(OUT, "advancements.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print("advancement hints for", len(adv), "items")
    os.makedirs(OUT, exist_ok=True)
    for v in books.values():
        for b in v:
            b.pop("_key", None)
    json.dump(books, open(os.path.join(OUT, "books.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    json.dump(keys, open(os.path.join(OUT, "desc_keys.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    by_book = defaultdict(int)
    for v in books.values():
        for b in v:
            by_book[(b["book"], "+".join(b["text"]))] += 1
    print("book pages for", len(books), "things:", dict(by_book))
    print("described by lang keys:", len(keys))


main()
