# Para continuar

Actualizado el **20 de octubre de 2026**.

- **Dónde está la app:** `main` incluye todo hasta el **paso 32** (presupuesto según la guía, RP-01 a RP-10). Base de datos
  versión 6. Instalada en el teléfono (`com.app.centavot`).
- **GitHub:** `main` subido el 20 de octubre, en la app y en documentos. Subir solo cuando se pida.
- **Lo ya hecho:** está en [`reporte.md`](reporte.md).
- **Requerimientos y pendientes:** en `Centavot_03_Requerimientos.md` (repositorio de documentos).
  Aquí solo va lo que toca a la app.

## Dónde me quedé (20 de octubre, en pausa)

**Paso 33 en curso: menos texto y un bloque por grupo en el presupuesto.** Lo pidió el usuario ("se ve
mucho texto" y que "Arma tu presupuesto" tenga una división entre grupos), con la skill de diseño
`impeccable` (modo *distill*).

- **Rama:** `ux/presupuesto-bloques`, con un commit de trabajo en curso. Compila y pasan los 134 tests.
  No está fusionada en `main` ni subida.
- **Hecho en código:**
  - `BloqueGrupo.kt` (nuevo): cada grupo va en un bloque blanco con borde, título y total arriba, y
    una línea fina entre filas.
  - **Arma tu presupuesto:**
    - sin el párrafo de arriba;
    - un bloque por grupo, con su total;
    - el monto a la derecha, sin caja, como en un cuaderno;
    - "Agregar" dentro de cada grupo, con un diálogo que solo pide el nombre;
    - abajo, el balance en corto ("Te sobran S/ 240 · pásalos a tu ahorro").
  - **Tu presupuesto:**
    - resumen arriba, con el balance del plan y el aviso de deudas;
    - la tarjeta "Tu reparto" en filas;
    - un bloque por grupo, con lo real resaltado y el plan apagado;
    - sin la pista "Toca una partida…";
    - en el negocio, el grupo de ingresos se llama "Lo que entra" (antes salía "Ventas" dos veces).
  - **Montos cortos:** `formatearCorto()` en `Formato.kt` muestra "S/ 2,000" en vez de "S/ 2,000.00".
    Por ahora solo se usa en el presupuesto y en las metas.
  - **Metas:**
    - las sugerencias se ven como "Fondo de emergencia" y, debajo, chips "3 meses · S/ 3,090";
    - la tarjeta dice "S/ 57 por semana · hasta octubre 2027".
- **Falta:**
  1. **Ver en el teléfono:** "Arma tu presupuesto", "Tu presupuesto" y metas. Probé solo que el balance en
     vivo sale bien ("Te sobran S/ 240"). Para preparar la prueba:
     1. `scripts/prueba.sh limpia`;
     2. pasar el primer uso: Rosa, Mi negocio, RUS · Categoría 2;
     3. `scripts/sembrar.sh`;
     4. pasar a "Mi plata personal" y llenar el ejemplo del documento 04 §10.
  2. **Ajustar lo que se vea mal** en una sola pasada y quitar los imports que ya no se usan en
     `PresupuestoScreen.kt` (`Card`, `CardDefaults`, `LazyListScope`, `heading`…).
  3. **Revisar el texto de otras pantallas:** inicio, cobros y ajustes. La ayuda ya está corta.
  4. **Cerrar el paso:** registrarlo en `reporte.md` como paso 33, hacer el commit, fusionar en `main`,
     actualizar las capturas 35 a 48 y preguntar antes de instalar en la app real o subir a GitHub.

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
