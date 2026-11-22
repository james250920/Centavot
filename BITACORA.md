# Bitácora · App Android de Centavot

Última actualización: **22 de noviembre de 2026** · último commit `497c4bf` (subido a `Centavot`).

## Dónde me quedé

Subí **"Compartir" desde otras apps**: Centavot aparece como "Anotar en Centavot" al compartir una imagen (constancia
de Yape o Plin, foto de boleta); la lee con el OCR del backend y abre **Nuevo gasto** ("¡Yapeaste!") o **Nueva venta**
("Te yapearon") ya llenos. El filtro está en el APK compilado, pero **falta probarlo en el teléfono** (se desconectó).
El teléfono tiene instalada la versión anterior (foto de la boleta, aviso `2026-11e`).

## Estado

| Qué | Estado |
|---|---|
| Sincronización con https://cv.menfroyt-dev.com | ✅ Probada en el teléfono: sube y baja en ~4 s; borrados incluidos |
| Base local | ✅ Room versión 8 (cambios pendientes con disparadores de SQLite; celular y WhatsApp en el perfil) |
| Aviso de privacidad | ✅ Versión `2026-11f`; nada se sincroniza antes de aceptarlo |
| Celular y casilla de WhatsApp | ✅ En Ajustes → Tu perfil |
| Foto de la boleta (OCR) | ✅ En Nuevo gasto: cámara o galería; el gasto queda con origen FOTO |
| Compartir desde Yape/Plin | ⏳ Hecho, falta probar en el teléfono |
| Modo (negocio/personal) | ✅ De cada dispositivo, no se sincroniza |

## Pendiente

1. Instalar la versión nueva (`./gradlew :androidApp:assembleDebug` y `adb install -r`), aceptar el aviso `2026-11f`
   y probar Compartir desde Yape.
2. **Generar un AAB nuevo** (`./gradlew :androidApp:bundleRelease`) y subirlo a la prueba interna de Google Play: el
   publicado no sincroniza.
3. Registrar en Firebase las **huellas SHA-1 de Play** (llave de subida y firma de Google), o el ingreso con Google
   falla en la versión de Play.
4. **Respaldar la llave de subida** (`~/keystores/centavot-upload.jks`) fuera de la laptop.
5. iOS (`iosApp/`) no tiene Firebase ni sincronización; en iPhone se usa la PWA.

## Notas

- La URL del backend sale de `centavot.apiUrl` en `local.properties` (por defecto `https://cv.menfroyt-dev.com`); la
  versión de depuración permite `http://localhost` para probar con `adb reverse`.
- Al probar en el teléfono: revisar que esté libre, `adb shell svc power stayon usb` durante la prueba y `false` al
  terminar.

## Historial reciente

| Fecha | Commit | Qué |
|---|---|---|
| 22/11 | `497c4bf` | Centavot aparece en Compartir (Yape, Plin, boletas); aviso `2026-11f` |
| 22/11 | `2ff233e` | Foto de la boleta en Nuevo gasto (OCR); aviso `2026-11e` |
| 22/11 | `3b8555a` | El modo es de cada dispositivo |
| 22/11 | `69891d4` | Celular y WhatsApp en Ajustes (Room v8); aviso `2026-11d` |
| 22/11 | `09b794e` | Aviso explica la sincronización (`2026-11c`) |
| 17/11 | `28f6d0e` | Sincronización con el backend (Room v7) |
