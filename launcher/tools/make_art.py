"""The program's background and icon, drawn in code (no borrowed artwork):
  python make_art.py  ->  ../src/WildsLauncher/Assets/{background.png, icon.png, icon.ico}
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "WildsLauncher", "Assets")


def background():
    random.seed(7)
    W, H, S = 1100, 680, 2
    img = Image.new("RGB", (W * S, H * S))
    d = ImageDraw.Draw(img)
    top, bot = (10, 18, 32), (38, 52, 58)
    for y in range(H * S):
        t = y / (H * S)
        d.line([(0, y), (W * S, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t ** 1.2) for i in range(3)))
    for _ in range(260):
        x, y = random.randrange(W * S), random.randrange(int(H * S * 0.55))
        r, b = random.choice([1, 1, 1, 2, 2, 3]), random.randint(120, 230)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(b, b, int(b * 0.95)))
    # the moon in the open sky between the title and the settings panel
    glow = Image.new("L", (W * S, H * S), 0)
    g = ImageDraw.Draw(glow)
    mx, my = int(W * S * 0.6), int(H * S * 0.13)
    for r in range(260, 0, -6):
        g.ellipse([mx - r, my - r, mx + r, my + r], fill=int(70 * (1 - r / 260) ** 2))
    img.paste((210, 200, 150), (0, 0), glow.filter(ImageFilter.GaussianBlur(30)))
    d.ellipse([mx - 40, my - 40, mx + 40, my + 40], fill=(236, 228, 196))

    def ridge(base, amp, col, seed):
        random.seed(seed)
        pts, x, y = [], 0, base
        while x <= W * S + 40:
            pts.append((x, y))
            x += random.randint(30, 70)
            y = base + random.uniform(-amp, amp) * 0.5 + math.sin(x / (W * S) * math.pi * random.uniform(1.5, 2.5)) * amp * 0.6
        d.polygon(pts + [(W * S, H * S), (0, H * S)], fill=col)

    ridge(H * S * 0.58, 90, (28, 40, 52), 1)
    ridge(H * S * 0.66, 70, (22, 34, 40), 2)
    ridge(H * S * 0.75, 50, (16, 26, 28), 3)

    def pine(x, base, h, col):
        w = h * 0.38
        for i in range(4):
            yy, ww = base - h + i * h * 0.2, w * (0.35 + i * 0.22)
            d.polygon([(x, yy), (x - ww, yy + h * 0.32), (x + ww, yy + h * 0.32)], fill=col)
        d.rectangle([x - h * 0.03, base - h * 0.12, x + h * 0.03, base], fill=col)

    for _ in range(70):
        pine(random.randrange(-40, W * S + 40), H * S * 0.86 + random.uniform(-30, 60), random.uniform(110, 240), (10, 17, 17))
    d.rectangle([0, int(H * S * 0.92), W * S, H * S], fill=(8, 13, 13))
    img = img.resize((W, H), Image.LANCZOS)
    shade = Image.new("L", (W, H), 0)
    sd = ImageDraw.Draw(shade)
    for x in range(W):
        sd.line([(x, 0), (x, H)], fill=int(120 * max(0, 1 - x / (W * 0.7))))
    img.paste((0, 0, 0), (0, 0), shade)
    img.save(os.path.join(OUT, "background.png"), optimize=True)


def icon():
    N = 1024
    ic = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(ic)
    d.ellipse([20, 20, N - 20, N - 20], fill=(214, 170, 80, 255))
    d.ellipse([64, 64, N - 64, N - 64], fill=(20, 34, 38, 255))
    scene = Image.new("RGBA", (N, N))
    s = ImageDraw.Draw(scene)
    for y in range(N):
        t = y / N
        s.line([(0, y), (N, y)], fill=(int(18 + 20 * t), int(32 + 28 * t), int(44 + 12 * t), 255))
    s.polygon([(60, 760), (380, 380), (520, 540), (650, 330), (980, 760)], fill=(58, 92, 94, 255))
    s.polygon([(380, 380), (430, 450), (400, 446), (362, 474), (338, 440)], fill=(232, 234, 222, 255))
    s.polygon([(650, 330), (712, 418), (676, 412), (640, 444), (608, 402)], fill=(232, 234, 222, 255))
    s.rectangle([0, 760, N, N], fill=(16, 40, 32, 255))
    cx = 512
    for i in range(4):
        y, w = 380 + i * 92, 74 + i * 50
        s.polygon([(cx, y), (cx - w, y + 146), (cx + w, y + 146)], fill=(22, 84, 58, 255))
    s.rectangle([cx - 24, 800, cx + 24, 880], fill=(84, 58, 34, 255))
    pts = []
    for k in range(8):
        a, rr = k * math.pi / 4 - math.pi / 2, (130 if k % 2 == 0 else 34)
        pts.append((cx + rr * math.cos(a), 250 + rr * math.sin(a)))
    s.polygon(pts, fill=(240, 196, 96, 255))
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).ellipse([96, 96, N - 96, N - 96], fill=255)
    ic.paste(scene, (0, 0), mask)
    small = ic.resize((256, 256), Image.LANCZOS)
    small.save(os.path.join(OUT, "icon.png"))
    small.save(os.path.join(OUT, "icon.ico"), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])


if __name__ == "__main__":
    background()
    icon()
