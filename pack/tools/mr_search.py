"""Search Modrinth for NeoForge 1.21.1 mods.  python mr_search.py "query" ["query" ...] [--n 8]"""
import json, sys, urllib.parse, urllib.request

n = 8
args = [a for a in sys.argv[1:]]
if "--n" in args:
    i = args.index("--n"); n = int(args[i + 1]); del args[i:i + 2]
facets = json.dumps([["categories:neoforge"], ["versions:1.21.1"], ["project_type:mod"]])
for q in args:
    url = "https://api.modrinth.com/v2/search?" + urllib.parse.urlencode({"query": q, "facets": facets, "limit": n, "index": "relevance"})
    req = urllib.request.Request(url, headers={"User-Agent": "wilds-wanderers-pack/1.0"})
    hits = json.load(urllib.request.urlopen(req, timeout=30))["hits"]
    print(f"== {q}")
    for h in hits:
        print(f"  {h['slug']:<34} {h['downloads']:>10}  {h['client_side'][:3]}/{h['server_side'][:3]}  {h['title']} — {h['description'][:110]}")
