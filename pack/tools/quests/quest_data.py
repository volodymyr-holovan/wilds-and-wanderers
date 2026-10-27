"""Facts the quest generator writes into descriptions, read once from the mod jars and the wiki's generated data:
the lang key of every item and mob (so a description shows the name in the player's language), how each item is made
(recipe types) and where it comes from (mob drops, chests of which structures, trades, natural generation).

Cached in pack/tools/quests/.cache/quest_data.json; delete it after the mods change.
"""
import glob, json, os, re, zipfile
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
CACHE = os.path.join(HERE, ".cache", "quest_data.json")
GEN = os.path.join(ROOT, "mods-src", "wilds_wiki", "src", "main", "resources", "assets", "wilds_wiki", "generated")
VANILLA = os.path.join(os.environ["APPDATA"], ".minecraft", "versions", "1.21.1", "1.21.1.jar")

# how a recipe type reads in a description; types not listed read as "<mod>: <type>"
RECIPE_NAMES = {
    "minecraft:crafting_shaped": "верстак", "minecraft:crafting_shapeless": "верстак", "minecraft:smelting": "піч",
    "minecraft:blasting": "доменна піч", "minecraft:smoking": "коптильня", "minecraft:campfire_cooking": "багаття",
    "minecraft:stonecutting": "каменеріз", "minecraft:smithing_transform": "ковальський стіл", "minecraft:smithing_trim": "ковальський стіл",
    "create:mixing": "Create: змішувач", "create:pressing": "Create: прес", "create:crushing": "Create: дробарка",
    "create:milling": "Create: жорна", "create:cutting": "Create: пилка", "create:deploying": "Create: розміщувач",
    "create:filling": "Create: наповнювач", "create:emptying": "Create: спорожнювач", "create:compacting": "Create: пресування в чані",
    "create:mechanical_crafting": "Create: механічний верстак", "create:sequenced_assembly": "Create: послідовне складання",
    "create:haunting": "Create: вентилятор + вогонь душ", "create:splashing": "Create: вентилятор + вода",
    "create:item_application": "Create: застосування предмета", "create:sandpaper_polishing": "Create: наждачний папір",
    "mekanism:enriching": "Mekanism: збагачувач", "mekanism:crushing": "Mekanism: подрібнювач", "mekanism:combining": "Mekanism: поєднувач",
    "mekanism:metallurgic_infusing": "Mekanism: металургійний наповнювач", "mekanism:purifying": "Mekanism: очищувальна камера",
    "mekanism:injecting": "Mekanism: камера хімічного впорскування", "mekanism:compressing": "Mekanism: компресор осмію",
    "mekanism:sawing": "Mekanism: точна лісопилка", "mekanism:chemical_infusing": "Mekanism: хімічний наповнювач",
    "mekanism:crystallizing": "Mekanism: хімічний кристалізатор", "mekanism:pigment_extracting": "Mekanism: екстрактор пігменту",
    "ae2:inscriber": "AE2: висікач", "ae2:charger": "AE2: зарядник", "ae2:transform": "AE2: перетворення у воді",
    "ars_nouveau:enchanting_apparatus": "Ars: зачаровувальний апарат", "ars_nouveau:imbuement": "Ars: камера насичення",
    "ars_nouveau:glyph": "Ars: стіл писаря", "ars_nouveau:crush": "Ars: заклинання «Подрібнення»",
    "irons_spellbooks:alchemist_cauldron_brew": "Iron's: казан алхіміка", "irons_spellbooks:alchemist_cauldron_empty": "Iron's: казан алхіміка",
    "farmersdelight:cooking": "Farmer's Delight: каструля", "farmersdelight:cutting": "Farmer's Delight: обробна дошка",
    "twilightforest:uncrafting": "Сутінковий ліс: розкрафтовувальний стіл", "iceandfire:dragonforge": "Ice and Fire: драконяча кузня",
}


def _lang(z, ns):
    for loc in ("en_us",):
        try:
            return json.loads(z.read(f"assets/{ns}/lang/{loc}.json").decode("utf-8-sig"), strict=False)
        except (KeyError, ValueError):
            return {}


def build():
    keys = set()
    recipes = defaultdict(set)
    jars = sorted(glob.glob(os.path.join(ROOT, "client", "mods", "*.jar")) + glob.glob(os.path.join(ROOT, "server", "mods", "*.jar")))
    for jar in [VANILLA] + jars:
        try:
            z = zipfile.ZipFile(jar)
        except (OSError, zipfile.BadZipFile):
            continue
        for n in z.namelist():
            m = re.match(r"assets/([^/]+)/lang/en_us\.json$", n)
            if m:
                try:
                    keys |= set(json.loads(z.read(n).decode("utf-8-sig"), strict=False))
                except ValueError:
                    pass
                continue
            m = re.match(r"data/([^/]+)/recipes?/(.+)\.json$", n)
            if m:
                try:
                    r = json.loads(z.read(n))
                except ValueError:
                    continue
                t = r.get("type", "")
                res = r.get("result") or r.get("output") or (r.get("results") or [None])[0]
                if isinstance(res, dict):
                    rid = res.get("id") or res.get("item")
                elif isinstance(res, str):
                    rid = res
                else:
                    rid = None
                if isinstance(rid, str) and ":" in rid and t:
                    recipes[rid].add(t)
    kube = os.path.join(ROOT, "pack", "overrides", "kubejs", "data")
    for f in glob.glob(os.path.join(kube, "*", "recipe", "**", "*.json"), recursive=True):
        try:
            r = json.load(open(f, encoding="utf-8"))
            res = r.get("result")
            rid = res.get("id") if isinstance(res, dict) else res
            if rid:
                recipes[rid].add(r.get("type", ""))
        except (ValueError, AttributeError):
            pass
    data = {"keys": sorted(keys), "recipes": {k: sorted(v) for k, v in recipes.items()}}
    os.makedirs(os.path.dirname(CACHE), exist_ok=True)
    json.dump(data, open(CACHE, "w", encoding="utf-8"))
    return data


class Facts:
    def __init__(self):
        d = json.load(open(CACHE, encoding="utf-8")) if os.path.exists(CACHE) else build()
        self.keys = set(d["keys"])
        self.recipes = d["recipes"]
        self.loot = json.load(open(os.path.join(GEN, "loot.json"), encoding="utf-8")) if os.path.exists(os.path.join(GEN, "loot.json")) else {"sources": {}}
        self.world = json.load(open(os.path.join(GEN, "world.json"), encoding="utf-8")) if os.path.exists(os.path.join(GEN, "world.json")) else {}

    def item_key(self, item):
        ns, path = item.split(":", 1)
        for k in (f"block.{ns}.{path}", f"item.{ns}.{path}"):
            if k in self.keys:
                return k
        return f"item.{ns}.{path}"

    @staticmethod
    def entity_key(e):
        ns, path = e.split(":", 1)
        return f"entity.{ns}.{path}"

    @staticmethod
    def structure_key(s):
        ns, path = s.split(":", 1)
        return f"wilds_wiki.structure.{ns}.{path.replace('/', '.')}"

    @staticmethod
    def biome_key(b):
        ns, path = b.split(":", 1)
        return f"biome.{ns}.{path.replace('/', '.')}"

    def made_in(self, item):
        """the stations an item is made in, as readable names, most common first"""
        out = []
        for t in self.recipes.get(item, []):
            name = RECIPE_NAMES.get(t)
            if not name:
                ns, _, path = t.partition(":")
                name = f"{ns}: {path.replace('_', ' ')}"
            if name not in out:
                out.append(name)
        return out

    def sources(self, item):
        """{"mobs": [...], "structures": [...], "biomes": [...], "trades": bool, "skeleton": [...]}"""
        out = {"mobs": [], "skeleton": [], "structures": [], "biomes": [], "trades": False}
        for kind, src in self.loot.get("sources", {}).get(item, []):
            if kind == "entity" and src not in out["mobs"]:
                out["mobs"].append(src)
            elif kind == "skeleton" and src not in out["skeleton"]:
                out["skeleton"].append(src)
        w = self.world
        out["structures"] = list(w.get("item_structures", {}).get(item, []))
        out["biomes"] = list(w.get("block_biomes", {}).get(item, []))
        out["trades"] = item in w.get("trades", {})
        return out


if __name__ == "__main__":
    d = build()
    print(len(d["keys"]), "lang keys,", len(d["recipes"]), "items with recipes ->", CACHE)
