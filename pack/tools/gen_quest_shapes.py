"""Quest icons as medallions: FTB Quests' shapes redrawn for a fantasy journal (22.11).

  python pack/tools/gen_quest_shapes.py
    -> pack/overrides/kubejs/assets/ftbquests/textures/shapes/<shape>/{background,outline,shape}.png (+ .mcmeta)

FTB Quests draws a quest as three layers of one shape: "background" (tinted with the theme's quest colour), "outline"
(tinted with the quest's state: locked, open, done...) and "shape" (the mask for hovering and selection). They stay
grey and white so the tints keep working. Here the background is an engraved disc with a darker ring and a soft light
from the top, the outline a double rim with small studs, like a medallion or a seal on a map.
"""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "pack", "overrides", "kubejs", "assets", "ftbquests", "textures", "shapes")
N, SS = 128, 4            # texture size, supersampling
M = N * SS
rng = np.random.default_rng(5)


def polygon(sides, r, rot=0.0, cx=M / 2, cy=M / 2):
    return [(cx + r * math.cos(rot + 2 * math.pi * i / sides), cy + r * math.sin(rot + 2 * math.pi * i / sides)) for i in range(sides)]


def gear(r_out, r_in, teeth=10):
    pts = []
    for i in range(teeth * 4):
        a = 2 * math.pi * i / (teeth * 4) - math.pi / 2
        r = r_out if (i % 4) in (1, 2) else r_in
        pts.append((M / 2 + r * math.cos(a), M / 2 + r * math.sin(a)))
    return pts


def mask_of(shape, inset):
    """the shape's area, shrunk by inset (fraction of the half size)"""
    im = Image.new("L", (M, M), 0)
    d = ImageDraw.Draw(im)
    r = M / 2 * (0.96 - inset)
    if shape == "circle":
        d.ellipse([M / 2 - r, M / 2 - r, M / 2 + r, M / 2 + r], fill=255)
    elif shape == "hexagon":
        d.polygon(polygon(6, r, math.pi / 6), fill=255)
    elif shape == "octagon":
        d.polygon(polygon(8, r, math.pi / 8), fill=255)
    elif shape == "diamond":
        d.polygon(polygon(4, r, 0), fill=255)
    elif shape == "rsquare":
        k = r * 0.86
        d.rounded_rectangle([M / 2 - k, M / 2 - k, M / 2 + k, M / 2 + k], radius=k * 0.32, fill=255)
    elif shape == "gear":
        d.polygon(gear(r, r * 0.84, 10), fill=255)
    return im


def studs(shape, r):
    n = {"circle": 8, "hexagon": 6, "octagon": 8, "diamond": 4, "rsquare": 4, "gear": 10}[shape]
    rot = {"hexagon": math.pi / 6, "octagon": math.pi / 8, "rsquare": math.pi / 4, "gear": -math.pi / 2 + math.pi / 10}.get(shape, 0)
    if shape == "rsquare":
        r *= 1.12
    return polygon(n, r, rot)


def make(shape):
    outer = mask_of(shape, 0.0)
    rim_in = mask_of(shape, 0.10)          # inside the outer rim
    groove_o, groove_i = mask_of(shape, 0.17), mask_of(shape, 0.205)
    a_out, a_rim, a_go, a_gi = (np.asarray(x, dtype=np.float32) / 255 for x in (outer, rim_in, groove_o, groove_i))

    # background: a disc lit from the top, an engraved groove, faint hammered texture
    y, x = np.mgrid[0:M, 0:M].astype(np.float32)
    dist = np.hypot(x - M / 2, y - M / 2) / (M / 2)
    light = 0.78 + 0.22 * np.clip(1 - dist, 0, 1) + 0.10 * np.clip((M / 2 - y) / M, -0.5, 0.5)
    noise = np.asarray(Image.fromarray((rng.random((M // 8, M // 8)) * 255).astype(np.uint8)).resize((M, M), Image.BICUBIC), dtype=np.float32) / 255
    val = light * (0.93 + 0.07 * noise)
    groove = np.clip(a_go - a_gi, 0, 1)
    val = val * (1 - 0.28 * groove)
    # dark bronze (FTB draws the background untinted for most themes): the item on it must stand out
    bronze = np.array([92, 72, 46]) + np.array([60, 48, 30]) * (val - 0.7)[..., None] / 0.4
    bg_rgba = np.dstack([np.clip(bronze, 0, 255), a_out * 255]).astype(np.uint8)

    # outline: the outer rim and a thin inner line, with studs on the rim
    rim = np.clip(a_out - a_rim, 0, 1)
    thin = np.clip(mask_of_line(shape), 0, 1)
    ol = np.maximum(rim, thin * 0.75)
    ol_img = Image.fromarray((ol * 255).astype(np.uint8))
    d = ImageDraw.Draw(ol_img)
    for (sx, sy) in studs(shape, M / 2 * 0.91):
        s = M * 0.032
        d.ellipse([sx - s, sy - s, sx + s, sy + s], fill=255)
    ol = np.asarray(ol_img, dtype=np.float32) / 255
    shade = 0.82 + 0.18 * np.clip((M / 2 - y) / (M / 2), -1, 1)    # the rim catches the light from above
    ol_rgba = np.dstack([shade * 255, shade * 255, shade * 255, ol * 255]).astype(np.uint8)

    sh_rgba = np.dstack([np.full((M, M), 255), np.full((M, M), 255), np.full((M, M), 255), a_out * 255]).astype(np.uint8)
    return [Image.fromarray(a, "RGBA").resize((N, N), Image.LANCZOS) for a in (bg_rgba, ol_rgba, sh_rgba)]


def mask_of_line(shape):
    a = np.asarray(mask_of(shape, 0.135), dtype=np.float32) / 255
    b = np.asarray(mask_of(shape, 0.150), dtype=np.float32) / 255
    return a - b


def main():
    for shape in ("circle", "hexagon", "octagon", "diamond", "rsquare", "gear"):
        d = os.path.join(OUT, shape)
        os.makedirs(d, exist_ok=True)
        for name, im in zip(("background", "outline", "shape"), make(shape)):
            im.save(os.path.join(d, name + ".png"))
            open(os.path.join(d, name + ".png.mcmeta"), "w", newline="\n").write('{\n  "texture": {\n    "blur": true\n  }\n}\n')
    print("shapes written to", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
