# Para continuar

Actualizado el **20 de octubre de 2026**.

- **Dónde está la app:** `main` incluye todo hasta el **paso 34** (marca Andina: colores, letra Sora, logotipo e ícono). Base de datos
  versión 6. Instalada en el teléfono (`com.app.centavot`) desde el 20 de octubre.
- **GitHub:** subido hasta el paso 32. Los pasos 33 y 34 están solo en este equipo: no subir hasta que se pida.
- **Lo ya hecho:** está en [`reporte.md`](reporte.md).
- **Requerimientos y pendientes:** en `Centavot_03_Requerimientos.md` (repositorio de documentos).
  Aquí solo va lo que toca a la app.

## Falta por hacer

### 1. Se puede hacer ya (sin backend y sin esperar a nadie)

| ID | Qué | Prioridad | Esfuerzo |
|---|---|---|---|
| Q2 | Pruebas de interfaz automatizadas (hoy se prueba a mano en el teléfono) | 🟠 | M |

### 1b. Registro automático (reunión con usuarios del 10 de octubre)

No se programó: cambia la promesa de que la app no usa internet ni lee nada fuera de ella. Hay que
llevar una propuesta (cómo funcionaría, privacidad y costos) a la siguiente reunión con los usuarios.

| Idea | Quién la pidió | Qué implica |
|---|---|---|
| Leer los correos de pago (Yape, Plin, tarjeta, Uber) y autocompletar el gasto | Ronal | Acceso al correo (Gmail): internet, iniciar sesión con Google y permisos; hoy la app no tiene permiso de internet |
| Leer las notificaciones de pago de Yape o Plin en el teléfono | Alternativa sin internet | Permiso de acceso a notificaciones; actualizar el aviso de privacidad (Ley 29733) |
| Foto de la boleta o factura con OCR | James, con reservas de Ronal | Es el B7 de los pendientes: primero la prueba H6 con 50 fotos (≥ 85 % correcto) |
| QR para registrar | James | Útil sobre todo en el negocio; definir qué QR se leería (¿boleta electrónica?) |

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
  - `sembrar.sh` solo funciona con la versión de debug: la de release no permite `run-as`.
  - Antes de tocar la pantalla, revisar que el teléfono no esté en uso (una llamada flotante o
    WhatsApp abierto): `ui.py` se detiene si la copia de prueba no está al frente, pero no ve las
    ventanas flotantes.
- **Comandos:**

  | Para | Comando |
  |---|---|
  | Tests | `./gradlew :shared:testAndroidHostTest --offline` |
  | APK de debug | `./gradlew :androidApp:assembleDebug` |
  | APK de release (con R8) | `./gradlew :androidApp:assembleRelease` (la primera vez necesita conexión) |

- **Tema:** todas las pantallas van siempre en claro, aunque el teléfono esté en oscuro. Las
  capturas también se toman en claro.
- **Teléfono:** Samsung Galaxy A15 con Android 16, densidad 450 (384 dp de ancho).
- **Cada cambio va así:**
  1. una rama nueva;
  2. un paso nuevo en `reporte.md`;
  3. la prueba en el teléfono;
  4. el PR.
- Commits y PR con el usuario **james250920**.
