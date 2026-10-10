# Reporte de mejoras UX/UI

Mejoras que salieron de la revisión UX/UI del 10 de octubre de 2026 (heurísticas de Nielsen, puntaje
inicial **28/40**). Se aplican una por una en la rama `feat/mejoras-ux`; cada paso queda registrado
aquí al terminarlo. Las pruebas en teléfono se hacen con una copia aparte (`com.app.centavot.prueba`)
y datos de demostración, en un Samsung Galaxy A15 con Android 16.

| # | Paso | Problema de la revisión | Estado |
|---|---|---|---|
| 1 | Registrar venta más simple | [P1] Demasiadas decisiones para la acción más frecuente | ✅ |
| 2 | Cobros sin acciones tapadas | [P1] El botón flotante tapa acciones; 5 acciones por cobro | ✅ |
| 3 | Confirmación y deshacer al guardar | [P2] Gasto, cobro y abono se cierran sin decir nada | ✅ |
| 4 | Colores con un solo significado | [P2] Ámbar para alertas y para cosas neutras | ✅ |
| 5 | Barra superior de Inicio | [P2] 4 íconos sin texto | ✅ |
| 6 | Detalles finales | [P3] Montos, gráfico, compartir y controles distintos | ⏳ |

---

## Paso 1 · Registrar venta más simple

**Problema.** La pantalla mostraba unas 15 decisiones a la vez (5 ventas frecuentes, el monto, 6 montos
rápidos, "¿Qué vendiste?", "¿De dónde vino la plata?" y la fecha), y preguntaba el origen y la fecha en
cada venta aunque casi siempre son "Venta del negocio" y "Hoy".

**Qué se hizo.**
- Lo habitual ya no se pregunta. "¿Qué vendiste?", "¿De dónde vino la plata?" y la fecha quedan
  plegados bajo una fila **"Cambiar detalles"** que muestra lo que se va a guardar:
  *"Venta del negocio · Hoy"*. Al tocarla se abren los campos; el resumen cambia si se cambia algo
  (ej. *"Ingreso personal · Ayer · Pan"*).
- Los detalles empiezan **abiertos** cuando ya hay algo distinto: al editar una venta, si es ingreso
  personal, si la fecha no es hoy, si tiene nombre o si la fecha tiene un error.
- La tecla "listo" del teclado numérico **guarda la venta**, sin tener que llegar al botón.
- El texto de ayuda de "¿Qué vendiste?" se acortó a la mitad.
- Se mantienen las ventas frecuentes (un toque) y los montos rápidos (dos toques).

**Resultado.** Una venta nueva muestra 3 grupos en vez de 6, y toda la pantalla cabe sin bajar.
Registrar una venta sigue siendo: monto → Guardar (o una venta frecuente, un toque).

**Verificación.**
- 4 tests nuevos (`VentaDetallesTest`): el resumen y cuándo empiezan abiertos los detalles. **79 tests, 0 fallas.**
- En el teléfono: pantalla plegada y abierta, guardar con el monto rápido S/ 5 y guardar escribiendo
  7.5 y tocando "listo" en el teclado. Las dos mostraron "Guardaste…".

**Archivos.** `VentaScreen.kt`, `VentaViewModel.kt` (`resumenDetallesVenta`, `detallesAbiertosAlInicio`),
`Iconos.kt` (íconos Desplegar, Plegar y Más opciones), `VentaDetallesTest.kt`.

---

## Paso 2 · Cobros sin acciones tapadas

**Problema.** El botón flotante ancho "Registrar cobro" tapaba las acciones de los cobros que pasaban
por debajo (en la captura, el "Abonar" de la Señora Juana). Cada cobro mostraba 5 acciones: Cobrar,
Abonar, Editar, WhatsApp y un tacho sin texto **pegado a WhatsApp**, fácil de tocar sin querer.

**Qué se hizo.**
- **"Registrar cobro" pasa a la lista**, como botón ancho con texto, debajo de "Te deben". En reposo
  ya no flota sobre nada.
- El **"+" flotante** aparece solo cuando ese botón queda más de la mitad fuera de la vista, para
  registrar sin volver arriba. Tiene su nombre para TalkBack ("Registrar cobro").
- A la vista quedan solo las acciones del día a día: **Cobrar, Abonar y WhatsApp** (el recordatorio
  que pidió la entrevista), en una sola fila.
- **Editar y Eliminar** van en un menú **⋮** junto al monto, con su nombre escrito. Eliminar va en
  rojo y sigue pidiendo confirmación. En un cobro ya cobrado el menú solo tiene Eliminar.
- Cobrar y Abonar usan un relleno lateral de 16 dp (antes 24) para que los tres botones quepan en una
  fila en un teléfono de 384 dp de ancho.

**Resultado.** Cada cobro pasa de 5 acciones visibles a 3 más el menú, el tacho ya no está junto a
WhatsApp, y ningún botón tapa contenido en reposo.

**Verificación.**
- En el teléfono: lista arriba y abajo, los tres botones en una fila, el "+" aparece al bajar y abre
  "Registrar cobro", el menú ⋮ abre Editar (lleva al cobro de Don Lucho) y Eliminar (muestra la
  confirmación; se canceló).
- Durante la prueba aparecieron y se corrigieron dos defectos de este mismo paso: WhatsApp pasaba a una
  segunda línea y el "+" no aparecía cuando quedaba visible un borde de 9 dp del botón de la lista.
- **79 tests, 0 fallas** (este paso no cambia lógica).

**Archivos.** `CobrosScreen.kt` (botón en la lista, `MenuCobro`, regla del "+" flotante).

---

## Paso 3 · Confirmación y deshacer al guardar

**Problema.** Al guardar un gasto o un cobro la pantalla se cerraba sin decir nada, y Cobrar o Abonar
cambiaban el cobro sin confirmación visible ni forma de volver atrás. El comerciante no sabía si se
guardó, con riesgo de registrarlo dos veces. La app no tenía ningún aviso breve (snackbar).

**Qué se hizo.**
- **Un canal de avisos para toda la app** (`Avisos`). Las pantallas publican el mensaje y la
  navegación principal lo muestra como snackbar de Material, aunque la pantalla que lo pidió ya se
  haya cerrado. TalkBack lo lee.
- **Con "Deshacer"** (el aviso dura más y tiene una X para cerrarlo):

  | Acción | Aviso | Deshacer hace |
  |---|---|---|
  | Gasto nuevo | "Gasto de S/ 12.00 guardado" | Elimina ese gasto |
  | Cobro nuevo | "Cobro a Pedro guardado" (o "… y sumado a tus ventas") | Elimina el cobro y, si se creó, también su venta |
  | Cobrar | "Don Lucho te pagó S/ 200.00" | El cobro vuelve a pendiente, como estaba |
  | Abono | "Abono de S/ 50.00 de Don Lucho guardado" | El cobro vuelve a como estaba antes del abono |
  | Venta (en la misma pantalla) | "Guardaste S/ 5.00…" con botón **Deshacer** | Quita la venta y dice "Quitaste la venta de S/ 5.00." |

- **Sin deshacer** (aviso corto): "Cambios guardados" al editar, y "Gasto eliminado", "Venta
  eliminada" o "Cobro eliminado" al eliminar (eliminar ya pide confirmación antes).
- Todo lo que se deshace **queda anotado en Actividad**, que nunca se borra
  (ej. *"Deshiciste el pago de S/ 200.00…"*).
- Dominio: `RestaurarCobroUseCase` (devuelve un cobro a como estaba) y `RegistrarCobroYVentaUseCase`
  ahora devuelve también el id de la venta creada, para poder deshacerla.

**Verificación.**
- 4 tests nuevos (`DeshacerTest`): el cobro devuelve el id de su venta, sin venta no devuelve nada,
  deshacer un abono que cerró el cobro lo deja idéntico, y deshacer el pago queda en Actividad.
  **83 tests, 0 fallas.**
- En el teléfono, cada caso guardado y deshecho:
  - gasto S/ 12 → "Gastaste en el negocio" vuelve a S/ 12.00;
  - Cobrar a Don Lucho y abono de S/ 50 → "Te deben" vuelve a S/ 255.00;
  - venta S/ 5 → "Quitaste la venta de S/ 5.00.";
  - cobro "Pan" a Pedro sumado a ventas → desaparece el cobro y "Vendiste" vuelve a S/ 153.70.

**Observación para el paso 6.** En "Registrar cobro" el teclado numérico tapa el campo "Motivo";
se llega con la tecla "siguiente" del teclado, pero no se ve.

**Archivos.** `Avisos.kt` (nuevo), `NavegacionPrincipal.kt` (snackbar), `GastoViewModel.kt`,
`VentaViewModel.kt`, `VentaScreen.kt`, `CobroViewModel.kt`, `CobrosViewModel.kt`, `CobroUseCases.kt`,
`Modulos.kt`, `PendientesTest.kt`, `DeshacerTest.kt` (nuevo).

---

## Paso 4 · Colores con un solo significado

**Problema.** El ámbar marcaba el aviso del tope, pero también la encuesta "Una pregunta rápida",
"Te deben" en Reportes, la línea de gastos del gráfico, el monto "Gastaste" y la etiqueta
"Sin clasificar". El verde oscuro se usaba a la vez para lo que te deben y para el mensaje de
privacidad. Si el color no es confiable, el aviso del tope pierde fuerza.

**Qué se hizo.** Se fijó una regla (escrita en `Tema.kt`) y se aplicó en toda la app, sin salir del
tema Material actual:

| Color | Significa | Dónde |
|---|---|---|
| Verde (primary) | Plata a favor o algo que salió bien | Ventas, ganancia, "Guardaste", "Cobrado", tope sin problemas |
| Ámbar (tertiary) | Atención | Solo avisos de tope al 80 % y 90 % (tarjeta, línea en "Hoy", punto en "Mi negocio", notificaciones) |
| Rojo (error) | Límite o acción sin vuelta atrás | Tope alcanzado (tarjeta, línea y punto), Eliminar |
| Neutro (superficies) | Información | Encuesta, primeros pasos, "Te deben" (Cobros y Reportes), privacidad, ayuda, "Sin clasificar" |

Cambios puntuales:
- "Una pregunta rápida" y "Empieza en 3 pasos": de ámbar y verde a superficie neutra.
- "Te deben" en Cobros (verde oscuro) y en Reportes › Me deben (ámbar): superficie neutra en los dos.
- "Tus datos son tuyos" (Ajustes, bienvenida y Ayuda) y "¿No quieres dejar tu cuaderno?": neutras;
  el candado se pinta en verde.
- "Gastaste en el negocio": de ámbar al color del texto. La línea de gastos del gráfico: de ámbar
  a gris (sigue punteada, así no se distingue solo por color).
- La línea del tope en "Hoy" y el punto de "Mi negocio" pasan a **rojo** cuando se llega al tope
  (antes siempre ámbar).
- "Sin clasificar": de ámbar a gris.

**Verificación.**
- En el teléfono, Inicio, Cobros, el gráfico de Reportes y Me deben en **tema oscuro y tema claro**:
  el ámbar solo aparece en el aviso del tope y el punto de "Mi negocio". El teléfono volvió a su tema
  original (oscuro).
- **83 tests, 0 fallas** (este paso no cambia lógica).

**Archivos.** `Tema.kt`, `InicioScreen.kt`, `CobrosScreen.kt`, `ReporteScreen.kt`, `AjustesScreen.kt`,
`AyudaScreen.kt`, `EtiquetaCategoria.kt`, `GraficoLineas.kt`.

---

## Paso 5 · Barra superior de Inicio

**Problema.** Arriba de Inicio había 4 íconos sin texto: campana, "?", un reloj y un engranaje. Para
alguien con poca práctica digital el reloj (que abre Actividad) no dice nada.

**Qué se hizo.**
- **El reloj sale de Inicio.** Actividad pasa a Ajustes como una fila con nombre y para qué sirve:
  **"Tu actividad — Todo lo que registraste, cobraste o eliminaste, con fecha y hora."**, justo antes
  de "Cómo vienes usando Centavot".
- Arriba quedan 3 íconos: **campana** (avisos, con su número), **"?"** (ayuda) y **engranaje** (ajustes).

**Cambio respecto del plan.** El plan proponía mover también la Ayuda a Ajustes. Se dejó en Inicio
porque "?" es el ícono más reconocible y la ayuda tiene que encontrarse en segundos, sobre todo el
primer día; esconderla dentro de Ajustes empeoraría lo que la revisión pedía mejorar. El único ícono
ambiguo era el reloj.

**Verificación.**
- En el teléfono: Inicio muestra campana, "?" y engranaje (TalkBack: "Notificaciones, 1 sin leer",
  "Cómo usar Centavot", "Ajustes"); en Ajustes, "Tu actividad" abre el historial.
- **83 tests, 0 fallas.**

**Archivos.** `InicioScreen.kt`, `AjustesScreen.kt` (`FilaActividad`), `NavegacionPrincipal.kt`.
