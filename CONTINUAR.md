# Para continuar

Actualizado el **13 de octubre de 2026**.

- **Dónde está la app:** `main` incluye todo hasta el paso 24 y está instalado en el teléfono
  (`com.app.centavot`).
- **Lo ya hecho:** está en [`reporte.md`](reporte.md).
- **Lista completa de pendientes:** está en `Centavot_Pendientes.md`, en el repositorio de documentos.
  Aquí solo va lo que toca a la app.

## Falta por hacer

### 1. Se puede hacer ya (sin backend y sin esperar a nadie)

| ID | Qué | Prioridad | Esfuerzo |
|---|---|---|---|
| P5 | Exportar el resumen del contador en PDF (hoy solo CSV) | 🟠 | M |
| Q2 | Pruebas de interfaz automatizadas (hoy se prueba a mano en el teléfono) | 🟠 | M |
| Q7 | Actualizar `Centavot_Prototipo_App.md` con las pantallas actuales | 🟠 | S |

### 2. Necesita a una persona o un teléfono

| ID | Qué | Nota |
|---|---|---|
| Q3 | Escuchar Inicio, Venta y Cobros con TalkBack prendido | Fijarse sobre todo en los montos: "S/ 3.50" puede salir "ese barra tres punto cincuenta". Por adb no se pudo manejar TalkBack |
| Q5 | Probar en un teléfono real de 2 GB con Android 8–10 | En el A15 abre en ≈0.5 s; uno así puede tardar 2 o 3 veces más. El emulador no arranca en esta computadora |
| P6 | Videos cortos de capacitación (venta, gasto, fiado, cierre del día) | Para la demo presencial |

### 3. Espera una decisión

| ID | Qué | Espera a | Qué cambia en la app |
|---|---|---|---|
| P4 | Opciones RMT y "aún no tengo RUC" al elegir el régimen, sin alertas de tope | Contador | Pantalla de régimen |
| DE1 | Si un fiado o pedido cuenta como venta al registrarlo (hoy sí, por defecto) | Contador | Ventas, tope y resumen del contador |
| DE2 | Qué gastos cuentan como "compras" para el tope del Nuevo RUS (hoy todos los del negocio) | Contador | Alertas de tope |
| DE3 | Topes y UIT vigentes del Nuevo RUS y del RER | Contador / SUNAT | `TopesReferenciales.kt` |
| DE7 | Correo de contacto oficial del responsable de los datos | Equipo | Aviso de privacidad (Ley 29733) |

### 4. Después de la decisión de diciembre

- **Backend (B1–B8):** login, sincronización, notificaciones push, indicadores de todos los usuarios,
  topes desde el servidor, foto del cuaderno y cifrado. El detalle está en `Centavot_Pendientes.md`
  §2.
- **Regla hasta diciembre:** no se agregan funciones nuevas. Solo entra lo que sirva para validar con
  usuarios y lo que pidan el contador o el piloto.

## Cómo trabajar

- **Copia de prueba aparte**, que no toca la app real del teléfono (`com.app.centavot.prueba`):
  - `scripts/prueba.sh` la compila e instala; con `limpia` empieza desde la bienvenida.
  - `scripts/ui.py` la maneja (`tap`, `texto`, `dump`, `shot`...). Se detiene si la copia de
    prueba no está al frente.
  - `scripts/sembrar.sh` carga los datos de demostración (la bodega de Rosa, de mayo a hoy, con
    `scripts/demo.py`). Antes hay que pasar la bienvenida en la app.
  - Al terminar: `adb uninstall com.app.centavot.prueba`.
- **Comandos:**

  | Para | Comando |
  |---|---|
  | Tests | `./gradlew :shared:testAndroidHostTest --offline` |
  | APK de debug | `./gradlew :androidApp:assembleDebug` |
  | APK de release (con R8) | `./gradlew :androidApp:assembleRelease` (la primera vez necesita conexión) |

- **Teléfono:** Samsung Galaxy A15 con Android 16, densidad 450 (384 dp de ancho).
- **Cada cambio va así:**
  1. una rama nueva;
  2. un paso nuevo en `reporte.md`;
  3. la prueba en el teléfono;
  4. el PR.
- Commits y PR con el usuario **james250920**.
