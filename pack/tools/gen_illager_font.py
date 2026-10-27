"""Cyrillic for Minecraft's two rune fonts: the illager runes (minecraft:illageralt) and the enchanting table's
Standard Galactic (minecraft:alt).

Books and texts written in these fonts (Iron's Spellbooks' untranslated Archevoker's Logbook, enchanting, books in
structures of other mods) showed empty squares in Ukrainian: the rune bitmaps only have Latin letters. This adds font
providers that draw every Ukrainian letter with the rune of the Latin letter it sounds like, so the text looks like the
intended cipher, as it does in English. Ukrainian punctuation the runes lack (dashes, guillemets, colons, ellipsis...)
is drawn with the nearest rune punctuation, or as a narrow gap where the font has none. (A fallback to the normal font does not work here: our providers come
before vanilla's, so it would take over the space and the Latin runes.)

Font providers of all resource packs are merged, so these files only add to the vanilla fonts.
The rune grids are read from the vanilla client jar.
  python gen_illager_font.py  ->  pack/overrides/kubejs/assets/minecraft/font/{illageralt,alt}.json
"""
import glob, json, os, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "overrides", "kubejs", "assets", "minecraft", "font")
JAR = glob.glob(os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "versions", "1.21.1", "1.21.1.jar"))[0]
LATIN = {"а": "a", "б": "b", "в": "v", "г": "h", "ґ": "g", "д": "d", "е": "e", "є": "e", "ж": "z", "з": "z", "и": "y", "і": "i",
         "ї": "i", "й": "j", "к": "k", "л": "l", "м": "m", "н": "n", "о": "o", "п": "p", "р": "r", "с": "s", "т": "t", "у": "u",
         "ф": "f", "х": "x", "ц": "c", "ч": "c", "ш": "w", "щ": "w", "ь": "q", "ю": "u", "я": "a", "ё": "e", "ы": "y", "э": "e",
         "ъ": "q", "'": ",", "’": ","}
# punctuation -> rune punctuation that exists (tried in order)
PUNCT = {"—": "-", "–": "-", "«": ",", "»": ",", "“": ",", "”": ",", "„": ",", '"': ",", "…": ".", ":": ".", ";": ",",
         "(": "-", ")": "-", "!": "!.", "?": "?."}


def cyrillic(font):
    vanilla = json.loads(zipfile.ZipFile(JAR).read(f"assets/minecraft/font/{font}.json"))
    bitmap = next(p for p in vanilla["providers"] if p["type"] == "bitmap")
    grid = bitmap["chars"]
    cell = {ch: (r, c) for r, row in enumerate(grid) for c, ch in enumerate(row) if ch != "\0"}
    pairs = []
    for cy, la in LATIN.items():
        for a, b in ((cy, la), (cy.upper(), la.upper())):
            # a letter the font lacks in this case: the other case's rune
            target = b if b in cell else b.swapcase() if b.swapcase() in cell else None
            if target and a not in cell and (a, target) not in pairs:
                pairs.append((a, target))
    blanks = {}
    for a, targets in PUNCT.items():
        target = next((t for t in targets if t in cell), None)
        if a in cell:
            continue
        if target:
            pairs.append((a, target))
        else:
            blanks[a] = 3   # a rune font without punctuation (Standard Galactic): a narrow gap instead of a square
    # a bitmap provider maps each cell to one character: letters sharing a rune go into separate layers
    layers = []
    for cy, la in pairs:
        for layer in layers:
            if cell[la] not in layer:
                layer[cell[la]] = cy
                break
        else:
            layers.append({cell[la]: cy})
    providers = []
    for layer in layers:
        rows = ["".join(layer.get((r, c), "\0") for c in range(len(grid[0]))) for r in range(len(grid))]
        providers.append({"type": "bitmap", "file": bitmap["file"], "ascent": bitmap["ascent"], "chars": rows})
    if blanks:
        providers.append({"type": "space", "advances": blanks})
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, font + ".json")
    json.dump({"providers": providers}, open(path, "w", encoding="utf-8"), ensure_ascii=True, indent=1)
    print(font, len(pairs), "characters in", len(providers), "providers ->", path)


for f in ("illageralt", "alt"):
    cyrillic(f)
