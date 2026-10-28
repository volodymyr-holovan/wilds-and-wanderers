#!/bin/bash
# Join test against the LOCAL test server: launch the client with auto-join, wait until the server log says the
# player joined (or failed), keep the player online for STAY seconds, then report and close the client.
# usage: join_test.sh [name] [stay_seconds]
# env: SCENARIO=file, XMX=4G, CLIENT_JVM="-Dprop=value -Dother=value" (JVM arguments, no spaces inside one), SIZE=1600x900, GAME_DIR=another game folder
R="/e/Minecraft Server 1.21.1"; I="${GAME_DIR:-/c/Users/ketch/Games/Wilds-and-Wanderers-1.21.1}"; LOG="$R/testserver/console.log"
NAME="${1:-WW_Check}"; STAY="${2:-60}"
start=$(wc -l < "$LOG")
out=$(python -I "$R/pack/tools/launch_client.py" --game-dir "$I" --name "$NAME" --join 127.0.0.1:25570 --java "$R/_tools/java21/bin/java.exe" --xmx "${XMX:-6G}" $(for a in $CLIENT_JVM; do printf -- "--jvm-arg=%s " "$a"; done) ${SIZE:+--size "$SIZE"})
P=${out##* }; echo "$out"
alive() { powershell -NoProfile -Command "if (-not (Get-Process -Id $P -ErrorAction SilentlyContinue)) { exit 1 }"; }
result=TIMEOUT
for i in $(seq 1 120); do
  sleep 5
  new=$(tail -n +"$start" "$LOG" | grep -a -E "$NAME (joined the game|lost connection|left the game)|Couldn't place player")
  if echo "$new" | grep -qE "lost connection|Couldn't place"; then result=FAILED; break; fi
  if tail -n +"$start" "$LOG" | grep -aqE "Player $NAME joined|$NAME joined the game"; then result=JOINED; break; fi
  alive || { result=CLIENT_EXITED; break; }
done
if [ "$result" = JOINED ]; then
  RC="$(cat "$R/_tools/test_rcon_password.txt")"
  rcon() { python -I "$R/pack/tools/rcon.py" 127.0.0.1 25580 "$RC" "$1" 2>&1 | tail -n 2 | cut -c1-200; }
  sleep 5; rcon "auth force-register $NAME test1234"; rcon "auth force-login $NAME"
  # optional scenario: one console command per line, run while the player is online ({p} = player name)
  if [ -n "$SCENARIO" ]; then while IFS= read -r c; do [ -n "$c" ] && { echo "> $c"; rcon "${c//\{p\}/$NAME}"; sleep 1; }; done < "$SCENARIO"; fi
  sleep "$STAY"
  tail -n +"$start" "$LOG" | grep -aqE "$NAME (lost connection|left the game)" && result=DROPPED_AFTER_JOIN
  alive || result=CLIENT_DIED_AFTER_JOIN
fi
echo "RESULT: $result"
tail -n +"$start" "$LOG" | grep -a -E "$NAME|/ERROR\]|Exception" | grep -av "DISTXFORM\|Error loading class" | cut -c1-240 | head -25
echo "--- client errors:"
grep -a -E "Client disconnected|/ERROR\]|/FATAL\]" "$I/logs/latest.log" | cut -c1-240 | tail -15
powershell -NoProfile -Command "Stop-Process -Id $P -Force -ErrorAction SilentlyContinue"
