"""List mods that declare another installed mod as incompatible/discouraged (neoforge.mods.toml, incl. jar-in-jar)."""
import io, os, sys, tomllib, zipfile

def scan(zf, owner, mods, rules):
    names = set(zf.namelist())
    if "META-INF/neoforge.mods.toml" in names:
        try:
            meta = tomllib.loads(zf.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace"))
        except Exception:
            meta = {}
        for m in meta.get("mods", []):
            mods[m.get("modId")] = (m.get("version", "?"), owner)
        for mid, deps in (meta.get("dependencies") or {}).items():
            for d in deps:
                if d.get("type", "").lower() in ("incompatible", "discouraged"):
                    rules.append((owner, mid, d.get("modId"), d.get("versionRange", ""), d.get("type")))
    for n in names:
        if n.startswith("META-INF/jarjar/") and n.endswith(".jar"):
            with zipfile.ZipFile(io.BytesIO(zf.read(n))) as inner:
                scan(inner, owner + "!" + n.rsplit("/", 1)[-1], mods, rules)

mods, rules = {}, []
for d in sys.argv[1:]:
    for f in sorted(os.listdir(d)):
        if f.endswith(".jar"):
            with zipfile.ZipFile(os.path.join(d, f)) as z:
                scan(z, f, mods, rules)
for owner, mid, other, rng, kind in rules:
    if other in mods:
        print(f"{kind.upper()}: {mid} says {other} {rng or '(any)'} -- installed {mods[other][0]} ({mods[other][1]})")
print(len(rules), "rules checked")
