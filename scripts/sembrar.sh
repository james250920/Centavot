#!/bin/bash
# Llena la copia de prueba con los datos de demostración (scripts/demo.py).
# Uso: scripts/sembrar.sh [AAAA-MM-DD]   (por defecto, hoy). Antes hay que pasar la bienvenida en la app.
set -e
cd "$(dirname "$0")/.."
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
P=com.app.centavot.prueba
T=$(mktemp -d)
"$ADB" shell am force-stop $P
for f in centavot.db centavot.db-wal centavot.db-shm; do "$ADB" exec-out run-as $P cat databases/$f > "$T/$f" 2>/dev/null || true; done
sqlite3 "$T/centavot.db" "pragma wal_checkpoint(TRUNCATE);" >/dev/null
python3 -I scripts/demo.py "$T/centavot.db" "${1:-$(date +%F)}"
"$ADB" push "$T/centavot.db" /data/local/tmp/c.db >/dev/null
"$ADB" shell "run-as $P sh -c 'rm -f databases/centavot.db*; cat /data/local/tmp/c.db > databases/centavot.db'; rm /data/local/tmp/c.db"
rm -r "$T"
"$ADB" shell am start -n $P/com.app.centavot.MainActivity >/dev/null
sleep 3
