"""Read a Sponge .schem (v2/v3) or a Litematica .litematic into one block grid.

Schematic.load(path) -> Schematic with .size (W, H, L), .palette (list of block-state strings), .blocks (numpy array
[y, z, x] of palette indices), .block_entities ({(x, y, z): nbt compound}) and .entities (list of nbt compounds with
Pos relative to the grid). Used by the spawn tools to study a downloaded build and turn it into commands.
"""
import math
import numpy as np
import nbtlib


def _state(name, props):
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]"


class Schematic:
    def __init__(self, size, palette, blocks, block_entities, entities, source):
        self.size, self.palette, self.blocks = size, palette, blocks
        self.block_entities, self.entities, self.source = block_entities, entities, source

    @staticmethod
    def load(path):
        f = nbtlib.load(path)
        return Schematic._litematic(f, path) if "Regions" in f else Schematic._sponge(f, path)

    # --- Sponge: varint indices in y, z, x order -------------------------------------------------------------------
    @staticmethod
    def _sponge(f, path):
        r = f["Schematic"] if "Schematic" in f else f
        W, H, L = int(r["Width"]), int(r["Height"]), int(r["Length"])
        blk = r["Blocks"] if "Blocks" in r else r
        pal = blk["Palette"]
        data = bytes((int(x) + 256) % 256 for x in (blk["Data"] if "Data" in blk else r["BlockData"]))
        out = np.empty(W * H * L, dtype=np.int32)
        i = n = 0
        while i < len(data):
            v = s = 0
            while True:
                b = data[i]; i += 1
                v |= (b & 0x7F) << s; s += 7
                if not b & 0x80:
                    break
            out[n] = v; n += 1
        palette = [None] * len(pal)
        for k, v in pal.items():
            palette[int(v)] = str(k)
        bes = {}
        for be in (blk.get("BlockEntities") or r.get("TileEntities") or []):
            x, y, z = (int(c) for c in be["Pos"])
            data_ = be["Data"] if "Data" in be else be
            bes[(x, y, z)] = (str(be.get("Id", data_.get("id", ""))), data_)
        ents = []
        for e in (r.get("Entities") or []):
            ents.append(e)
        return Schematic((W, H, L), palette, out.reshape(H, L, W), bes, ents, path)

    # --- Litematica: per-region packed longs that may span two longs -----------------------------------------------
    @staticmethod
    def _litematic(f, path):
        regions = []
        for name, reg in f["Regions"].items():
            px, py, pz = (int(reg["Position"][k]) for k in "xyz")
            sx, sy, sz = (int(reg["Size"][k]) for k in "xyz")
            # a negative size grows towards -axis from the position
            x0, y0, z0 = (p + s + 1 if s < 0 else p for p, s in ((px, sx), (py, sy), (pz, sz)))
            W, H, L = abs(sx), abs(sy), abs(sz)
            pal = [_state(str(e["Name"]), {str(k): str(v) for k, v in e.get("Properties", {}).items()})
                   for e in reg["BlockStatePalette"]]
            bits = max(2, math.ceil(math.log2(len(pal))))
            longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in reg["BlockStates"]]
            n = W * H * L
            vals = np.empty(n, dtype=np.int32)
            mask = (1 << bits) - 1
            for i in range(n):
                start = i * bits
                li, off = divmod(start, 64)
                v = longs[li] >> off
                if off + bits > 64:
                    v |= longs[li + 1] << (64 - off)
                vals[i] = v & mask
            grid = vals.reshape(H, L, W)  # index = (y * L + z) * W + x
            bes = {}
            for be in reg.get("TileEntities", []):
                x, y, z = int(be["x"]), int(be["y"]), int(be["z"])
                bes[(x, y, z)] = (str(be.get("id", "")), be)
            ents = list(reg.get("Entities", []))
            regions.append(((x0, y0, z0), (W, H, L), pal, grid, bes, ents))
        # merge regions into one grid
        mins = [min(r[0][a] for r in regions) for a in range(3)]
        maxs = [max(r[0][a] + r[1][a] for r in regions) for a in range(3)]
        W, H, L = (maxs[a] - mins[a] for a in range(3))
        palette = ["minecraft:air"]
        index = {"minecraft:air": 0}
        out = np.zeros((H, L, W), dtype=np.int32)
        bes, ents = {}, []
        for (x0, y0, z0), (w, h, l), pal, grid, rbes, rents in regions:
            remap = np.array([index.setdefault(s, len(index)) for s in pal], dtype=np.int32)
            palette += [s for s in pal if s not in palette]
            ox, oy, oz = x0 - mins[0], y0 - mins[1], z0 - mins[2]
            sub = remap[grid]
            dst = out[oy:oy + h, oz:oz + l, ox:ox + w]
            keep = sub != 0
            dst[keep] = sub[keep]
            for (x, y, z), v in rbes.items():
                bes[(x + ox, y + oy, z + oz)] = v
            ents += rents
        palette = [None] * len(index)
        for s, i in index.items():
            palette[i] = s
        return Schematic((W, H, L), palette, out, bes, ents, path)

    # --- helpers -----------------------------------------------------------------------------------------------------
    def name_at(self, x, y, z):
        return self.palette[self.blocks[y, z, x]]

    def solid_mask(self):
        air = np.array([s.split("[")[0] in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void")
                        for s in self.palette])
        return ~air[self.blocks]
