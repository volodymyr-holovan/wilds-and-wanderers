"""Quick look at a schematic without the game: a top-down map (the top block of every column, shaded by height) and a
side view, with a grid every 10 blocks.  usage: render.py <schematic> <out.png> [scale]"""
import sys
import numpy as np
from PIL import Image, ImageDraw
from schem import Schematic

COLORS = [  # first matching word wins
    ("water", (52, 94, 200)), ("lava", (230, 110, 20)), ("leaves", (60, 130, 50)), ("azalea", (80, 140, 60)),
    ("moss", (90, 130, 50)), ("grass_block", (100, 150, 60)), ("short_grass", (100, 160, 60)), ("fern", (80, 140, 60)),
    ("vine", (70, 120, 40)), ("flower", (220, 120, 160)), ("candle", (240, 230, 200)), ("lantern", (250, 200, 90)),
    ("torch", (250, 200, 90)), ("glow", (250, 220, 120)), ("deepslate", (70, 70, 78)), ("blackstone", (40, 36, 40)),
    ("diorite", (215, 215, 215)), ("calcite", (225, 225, 220)), ("quartz", (235, 230, 225)), ("andesite", (135, 135, 135)),
    ("tuff", (110, 110, 100)), ("cobble", (120, 120, 120)), ("stone", (125, 125, 125)), ("warped", (40, 150, 140)),
    ("crimson", (140, 40, 50)), ("spruce", (100, 75, 45)), ("birch", (200, 185, 130)), ("dark_oak", (65, 45, 25)),
    ("oak", (160, 130, 80)), ("pale_oak", (220, 210, 200)), ("cherry", (220, 160, 170)), ("gray_wool", (110, 110, 115)),
    ("white", (235, 235, 235)), ("wool", (170, 170, 170)), ("copper", (80, 160, 130)), ("chain", (60, 60, 70)),
    ("dirt", (130, 95, 65)), ("sand", (220, 205, 150)), ("gravel", (140, 130, 125)), ("glass", (180, 210, 230)),
]


def color(name):
    for k, c in COLORS:
        if k in name:
            return c
    return (200, 0, 200)


def render(path, out, scale=6):
    s = Schematic.load(path)
    W, H, L = s.size
    m = s.solid_mask()
    top = np.full((L, W), -1)
    for y in range(H):
        top[m[y]] = y
    img = Image.new("RGB", (W * scale, L * scale + H * scale // 2 + 10), (20, 20, 25))
    d = ImageDraw.Draw(img)
    cols = [color(p.split("[")[0]) for p in s.palette]
    for z in range(L):
        for x in range(W):
            y = top[z, x]
            if y < 0:
                continue
            c = cols[s.blocks[y, z, x]]
            k = 0.55 + 0.45 * y / max(1, H - 1)
            d.rectangle([x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1], fill=tuple(int(v * k) for v in c))
    for g in range(0, max(W, L), 10):
        d.line([(g * scale, 0), (g * scale, L * scale)], fill=(255, 255, 255, 60))
        d.line([(0, g * scale), (W * scale, g * scale)], fill=(255, 255, 255, 60))
        d.text((g * scale + 1, 1), str(g), fill=(255, 255, 0))
        d.text((1, g * scale + 1), str(g), fill=(255, 255, 0))
    # side view from the south (looking north): nearest solid block along z
    oy = L * scale + 10
    hs = scale // 2
    for x in range(W):
        col = m[:, :, x]                    # [y, z]
        for y in range(H):
            zs = np.where(col[y])[0]
            if zs.size == 0:
                continue
            z = zs.max()
            c = cols[s.blocks[y, z, x]]
            k = 0.5 + 0.5 * z / max(1, L - 1)
            d.rectangle([x * scale, oy + (H - 1 - y) * hs, x * scale + scale - 1, oy + (H - 1 - y) * hs + hs - 1],
                        fill=tuple(int(v * k) for v in c))
    img.save(out)


if __name__ == "__main__":
    render(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 6)
