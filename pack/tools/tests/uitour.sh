#!/bin/bash
# Screenshots of the whole interface at every GUI scale (wilds_core UiTour): ui_<screen>_s<scale>.png in
# /e/wwtest10/.minecraft/screenshots.   usage: uitour.sh [tour spec, default "true"] [WxH, default 1920x1080]
R="/e/Minecraft Server 1.21.1"; cd "$R"
RC="$(cat _tools/test_rcon_password.txt)"
r(){ python -I pack/tools/rcon.py 127.0.0.1 25580 "$RC" "$1" 2>&1 | tail -n +2 | grep -v "^$" | cut -c1-300; }
SPEC="${1:-true}"; SIZE="${2:-1920x1080}"
sed -i 's/^level-name=.*/level-name=balance_test/' testserver/server.properties
python -I pack/tools/sync_overrides.py /e/wwtest10/.minecraft >/dev/null
rm -f /e/wwtest10/.minecraft/screenshots/ui_*.png
bash pack/tools/tests/restart_with_jar.sh "" | tail -1
start=$(wc -l < testserver/console.log)
(GAME_DIR=/e/wwtest10/.minecraft SIZE=$SIZE XMX=4G CLIENT_JVM="-Dwilds_core.uitour=$SPEC" bash pack/tools/join_test.sh WW_Col 900 > "$TEMP/uitour_join.log" 2>&1 &)
until tail -n +"$start" testserver/console.log | grep -aqE "WW_Col joined the game|WW_Col lost connection"; do sleep 3; done
sleep 8
r "tp WW_Col -600 90 -1478"; r "time set 6000"; r "weather clear"; r "gamemode survival WW_Col"
r "effect give WW_Col minecraft:resistance infinite 4 true"; r "effect give WW_Col minecraft:regeneration infinite 4 true"
r "item replace entity WW_Col weapon.mainhand with minecraft:diamond_sword"; r "item replace entity WW_Col armor.chest with minecraft:iron_chestplate"
r "give WW_Col minecraft:bread 16"; r "give WW_Col minecraft:torch 32"
for i in 1 2 3; do r "tellraw WW_Col {\"text\":\"Тестове повідомлення $i: як чат виглядає поверх інтерфейсу\"}"; done
for i in $(seq 1 150); do sleep 5; grep -aq "\[uitour\] done" /e/wwtest10/.minecraft/logs/latest.log && break; done
grep -a "\[uitour\]" /e/wwtest10/.minecraft/logs/latest.log | grep -v "pressed" | sed 's/.*\[uitour\] //' | cut -c1-160
ls /e/wwtest10/.minecraft/screenshots/ | grep -c "^ui_"
