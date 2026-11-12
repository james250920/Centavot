#!/bin/bash
# Construye e instala una copia de prueba (com.app.centavot.prueba) que no toca la app real del teléfono.
# Uso: scripts/prueba.sh            → compila e instala
#      scripts/prueba.sh limpia     → además borra sus datos (empieza desde la bienvenida)
set -e
cd "$(dirname "$0")/.."
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
P=com.app.centavot.prueba
APK=build/centavot-prueba.apk
restaurar() { git checkout -- androidApp/build.gradle.kts androidApp/src/main/res/values/strings.xml androidApp/google-services.json; }
trap restaurar EXIT
sed -i 's/applicationId = "com.app.centavot"/applicationId = "com.app.centavot.prueba"/' androidApp/build.gradle.kts
sed -i 's/>Centavot</>Centavot prueba</' androidApp/src/main/res/values/strings.xml
# Firebase exige que el paquete esté en google-services.json; si la copia de prueba no está registrada, se usa la de la app.
grep -q '"com.app.centavot.prueba"' androidApp/google-services.json || sed -i 's/"package_name": "com.app.centavot"/"package_name": "com.app.centavot.prueba"/' androidApp/google-services.json
./gradlew :androidApp:assembleDebug --offline -q 2>&1 | grep -E "^e:" || true
mkdir -p build && cp androidApp/build/outputs/apk/debug/androidApp-debug.apk "$APK"
"$ADB" install -r "$APK" | tail -1
if [ "$1" = "limpia" ]; then "$ADB" shell pm clear $P >/dev/null; fi
"$ADB" shell am start -n $P/com.app.centavot.MainActivity >/dev/null
