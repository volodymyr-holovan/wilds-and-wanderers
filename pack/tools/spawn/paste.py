"""Turn a downloaded spawn build into a datapack of functions that sets it into the terrain of a world.

  python paste.py <schematic> <world dir> <center x> <center z> [--rotate 0|90|180|270] [--base Y] [--ring 16]
         [--spawn dx,dy,dz]   (the spawn point relative to the schematic's corner, default its middle)

Reads the ground heights around the place straight from the world's region files (the chunks must exist: force-load
them first), then writes <world>/datapacks/wilds_spawn_build/ with
  wilds_spawn_build:prepare  - clears the place and shapes the ground: under the build the ground is brought to the
                               build's base, around it a ring of slopes leads back to the natural ground
  wilds_spawn_build:build_N  - the build itself, bottom-up, solid blocks first and then what hangs on them
                               (torches, plants, signs...), so nothing falls off
  wilds_spawn_build:finish   - signs and other block entities, then drops are cleared
and prints the commands to run and the area the build covers (for the spawn protection radius).

Blocks this Minecraft (1.21.1) does not have are replaced (see SUBSTITUTE); leaves are made persistent so they never
decay away from their logs.
"""
import argparse, json, math, os, re, sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from schem import Schematic          # noqa: E402
from seed_search import surface      # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))

# blocks newer than 1.21.1 -> what this version has
SUBSTITUTE = {
    "minecraft:pale_oak_trapdoor": "minecraft:birch_trapdoor",
    "minecraft:pale_oak_wall_sign": "minecraft:birch_wall_sign",
    "minecraft:pale_oak_sign": "minecraft:birch_sign",
    "minecraft:pale_oak_shelf": "minecraft:chiseled_bookshelf",
    "minecraft:warped_shelf": "minecraft:chiseled_bookshelf",
    "minecraft:waxed_oxidized_copper_chain": "minecraft:chain",
    "minecraft:pale_oak_planks": "minecraft:birch_planks",
    "minecraft:pale_oak_log": "minecraft:birch_log",
    "minecraft:pale_oak_leaves": "minecraft:birch_leaves",
    "minecraft:pale_moss_block": "minecraft:moss_block",
    "minecraft:pale_moss_carpet": "minecraft:moss_carpet",
}
# properties a substitute does not have (dropped) / must have
DROP_PROPS = {"minecraft:chiseled_bookshelf": {"powered", "side_chain", "waterlogged"},
              "minecraft:chain": set()}
SHELF_SLOTS = {f"slot_{i}_occupied": "false" for i in range(6)}

# what hangs on, stands on or needs other blocks: placed after everything solid
FRAGILE = re.compile(r"torch|lantern|candle|sign|banner|button|lever|pressure_plate|carpet|rail|ladder|vine|chain|"
                     r"flower|_sapling|short_grass|tall_grass|fern|bush|mushroom|dandelion|poppy|orchid|allium|bluet|"
                     r"tulip|daisy|cornflower|lily|rose|peony|lilac|sunflower|pitcher|torchflower|azalea$|moss_carpet|"
                     r"snow$|door|bed|flower_pot|potted|head|skull|wall_|cobweb|hanging_roots|spore_blossom|"
                     r"dripleaf|glow_lichen|sculk_vein|amethyst_cluster|_bud|pointed_dripstone|seagrass|kelp|sugar_cane|"
                     r"bamboo|cactus|sweet_berry|cave_vines|weeping|twisting|crop|wheat|carrots|potatoes|beetroots|"
                     r"stem|lichen|tripwire|string|redstone_wire|repeater|comparator|item_frame|campfire|bell")
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void"}


def fix_state(state):
    name, _, props = state.partition("[")
    props = props.rstrip("]")
    kv = dict(p.split("=", 1) for p in props.split(",") if p) if props else {}
    if name in SUBSTITUTE:
        name = SUBSTITUTE[name]
        for k in DROP_PROPS.get(name, ()):
            kv.pop(k, None)
        if name == "minecraft:chiseled_bookshelf":
            kv = {"facing": kv.get("facing", "north"), **SHELF_SLOTS}
        if name == "minecraft:chain":
            kv = {"axis": kv.get("axis", "y"), "waterlogged": kv.get("waterlogged", "false")}
    if name.endswith("_leaves") or name == "minecraft:azalea_leaves" or name == "minecraft:flowering_azalea_leaves":
        kv["persistent"] = "true"
    return name + ("[" + ",".join(f"{k}={v}" for k, v in sorted(kv.items())) + "]" if kv else "")


ROT_FACING = {"north": "east", "east": "south", "south": "west", "west": "north"}


def rotate_state(state, turns):
    """turns x 90 degrees clockwise (seen from above)"""
    if turns % 4 == 0:
        return state
    name, _, props = state.partition("[")
    if not props:
        return state
    kv = dict(p.split("=", 1) for p in props.rstrip("]").split(","))
    for _ in range(turns % 4):
        if "facing" in kv and kv["facing"] in ROT_FACING:
            kv["facing"] = ROT_FACING[kv["facing"]]
        if "axis" in kv and kv["axis"] in ("x", "z"):
            kv["axis"] = "z" if kv["axis"] == "x" else "x"
        if "rotation" in kv:
            kv["rotation"] = str((int(kv["rotation"]) + 4) % 16)
        sides = {d: kv.get(d) for d in ("north", "east", "south", "west")}
        if any(v is not None for v in sides.values()):
            for d in sides:
                kv.pop(d, None)
            for d, v in sides.items():
                if v is not None:
                    kv[ROT_FACING[d]] = v
        if "shape" in kv and kv["shape"] in ("north_south", "east_west"):
            kv["shape"] = "east_west" if kv["shape"] == "north_south" else "north_south"
    return name + "[" + ",".join(f"{k}={v}" for k, v in sorted(kv.items())) + "]"


def snbt(tag):
    return tag.snbt()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("schematic"); ap.add_argument("world")
    ap.add_argument("cx", type=int); ap.add_argument("cz", type=int)
    ap.add_argument("--rotate", type=int, default=0)
    ap.add_argument("--base", type=int)
    ap.add_argument("--ring", type=int, default=32, help="the widest slope around the build")
    ap.add_argument("--sink", type=int, default=0, help="the schematic layer that is ground level (default 0)")
    a = ap.parse_args()

    s = Schematic.load(a.schematic)
    blocks, palette = s.blocks, [fix_state(p) for p in s.palette]
    turns = (a.rotate // 90) % 4
    bes = dict(s.block_entities)
    for _ in range(turns):           # rotate the grid clockwise: new[x', z'] = old[x, z] with x' = L-1-z, z' = x
        H, L, W = blocks.shape
        blocks = np.rot90(blocks, k=-1, axes=(1, 2)).copy()  # axes (z, x)
        bes = {(L - 1 - z, y, x): v for (x, y, z), v in bes.items()}
    palette = [rotate_state(p, turns) for p in palette]
    H, L, W = blocks.shape
    solid = ~np.array([p.split("[")[0] in AIR for p in palette])[blocks]
    foot = solid.any(axis=0)                                   # [z, x]
    x0, z0 = a.cx - W // 2, a.cz - L // 2                      # world corner of the grid
    R = a.ring
    region = os.path.join(a.world, "region")
    h, _ = surface(region, x0 - R, z0 - R, x0 + W - 1 + R, z0 + L - 1 + R)    # [z, x] incl. ring
    if (h == -999).any():
        sys.exit(f"the chunks around are not generated: {(h == -999).sum()} columns missing (force-load the area first)")
    fz, fx = np.where(foot)
    nat_foot = h[fz + R, fx + R]
    base = a.base if a.base is not None else int(np.median(nat_foot))
    ground_y = base - 1 + a.sink                                # world y of the ground under schematic layer `sink`
    oy = base - a.sink                                          # world y of schematic layer 0
    print(f"grid {W}x{H}x{L} at corner {x0},{oy},{z0}; base {base} (natural ground under it {nat_foot.min()}..{nat_foot.max()})")

    # distance of every column (incl. ring) to the footprint
    from scipy.ndimage import distance_transform_edt  # noqa
    big = np.zeros((L + 2 * R, W + 2 * R), dtype=bool)
    big[R:R + L, R:R + W] = foot
    dist = distance_transform_edt(~big)
    top = min(319, max(oy + H + 2, int(h.max()) + 40))
    prepare = ["gamerule doTileDrops false", "gamerule doEntityDrops false"]
    terrain = []
    for zz in range(L + 2 * R):
        for xx in range(W + 2 * R):
            d = dist[zz, xx]
            nat = int(h[zz, xx])
            # the slope is as wide as the drop needs: about two blocks out for every block up or down, at least 10
            reach = min(R, max(10, 2.2 * abs(nat - ground_y)))
            if d > reach:
                continue
            if d == 0:
                g = ground_y
            else:
                t = min(1.0, d / reach)
                w = t * t * (3 - 2 * t)                       # smoothstep
                g = round(ground_y + (nat - ground_y) * w)
            wx, wz = x0 - R + xx, z0 - R + zz
            terrain.append((wx, wz, g, nat, d == 0))
    # clear above the new ground (leaves and logs of trees standing in the ring too), then raise the ground where needed
    for wx, wz, g, nat, inside in terrain:
        prepare.append(f"fill {wx} {g + 1} {wz} {wx} {top} {wz} air")
    for wx, wz, g, nat, inside in terrain:
        if g > nat:
            lo = max(nat - 2, -60)
            if g - 3 > lo:
                prepare.append(f"fill {wx} {lo} {wz} {wx} {g - 3} {wz} stone")
            prepare.append(f"fill {wx} {max(lo, g - 2)} {wz} {wx} {g - 1} {wz} dirt")
        if g < nat:                                   # a cut: soil under the new grass, not bare stone
            prepare.append(f"fill {wx} {g - 3} {wz} {wx} {g - 1} {wz} dirt replace #minecraft:base_stone_overworld")
        if not inside:
            prepare.append(f"setblock {wx} {g} {wz} grass_block")
        else:
            prepare.append(f"setblock {wx} {g} {wz} dirt")

    # the build: per layer, runs of one state along x become one fill
    solid_cmds, fragile_cmds = [], []
    for y in range(H):
        for z in range(L):
            row = blocks[y, z]
            x = 0
            while x < W:
                v = row[x]
                st = palette[v]
                if st.split("[")[0] in AIR:
                    x += 1
                    continue
                e = x
                while e + 1 < W and row[e + 1] == v and (e + 1, y, z) not in bes and (x, y, z) not in bes:
                    e += 1
                name = st.split("[")[0].split(":")[1]
                cmd = (f"fill {x0 + x} {oy + y} {z0 + z} {x0 + e} {oy + y} {z0 + z} {st}" if e > x
                       else f"setblock {x0 + x} {oy + y} {z0 + z} {st}")
                (fragile_cmds if FRAGILE.search(name) else solid_cmds).append(cmd)
                x = e + 1
    finish = []
    for (bx, by, bz), (bid, tag) in bes.items():
        st = palette[blocks[by, bz, bx]]
        n = st.split("[")[0]
        if "sign" not in n and "banner" not in n and "skull" not in n and "head" not in n:
            continue                      # shelves become empty bookshelves; chests etc. keep no items
        keep = {k: v for k, v in tag.items() if k in ("front_text", "back_text", "is_waxed", "patterns", "profile", "note_block_sound", "custom_name")}
        import nbtlib
        finish.append(f"setblock {x0 + bx} {oy + by} {z0 + bz} {st}{nbtlib.Compound(keep).snbt() if keep else ''} replace")
    finish += ["kill @e[type=item]", "gamerule doTileDrops true", "gamerule doEntityDrops true"]

    out = os.path.join(a.world, "datapacks", "wilds_spawn_build")
    fn = os.path.join(out, "data", "wilds_spawn_build", "function")
    os.makedirs(fn, exist_ok=True)
    for old in os.listdir(fn):
        os.remove(os.path.join(fn, old))
    json.dump({"pack": {"pack_format": 48, "description": "Wilds & Wanderers: the spawn build (one-off)"}},
              open(os.path.join(out, "pack.mcmeta"), "w"))

    def write(name, lines):
        open(os.path.join(fn, name + ".mcfunction"), "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")

    write("prepare", prepare)
    cmds = solid_cmds + fragile_cmds
    parts = [cmds[i:i + 20000] for i in range(0, len(cmds), 20000)]
    for i, p in enumerate(parts):
        write(f"build_{i}", p)
    write("finish", finish)
    span = [(x0 - R, z0 - R), (x0 + W - 1 + R, z0 + L - 1 + R)]
    far = max(math.hypot(wx - a.cx, wz - a.cz) for wx, wz, g, nat, inside in terrain)
    far_build = max(math.hypot(x0 + x - a.cx, z0 + z - a.cz) for z, x in zip(fz, fx))
    print(f"prepare {len(prepare)}, build {len(cmds)} in {len(parts)} parts, finish {len(finish)}")
    print(f"area {span}; farthest build column {far_build:.0f}, farthest reshaped ground {far:.0f} blocks from the center")
    print("run: datapack enable \"file/wilds_spawn_build\" (or /reload), forceload the area, then "
          + ", ".join(["function wilds_spawn_build:prepare"] + [f"function wilds_spawn_build:build_{i}" for i in range(len(parts))]
                      + ["function wilds_spawn_build:finish"]))
    json.dump(dict(corner=[x0, oy, z0], size=[W, H, L], base=base, center=[a.cx, a.cz], area=span,
                   far_build=far_build, far_ground=far, parts=len(parts)),
              open(os.path.join(out, "placement.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
