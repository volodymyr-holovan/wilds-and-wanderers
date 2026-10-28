"""Structure templates saved by Minecraft 1.21.2+ name entity attributes the new way ("minecraft:armor"); 1.21.1 knows
them as "minecraft:generic.armor" / "minecraft:player.block_break_speed" and logs "Ignoring unknown attribute" for every
mob such a structure places, which then keeps its default armour, speed and range. This writes corrected copies of
those templates into pack/overrides-server/kubejs/data (KubeJS data wins over the mod's own). Templates saved with Forge 1.20 still carry
"forge:entity_gravity" / "forge:step_height_addition", attributes NeoForge 1.21 dropped (their defaults are the vanilla
ones): those entries are removed. Run again after updating any mod with structures, so no stale copy stays behind.

  python pack/tools/fix_structure_attributes.py
"""
import glob, gzip, io, os, re, zipfile
import nbtlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
# server only and not in the repository: these are other mods' files (sync_overrides.py copies it to servers)
OUT = os.path.join(ROOT, "pack", "overrides-server", "kubejs", "data")
MODS = ["*.jar"]
PLAYER = {"block_break_speed", "block_interaction_range", "entity_interaction_range", "mining_efficiency", "sneaking_speed",
          "submerged_mining_speed", "sweeping_damage_ratio"}


def old_name(v):
    m = re.fullmatch(r"minecraft:([a-z_]+)", v)
    if not m:
        return None
    a = m.group(1)
    if a == "spawn_reinforcements":
        return "minecraft:zombie.spawn_reinforcements"
    return f"minecraft:{'player' if a in PLAYER else 'generic'}.{a}"


def fix(tag, in_attributes=False):
    changed = 0
    if isinstance(tag, nbtlib.Compound):
        for k, v in list(tag.items()):
            if in_attributes and k in ("id", "Name") and isinstance(v, nbtlib.String):
                new = old_name(str(v))
                if new:
                    tag[k] = nbtlib.String(new)
                    changed += 1
            else:
                changed += fix(v, k in ("attributes", "Attributes"))
    elif isinstance(tag, nbtlib.List):
        if in_attributes:
            keep = [v for v in tag if not (isinstance(v, nbtlib.Compound)
                                           and str(v.get("id", v.get("Name", ""))).startswith("forge:"))]
            if len(keep) != len(tag):
                changed += len(tag) - len(keep)
                del tag[:]
                tag.extend(keep)
        for v in tag:
            changed += fix(v, in_attributes)
    return changed


def main():
    total = files = 0
    per_mod = {}
    for pat in MODS:
        for jar in sorted(glob.glob(os.path.join(ROOT, "server", "mods", pat))):
            try:
                z = zipfile.ZipFile(jar)
            except zipfile.BadZipFile:
                continue
            for n in z.namelist():
                if not (n.startswith("data/") and n.endswith(".nbt") and "/structure/" in n):
                    continue
                raw = z.read(n)
                try:
                    raw = gzip.decompress(raw)
                except gzip.BadGzipFile:
                    pass                      # a few templates are stored uncompressed
                if b"ttributes" not in raw:   # nothing with attributes in it: leave the mod's file alone
                    continue
                try:
                    f = nbtlib.File.parse(io.BytesIO(raw))
                except Exception:
                    continue
                c = fix(f)
                if not c:
                    continue
                dst = os.path.join(OUT, *n.split("/")[1:])
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                buf = io.BytesIO()
                f.write(buf)
                open(dst, "wb").write(gzip.compress(buf.getvalue()))
                files += 1
                total += c
                per_mod[os.path.basename(jar)] = per_mod.get(os.path.basename(jar), 0) + 1
    for m, n in sorted(per_mod.items()):
        print(f"  {m}: {n}")
    print(f"{files} templates, {total} attribute entries fixed")


if __name__ == "__main__":
    main()
