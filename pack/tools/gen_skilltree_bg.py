"""The background of the skill tree «Шлях мандрівника»: a dark linen cloth embroidered in cross-stitch, every branch in its
own thread under it, ornaments on the borders between branches, a rose under the root, and each branch's emblem and name
stitched on the rim.

  python gen_skilltree_bg.py      -> pack/overrides/kubejs/assets/wilds/textures/gui/skilltree.png (4096 x 4096)

Pufferfish draws the picture ("position": "fill") centred on the root and scaled with the tree; measured on the screen, the
tree (gen_skilltree.py: inner ring 120, first nodes 210, big abilities 770, keystones 862) takes the middle ~46% of it, one
pixel of a 4096 picture being ~0.92 units of the tree. At the screen's usual zoom a stitch of 4 pixels is ~11 pixels on the
screen: fine enough to look sewn, coarse enough to read as cross-stitch. So everything below is placed in tree units (U converts), branches at the angles
gen_skilltree.py gives them (Воїн at the top, then clockwise), and the rings of ornament keep off the rings of nodes.
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "overrides", "kubejs", "assets", "wilds", "textures", "gui", "skilltree.png")
N, C, S = 4096, 2048, 4          # picture size, centre, one stitch cell (pixels)
U = 1.086                        # picture pixels per tree unit
rnd = random.Random(7)

# branches clockwise from the top, as gen_skilltree.ORDER; thread colours a little dimmed for a dark cloth
BRANCHES = [("ВОЇН", (176, 48, 42), "sword"), ("МИСЛИВЕЦЬ", (70, 132, 58), "bow"), ("МАГ", (120, 82, 190), "star"),
            ("МАЙСТЕР", (200, 150, 44), "hammer"), ("МОРЕПЛАВЕЦЬ", (52, 92, 176), "anchor"), ("СЛІДОПИТ", (40, 150, 136), "compass"),
            ("ДИПЛОМАТ", (196, 196, 206), "scroll"), ("ТОРГОВЕЦЬ", (200, 108, 44), "coin")]
GOLD, RED, BLACK = (214, 172, 84), (150, 34, 30), (30, 22, 20)
# in tree units: the band between the inner ring and the first nodes, the fields of the branches, the rim beyond the
# keystones, and where the emblems and names sit
R_INNER_BAND, R_FIELD_IN, R_FIELD_OUT, R_RIM, R_EMBLEM, R_NAME = 165, 236, 820, 905, 962, 1032


def centre_angle(i):
    return 90 - i * 45


def dim(col, k):
    return tuple(min(255, int(v * k)) for v in col)


# ---------------------------------------------------------------- cloth
def cloth():
    """plain weave (warp and weft one pixel each), slow unevenness of the cloth, darker towards the edges"""
    import numpy as np
    y, x = np.mgrid[0:N, 0:N]
    weave = np.where(((x // 2) + (y // 2)) % 2 == 0, 6.0, 0.0)
    low = Image.fromarray((np.random.default_rng(7).random((32, 32)) * 255).astype("uint8")).resize((N, N), Image.BICUBIC)
    uneven = (np.asarray(low, dtype=float) / 255 - 0.5) * 8
    d = np.hypot(x - C, y - C) / C
    dark = np.maximum(0.4, 1.0 - 0.45 * d * d)
    base = 27 + weave + uneven
    rgb = np.stack([base * dark * 1.05, (base - 4) * dark, (base - 7) * dark], axis=-1)
    return Image.fromarray(np.clip(rgb, 0, 255).astype("uint8"), "RGB")


# ---------------------------------------------------------------- stitches
def cross(d, gx, gy, col, half=False, s=S):
    """one cross-stitch in cell (gx, gy) of a grid of s-pixel cells: the lower diagonal darker, the upper one lit"""
    x, y = gx * s, gy * s
    j = rnd.uniform(0.86, 1.08)
    c1, c2 = dim(col, 0.62 * j), dim(col, j)
    w = 2 if s >= 6 else 1
    if not half:
        d.line([x, y, x + s - 1, y + s - 1], fill=c1, width=w)
    d.line([x, y + s - 1, x + s - 1, y], fill=c2, width=w)


def blend_cross(img, gx, gy, col, alpha):
    """a half-stitch only partly over the cloth (the sparse fields under the branches)"""
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cross(ImageDraw.Draw(layer), 0, 0, col, half=True)
    layer.putalpha(layer.split()[3].point(lambda v: int(v * alpha)))
    img.paste(layer, (gx * S, gy * S), layer)


def polar_cell(angle, r_units, s=S):
    a = math.radians(angle)
    return int((C + math.cos(a) * r_units * U) // s), int((C - math.sin(a) * r_units * U) // s)


def stamp(d, pattern, cx, cy, colours, scale=1):
    """a pattern of characters (one per stitch, '.' = none) centred on cell (cx, cy); scale doubles each stitch"""
    rows = pattern.strip("\n").split("\n")
    h, w = len(rows), max(len(r) for r in rows)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch not in colours:
                continue
            for dy in range(scale):
                for dx in range(scale):
                    cross(d, cx + (i - w // 2) * scale + dx, cy + (j - h // 2) * scale + dy, colours[ch])


def ring(d, r_units, col):
    """a stitched circle at radius r"""
    seen = set()
    steps = int(2 * math.pi * r_units * U / S) * 2
    for k in range(steps):
        cell = polar_cell(360 * k / steps, r_units)
        if cell not in seen:
            seen.add(cell)
            cross(d, *cell, col)


# ---------------------------------------------------------------- ornaments
ROSE = """
.....a.....a.....
....aba...aba....
...abcba.abcba...
....abcbabcba....
.....abcccba.....
.a....bcccb....a.
aba..bccdccb..aba
.abcbcddddcbcba..
..bcccdd.ddcccb..
.abcbcddddcbcba..
aba..bccdccb..aba
.a....bcccb....a.
.....abcccba.....
....abcbabcba....
...abcba.abcba...
....aba...aba....
.....a.....a.....
"""
RHOMB = """
...a...
..aba..
.ab.ba.
ab.c.ba
.ab.ba.
..aba..
...a...
"""
SMALL_ROSE = """
..a..
.aba.
abcba
.aba.
..a..
"""
TEETH = """
a.......
aa......
aaa.....
aaaa....
aaaaa...
aaaaaa..
aaaaaaa.
"""
EMBLEMS = {
    "sword": """
.......a.
......aba
.....aba.
....aba..
...aba...
b.aba....
.bba.....
..bb.....
.b..b....
b........
""",
    "bow": """
..aa......
.a..b.....
a....b....
a.....b...
a......b..
a.....b...
a....b....
.a..b.....
..aa......
""",
    "star": """
....a....
....a....
.a..a..a.
..a.b.a..
aaabcbaaa
..a.b.a..
.a..a..a.
....a....
....a....
""",
    "hammer": """
aaaaa....
aaaaab...
aaaaa.b..
.....b.b.
......b.b
.......b.
""",
    "anchor": """
....a....
...a.a...
....a....
..aaaaa..
....a....
....a....
a...a...a
.a..a..a.
..aaaaa..
""",
    "compass": """
...aaa...
..a.b.a..
.a..b..a.
a...b...a
a.ccbcc.a
a...c...a
.a..c..a.
..a...a..
...aaa...
""",
    "scroll": """
.aaaaaaa.
a.bbbbb.a
.a.....a.
.a.bbb.a.
.a.....a.
.a.bbb.a.
a.......a
.aaaaaaa.
""",
    "coin": """
...aaa...
..abbba..
.abcccba.
.abcacba.
.abcccba.
..abbba..
...aaa...
""",
}


def name_label(name, col, emblem, font):
    """the branch's emblem and, beside it, its name, stitched on a transparent piece of cloth"""
    mask = Image.new("L", (200, 20), 0)
    ImageDraw.Draw(mask).text((0, 0), name, font=font, fill=255)
    mask = mask.crop(mask.getbbox())
    w, h = mask.size
    t = 3                                          # the letters' stitch, finer than the rest
    rows = EMBLEMS[emblem].strip("\n").split("\n")
    ew, eh = max(len(r) for r in rows) * S, len(rows) * S
    lw, lh = ew + 10 + w * t, max(eh, h * t) + 8
    label = Image.new("RGBA", (lw, lh), (0, 0, 0, 0))
    d = ImageDraw.Draw(label)
    colours = {"a": col, "b": dim(col, 1.3), "c": GOLD}
    for j, row in enumerate(rows):
        for k, ch in enumerate(row):
            if ch in colours:
                cross(d, k, j + (lh - eh) // (2 * S), colours[ch])
    mp = mask.load()
    oy = (lh - h * t) // 2
    for j in range(h):
        for k in range(w):
            if mp[k, j] > 100:
                x, y = ew + 10 + k * t, oy + j * t
                d.line([x, y + t - 1, x + t - 1, y], fill=dim(GOLD, rnd.uniform(0.9, 1.08)), width=1)
                d.line([x, y, x + t - 1, y + t - 1], fill=dim(GOLD, 0.62), width=1)
    return label


def main():
    img = cloth()

    # each branch: a sparse lattice of half-stitches in its thread, from the first nodes to the big abilities, fading out
    for gy in range(N // S):
        for gx in range(N // S):
            if (gx + gy) % 3:
                continue
            x, y = (gx * S + S / 2 - C) / U, (C - (gy * S + S / 2)) / U
            r = math.hypot(x, y)
            if r < R_FIELD_IN or r > R_FIELD_OUT:
                continue
            ang = math.degrees(math.atan2(y, x))
            bi = round((90 - ang) / 45) % 8
            off = abs(((90 - bi * 45) - ang + 180) % 360 - 180)
            if off > 21:
                continue
            fade = 1 - (r - R_FIELD_IN) / (R_FIELD_OUT - R_FIELD_IN)
            blend_cross(img, gx, gy, BRANCHES[bi][1], 0.30 + 0.40 * fade * (1 - off / 26))
    d = ImageDraw.Draw(img)

    # the borders between branches: chains of small rhombi in gold, each with a stitch of the branch's colour
    for i in range(8):
        a = centre_angle(i) - 22.5
        r = 300
        while r < R_RIM - 40:
            cx, cy = polar_cell(a, r)
            stamp(d, RHOMB, cx, cy, {"a": dim(GOLD, 0.7), "c": BRANCHES[i][1]})
            r += 40

    # under the root: the rose; around the inner ring: a red and gold band
    stamp(d, ROSE, C // S, C // S, {"a": dim(GOLD, 0.7), "b": RED, "c": GOLD, "d": (230, 205, 150)}, scale=2)
    ring(d, R_INNER_BAND - 6, dim(RED, 0.9))
    ring(d, R_INNER_BAND + 6, dim(GOLD, 0.55))

    # the rim beyond the keystones: two rings with roses between the branches
    ring(d, R_RIM - 10, dim(GOLD, 0.65))
    ring(d, R_RIM + 10, dim(RED, 0.8))
    for i in range(8):
        cx, cy = polar_cell(centre_angle(i) - 22.5, R_RIM)
        stamp(d, SMALL_ROSE, cx, cy, {"a": GOLD, "b": BRANCHES[i][1], "c": (240, 225, 190)}, scale=2)

    # each branch: its emblem and name, where the screen can show them (it never zooms out past the tree's square):
    # just beyond the keystone for the branches on the axes (upright at the top and bottom, turned on the sides), in the
    # corners, with more room, for the diagonal ones
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/georgiab.ttf", 10)
    except OSError:
        font = ImageFont.load_default()
    for i, (name, col, emblem) in enumerate(BRANCHES):
        a = centre_angle(i)
        label = name_label(name, col, emblem, font)
        if i in (2, 6):                                  # Маг on the right, Дипломат on the left: read along the edge
            label = label.rotate(-90 if i == 2 else 90, expand=True)
        r = 897 if i % 2 == 0 else 1000
        x = C + math.cos(math.radians(a)) * r * U - label.width / 2
        y = C - math.sin(math.radians(a)) * r * U - label.height / 2
        img.paste(label, (int(x), int(y)), label)
    d = ImageDraw.Draw(img)

    # beyond the names: a band of roses and rhombi, red and black on gold, the old Ukrainian way
    steps = 48
    for k in range(steps):
        cx, cy = polar_cell(360 * k / steps, 1180)
        if k % 2 == 0:
            stamp(d, SMALL_ROSE, cx, cy, {"a": RED, "b": BLACK, "c": GOLD}, scale=3)
        else:
            stamp(d, RHOMB, cx, cy, {"a": dim(GOLD, 0.7), "c": RED}, scale=2)
    ring(d, 1130, dim(RED, 0.75))
    ring(d, 1230, dim(RED, 0.75))

    # the edge of the cloth: "wolf's teeth" in red and black between two gold lines
    cells = N // S
    for k in range(10, cells - 26, 16):
        for cx, cy in ((k + 8, 16), (k + 8, cells - 17), (16, k + 8), (cells - 17, k + 8)):
            stamp(d, TEETH, cx, cy, {"a": RED if (k // 8) % 2 else BLACK}, scale=2)
    for k in range(5, cells - 5):
        for cx, cy in ((k, 5), (k, cells - 6), (5, k), (cells - 6, k)):
            cross(d, cx, cy, dim(GOLD, 0.6))

    img = img.filter(ImageFilter.GaussianBlur(0.3))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT, optimize=True)
    print("written", os.path.relpath(OUT), os.path.getsize(OUT) // 1024, "KB")


if __name__ == "__main__":
    main()
