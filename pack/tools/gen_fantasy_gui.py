"""The game's own widgets in the pack's style (22.11): buttons, sliders, text fields and the hotbar as dark wood with
gold trim, the same palette as the quest book and the skill tree, so every menu feels like one fantasy journal.

  python pack/tools/gen_fantasy_gui.py
    -> pack/overrides/kubejs/assets/minecraft/textures/gui/sprites/{widget,hud}/*.png (+ the vanilla .mcmeta beside)

Every sprite keeps vanilla's size and nine-slice borders (read from the game's jar), so screens of every mod that use
them stay laid out exactly as before; only the look changes. Pixel art at 1x, like the game.
"""
import io, os, zipfile
import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "pack", "overrides", "kubejs", "assets", "minecraft", "textures", "gui", "sprites")
JAR = os.path.expandvars(r"%APPDATA%/.minecraft/versions/1.21.1/1.21.1.jar")
rng = np.random.default_rng(3)

WOOD_D, WOOD_L = np.array([34, 23, 15]), np.array([62, 43, 27])
EDGE = (14, 9, 6, 255)
GOLD, GOLD_HI, GOLD_DULL = (176, 132, 52, 255), (240, 196, 92, 255), (104, 92, 74, 255)


def wood(w, h, light=1.0, grey=False):
    """vertical light (lit from above) and horizontal grain"""
    y = np.linspace(1, 0, h)[:, None] * np.ones((1, w))
    base = WOOD_D + (WOOD_L - WOOD_D) * (0.35 + 0.45 * y)[..., None]
    grain = np.zeros((h, w))
    for row in range(h):
        grain[row] = 0.08 * np.sin(np.linspace(0, rng.uniform(6, 14), w) + rng.uniform(0, 6)) + rng.normal(0, 0.03, w)
    img = np.clip(base * (1 + grain[..., None]) * light, 0, 255)
    if grey:
        g = img.mean(axis=2, keepdims=True)
        img = g * 0.8 + img * 0.2
    a = np.full((h, w, 1), 255)
    return Image.fromarray(np.concatenate([img, a], axis=2).astype(np.uint8), "RGBA").copy()


def frame(im, gold, inset=1):
    px = im.load()
    w, h = im.size
    for x in range(w):
        px[x, 0] = EDGE; px[x, h - 1] = EDGE
    for y in range(h):
        px[0, y] = EDGE; px[w - 1, y] = EDGE
    # the gold line inside the dark edge, brighter on top
    for x in range(inset, w - inset):
        px[x, inset] = tuple(min(255, c + 30) for c in gold[:3]) + (255,)
        px[x, h - 1 - inset] = tuple(max(0, c - 40) for c in gold[:3]) + (255,)
    for y in range(inset, h - inset):
        px[inset, y] = gold; px[w - 1 - inset, y] = tuple(max(0, c - 25) for c in gold[:3]) + (255,)
    return im


def button(kind):
    w, h = 200, 20
    if kind == "disabled":
        return frame(wood(w, h, 0.75, grey=True), GOLD_DULL)
    if kind == "highlighted":
        im = frame(wood(w, h, 1.25), GOLD_HI)
        px = im.load()
        for x in range(2, w - 2):   # a second gold line: the hovered button glows
            px[x, 2] = (200, 160, 70, 255)
        return im
    return frame(wood(w, h), GOLD)


def text_field(highlighted):
    w, h = 200, 20
    im = Image.new("RGBA", (w, h), (12, 9, 7, 235))
    return frame(im, GOLD_HI if highlighted else GOLD_DULL)


def hotbar():
    w, h = 182, 22
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    board = wood(w, h, 0.9)
    px, bx = im.load(), board.load()
    for y in range(h):
        for x in range(w):
            inside = any(2 + 20 * i <= x <= 19 + 20 * i for i in range(9)) and 2 <= y <= 19
            px[x, y] = (16, 12, 9, 150) if inside else bx[x, y]
    for x in range(w):
        px[x, 0] = EDGE; px[x, h - 1] = EDGE
    for y in range(h):
        px[0, y] = EDGE; px[w - 1, y] = EDGE
    for i in range(10):   # gold dividers between the slots
        x = 1 + 20 * i
        for y in range(1, h - 1):
            px[x, y] = GOLD if y % 5 else GOLD_HI
    for x in range(1, w - 1):
        px[x, 1] = (150, 112, 46, 255); px[x, h - 2] = (96, 70, 30, 255)
    return im


def selection():
    w, h = 24, 23
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for x in range(w):
        for t in (0, 1):
            px[x, t] = GOLD_HI if t else EDGE
            px[x, h - 1 - t] = GOLD if t else EDGE
    for y in range(h):
        for t in (0, 1):
            px[t, y] = GOLD_HI if t else EDGE
            px[w - 1 - t, y] = GOLD if t else EDGE
    for (cx, cy) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):   # rivets in the corners
        px[cx, cy] = (255, 236, 170, 255)
    return im


def offhand(left):
    w, h = 29, 24
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    board = wood(w, h, 0.9)
    px, bx = im.load(), board.load()
    x0 = 0 if left else 7
    for y in range(h):
        for x in range(x0, x0 + 22):
            inside = x0 + 3 <= x <= x0 + 18 and 3 <= y <= 18
            px[x, y] = (16, 12, 9, 150) if inside else bx[x, y]
    for x in range(x0, x0 + 22):
        px[x, 0] = EDGE; px[x, 23] = EDGE; px[x, 1] = GOLD_HI; px[x, 22] = GOLD
    for y in range(h):
        px[x0, y] = EDGE; px[x0 + 21, y] = EDGE; px[x0 + 1, y] = GOLD; px[x0 + 20, y] = GOLD
    return im


def slider_handle(highlighted):
    im = wood(8, 20, 1.3 if highlighted else 1.05)
    return frame(im, GOLD_HI if highlighted else GOLD)


def main():
    z = zipfile.ZipFile(JAR)

    def save(rel, im):
        p = os.path.join(OUT, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        van = Image.open(io.BytesIO(z.read("assets/minecraft/textures/gui/sprites/" + rel)))
        assert van.size == im.size, (rel, van.size, im.size)
        im.save(p)
        meta = "assets/minecraft/textures/gui/sprites/" + rel + ".mcmeta"
        if meta in z.namelist():
            open(p + ".mcmeta", "wb").write(z.read(meta))
    save("widget/button.png", button("normal"))
    save("widget/button_highlighted.png", button("highlighted"))
    save("widget/button_disabled.png", button("disabled"))
    save("widget/slider.png", button("disabled"))
    save("widget/slider_highlighted.png", button("normal"))
    save("widget/slider_handle.png", slider_handle(False))
    save("widget/slider_handle_highlighted.png", slider_handle(True))
    save("widget/text_field.png", text_field(False))
    save("widget/text_field_highlighted.png", text_field(True))
    save("hud/hotbar.png", hotbar())
    save("hud/hotbar_selection.png", selection())
    save("hud/hotbar_offhand_left.png", offhand(True))
    save("hud/hotbar_offhand_right.png", offhand(False))
    print("widgets written to", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
