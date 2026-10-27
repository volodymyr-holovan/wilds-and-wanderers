"""Refresh the server upload bundle and list what changed since the last upload.

  python bundle_changes.py [deploy/server_upload]

Copies pack/overrides into the bundle (server side: no overrides-initial), rewrites manifest.json and writes
changed.json with the files that are new or different from the previous manifest. Upload only changed.json's
files with crafty_helpers.js (uploadLocal for each "p").
"""
import hashlib, json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "..", "..", "deploy", "server_upload")
man = os.path.join(root, "manifest.json")
old = {e["p"]: e["h"] for e in json.load(open(man))} if os.path.exists(man) else {}
subprocess.run([sys.executable, "-I", os.path.join(HERE, "sync_overrides.py"), root, "--no-initial"], check=True)
subprocess.run([sys.executable, "-I", os.path.join(HERE, "make_manifest.py"), root], check=True)
new = json.load(open(man))
changed = [e for e in new if old.get(e["p"]) != e["h"]]
json.dump(changed, open(os.path.join(root, "changed.json"), "w"))
print(len(changed), "changed files,", sum(e["s"] for e in changed) // 1024, "KB")
for e in changed:
    print("  ", e["p"])
