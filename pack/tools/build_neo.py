"""Resolve and download the Wilds & Wanderers NeoForge 1.21.1 pack (Modrinth + CurseForge)."""
import hashlib, json, os, sys, urllib.parse, urllib.request

MR = "https://api.modrinth.com/v2"
GV, LOADER = "1.21.1", "neoforge"
UA = {"User-Agent": "wilds-and-wanderers-pack/2.0"}
ROOT = sys.argv[1]
# reproducible: a project already in docs/mods.json keeps that exact file; --upgrade resolves everything afresh
UPGRADE = "--upgrade" in sys.argv
PINNED = {}
if not UPGRADE and os.path.exists(os.path.join(ROOT, "docs", "mods.json")):
    PINNED = {m["slug"]: m for m in json.load(open(os.path.join(ROOT, "docs", "mods.json"), encoding="utf-8"))}

# Placement: "both" (server + client), "server", "client", "opt-client"
MODRINTH = {
    # --- technology ---
    "create": "both", "create-enchantment-industry": "both", "createaddition": "both", "copycats": "both",
    "mekanism": "both", "mekanism-generators": "both", "mekanism-tools": "both",
    "create-aeronautics": "both", "sable": "both",
    "ae2": "both", "applied-energistics-2-wireless-terminals": "both", "applied-mekanistics": "both",
    # --- magic & RPG ---
    "ars-nouveau": "both", "irons-spells-n-spellbooks": "both", "ars-n-spells": "both",
    "apotheosis": "both", "better-combat": "both",
    "skills": "both",
    # --- adventure, bosses, creatures ---
    "aether": "both", "l_enders-cataclysm": "both", "mowzies-mobs": "both",
    "iceandfire-ce": "both", "iceandfire_create": "both",
    "alexs-caves-continued": "both", "alexs-mobs-continued": "both", "friends-and-foes-forge": "both",
    # --- quests & NPCs --- (quest givers are Wilds Core's own; Easy NPC and Questborne were removed on 27.10 so that
    # the world has one kind of NPC with one look, one dialogue and one quest book)
    "bountiful": "both",
    # --- one equipment system: Accessories (Aether's) with Curios routed into it, so every ring, belt, glove, spell
    # book and backpack slot is in one screen and an item of any mod fits the slot of its kind ---
    "accessories": "both", "accessories-compat-layer": "both",
    # --- world generation ---
    "terralith": "both", "tectonic": "both", "lithostitched": "both", "towns-and-towers": "both",
    "dungeons-and-taverns": "both", "yungs-better-dungeons": "both", "yungs-better-mineshafts": "both",
    "yungs-better-strongholds": "both",
    # --- survival life ---
    "farmers-delight": "both", "serene-seasons": "both", "glitchcore": "both",
    "waystones": "both", "lootr": "both", "corpse": "both",
    # --- gathering: tree felling (hired lumberjacks and miners come from MineColonies, below) ---
    "fallingtree": "both",
    "sophisticated-backpacks": "both", "sophisticated-storage": "both",
    # libraries that the jars require but Modrinth does not list as dependencies
    "kambrik": "both", "kotlin-for-forge": "both", "irons-lib": "both", "architectury-api": "both",
    # --- pack infrastructure ---
    "kubejs": "both", "kubejs-create": "both", "kubejs-mekanism": "both", "almostunified": "both",
    "open-parties-and-claims": "both",
    # --- server administration ---
    "grieflogger": "server", "glra": "server", "easylogin": "server",
    "skinrestorer": "server", "simple-trading": "server", "chunky": "server",
    # --- performance ---
    "modernfix": "both", "ferrite-core": "both", "fastsuite": "both", "clumps": "both", "spark": "both",
    "structure-layout-optimizer": "server",
    "sodium": "client", "sodium-extra": "client", "reeses-sodium-options": "client",
    "immediatelyfast": "client", "entityculling": "client", "moreculling": "client", "dynamic-fps": "client",
    # smooth frame times on weak PCs (27.10): micro-optimizations of the client tick, particle culling, faster world
    # joining. ScalableLux is left out: Sable (Create Aeronautics) refuses to load with it
    "badoptimizations": "client", "particle-core": "client", "ksyxis": "both",
    # --- interface ---
    "jei": "both", "jade": "both", "appleskin": "both", "xaeros-minimap": "both", "xaeros-world-map": "both",
    "inventory-essentials": "both", "shulkerboxtooltip": "both",
    "mouse-tweaks": "client", "controlling": "client", "chat-heads": "client",
    # finding the way: quests point players at these instead of coordinates
    "explorers-compass": "both", "natures-compass": "both",
    # epic fantasy/celtic soundtrack (client only)
    "reactive-music": "client",
    # in-game web browser (Home key): a picture-in-picture Chromium window over the game; MCEF fetches Chromium on first start
    "mcef": "client", "minechromium": "client",
    # --- living world (24.10): ores that blend in, ground details, wildlife, ~hundreds of new structures ---
    "seamless-ores": "both", "gew2": "both", "naturalist": "both",
    "moogs-voyager-structures": "server", "mos-moogs-ocean-structures": "server", "moogs-structure-lib": "server",
    "when-dungeons-arise": "server", "when-dungeons-arise-seven-seas": "server", "ct-overhaul-village": "server",
    "tidal-towns": "server", "hopo-better-underwater-ruins": "server", "yungs-better-ocean-monuments": "server",
    "ocean-lily-pad-village": "both",
    # seas: creatures, plants, fish, food, ships
    "hybrid-aquatic": "both", "fish-of-thieves": "both", "upgrade-aquatic": "both", "oceans-delight": "both",
    "small-ships": "both",
    # fantasy peoples: kobolds, goblins, orcs with their villages and huts
    "kobolds": "both", "goblins-tyranny": "both", "orc-hut": "both",
    # villages: guards, new professions, data-driven trades with modded items, names, villager handling
    "guard-villagers": "both", "villagersplus": "both", "villagerstradingplus": "both", "villager-names-serilum": "both",
    "chefs-delight": "both", "easy-villagers": "both",
    # simple camps: tents that are placed from an item, sleeping bags and hammocks
    "simply-tents": "both", "comforts": "both",
    # pack scripting: loot tables and villager trades from KubeJS
    "lootjs": "server", "morejs": "both",
    # faster chunk generation on a weak server
    "lithium": "both", "noisiumforked": "server", "c2me-neoforge": "server",
    # --- the Nether and the End (28.10): Nether biomes, castles and weapons (Incendium), manors, citadel and new mobs
    # (Eternal Nether), rebuilt fortresses, lava fishing, Nether fruit; the End with four biomes, mobs, structures and
    # music (Enderscape) and a new dragon island ---
    "incendium": "both", "eternal-nether": "both", "yungs-better-nether-fortresses": "server",
    "nether-depths-upgrade": "both", "nethers-exoticism": "both",
    "enderscape": "both", "yungs-better-end-island": "server",
    # --- optional for stronger PCs ---
    "distanthorizons": "client", "iris": "client",
}

# CurseForge-only projects: project id -> placement (files come from the public CFWidget API)
CURSEFORGE = {
    227639: ("twilightforest", "both"),
    289412: ("ftb-quests", "both"), 404468: ("ftb-teams", "both"), 404465: ("ftb-library", "both"),
    410811: ("ftb-essentials", "server"),
    238794: ("nomadic-tents", "both"),
    # colonies with hired workers: forester, miner, farmer, builder... and their libraries
    245506: ("minecolonies", "both"), 298744: ("structurize", "both"), 522992: ("blockui", "both"),
    527361: ("domum-ornamentum", "both"), 303278: ("multi-piston", "both"),
}


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.load(r)


def fetch(url, attempts=5):
    """Download with retries: CDNs occasionally reset long transfers."""
    import time
    for i in range(attempts):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"}), timeout=600) as r:
                return r.read()
        except Exception as e:
            if i == attempts - 1:
                raise
            print("retry", url.rsplit("/", 1)[-1], e)
            time.sleep(3 * (i + 1))


def pick_mr(project):
    q = urllib.parse.urlencode({"loaders": f'["{LOADER}"]', "game_versions": f'["{GV}"]'})
    vs = get(f"{MR}/project/{project}/version?{q}")
    for t in ("release", "beta", "alpha"):
        for v in vs:
            if v["version_type"] == t:
                return v
    return None


def targets(where):
    return {"both": ["server/mods", "client/mods"], "server": ["server/mods"],
            "client": ["client/mods"], "opt-client": ["optional/client-mods"]}[where]


def merge(a, b):
    places = set(targets(a)) | set(targets(b))
    if "server/mods" in places and "client/mods" in places:
        return "both"
    if "server/mods" in places:
        return "server"
    return "client" if "client/mods" in places else "opt-client"


resolved, missing = {}, []
queue = list(MODRINTH.items())
while queue:
    ref, where = queue.pop(0)
    try:
        p = get(f"{MR}/project/{ref}")
    except Exception:
        missing.append(ref); continue
    slug = p["slug"]
    if slug in resolved:
        if resolved[slug]["where"] != where:
            resolved[slug]["where"] = merge(resolved[slug]["where"], where)
        continue
    if slug in PINNED and PINNED[slug]["source"] == "modrinth":
        resolved[slug] = dict(PINNED[slug], where=where, dependency=slug not in MODRINTH)
        try:
            pv = get(f"{MR}/version_file/{PINNED[slug]['sha1']}?algorithm=sha1")
            for d in pv["dependencies"]:
                if d["dependency_type"] == "required" and d.get("project_id"):
                    queue.append((d["project_id"], where.replace("opt-client", "client") if where != "opt-client" else "opt-client"))
        except Exception:
            pass
        continue
    v = pick_mr(p["id"])
    if not v:
        missing.append(slug); continue
    f = next((x for x in v["files"] if x["primary"]), v["files"][0])
    resolved[slug] = {"source": "modrinth", "title": p["title"], "slug": slug, "version": v["version_number"],
                      "type": v["version_type"], "client_side": p["client_side"], "server_side": p["server_side"],
                      "where": where, "dependency": slug not in MODRINTH, "file": f["filename"], "url": f["url"],
                      "sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"], "size": f["size"]}
    for d in v["dependencies"]:
        if d["dependency_type"] == "required" and d.get("project_id"):
            queue.append((d["project_id"], where.replace("opt-client", "client") if where != "opt-client" else "opt-client"))

for pid, (slug, where) in CURSEFORGE.items():
    if slug in PINNED and PINNED[slug]["source"] == "curseforge":
        resolved[slug] = dict(PINNED[slug], where=where)
        continue
    d = get(f"https://api.cfwidget.com/{pid}")
    fs = [f for f in d["files"] if GV in f["versions"] and "NeoForge" in f["versions"]]
    rel = [f for f in fs if f["type"] == "release"] or fs
    rel.sort(key=lambda f: f["uploaded_at"], reverse=True)
    if not rel:
        missing.append(f"cf:{slug}"); continue
    f = rel[0]
    url = f"https://mediafilez.forgecdn.net/files/{f['id'] // 1000}/{f['id'] % 1000}/{urllib.parse.quote(f['name'])}"
    resolved[slug] = {"source": "curseforge", "title": d["title"], "slug": slug, "cf_project": pid, "cf_file": f["id"],
                      "version": f.get("display", f["name"]), "type": f["type"], "where": where, "dependency": False,
                      "file": f["name"], "url": url, "sha1": None, "sha512": None, "size": f.get("filesize")}

for m in sorted(resolved.values(), key=lambda m: m["slug"]):
    print(f'{m["source"][:2]} {m["slug"]:44} {str(m["version"])[:34]:34} {m["type"]:7} {m["where"]:10} dep={m["dependency"]}')
print("MISSING:", missing)

# download (verify Modrinth by sha1; record sha1/sha512 for CurseForge files we fetched ourselves)
for m in resolved.values():
    data = None
    for t in targets(m["where"]):
        d = os.path.join(ROOT, t); os.makedirs(d, exist_ok=True)
        path = os.path.join(d, m["file"])
        if os.path.exists(path) and (m["sha1"] is None or hashlib.sha1(open(path, "rb").read()).hexdigest() == m["sha1"]):
            if m["sha1"] is None:
                data = open(path, "rb").read()
            continue
        if data is None:
            data = fetch(m["url"])
            if m["sha1"] and hashlib.sha1(data).hexdigest() != m["sha1"]:
                raise SystemExit(f"hash mismatch {m['file']}")
        open(path, "wb").write(data)
    if m["sha1"] is None and data is not None:
        m["sha1"] = hashlib.sha1(data).hexdigest(); m["sha512"] = hashlib.sha512(data).hexdigest(); m["size"] = len(data)

# remove jars that are no longer part of the pack
keep = {(t, m["file"]) for m in resolved.values() for t in targets(m["where"])}
for t in ("server/mods", "client/mods", "optional/client-mods"):
    d = os.path.join(ROOT, t)
    if os.path.isdir(d):
        for fn in os.listdir(d):
            # wilds_*.jar are the pack's own mods (mods-src/), built locally, not downloaded
            if fn.endswith(".jar") and (t, fn) not in keep and not fn.startswith("wilds_"):
                os.remove(os.path.join(d, fn)); print("removed stale", t, fn)

os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)
with open(os.path.join(ROOT, "docs", "mods.json"), "w", encoding="utf-8") as fh:
    json.dump(sorted(resolved.values(), key=lambda m: m["slug"]), fh, indent=1, ensure_ascii=False)
print("done", len(resolved), "projects")
