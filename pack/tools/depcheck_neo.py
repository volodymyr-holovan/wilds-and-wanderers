"""Check NeoForge mod dependencies in a mods folder (neoforge.mods.toml, incl. jar-in-jar)."""
import io, os, sys, tomllib, zipfile

SIDE = sys.argv[2].upper() if len(sys.argv) > 2 else "SERVER"   # SERVER or CLIENT
BUILTIN = {"minecraft", "neoforge", "forge", "java", "javafml", "mixinextras"}


def scan(zf, provided, needs, owner):
    names = set(zf.namelist())
    toml = "META-INF/neoforge.mods.toml" if "META-INF/neoforge.mods.toml" in names else (
        "META-INF/mods.toml" if "META-INF/mods.toml" in names else None)
    if toml:
        try:
            meta = tomllib.loads(zf.read(toml).decode("utf-8", "replace"))
        except Exception as e:
            print(f"  WARN cannot parse {owner}:{toml}: {e}")
            meta = {}
        for mod in meta.get("mods", []):
            provided.setdefault(mod.get("modId"), owner)
        for mid, deps in (meta.get("dependencies") or {}).items():
            for d in deps:
                required = d.get("type", "").lower() == "required" or d.get("mandatory") is True
                side = d.get("side", "BOTH").upper()
                if required and side in ("BOTH", SIDE):
                    needs.append((owner, mid, d.get("modId"), d.get("versionRange", "")))
    for n in names:
        if n.startswith("META-INF/jarjar/") and n.endswith(".jar"):
            try:
                with zipfile.ZipFile(io.BytesIO(zf.read(n))) as inner:
                    scan(inner, provided, needs, owner + "!" + n.rsplit("/", 1)[-1])
            except zipfile.BadZipFile:
                pass


provided, needs, bad = {}, [], []
for fn in sorted(os.listdir(sys.argv[1])):
    if not fn.endswith(".jar"):
        continue
    try:
        with zipfile.ZipFile(os.path.join(sys.argv[1], fn)) as z:
            scan(z, provided, needs, fn)
    except zipfile.BadZipFile:
        bad.append(fn)

missing = [n for n in needs if n[2] not in provided and n[2] not in BUILTIN]
for owner, mid, dep, ver in missing:
    print(f"MISSING: {mid} ({owner}) needs {dep} {ver}")
# duplicate mod ids across top-level jars = guaranteed crash
tops = {}
for fn in sorted(os.listdir(sys.argv[1])):
    if fn.endswith(".jar"):
        try:
            z = zipfile.ZipFile(os.path.join(sys.argv[1], fn))
            t = "META-INF/neoforge.mods.toml" if "META-INF/neoforge.mods.toml" in z.namelist() else None
            if t:
                for mod in tomllib.loads(z.read(t).decode("utf-8", "replace")).get("mods", []):
                    tops.setdefault(mod.get("modId"), []).append(fn)
        except Exception:
            pass
for mid, fns in tops.items():
    if len(fns) > 1:
        print(f"DUPLICATE modId {mid}: {fns}")
print(f"BAD ZIP: {bad}" if bad else "all jars valid")
print(f"{len(provided)} mod ids, {len(needs)} required deps, {len(missing)} missing")
