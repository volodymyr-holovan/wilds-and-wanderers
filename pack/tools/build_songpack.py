"""Build the pack's Reactive Music songpack into a game folder: resourcepacks/WildsMusic.

  python build_songpack.py <game dir>

The tracks come from the Reactive Music jar in <game dir>/mods (the artists' music it ships), the song rules from
pack/music/ReactiveMusic.yaml. Every song the YAML names must exist. The songpack is made active in
config/ReactiveMusic.json5 (no delay between songs, the pack's own switch speed).
"""
import glob, json, os, re, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
YAML = os.path.join(HERE, "..", "music", "ReactiveMusic.yaml")
game = sys.argv[1]
jar = glob.glob(os.path.join(game, "mods", "reactivemusic-*.jar"))
if not jar:
    sys.exit("Reactive Music is not installed in " + game)
text = open(YAML, encoding="utf-8").read()
name = re.search(r'^name:\s*"([^"]+)"', text, re.M)[1]
wanted = set()
for line in re.findall(r"songs:\s*\[([^\]]*)\]", text, re.S):
    wanted |= set(re.findall(r'"([^"]+)"', line))
out = os.path.join(game, "resourcepacks", "WildsMusic")
os.makedirs(os.path.join(out, "music"), exist_ok=True)
with zipfile.ZipFile(jar[0]) as z:
    have = {os.path.splitext(os.path.basename(n))[0]: n for n in z.namelist() if n.startswith("musicpack/music/") and n.endswith(".mp3")}
    missing = wanted - set(have)
    if missing:
        sys.exit("songs not in the jar: " + ", ".join(sorted(missing)))
    for song in sorted(wanted):
        dst = os.path.join(out, "music", song + ".mp3")
        if not os.path.exists(dst) or os.path.getsize(dst) != z.getinfo(have[song]).file_size:
            open(dst, "wb").write(z.read(have[song]))
open(os.path.join(out, "ReactiveMusic.yaml"), "w", encoding="utf-8").write(text)
open(os.path.join(out, "pack.mcmeta"), "w", encoding="utf-8").write(json.dumps(
    {"pack": {"pack_format": 34, "description": "Wilds & Wanderers: музика"}}, ensure_ascii=False))
cfg = os.path.join(game, "config", "ReactiveMusic.json5")
c = json.load(open(cfg, encoding="utf-8")) if os.path.exists(cfg) else {}
c.update({"loadedUserSongpack": name, "musicDelayLength2": "SONGPACK_DEFAULT", "musicSwitchSpeed2": "SONGPACK_DEFAULT"})
# Reactive Music falls silent while a mod plays its own boss or place theme (it matches parts of the sound path),
# wherever the sound is, so the two never play over each other
MUTE = ["_music", "music.boss", "music_wormboss", "dead_king.music", "music.fire_boss", "_theme", "music.dragon", "music_disc"]
for key in ("soundsMuteMusic", "soundsMuteMusicIgnoreDistance"):
    c[key] = sorted(set(c.get(key, [])) | set(MUTE))
os.makedirs(os.path.dirname(cfg), exist_ok=True)
json.dump(c, open(cfg, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
print(f"songpack '{name}': {len(wanted)} songs in {out}")
