#!/bin/bash
# Restart the local test server and wait for "Done"; prints the Done line or crash info.
cd "/e/Minecraft Server 1.21.1"
P="$(cat _tools/test_rcon_password.txt)"
python -I pack/tools/rcon.py 127.0.0.1 25580 "$P" "stop" >/dev/null 2>&1
for i in $(seq 1 60); do
  sleep 2
  n=$(powershell -NoProfile -Command "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*win_args.txt*' }).Count")
  [ "$n" = "0" ] && break
done
# a server that ignored "stop" would keep the port and silently serve the old mods: kill it
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*win_args.txt*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"
python -I pack/tools/sync_overrides.py testserver --no-initial >/dev/null || { echo "sync_overrides failed"; exit 1; }
(cd testserver && ../_tools/java21/bin/java.exe @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.264/win_args.txt nogui > console.log 2>&1 < /dev/null &) > /dev/null 2>&1   # the subshell must not hold a caller's pipe open
for i in $(seq 1 150); do
  sleep 3
  grep -aqE "Done \(|Crash report saved|Mod loading has failed" testserver/console.log && break
done
grep -aE "Done \(|Crash report saved|Mod loading has failed" testserver/console.log
