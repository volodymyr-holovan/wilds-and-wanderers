#!/bin/bash
# Structures and trades for the wiki, unattended, on the LOCAL test server (after the biome dump):
# installs the wilds_dump dev mod, starts the flat photo dimension afresh, places and scans every structure
# (one per tick), then dumps the trades. Output: testserver/wilds_dump/{structures.jsonl,trades.json}.
R="/e/Minecraft Server 1.21.1"
P="$(cat "$R/_tools/test_rcon_password.txt")"
rcon() { python -I "$R/pack/tools/rcon.py" 127.0.0.1 25580 "$P" "$1" 2>&1 | tail -n 1; }
python -I "$R/mods-src/wilds_dump/build.py" --install | tail -1
python -I "$R/pack/tools/rcon.py" 127.0.0.1 25580 "$P" "stop" >/dev/null 2>&1
sleep 20
rm -rf "$R/testserver/world/dimensions/wilds_dump"
bash "$R/pack/tools/restart_test.sh" | tail -1
sleep 15   # RCON starts listening a moment after "Done"
LOG="$R/testserver/console.log"
rcon "wildsdump biomes"   # resumes: only biomes without a sample yet
for i in $(seq 1 360); do
  sleep 20
  grep -aqE "WildsDump: ([0-9]+)/\1\s*$" "$LOG" && break
  grep -aqE "Crash report saved|Server Watchdog" "$LOG" && { echo "CRASH during biomes"; exit 1; }
done
echo "biomes done"
start=$(wc -l < "$LOG")
rcon "wildsdump structures"
for i in $(seq 1 720); do
  sleep 20
  tail -n +"$start" "$LOG" | grep -aqE "WildsDump: ([0-9]+)/\1\s*$" && break
  grep -aqE "Crash report saved|Server Watchdog" "$LOG" && { echo "CRASH during structures"; exit 1; }
done
echo "structures: $(wc -l < "$R/testserver/wilds_dump/structures.jsonl") lines"
rcon "wildsdump trades"
for i in $(seq 1 60); do sleep 5; grep -aq "wrote trades.json" "$LOG" && break; done
grep -a "WildsDump" "$LOG" | tail -3
