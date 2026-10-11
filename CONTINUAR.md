# Dónde nos quedamos

Última sesión: **10 de octubre de 2026**. El detalle de cada cambio está en [`reporte.md`](reporte.md).

## Estado

- `main` tiene todo lo hecho hasta el **paso 18** (PR #10), instalado en el teléfono (`com.app.centavot`).
- Ronda 1 (pasos 1–9): mejoras de la revisión UX/UI.
- Ronda 2 (pasos 10–18):
  - modo personal o negocio;
  - 35 % menos texto en las pantallas;
  - botones de tamaño estándar.
- Base de datos en versión **4** (migración automática 3 → 4).
- 99 tests y 0 fallas.

## Lo último que se hizo

| Paso | Qué | PR |
|---|---|---|
| 10 | Modo personal o negocio: se elige en la bienvenida y se cambia desde Inicio y Ajustes. "Saqué para la casa" | #7 |
| 11–17 | Menos texto: ayudas plegables (`AyudaPlegable`), una línea por mensaje y Ayuda en lista | #9 |
| 18 | Tamaños: Venta/Gasto 56 dp, botones de formulario 48 dp, números menos grandes | #10 |

## Para retomar

1. **Capturas desactualizadas.** `capturas-app/` (repo de documentos) y la galería
   (https://claude.ai/artifact/2pS1n5pH9wHK1K9F1ddLPW) muestran la interfaz de **antes** de los
   pasos 11–18. Hay que volver a tomarlas.
2. Siguientes pendientes de la app, según `Centavot_Pendientes.md` §8:
   - **P13**: las ventas frecuentes se llenan con los totales del día.
   - **Q3**: probar con TalkBack.
   - **Q5**: probar en un teléfono de gama baja.
   - **P4**: opciones RMT y "aún no tengo RUC". Espera lo que diga el contador.
3. Regla hasta diciembre: no se agregan funciones nuevas. Solo entra lo que sirva para validar con
   usuarios.

## Cómo probar

- Copia de prueba aparte, que no toca la app real del teléfono:
  - el paquete es `com.app.centavot.prueba`;
  - se compila con `applicationId` + `.prueba` y se instala con `adb install`.
  - El script de la sesión que lo hacía (`prueba.sh`) estaba en una carpeta temporal y se pierde,
    así que hay que rehacerlo.
- Tests: `./gradlew :shared:testAndroidHostTest --offline`.
- APK: `./gradlew :androidApp:assembleDebug`.
- Teléfono: Samsung Galaxy A15 con Android 16, densidad 450 (384 dp de ancho).
- Cada cambio va así:
  1. una rama nueva;
  2. un paso nuevo en `reporte.md`;
  3. la prueba en el teléfono;
  4. el PR.
- Commits y PR con el usuario **james250920**.
