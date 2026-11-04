"""The quest book's look: a traveller's journal instead of FTB's grey squares.

  python gen_quest_theme.py
    -> pack/overrides/kubejs/assets/wilds/textures/gui/quests/{cloth,paper}.png   (seamless tiles)
    -> pack/overrides/kubejs/assets/ftbquests/ftb_quests_theme.txt                (FTB Quests' own theme, overridden)

The book's background is the same dark linen as the skill tree (gen_skilltree_bg.py), with a faint embroidered rhombus
every 64 pixels; quest windows, menus and the chapter list are dark parchment in a gold frame; text is warm white,
links and highlights gold, done quests green, lines between quests gold and green. The theme file is FTB Quests' own,
read from its jar, with only the values below changed, so a newer FTB Quests keeps every key it knows.
"""
import glob, os, re, zipfile
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
TEX = os.path.join(ROOT, "pack", "overrides", "kubejs", "assets", "wilds", "textures", "gui", "quests")
THEME = os.path.join(ROOT, "pack", "overrides", "kubejs", "assets", "ftbquests", "ftb_quests_theme.txt")
rng = np.random.default_rng(11)


def periodic_noise(n, cells):
    """smooth noise that tiles: random values on a coarse torus grid, bilinear between them"""
    g = rng.random((cells, cells))
    y, x = np.mgrid[0:n, 0:n] * (cells / n)
    x0, y0 = np.floor(x).astype(int), np.floor(y).astype(int)
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = g[y0 % cells, x0 % cells], g[y0 % cells, (x0 + 1) % cells]
    c, d = g[(y0 + 1) % cells, x0 % cells], g[(y0 + 1) % cells, (x0 + 1) % cells]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def cloth(n=128):
    y, x = np.mgrid[0:n, 0:n]
    weave = np.where(((x // 2) + (y // 2)) % 2 == 0, 5.0, 0.0)
    base = 26 + weave + (periodic_noise(n, 4) - 0.5) * 10 + (rng.random((n, n)) - 0.5) * 3
    rgb = np.stack([base * 1.06, base - 4, base - 8], axis=-1)
    img = np.clip(rgb, 0, 255)
    # a faint gold rhombus, stitched, every 64 pixels
    for cy in range(32, n, 64):
        for cx in range(32, n, 64):
            for k in range(-5, 6):
                for px, py in ((cx + k, cy + (5 - abs(k))), (cx + k, cy - (5 - abs(k)))):
                    img[py % n, px % n] = [92, 74, 40]
    return Image.fromarray(img.astype("uint8"), "RGB")


def paper(n=128):
    y, x = np.mgrid[0:n, 0:n]
    fibre = (periodic_noise(n, 16) - 0.5) * 9 + (periodic_noise(n, 5) - 0.5) * 12 + (rng.random((n, n)) - 0.5) * 4
    base = 46 + fibre
    rgb = np.stack([base * 1.12 + 4, base * 0.98, base * 0.80 - 2], axis=-1)
    return Image.fromarray(np.clip(rgb, 0, 255).astype("uint8"), "RGB")


CHANGES = {
    "background": "wilds:textures/gui/quests/cloth.png; color=#FFFFFFFF; tile_size=128",
    "chapter_panel_background": "hollow_rectangle:#8C6A2E; round_edges + wilds:textures/gui/quests/paper.png; tile_size=128; padding=1",
    "key_reference_background": "wilds:textures/gui/quests/paper.png; tile_size=128",
    "selected_chapter_highlight_1": "#70E8B84A",
    "selected_chapter_highlight_2": "#30E8B84A",
    "text_color": "#F2E6CF",
    "hover_text_color": "#FFD978",
    "disabled_text_color": "#8C8170",
    "widget_border": "#2A1E10",
    "widget_background": "#50000000",
    "symbol_in": "#E8B84A",
    "symbol_out": "#8C6A2E",
    "context_menu": "hollow_rectangle:#8C6A2E; round_edges + wilds:textures/gui/quests/paper.png; tile_size=128; padding=1",
    "tasks_text_color": "#8FC4FF",
    "rewards_text_color": "#FFB347",
    "quest_view_background": "hollow_rectangle:#C9A45C; round_edges + wilds:textures/gui/quests/paper.png; tile_size=128; padding=1",
    "quest_view_border": "#C9A45C",
    "quest_view_title": "#E8C77A",
    "quest_completed_color": "#C87CD67C",
    "quest_started_color": "#C8F2C94C",
    "quest_not_started_color": "#A0FFF6E0",
    "quest_locked_color": "#FF7A7060",
    "dependency_line_completed_color": "#7CD67C",
    "dependency_line_uncompleted_color": "#C8D8B48A",
    "dependency_line_unavailable_color": "#64A08A70",
    "dependency_line_requires_color": "#66C8E8",
    "dependency_line_required_for_color": "#F2C94C",
    "dependency_line_selected_speed": "1.5",
    "dependency_line_unselected_speed": "0.15",
    "dependency_line_thickness": "0.2",
}


def theme():
    jar = glob.glob(os.path.join(ROOT, "client", "mods", "ftb-quests-*.jar"))[0]
    text = zipfile.ZipFile(jar).read("assets/ftbquests/ftb_quests_theme.txt").decode("utf-8")
    seen = set()

    def swap(m):
        key = m.group(1)
        if key in CHANGES:
            seen.add(key)
            return f"{key}:{m.group(2)}{CHANGES[key]}"
        return m.group(0)

    text = re.sub(r"(?m)^([a-z_0-9]+):(\s*)(.*)$", swap, text)
    missing = set(CHANGES) - seen
    if missing:
        raise SystemExit(f"keys FTB Quests no longer has: {sorted(missing)}")
    header = ""   # the theme format has no comment lines: this script says what made the file
    os.makedirs(os.path.dirname(THEME), exist_ok=True)
    open(THEME, "w", encoding="utf-8", newline="\n").write(header + text)


os.makedirs(TEX, exist_ok=True)
cloth().save(os.path.join(TEX, "cloth.png"), optimize=True)
paper().save(os.path.join(TEX, "paper.png"), optimize=True)
theme()
print("quest theme written:", os.path.relpath(THEME, ROOT))
