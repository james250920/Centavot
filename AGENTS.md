# AGENTS.md · Centavot · App Android

Guía para cualquier agente o persona que trabaje en esta carpeta (`/home/james/ESAN/Startup/App`).

<!-- red-centavot:inicio -->
> **Red de Centavot.** Este archivo es parte de un proyecto de cinco piezas. Antes de cambiar algo, lee el
> `AGENTS.md` de la carpeta donde trabajas: dice cómo se conectan y qué reglas seguir.

| Parte | Carpeta local | Repositorio | Dónde corre |
|---|---|---|---|
| Documentos (raíz) | `/home/james/Proyectos/StartUp` | [Centavot-docs](https://github.com/james250920/Centavot-docs) | — |
| Backend (API) | `/home/james/Proyectos/StartUp/backend` | [centavot_bk](https://github.com/james250920/centavot_bk) | https://cv.menfroyt-dev.com (puerto 3939) |
| Web (PWA) | `/home/james/Proyectos/StartUp/frontend` | [centavot_frontend](https://github.com/james250920/centavot_frontend) | https://centavot.menfroyt-dev.com (puerto 4231) |
| Landing | `/home/james/Proyectos/StartUp/WEB` | [Centavot-web](https://github.com/james250920/Centavot-web) | https://landing.menfroyt-dev.com (puerto 4335) |
| App Android ← **estás aquí** | `/home/james/ESAN/Startup/App` | [Centavot](https://github.com/james250920/Centavot) | Google Play (prueba interna) · `com.app.centavot` |

Se conectan así: **landing → web** (enlaces) · **web y app → backend** (API `/v1` con el token de Google) · **backend → PostgreSQL** (esquema `centavot`, script en `Centavot-docs/base de datos/`).
<!-- red-centavot:fin -->

## Esta parte

App de Centavot en Kotlin Multiplatform + Compose, con Room (base local) y sincronización con el backend.

Comandos: `./gradlew :androidApp:assembleDebug` y `./gradlew :shared:testAndroidHostTest`.

Lee primero `BITACORA.md` y `README.md`.

## Cómo se conectan

| De | A | Cómo |
|---|---|---|
| Landing | Web | Enlaces: "Abrir la app" → `https://centavot.menfroyt-dev.com/` y "Pruébala ahora, sin cuenta" → `/?demo=1` (entra directo a la demo) |
| Web | Backend | `QCLI_API_URL` (por defecto `https://cv.menfroyt-dev.com`, se fija al construir la imagen). Rutas `/v1/*` con `Authorization: Bearer <token de Firebase>`; la CSP de `index.html` solo permite ese origen |
| App Android | Backend | `centavot.apiUrl` en `androidApp/build.gradle.kts` (por defecto `https://cv.menfroyt-dev.com`; para uno local, `local.properties`) |
| Web y app | Backend | Sincronización `GET/POST /v1/sync` (cambios desde una marca y borrados en `eliminados`); OCR `POST /v1/comprobantes/leer`; consentimiento `POST /v1/consentimientos` (sin él, 403) |
| Web (demo) | Backend | Rutas públicas sin cuenta: `POST /calificaciones` y, solo los días que se encienda, `/demo/ocr` y `/demo/comprobantes/leer` |
| Backend | PostgreSQL | Base `centavot_db`, esquema `centavot`, usuario `centavot_app` (sin superusuario: la seguridad por filas decide qué ve cada uno). El esquema y las migraciones viven en `Centavot-docs/base de datos/` |
| Backend | Firebase | Verifica el token de "Continuar con Google" (proyecto `centavot-67f60`) |
| Backend | Google Gemini | Lee las fotos de boletas y constancias de Yape/Plin (modelos Gemma) |

Las tres piezas con servidor se publican en **Coolify** detrás de **Cloudflare**; Coolify despliega solo al subir a
`main` en GitHub.

## Reglas del proyecto

- **Paquetes:** solo `pnpm` (nunca `npm` ni `npx`) en backend, web y landing; la app usa `./gradlew`.
- **Commits:** con el usuario `james250920`, sin líneas de coautoría de herramientas. **Subir a GitHub solo cuando el
  dueño lo pida** (Coolify despliega en cuanto llega a `main`).
- **Secretos:** nunca subir ni mostrar `.env`, `keystore.properties` ni llaves `.jks`. Revisar el diff antes de cada push.
- **Base de datos:** un cambio en producción necesita la aprobación del dueño, y la migración se aplica **antes** de subir
  el código que la necesita.
- **Diseño:** tema claro siempre. Marca: Tinta `#1B1410`, Lana `#FAF6EF`, Maíz `#F4B400`, Carmín `#C1121F`, letra Sora.
- **Datos personales (Ley 29733):** cualquier dato nuevo que salga del teléfono o vaya a terceros debe estar en el aviso
  de privacidad.
