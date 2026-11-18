"""Small textures of the Final Castle completion (the models' own textures come from make_models.py).

  python make_textures.py <TF source checkout>

rune_scepter.png <- textures/item/twilight_scepter.png: the Scepter of Twilight with a violet orb (CC BY-NC-SA 4.0,
                    like all of TF's art, so shared under the same licence)
rune_bolt.png    -> textures/entity/: a violet orb with a rune, drawn here (the Rune Sovereign's and the scepter's bolt)
"""
import math
import sys
from pathlib import Path

from PIL import Image

TEX = "src/main/resources/assets/twilightforest/textures"


def clamp(v):
    return max(0, min(255, int(v)))


def scepter(src, dst):
    im = Image.open(src).convert("RGBA")
    out = Image.new("RGBA", im.size)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a and b > r + 15:  # the blue orb -> violet
                lum = (r + g + b) / 3.0
                out.putpixel((x, y), (clamp(lum * 2.6 + 30), clamp(lum * 0.9), clamp(lum * 3.4 + 50), a))
            else:
                out.putpixel((x, y), (r, g, b, a))
    out.save(dst)


def bolt(dst):
    """16x16: a soft violet orb, brighter to the middle, with a small rune cut out of light."""
    rune = ["..#..", ".###.", "#.#.#", "..#..", ".#.#."]
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 7.5
            if d > 1.0:
                continue
            k = 1.0 - d
            out.putpixel((x, y), (clamp(120 + 130 * k), clamp(50 + 120 * k * k), clamp(200 + 55 * k), clamp(255 * min(1.0, k * 2.2))))
    for j, row in enumerate(rune):
        for i, ch in enumerate(row):
            if ch == "#":
                out.putpixel((5 + i, 5 + j), (255, 240, 255, 255))
    out.save(dst)


def main():
    root = Path(sys.argv[1]) / TEX
    scepter(root / "item/twilight_scepter.png", root / "item/rune_scepter.png")
    bolt(root / "entity/rune_bolt.png")
    print("textures written to", root)


if __name__ == "__main__":
    main()
