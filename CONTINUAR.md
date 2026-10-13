# Dónde nos quedamos

Última sesión: **13 de octubre de 2026**. El detalle de cada cambio está en [`reporte.md`](reporte.md).

## Estado

- `main` tiene todo lo hecho hasta el **paso 22** (PR #15), instalado en el teléfono (`com.app.centavot`).
- Ronda 1 (pasos 1–9): mejoras de la revisión UX/UI.
- Ronda 2 (pasos 10–18):
  - modo personal o negocio;
  - 35 % menos texto en las pantallas;
  - botones de tamaño estándar.
- Base de datos en versión **4** (migración automática 3 → 4).
- 101 tests y 0 fallas.

## Lo último que se hizo

| Paso | Qué | PR |
|---|---|---|
| 10 | Modo personal o negocio: se elige en la bienvenida y se cambia desde Inicio y Ajustes. "Saqué para la casa" | #7 |
| 11–17 | Menos texto: ayudas plegables (`AyudaPlegable`), una línea por mensaje y Ayuda en lista | #9 |
| 18 | Tamaños: Venta/Gasto 56 dp, botones de formulario 48 dp, números menos grandes | #10 |
| 19 | P13: solo son frecuentes las ventas que se repiten (el total del día ya no llena la lista) | #11 |
| 20–21 | TalkBack (plegables, títulos, botones de cobros) y gama baja (barra de abajo, peso y rendimiento medidos) | #13 |
| 22 | R8 en release: el APK baja de 16.9 a 7.6 MB | #15 |

## Para retomar

1. Pendientes que necesitan a una persona o un teléfono:
   - **Q3**: escuchar Inicio, Venta y Cobros con TalkBack, sobre todo cómo se leen los montos.
   - **Q5**: probar en un teléfono de 2 GB con Android 8–10.
2. **P4**: opciones RMT y "aún no tengo RUC". Espera lo que diga el contador.
3. Regla hasta diciembre: no se agregan funciones nuevas. Solo entra lo que sirva para validar con
   usuarios.

## Cómo probar

- Copia de prueba aparte, que no toca la app real del teléfono (`com.app.centavot.prueba`):
  - `scripts/prueba.sh` la compila e instala; con `limpia` empieza desde la bienvenida.
  - `scripts/ui.py` la maneja (`tap`, `texto`, `dump`, `shot`...). Se detiene si la copia de
    prueba no está al frente.
  - Datos de demostración: se baja la base con `run-as`, se llena con `scripts/demo.py`
    (la bodega de Rosa, de mayo a hoy) y se vuelve a subir.
  - Al terminar: `adb uninstall com.app.centavot.prueba`.
- Tests: `./gradlew :shared:testAndroidHostTest --offline`.
- APK: `./gradlew :androidApp:assembleDebug`. Release (con R8): `./gradlew :androidApp:assembleRelease`; la primera vez necesita conexión.
- Teléfono: Samsung Galaxy A15 con Android 16, densidad 450 (384 dp de ancho).
- El emulador no arranca en esta computadora (se cierra al iniciar).
- Cada cambio va así:
  1. una rama nueva;
  2. un paso nuevo en `reporte.md`;
  3. la prueba en el teléfono;
  4. el PR.
- Commits y PR con el usuario **james250920**.
