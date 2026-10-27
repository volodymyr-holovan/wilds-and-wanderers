"""Screenshots from PhotoMode (game dir/wiki_photos/<structure|biome>/<ns>/<path>.png) -> small JPEGs the wiki ships:
src/main/resources/assets/wilds_wiki/photos/<structure|biome>/<ns>/<path>.jpg (400x225, ~20 KB each).

  python pack_photos.py [photo dir]

A photo that is almost one colour (the camera inside a block, an empty sky) is skipped and listed, so it can be retaken.
"""
import glob, os, sys
from PIL import Image, ImageStat

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\ketch\Games\Wilds-and-Wanderers-1.21.1\wiki_photos"
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "photos")
W, H = 400, 225

done, flat, size = 0, [], 0
for p in sorted(glob.glob(os.path.join(SRC, "**", "*.png"), recursive=True)):
    rel = os.path.relpath(p, SRC)[:-4] + ".jpg"
    img = Image.open(p).convert("RGB").resize((W, H), Image.LANCZOS)
    if max(ImageStat.Stat(img).stddev) < 12:
        flat.append(rel)
        continue
    dst = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    img.save(dst, "JPEG", quality=80, optimize=True, progressive=False)
    size += os.path.getsize(dst)
    done += 1
print(f"{done} photos, {size // 1024} KB")
if flat:
    print("skipped (one colour):", *flat, sep="\n  ")
