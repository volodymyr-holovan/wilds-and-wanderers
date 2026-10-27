"""Check Modrinth projects for a NeoForge 1.21.1 build.  python mr_check.py slug [slug ...] [--body]"""
import json, sys, urllib.parse, urllib.request

def get(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "wilds-wanderers-pack/1.0"}), timeout=30))

body = "--body" in sys.argv
for slug in [a for a in sys.argv[1:] if not a.startswith("--")]:
    try:
        p = get(f"https://api.modrinth.com/v2/project/{slug}")
        vs = get(f"https://api.modrinth.com/v2/project/{slug}/version?" + urllib.parse.urlencode({"loaders": '["neoforge"]', "game_versions": '["1.21.1"]'}))
    except Exception as e:
        print(f"== {slug}: {e}"); continue
    print(f"== {slug} | {p['title']} | {p['client_side']}/{p['server_side']} | dl {p['downloads']} | updated {p['updated'][:10]} | {p.get('license',{}).get('id')}")
    if not vs:
        print("   NO neoforge 1.21.1 version"); continue
    v = vs[0]
    deps = [(d.get("project_id"), d["dependency_type"]) for d in v["dependencies"]]
    names = []
    for pid, t in deps:
        if t in ("required", "incompatible") and pid:
            try:
                names.append(f"{get('https://api.modrinth.com/v2/project/' + pid)['slug']}:{t}")
            except Exception:
                names.append(f"{pid}:{t}")
    print(f"   {v['version_number']} ({v['date_published'][:10]}) {v['files'][0]['filename']}  deps: {', '.join(names) or '-'}")
    if body:
        print("   " + p["body"][:1500].replace("\n", "\n   "))
