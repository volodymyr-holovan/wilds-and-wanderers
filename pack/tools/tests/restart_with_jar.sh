#!/bin/bash
# Stop the test client and the local test server, install the freshly built wilds_core jar everywhere it runs,
# and start the server with the given extra JVM arguments.   usage: restart_with_jar.sh ["-Dprop=value ..."]
R="/e/Minecraft Server 1.21.1"; cd "$R"
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { \$_.CommandLine -like '*wwtest10*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"
J=mods-src/wilds_core/build/wilds_core-0.1.0.jar
printf -- "-Xms4G\n-Xmx6G\n" > testserver/user_jvm_args.txt
for a in $1; do echo "$a" >> testserver/user_jvm_args.txt; done
# restart_test.sh stops the server (killing one that ignores "stop"), syncs overrides and waits for "Done"
python -I pack/tools/rcon.py 127.0.0.1 25580 "$(cat _tools/test_rcon_password.txt)" "stop" >/dev/null 2>&1
for i in $(seq 1 40); do sleep 2; n=$(powershell -NoProfile -Command "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*win_args.txt*' }).Count"); [ "$n" = "0" ] && break; done
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*win_args.txt*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"
sleep 2
for d in client/mods server/mods testserver/mods /e/wwtest10/.minecraft/mods; do cp "$J" "$d/"; done
[ -f mods-src/wilds_wiki/build/wilds_wiki-0.2.0.jar ] && for d in client/mods /e/wwtest10/.minecraft/mods; do cp mods-src/wilds_wiki/build/wilds_wiki-0.2.0.jar "$d/"; done
bash pack/tools/restart_test.sh
