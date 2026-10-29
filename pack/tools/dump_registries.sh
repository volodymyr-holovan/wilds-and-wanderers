#!/bin/bash
# Dump registries of the running local test server into docs/registries (quest generator checks ids against them).
R="/e/Minecraft Server 1.21.1"; RC="$(cat "$R/_tools/test_rcon_password.txt")"
for r in minecraft:item minecraft:entity_type minecraft:worldgen/biome minecraft:worldgen/structure minecraft:dimension_type minecraft:attribute; do
  f="$R/docs/registries/$(echo "${r#minecraft:}" | tr '/' '_').txt"
  python -I "$R/pack/tools/rcon.py" 127.0.0.1 25580 "$RC" "kubejs dump registry $r" | grep -a '^- ' | sed 's/^- //' | sort > "$f"
  echo "$r: $(wc -l < "$f") -> $f"
done
