"""Write manifest.json (path, size, sha1) for every file in a folder; used to upload a bundle file by file."""
import hashlib, json, os, sys

root = sys.argv[1]
out = []
for base, _, files in os.walk(root):
    for f in files:
        p = os.path.join(base, f)
        rel = os.path.relpath(p, root).replace(os.sep, "/")
        if rel == "manifest.json" or rel.startswith("_") or rel == "changed.json":
            continue
        out.append({"p": rel, "s": os.path.getsize(p), "h": hashlib.sha1(open(p, "rb").read()).hexdigest()})
json.dump(out, open(os.path.join(root, "manifest.json"), "w"))
print(len(out), "files,", sum(x["s"] for x in out) // 1024 // 1024, "MB")
