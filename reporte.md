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
| 6 | Detalles finales | [P3] Montos, gráfico, compartir y controles distintos | ✅ |
| 7 | Aviso al guardar Ajustes y Régimen | [P2] Segunda revisión: las dos únicas pantallas que se cerraban sin aviso | ✅ |
| 8 | Registrar cobro más corto | [P2] Segunda revisión: el formulario más largo, 6 grupos | ✅ |
| 9 | Ajustes por secciones | [P2] Segunda revisión: 4 temas en una sola página | ✅ |

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

---

## Paso 6 · Detalles finales

**Problema.** El monto se pedía de tres formas ("¿Cuánto vendiste?", "Monto" en Gasto y en Cobro);
Gasto abría el teclado solo y tapaba "¿Para qué fue?", que es obligatorio; "Fiado / Pedido" usaba
chips y "Negocio / Personal" botones segmentados para la misma clase de elección; el gráfico no
tenía montos en el eje; y compartir en Reportes era un ícono sin texto.

**Qué se hizo.**
- **Montos con la misma forma:** pregunta en lenguaje del comerciante, prefijo "S/" y "0.00" de
  ejemplo en los tres: **"¿Cuánto vendiste?"**, **"¿Cuánto gastaste?"** y **"¿Cuánto te debe?"**
  (en un pedido sigue "Precio total del pedido").
- **Gasto ya no abre el teclado solo**, igual que Venta: se ven el monto y "¿Para qué fue?" sin
  cerrar nada.
- **"Fiado o préstamo / Pedido"** pasa a botones segmentados, como "Negocio / Personal".
- **Reportes:** el ícono de compartir pasa a botón con texto: **"Enviar al contador"** en Negocio y
  **"Compartir"** en Personal y Me deben.
- **Gráfico "Cómo te fue mes a mes":**
  - montos en el eje, en soles redondos, alineados con las tres líneas guía: *S/ 7,200 · S/ 3,600 · S/ 0*;
  - **defecto corregido** que no estaba en la revisión: los puntos se dibujaban de borde a borde y
    no quedaban sobre la etiqueta de su mes (el primero caía a la izquierda de "may"). Ahora cada
    punto va al centro de la columna de su mes.

**Sobre la observación del paso 3.** El campo "Motivo" de "Registrar cobro" no estaba roto: queda
debajo del monto y la tecla "siguiente" del teclado lleva a él. No se cambió.

**Verificación.**
- 2 tests nuevos (`GraficoTest`): montos del eje redondeados. **85 tests, 0 fallas.**
- En el teléfono: el eje muestra S/ 7,200, S/ 3,600 y S/ 0 con los puntos sobre sus meses;
  "Enviar al contador" en Negocio y "Compartir" en Me deben; Gasto abre sin teclado; "Registrar
  cobro" con los botones segmentados y "¿Cuánto te debe?".
- **Texto grande del sistema (130 %)** en Inicio, Cobros, el gráfico y Registrar venta: todo se lee
  sin cortes. En Cobros, WhatsApp pasa a una segunda línea, como se espera con ese tamaño, sin tapar
  nada. El teléfono volvió a 100 %.

**Archivos.** `GastoScreen.kt`, `CobroScreen.kt`, `ReporteScreen.kt`, `GraficoLineas.kt`
(`etiquetasEje`), `GraficoTest.kt` (nuevo).

---

## Segunda revisión (pasos 7 a 9)

Tras los pasos 1 a 6 se volvió a correr la revisión UX/UI: **31/40** (antes 28/40). Quedaron tres
problemas P2, que se resuelven en la rama `feat/mejoras-ux-2`, y uno P3 (identidad todavía de
Material de fábrica) que queda pendiente por decisión del equipo de mantener "Material sobrio".

---

## Paso 7 · Aviso al guardar Ajustes y Régimen

**Problema.** Después del paso 3, Ajustes y Régimen eran las dos únicas pantallas que se cerraban sin
confirmar que se guardó.

**Qué se hizo.**
- Ajustes avisa **"Ajustes guardados"**.
- Régimen dice qué cambió: **"Ahora estás en RUS · Categoría 2. Tu tope es S/ 8,000.00 al mes."**
  (o "al año" en el RER).
- **En el registro inicial no se avisa:** ahí la app ya avanza sola al paso siguiente, y el aviso
  aparecería tarde, al llegar a Inicio.
- **Defecto encontrado y corregido en la prueba:** en las pantallas sin barra de pestañas (formularios),
  el aviso tapaba el botón principal de abajo mientras duraba. Ahora se muestra encima de ese botón.

**Verificación.**
- 2 tests nuevos (`RegimenAvisoTest`): el mensaje mensual y el anual. **87 tests, 0 fallas.**
- En el teléfono: registro inicial completo (aviso de privacidad → bienvenida → régimen → Inicio) sin
  ningún aviso; "Ajustes guardados" al guardar Ajustes; al cambiar a Categoría 1 y luego a 2, el aviso
  con el nuevo tope aparece encima de "Guardar cambios" (1830 px contra 2081 px).

**Archivos.** `AjustesViewModel.kt`, `AjustesScreen.kt`, `RegimenViewModel.kt` (`mensajeRegimenGuardado`),
`RegimenScreen.kt`, `NavegacionPrincipal.kt`, `RegimenAvisoTest.kt` (nuevo).

---

## Paso 8 · Registrar cobro más corto

**Problema.** Era el formulario más largo: qué es, quién debe, monto, motivo, "Contarlo como venta" y
fecha, todo visible. Había que bajar para llegar a la fecha.

**Qué se hizo.**
- Mismo patrón que Registrar venta (paso 1): **"Contarlo como venta" y la fecha quedan plegados**
  bajo "Cambiar detalles", con el resumen de lo que se va a guardar: *"Se suma a tus ventas · Desde hoy"*.
  Al editar un cobro el resumen solo muestra la fecha (*"Desde 4 de octubre"*).
- Empiezan **abiertos** si ya hay algo distinto: no se suma a ventas, la fecha no es hoy o la fecha
  tiene un error.
- El componente plegable pasó a `components/DetallesPlegables.kt` y lo usan Venta y Cobro.
- **Defecto encontrado y corregido en la prueba:** tocar el texto "Contarlo como venta" no cambiaba
  nada; solo respondía el interruptor pequeño. Ahora toda la fila es el interruptor (y TalkBack la
  anuncia como interruptor).

**Resultado.** Un cobro nuevo se registra en una sola pantalla, sin bajar: qué es, quién, cuánto,
motivo y Registrar.

**Verificación.**
- 3 tests nuevos (`CobroDetallesTest`): el resumen nuevo y al editar, y cuándo empiezan abiertos.
  **90 tests, 0 fallas.**
- En el teléfono: formulario plegado con "Se suma a tus ventas · Desde hoy"; al abrir y tocar la
  fila, el resumen cambia a "No se suma a tus ventas · Desde hoy" y se guarda como "Cobro a Pedro
  guardado" (sin "y sumado a tus ventas"). Registrar venta sigue igual con el componente compartido.

**Archivos.** `CobroScreen.kt`, `CobroViewModel.kt` (`resumenDetallesCobro`, `detallesAbiertosAlInicio`),
`DetallesPlegables.kt` (nuevo, movido desde `VentaScreen.kt`), `VentaScreen.kt`, `CobroDetallesTest.kt` (nuevo).

---

## Paso 9 · Ajustes por secciones

**Problema.** Ajustes mezclaba en una sola página, sin separación clara, el perfil, la meta de ahorro,
el régimen, los indicadores de uso y la gestión de datos. La tarjeta "Tus datos son tuyos" iba arriba
de todo, lejos de exportar y borrar.

**Qué se hizo.** La página se ordena en cinco secciones con título (marcadas como encabezado para
TalkBack) y más espacio entre secciones que dentro de ellas:

| Sección | Contenido |
|---|---|
| Tu perfil | Nombre y a qué se dedica |
| Tu meta de ahorro | Explicación, ingreso mensual y porcentaje |
| Tu régimen | Régimen en dos líneas (nombre y "Tope: S/ 8,000.00 al mes") con "Cambiar" |
| Cómo vienes usando Centavot | "Tu actividad" e indicadores de uso |
| Tus datos | "Tus datos son tuyos", ver aviso, exportar y borrar |

- Cada sección tiene **un solo título**: se quitaron los títulos repetidos dentro de las tarjetas
  ("Régimen tributario", "Cómo vienes usando Centavot", "Tus datos").
- En el **registro inicial** no cambia el orden: arriba la bienvenida y "Tus datos son tuyos", antes de
  pedir cualquier dato; luego "Tu perfil" y "Tu meta de ahorro".

**Verificación.**
- En el teléfono: las cinco secciones en orden, el régimen en dos líneas, "Guardar cambios" sigue
  guardando y avisa "Ajustes guardados"; la bienvenida del registro inicial muestra la privacidad
  arriba y luego "Tu perfil".
- **90 tests, 0 fallas** (este paso no cambia lógica).

**Archivos.** `AjustesScreen.kt` (`SeccionAjustes`).

---

## Resumen

| | Antes | Después (pasos 1–9) |
|---|---|---|
| Decisiones visibles al registrar una venta | ~15 | 3 grupos (frecuentes, monto, "Cambiar detalles") |
| Acciones visibles por cobro | 5, con el tacho junto a WhatsApp | 3 + menú ⋮ (Editar y Eliminar con texto) |
| Botón que tapa contenido en Cobros | Sí | No en reposo; el "+" solo aparece al bajar |
| Confirmación al guardar | Solo en Venta | Gasto, venta, cobro, cobrar y abonar, con **Deshacer** |
| Significados del ámbar | 6 | 1 (avisos de tope) |
| Íconos sin texto arriba en Inicio | 4 | 3 reconocibles (avisos, ayuda, ajustes) |
| Formas de pedir un monto | 3 | 1 |
| Pantallas que guardan sin avisar | Gasto, cobro, cobrar, abonar, Ajustes, Régimen | Ninguna |
| Grupos visibles al registrar un cobro | 6 | 4 + "Cambiar detalles" |
| Secciones con título en Ajustes | 1 | 5 |
| Tests | 75 | 90 |

Todo se probó en un Samsung Galaxy A15 (Android 16) con una copia de prueba aparte, en tema oscuro,
tema claro (paso 4) y texto grande (paso 6). Puntaje de la revisión UX/UI: **28/40** al inicio,
**31/40** tras los pasos 1–6; los pasos 7–9 resuelven los tres P2 que quedaban.

---

# Ronda 2 · Modos y menos texto

Cambios desde el 10 de octubre por la tarde. Cada paso se registra aquí al terminarlo, con su
verificación, para saber en qué nos quedamos.

| # | Paso | Motivo | Estado |
|---|---|---|---|
| 10 | Modo personal o negocio | Reunión con asesores de negocio: no mezclar lo personal con el negocio | ✅ |
| 11 | Inicio con menos texto | "Veo mucho texto": tarjetas con párrafos y frases repetidas | ✅ |
| 12 | Régimen con menos texto | La misma explicación repetida en cada opción | ✅ |
| 13 | Bienvenida y Ajustes con menos texto | Una explicación debajo de cada sección y de cada campo | ✅ |
| 14 | Cobros con menos texto | Ayudas largas en el formulario y en la lista vacía | ✅ |
| 15 | Venta, gasto y retiro con menos texto | Ayudas que repiten lo que ya dice el campo | ✅ |
| 16 | Reportes con menos texto | Notas largas encima de los números | ✅ |
| 17 | Ayuda más corta | 402 palabras: la pantalla con más texto | ✅ |
| 18 | Botones y elementos de tamaño normal | "Que no estén muy grandes" | ✅ |
| 19 | Ventas frecuentes sin el total del día | P13: el total de cada noche llenaba las 6 ventas frecuentes | ✅ |
| 20 | Lector de pantalla (TalkBack) | Q3: ayudas plegables, títulos y botones de cobros | ✅ (falta escucharlo una persona) |
| 21 | Teléfono de gama baja | Q5: peso, arranque, memoria y pantalla angosta con letra grande | ✅ (medido en el A15, sin emulador) |
| 22 | Versión de release comprimida (R8) | El APK de release pesaba 16.9 MB | ✅ |
| 23 | Tres arreglos chicos | P8 caja después de medianoche · P9 aviso de tope repetido · P10 rubro de servicios | ✅ |
| 24 | Cobros antiguos y gastos por categoría | P7 aviso de fiados de más de un mes · P11 total por subcategoría en el reporte | ✅ |
| 25 | Tema claro siempre | Con el teléfono en modo oscuro, la app se veía oscura | ✅ |
| 26 | Resumen del contador en PDF | P5: solo se podía mandar en CSV | ✅ |
| 27 | Gráfico "Este mes" con su lectura | Reunión con usuarios: "como en la banca, pero que te diga qué significa" | ✅ |
| 28 | Presupuesto con plantilla | Reunión con usuarios: plantilla con checks, editable por mes, categorías propias al registrar | ✅ |
| 29 | Fuentes de ingreso | Reunión con usuarios: ingresos variables (clases, comisiones, inversiones) | ✅ |
| 30 | Metas de ahorro | Reunión con usuarios: metas con avance, abonos parciales y avisos | ✅ |
| 31 | Menos texto (ronda 3) | "Hay mucho texto", otra vez, tras sumar presupuesto y metas | ✅ |
| 32 | Presupuesto según la guía (RP-01 a RP-10) | Documentos 03 y 04 del 19 de octubre | ✅ |
| 33 | Presupuesto en bloques y menos texto (ronda 4) | "Se ve mucho texto"; "Arma tu presupuesto" sin división entre grupos | ✅ |
| 34 | Marca Andina en la app | Pack de marca del 20 de octubre | ✅ |
| 35 | Ámbar en vez de amarillo claro | "Hay un color amarillo claro (botón Gasto y otros); cámbialo por ámbar" | ✅ |
| 36 | Ícono sin el fondo negro | "El ícono tiene ese color negro alrededor; no lo quiero así" | ✅ |
| 37 | La moneda ocupa todo el ícono | "Pero que el logo ocupe todo" | ✅ |
| 38 | Quitar lo agregado por error y partidas al registrar | "Si agrego y me equivoco, ¿cómo lo borro?" y "al registrar ingreso o gasto tienen que aparecer esas categorías, con criterio" | ✅ |
| 39 | Fechas de Movimientos sin descuadre | "En Movimientos la opción para poner fecha descuadra la vista" (con la skill de diseño) | ✅ |

**Regla para toda la ronda:** cada pantalla dice una cosa a la vez. Se quita lo que repite el
título, lo que ya se dijo en otra pantalla y las ayudas que no responden una duda real. No se toca
el texto legal del aviso de privacidad (Ley 29733).

---

## Paso 10 · Modo personal o negocio

**Motivo.** En una reunión con personas con experiencia en negocios se recomendó separar lo personal
de lo del negocio: hay quienes solo quieren llevar su presupuesto, y mostrar las dos cosas junta
demasiada información.

**Qué se hizo.**
- **Registro:** aviso de privacidad → solo el **nombre** y **Mi plata personal** o **Mi negocio**.
  El régimen se pide solo en modo negocio. Rubro, ingreso mensual y % de ahorro pasan a Ajustes.
- **Cada modo ve solo lo suyo, con las mismas funciones:**

| | Negocio | Personal |
|---|---|---|
| Entrada | Venta | Ingreso |
| Gastos | Subcategorías de negocio | Subcategorías de casa |
| Cobros | Fiados y pedidos | Préstamos |
| Inicio | "Tu caja" y "Mi negocio" (tope) | "Tu plata" y "Mi mes" |
| Reportes | Negocio y Me deben | Mi plata y Me deben |

- El selector "Negocio / Personal" desapareció de los formularios: lo pone el modo.
- **Cambiar de modo** desde la etiqueta de arriba en Inicio o desde Ajustes; no se borra nada.
- **"Saqué para la casa"**: retiro que resta de la caja ("Queda en caja") sin bajar la ganancia, y
  aparece como ingreso "De tu negocio" en lo personal.
- Base de datos v4 (`perfil.modo`, `ingresos.retiroDelNegocio`, `cobros.categoria`); quien ya usaba
  la app queda en modo negocio.

**Verificación.** 99 tests. En el teléfono: registro en los dos modos, cambio de modo, volver a lo
personal desde el régimen, retiro, préstamo personal, reportes y migración v3 → v4 de los datos de
demostración. PR #8 (fusionado).

---

## Paso 11 · Inicio con menos texto

**Problema.** Inicio tenía párrafos dentro de las tarjetas y repetía lo mismo en dos lugares (el
porcentaje del tope estaba en la barra y otra vez en el mensaje).

| Dónde | Antes | Ahora |
|---|---|---|
| Pregunta del cuaderno | Título "Una pregunta rápida" + "¿Te resulta más fácil llevar tus cuentas con Centavot que con tu cuaderno y la calculadora?" | "¿Es más fácil que tu cuaderno?" y los dos botones |
| Primera vez (sin movimientos) | "Empieza en 3 pasos", tres pasos numerados, una nota sobre el cuaderno y tres enlaces | Una frase ("Toca Venta cada vez que vendas y Gasto cuando pagues algo.") y "Ayuda". Los botones grandes ya están arriba |
| Constancia | "Anotaste 3 de los últimos 7 días. Anotar cada día hace que tus cuentas cuadren." | "Anotaste 3 de los últimos 7 días." |
| Aviso del tope en "Hoy" | "Tus ventas van en 84 % de tu tope" | "Ventas: 84 % del tope" |
| Tarjeta del tope | "Tope de tu régimen · RUS · Categoría 2 · S/ 8,000.00 al mes" y "Tus ventas ya pasaron el 80 % del tope. Te quedan S/ 1,250.30 este mes." | "RUS · Categoría 2 · tope S/ 8,000.00 al mes" y "En ventas te quedan S/ 1,250.30 este mes." (el 84 % ya está en la barra) |
| Meta de ahorro sin definir | "Toca aquí para definir cuánto quieres ahorrar" | "Toca para definir tu meta" |

**Verificación.** En el teléfono con los datos de demostración: "Hoy" y "Mi negocio" (tope al 84 %).
99 tests, 0 fallas.

**Archivos.** `InicioScreen.kt`, `TarjetaTope.kt`.

---

## Paso 12 · Régimen con menos texto

**Problema.** Cada opción tenía una explicación de dos líneas, y la de las dos categorías del RUS era
la misma. Debajo había un párrafo de cuatro líneas sobre montos referenciales, la Clave SOL y el RMT.

**Qué se hizo.**
- Título único: "¿En qué régimen estás?" (antes "Para tu negocio" + "¿En qué régimen tributario
  estás?") y una línea: "Para avisarte antes de llegar a tu tope. No le enviamos nada a SUNAT."
- Cada opción en dos líneas: nombre y "Tope S/ 8,000.00 al mes · das boletas". El comprobante
  ("boletas" o "facturas") es lo que más distingue al RUS del RER para quien no sabe en cuál está.
- Las explicaciones pasan a **"¿No sabes cuál elegir?"**, que se abre al tocarla: qué cuenta el tope en
  cada régimen, dónde ver la categoría y qué hacer en el RMT o sin RUC.
- Nuevo componente **`AyudaPlegable`** (pregunta que se toca para ver la respuesta), para usarlo en los
  siguientes pasos.

**Verificación.** En el teléfono, desde Ajustes → Cambiar: tres opciones de dos líneas y la ayuda
plegable que abre y cierra. 99 tests, 0 fallas.

**Archivos.** `RegimenScreen.kt`, `AyudaPlegable.kt` (nuevo).

---

## Paso 13 · Bienvenida y Ajustes con menos texto

**Problema.** La bienvenida no entraba en una pantalla: la segunda opción de modo quedaba cortada.
En Ajustes, cada sección y cada campo tenían una explicación de dos líneas, y "Tus datos" repetía
que todo se guarda en el celular.

| Dónde | Antes | Ahora |
|---|---|---|
| Bienvenida | "Tu cuaderno, pero que suma solo. Dinos tu nombre y para qué la usarás." | "Tu cuaderno, pero que suma solo." |
| Modo (registro) | "Solo verás lo de tu elección. Puedes cambiarlo cuando quieras desde Inicio o aquí." | "Puedes cambiarlo cuando quieras." |
| Modo (Ajustes y hoja de Inicio) | "Al cambiar no se borra nada: lo del otro modo vuelve a aparecer cuando regreses a él." | "Al cambiar no se borra nada." |
| Opciones de modo | "Tu presupuesto: sueldo, gastos de la casa, a quién le prestaste y cuánto ahorras." / "Tu bodega o puesto: ventas, compras, fiados, el tope de tu régimen y el resumen para tu contador." | "Tu sueldo, los gastos de la casa y tu ahorro." / "Ventas, compras, fiados y el tope de tu régimen." |
| Meta de ahorro | Párrafo de dos líneas sobre cómo se calcula | Plegado en **"¿Cómo se calcula?"** |
| Ingreso mensual | "Lo que ganas en un mes normal, más o menos. Si tienes sueldo fijo, pon tu sueldo." | "Lo que ganas en un mes normal." |
| Porcentaje | "Entre 0 y 100, con un decimal como máximo." | "Entre 0 y 100." (el error sigue diciendo qué corregir) |
| Tu actividad | "Todo lo que registraste, cobraste o eliminaste, con fecha y hora." | "Todo lo que hiciste, con fecha y hora." |
| Tus datos | "Todo está solo en este celular y no se copia a la nube. Para no perderlo si cambias de celular, exporta tus datos y guárdalos donde quieras." | "No se copia a la nube. Si cambias de celular, exporta tus datos para no perderlos." |

**Verificación.** En el teléfono con una instalación limpia: la bienvenida entra completa (nombre,
las dos opciones y "Continuar"); en modo personal, la tarjeta de primera vez es una sola línea con
"Ayuda"; en Ajustes, "¿Cómo se calcula?" abre y cierra. 99 tests, 0 fallas.

**Archivos.** `AjustesScreen.kt`, `SelectorModo.kt`, `InicioScreen.kt`.

---

## Paso 14 · Cobros con menos texto

| Dónde | Antes | Ahora |
|---|---|---|
| Formulario: "¿Qué es?" | Debajo de "Fiado o préstamo / Pedido": "Un trabajo o pedido que te encargaron (zapatos, costura, menús). Anota el adelanto si te dieron uno." | Sin texto: las dos opciones se explican solas y el campo "Adelanto" aparece al elegir Pedido |
| Sin contactos | "Aún no tienes contactos. Agrega a la persona que te debe para registrar el cobro." | "Aún no tienes contactos." y el botón "Agregar contacto" |
| Adelanto | "Lo que ya te pagó. Te deberá el resto." | "Lo que ya te pagó." |
| Contarlo como venta | "Se suma hoy a tus ventas y a tu tope. Cuando te pague no se vuelve a contar." / "No se suma a tus ventas. Úsalo si ya anotaste la venta o si es un préstamo de plata." | "Se suma a tus ventas. Al cobrar no se vuelve a sumar." / "No se suma. Úsalo si ya anotaste la venta o si prestaste plata." |
| Lista vacía | Tres frases (qué anotar, "Cobrar", WhatsApp, "así no se te olvida nadie") | Negocio: "Anota lo que fías y los pedidos por cobrar." Personal: "Anota la plata que prestaste." |
| Contactos vacío | "Agrega a las personas a las que les fías o prestas para llevar la cuenta de lo que te deben." | "Agrega a quienes les fías o prestas." |

**Verificación.** En el teléfono con los datos de demostración: formulario de pedido con los
detalles abiertos (adelanto, "Contarlo como venta" y fecha). 99 tests, 0 fallas.

**Archivos.** `CobroScreen.kt`, `CobrosScreen.kt`, `ContactosScreen.kt`.

---

## Paso 15 · Venta, gasto y retiro con menos texto

| Dónde | Antes | Ahora |
|---|---|---|
| Ventas frecuentes | "Tus ventas frecuentes (un toque y listo)" | "Tus ventas frecuentes" (lo mismo con los ingresos) |
| Nombre de la venta o el ingreso | "Con nombre, la próxima vez aparece arriba para un toque." | "Con nombre, la próxima vez es un toque." |
| Saqué para la casa | "Se resta de tu caja y aparece como ingreso en tu plata personal." | Sin ayuda: la caja lo muestra al guardar ("Sacaste para la casa", "Queda en caja") |
| Eliminar venta, ingreso o retiro | "Úsalo solo si lo registraste por error. Ya no se contará en tus totales ni en tu tope, y quedará anotado en tu actividad." | "Ya no contará en tus totales ni en tu tope." (sin "ni en tu tope" si no es venta) |
| Eliminar gasto | Lo mismo que arriba | "Ya no contará en tus totales ni en tu tope." (en lo personal, sin el tope) |

**Verificación.** En el teléfono: venta y "Saqué para la casa" con los detalles abiertos. 99 tests, 0 fallas.

**Archivos.** `VentaScreen.kt`, `GastoScreen.kt`.

---

## Paso 16 · Reportes con menos texto

| Dónde | Antes | Ahora |
|---|---|---|
| Nota bajo el resumen | "Centavot no declara ni envía nada a SUNAT: este resumen es tuyo. Revísalo con tu contador antes de declarar. Con el botón de compartir lo envías en un archivo que se abre en Excel." | "No se envía a SUNAT. Revísalo con tu contador antes de declarar." (el botón "Enviar al contador" ya está arriba) |
| Lectura del mes flojo | "El mes más flojo fue julio 2026: S/ 4,300.00. Si se repite cada año, prepárate con tiempo." | "Tu mes más flojo fue julio 2026: S/ 4,300.00." |
| Variación | "Este mes vas vendiendo 6 % menos que el mes pasado." | "Este mes vendes 6 % menos que el pasado." |
| Sin régimen | "Ve a Ajustes para elegir tu régimen y ver este resumen." | "Está en Ajustes." (bajo el título "Elige tu régimen") |
| Mi plata | "Este reporte es solo para ti: no es para SUNAT." | "Solo para ti: no es para SUNAT." |
| Me deben vacío | "Cuando fíes o tengas pedidos por cobrar, aparecerán aquí." | Según el modo: "Aquí aparecen los fiados y pedidos por cobrar." / "Aquí aparece la plata que prestaste." |

Se mantuvo "(línea continua)" y "(punteada)" en la leyenda del gráfico: sin eso, las dos líneas solo
se distinguen por el color.

**Verificación.** En el teléfono: resumen, nota de una línea, gráfico y lecturas. 99 tests, 0 fallas.

**Archivos.** `ReporteScreen.kt`.

---

## Paso 17 · Ayuda más corta

**Problema.** Era la pantalla con más texto: siete tarjetas numeradas con dos o tres líneas cada una,
más una tarjeta sobre el cuaderno y otra sobre la privacidad.

**Qué se hizo.**
- Los pasos pasan de tarjetas a una **lista**: ícono, título y una línea. Sin números (no es una
  secuencia que haya que seguir en orden).
- Cada paso, en una línea. Ej. negocio: "Anota cada venta · Ponle nombre y la próxima vez es un
  toque."; "Tu tope · Te avisamos antes de que llegues."; personal: "Tu ahorro · En «Mi mes», según
  tu meta de Ajustes."
- Cambio de modo: "¿También tienes negocio? · Toca «Mi plata personal» arriba en Inicio. No se borra nada."
- Intro: "Tu cuaderno, pero que suma solo." (antes con "Úsalo varias veces al día, cada vez que
  vendas o gastes").
- Cuaderno: "Al cerrar el día, pasa aquí los totales." (antes tres frases). Privacidad: "Sin banco
  ni SUNAT. Todo queda en tu celular."

**Verificación.** En el teléfono, modo negocio: los siete pasos entran casi en una pantalla.
99 tests, 0 fallas.

**Archivos.** `AyudaScreen.kt`.

---

## Resumen de la ronda 2 (pasos 11–17)

Palabras de interfaz en el código de cada pantalla (incluye mensajes de error y las ayudas
plegadas, que no se ven hasta tocarlas):

| Pantalla | Antes | Después |
|---|---|---|
| Ayuda | 402 | 166 |
| Ajustes y bienvenida | 326 | 262 |
| Inicio | 290 | 173 |
| Reportes | 188 | 147 |
| Registrar cobro | 152 | 101 |
| Lista de cobros | 138 | 105 |
| Régimen | 126 | 97 (la mitad, plegada) |
| Venta, ingreso y retiro | 132 | 92 |
| **Total** | **1,754** | **1,143 (−35 %)** |

- La bienvenida entra en una pantalla y la tarjeta de primera vez es una sola línea.
- Nuevo componente `AyudaPlegable`: las explicaciones que solo necesita quien tiene la duda quedan a
  un toque ("¿No sabes cuál elegir?", "¿Cómo se calcula?").
- No se tocó el aviso de privacidad (texto legal, Ley 29733) ni los mensajes de error, que siguen
  diciendo qué corregir.
- 99 tests. Todo se probó en el teléfono con una copia aparte (`com.app.centavot.prueba`).

**Estado.** Fusionado en `main` (PR #9) e instalado en el teléfono.

---

## Paso 18 · Botones y elementos de tamaño normal

**Problema.** Varios elementos se veían grandes. Se midieron en el teléfono (densidad 450, 384 dp de
ancho) todos los elementos tocables: casi todos medían exactamente **48 dp**, el mínimo para tocar
con el dedo, así que no sobraban. Los grandes eran los que tenían un alto fijo mayor al estándar o
números con letra de cartel.

| Elemento | Antes | Ahora |
|---|---|---|
| Botones Venta / Gasto de Inicio | 64 dp | 56 dp (siguen siendo los más grandes: son la acción principal) |
| Botón principal de cada formulario (Guardar, Registrar, Continuar, Empezar, Entiendo y acepto) | 56 dp | 48 dp |
| "Registrar cobro" en la lista | 56 dp | 48 dp |
| Montos rápidos, ventas frecuentes, "Sí, más fácil / Todavía no", "Saqué para la casa" | Dibujados de 48 dp | Alto estándar: 40 dp a la vista y 48 dp para tocar |
| Campo del monto (venta, gasto, cobro) | 76 dp, letra `headlineMedium` | 72 dp, letra `headlineSmall` |
| Total de "Te deben" (Cobros y Reportes) | `displaySmall` | `headlineMedium` |
| Porcentaje del tope | `headlineMedium` | `headlineSmall` |
| Tarjetas de modo | Mín. 72 dp con 16 dp de relleno | Mín. 64 dp con 12 dp arriba y abajo |

- Ningún elemento tocable quedó por debajo de 48 dp.
- El aviso que aparece al guardar se corrió para quedar encima del botón, ahora más bajo
  (`ALTO_BOTON_INFERIOR` 88 → 80 dp).

**Verificación.** Medido de nuevo en el teléfono (ej. "Guardar venta" 352 × 48 dp, montos rápidos
74 × 48 dp para tocar) y revisado en Inicio, Mi negocio, Registrar venta, Cobros, Me deben y
Ajustes. 99 tests, 0 fallas.

**Archivos.** `InicioScreen.kt`, `VentaScreen.kt`, `GastoScreen.kt`, `CobroScreen.kt`,
`CobrosScreen.kt`, `ReporteScreen.kt`, `AjustesScreen.kt`, `RegimenScreen.kt`,
`AvisoPrivacidadScreen.kt`, `TarjetaTope.kt`, `SelectorModo.kt`, `NavegacionPrincipal.kt`.

---

## Paso 19 · Ventas frecuentes sin el total del día

**Problema (P13).** Hay comerciantes que no anotan cada venta: en la noche pasan el total del día con
el mismo nombre ("Ventas del día"). Cada total tiene otro monto, así que cada uno contaba como una
venta distinta que se hizo una vez. Esas ventas de una sola vez llenaban los lugares libres de
"Tus ventas frecuentes" y ofrecían montos como S/ 312.30 en un toque, que no se van a repetir.

**Cambio.** Una venta (o un ingreso, en lo personal) solo es frecuente si se repitió **al menos dos
veces con la misma descripción y el mismo monto**. Lo demás queda igual:

- primero lo que más se repite;
- a igual número de veces, primero lo más reciente;
- máximo 6.

| Datos de prueba | Antes | Ahora |
|---|---|---|
| Pan ×4, Gaseosa ×3, Menú ×2, Pollo ×1, "Ventas del día" 10 noches con montos distintos | Pan, Gaseosa, Menú y 3 totales del día | Pan, Gaseosa, Menú |

- Quien recién empieza no ve ventas frecuentes hasta que repite una. Antes aparecía cualquier venta
  desde la primera vez.
- Si un total del día sale igual dos noches, ese monto sí aparece. Es raro y no llena la lista.

**Verificación.**

- Tests: 2 nuevos (el total del día no desplaza a los productos; a igual repetición va lo más
  reciente). Se ajustaron 3 tests que contaban ventas de una sola vez. Pasan los 101 tests, sin
  fallas.
- En el teléfono, con la copia de prueba y los datos de la tabla, "Registrar venta" muestra Pan,
  Gaseosa y Menú.

**Archivos.**

- `Ingreso.kt` (`MIN_REPETICIONES_FRECUENTE = 2`).
- `ResumenYUsoTest.kt`, `IngresoUseCasesTest.kt` y `ModoTest.kt`.
- Nuevos `scripts/prueba.sh` y `scripts/ui.py`: instalan y manejan la copia de prueba. Ahora quedan
  en el repo.

---

## Paso 20 · Lector de pantalla (TalkBack)

**Problema (Q3).** No se había probado la app con TalkBack. Q3 pide revisar Inicio, Venta y Cobros,
los avisos con "Deshacer" y los detalles plegables.

**Cómo se revisó.**

1. Primero se intentó con TalkBack prendido en el teléfono. Los gestos enviados por adb no mueven el
   foco: un aviso del sistema tapa la pantalla. Se apagó enseguida y el teléfono quedó como estaba.
2. Por eso se revisó el árbol de accesibilidad de cada pantalla, que es lo que lee TalkBack, y el
   código de los componentes.

**Lo que ya estaba bien.**

- **Elementos tocables:** ninguno está sin nombre en Inicio (los dos modos), Venta ni Cobros.
- **Íconos:** todos los botones de ícono tienen nombre ("Volver", "Notificaciones, 1 sin leer",
  "Más opciones del cobro de Don Lucho").
- **Avisos al guardar:** se anuncian solos. El de Venta es una región que se lee al cambiar, y la
  barra de "Deshacer" da más tiempo cuando el lector está prendido.
- **Barras del tope:** dicen "Ventas: 81 por ciento del tope usado".
- **Gráficos:**
  - la dona lee cada parte con su monto y su porcentaje;
  - el de líneas lee el resumen de los meses.
- **"Cambiar detalles":** dice si está abierto o cerrado.

**Lo que se cambió.**

| Qué | Antes | Ahora |
|---|---|---|
| Ayudas plegables ("¿No sabes cuál elegir?", "¿Cómo se calcula?") | Se anunciaban como interruptor ("activado / desactivado") | Botón que dice "Abierto" o "Cerrado", igual que "Cambiar detalles" |
| Títulos de sección (Tu caja, Te deben, Fiados y pedidos, títulos de Reportes y de Venta) | Texto común | Marcados como título: con TalkBack se salta de uno a otro |
| Botones de cada cobro | "Cobrar", "Abonar", "WhatsApp", sin decir de quién | "Cobrar todo a Don Lucho", "Registrar abono de Don Lucho", "Recordarle a Don Lucho por WhatsApp" |

**Falta.** Que una persona recorra Inicio, Venta y Cobros con TalkBack prendido y escuche cómo se leen
los montos. Puede que "S/ 3.50" se lea "ese barra tres punto cincuenta".

**Archivos.**

- `AyudaPlegable.kt`.
- `InicioScreen.kt`, `CobrosScreen.kt`, `ReporteScreen.kt` y `VentaScreen.kt`.

---

## Paso 21 · Teléfono de gama baja

**Problema (Q5).** El segmento usa teléfonos económicos y no se había medido el peso del APK de
release.

**Cómo se probó.**

1. El emulador no arranca en esta computadora: se cierra al iniciar con cualquier modelo, incluido el
   Pixel 7 que ya estaba creado. Por eso se midió en el Galaxy A15.
2. En el A15 se instaló la versión de release como copia de prueba, con los datos de demostración
   (4,800 ventas).
3. Para la pantalla, el A15 se puso un momento como un teléfono angosto con letra grande: 360 dp de
   ancho (720 px) y letra al 130 %. Después se restauró.

| Medida | Resultado |
|---|---|
| APK debug | 20.8 MB |
| APK release, como está configurado (sin comprimir) | 16.9 MB |
| APK release con R8 (`isMinifyEnabled` + `isShrinkResources`) | **7.6 MB** |
| Arranque en frío (5 veces, release con R8) | 0.45 – 0.51 s |
| Desplazar Movimientos con 4,800 ventas | 1,692 cuadros: 0.06 % lentos, 99 % en menos de 18 ms |
| Memoria (PSS) | ≈104 MB |

- **R8:** la versión comprimida se recorrió entera (venta, movimientos, cobros, reportes, enviar al
  contador, ajustes y notificaciones) y no hubo ningún cierre. Se activó en el paso 22.
- **Pantalla angosta con letra grande:** todo entra, salvo "Movimientos" en la barra de abajo, que se
  cortaba. Ahora la letra de esa barra se achica lo justo para que entre.
- **Límite de la prueba:** el A15 es más rápido que un teléfono de 2 GB con Android 8–10. En uno de
  esos, el arranque puede tardar 2 o 3 veces más. Para confirmarlo hace falta probar en un teléfono
  así.

**Archivos.**

- `NavegacionPrincipal.kt`.
- Nuevo `scripts/demo.py`, que llena la copia de prueba con la bodega de Rosa (mayo a hoy) para las
  capturas y las mediciones.

---

## Paso 22 · Versión de release comprimida (R8)

**Problema.** La versión de release pesaba 16.9 MB. Para quien tiene un teléfono económico con poca
memoria y datos móviles limitados, eso pesa al descargar e instalar.

**Cambio.** En `androidApp/build.gradle.kts`, el tipo de compilación `release` ahora usa R8:

- `isMinifyEnabled = true` quita el código que no se usa y acorta los nombres;
- `isShrinkResources = true` quita los recursos que no se usan.

No hizo falta ninguna regla en `proguard-rules.pro`: Room, Koin, la navegación y el CSV funcionan sin
reglas propias. La versión de debug no cambia.

| APK | Peso |
|---|---|
| Debug | 20.8 MB |
| Release antes | 16.9 MB |
| **Release ahora** | **7.6 MB (−55 %)** |

**Verificación.**

1. `./gradlew :androidApp:assembleRelease` compila y pasa la revisión de `lint` de release. Hace
   falta conexión la primera vez, para descargar `lint`.
2. La versión comprimida, con el código actual, se instaló desde cero como copia de prueba y se
   recorrió:
   - aviso de privacidad y bienvenida;
   - régimen, con la ayuda plegable;
   - venta y gasto, revisados en Movimientos;
   - contacto nuevo, cobro y abono;
   - reporte y "Enviar al contador";
   - "Exportar todos mis datos";
   - cambio a modo personal y un ingreso;
   - "Borrar todos mis datos", que vuelve al aviso de privacidad.
3. No hubo ningún cierre en el registro del teléfono. Pasan los 101 tests.

**Archivos.** `androidApp/build.gradle.kts`.

---

## Paso 23 · Tres arreglos chicos (P8, P9 y P10)

### P8 · "Tu caja" después de medianoche

**Problema.** El día se calculaba una sola vez, al abrir la pantalla. Si la app quedaba abierta pasada
la medianoche, "Tu caja" seguía mostrando lo del día anterior.

**Cambio.**

- `Reloj` tiene ahora `dias()`, que da el día de hoy y vuelve a darlo cada vez que cambia. En el
  teléfono lo revisa una vez por minuto, así también sigue un cambio de hora o de zona.
- Lo siguen todos los cálculos que dependen del día:
  - la caja (hoy, semana y mes);
  - el resumen de gastos del mes;
  - el tope;
  - la constancia ("Anotaste 7 de los últimos 7 días");
  - la fecha de Inicio.

### P9 · Aviso de tope repetido

**Problema.** La primera versión avisaba solo por los gastos del negocio y guardaba el aviso con un
identificador sin la medida (`tope-RUS-800000-2026-10-80`). La versión actual lo guarda con la medida
(`…-COMPRAS-…`). Quien actualizara a mitad de mes podía recibir otra vez el mismo aviso de compras.

**Cambio.** Antes de dejar el aviso de compras, se revisa si ya existe el de la versión anterior; si
existe, no se repite. El de ventas sí llega, porque la primera versión no lo daba.

### P10 · Rubro "Servicios profesionales"

**Problema.** El nombre invitaba a quien da recibos por honorarios (4.ª categoría), que está fuera
del alcance.

**Cambio.** Ahora se llama **"Servicios (peluquería, lavandería)"**. Lo guardado no cambia: la base de
datos guarda el rubro por su clave (`SERVICIOS`).

### Verificación

- **Tests:** 4 nuevos en `PendientesChicosTest`:
  - la caja pasa al día nuevo sin cerrar la app;
  - el aviso de compras no se repite si ya llegó con el identificador anterior;
  - sin ese identificador, el aviso de compras sí llega;
  - el rubro no dice "profesional".

  Pasan los 105 tests.
- **Teléfono:** se probó con el **tema claro** (después se volvió a oscuro), con una copia de prueba
  y la bodega de demostración. Se revisaron Inicio (Hoy y Mi negocio), Ajustes con el rubro nuevo y
  Cobros. La medianoche no se puede simular en el teléfono sin cambiar la hora del sistema; la cubre
  el test.

**Archivos.**

- `Reloj.kt` y los casos de uso de resumen, tope y uso.
- `InicioViewModel.kt`, `NotificacionUseCases.kt`, `NotificacionRepository.kt`,
  `NotificacionEntity.kt`, `OtrosRepositoriosImpl.kt` y `Perfil.kt`.
- Nuevo `scripts/sembrar.sh`: carga los datos de demostración en la copia de prueba con un solo
  comando.

---

## Paso 24 · Cobros antiguos y gastos por categoría (P7 y P11)

### P7 · Aviso de cobros antiguos

**Problema.** Un fiado que nadie pagaba quedaba en la lista igual que uno de ayer. Nada le recordaba
al comerciante que ya llevaba más de un mes.

**Cambio.**

- **Regla:** un cobro pendiente desde hace **30 días o más** es antiguo
  (`Cobro.esAntiguo`, `DIAS_COBRO_ANTIGUO = 30`).
- **Aviso:** al abrir la app, `RevisarCobrosAntiguosUseCase` deja uno en Notificaciones por cada
  cobro antiguo: "Carmen te debe hace más de un mes · Le falta pagarte S/ 60.00 por "Fiado de la
  quincena". Puedes recordárselo por WhatsApp desde Cobros."
  - Cada cobro avisa **una sola vez**.
  - Vale para los dos modos: los fiados del negocio y los préstamos personales.
- **En Cobros:** la fecha de ese cobro sale en ámbar, el color de lo que pide atención, con los días:
  "Desde 5 de septiembre · hace 38 días".

### P11 · Total por subcategoría en el reporte del negocio

**Problema.** El total por subcategoría solo se veía en la dona de Inicio › Mi negocio, y solo para el
mes en curso. El reporte para el contador no lo traía.

**Cambio.**

- **En el reporte:** una tarjeta **"En qué gastaste"** con el total de cada subcategoría del mes
  elegido, de la mayor a la menor. Va debajo del resumen y sale solo si hay gastos.
- **En el Excel:** una tabla nueva, "COMPRAS Y GASTOS POR CATEGORÍA", entre el resumen y el detalle.
- Los gastos sin subcategoría se suman como "Sin categoría", con la misma etiqueta que la dona.

### Verificación

- **Tests:**
  - 3 nuevos: cuándo un cobro es antiguo, el aviso se deja una sola vez y la suma por subcategoría.
  - El test del Excel se actualizó con la tabla nueva.
  - Pasan los 108 tests.
- **Teléfono** (tema claro, copia de prueba y bodega de demostración):
  - aparece el aviso de Carmen en Notificaciones;
  - en Cobros, el fiado de hace 38 días sale en ámbar;
  - en el reporte, "En qué gastaste" muestra Mercadería S/ 1,414.95, Alquiler S/ 500.00, Luz S/ 114.43
    y Transporte S/ 54.00.

  Después el teléfono volvió a oscuro y se quitó la copia de prueba.

**Archivos.**

- `Cobro.kt`, `CobroUseCases.kt`, `InicioViewModel.kt`, `Modulos.kt` y `CobrosScreen.kt`.
- `ReporteSunat.kt`, `ReporteCsv.kt`, `SubcategoriaGasto.kt`, `ReporteScreen.kt` e `InicioScreen.kt`.
- En `scripts/demo.py`, un fiado de hace 38 días.

---

## Paso 25 · Tema claro siempre

**Problema.** La app seguía el modo del teléfono: con el teléfono en oscuro, todas las pantallas se
veían oscuras. Se pidió que todas las vistas se vean siempre con el tema claro, de fondo blanco.

**Cambio.**

- `CentavotTheme` usa siempre el esquema claro. Se quitó el esquema oscuro de `Tema.kt`.
- `MainActivity` pide íconos oscuros para la barra de estado y la de navegación. Si no, con el
  teléfono en oscuro quedarían blancos sobre fondo blanco.

**Verificación.**

- Con el teléfono **en modo oscuro**, la copia de prueba se ve clara en todas las pantallas que se
  revisaron: aviso de privacidad, bienvenida, Inicio, la hoja para cambiar de modo, Cobros, el menú
  ⋮ y el diálogo de abono.
- Los íconos de la hora, la batería y la navegación se leen bien.
- Pasan los 108 tests.

**Capturas.** Las 34 de `capturas-app/` y de la galería se tomaron de nuevo en claro, con la
bodega de demostración, que ahora incluye el fiado de hace 38 días.

**Archivos.** `Tema.kt` y `MainActivity.kt`.

---

## Paso 26 · Resumen del contador en PDF (P5)

**Problema.** El resumen para el contador solo salía en CSV. El CSV sirve para trabajar los números
en Excel, pero no para leerlo tal cual en el celular o imprimirlo. Además, con cientos de ventas al mes
se vuelve una lista larga.

**Cambio.**

- **Botón:** en Reportes › Negocio, "Enviar al contador" abre un menú con **"En PDF"** y **"En Excel"**.
  Las otras pestañas siguen compartiendo en Excel.
- **Contenido del PDF** (`ReporteSunat.aDocumento`, en el código común):
  - título "Resumen para el contador" y debajo el mes y el régimen;
  - resumen del mes: ventas, compras y gastos, y ganancia o pérdida;
  - compras y gastos por categoría;
  - **ventas por día**, con la cantidad anotada y el monto. Una bodega anota cientos al mes; el
    detalle de cada venta sigue en el Excel;
  - detalle de compras y gastos, con fecha, descripción, categoría, proveedor y monto;
  - en cada hoja, el aviso "Centavot no envía nada a SUNAT…", la fecha y el número de página.
- **Dibujo:** en Android lo hace `GeneradorPdfAndroid` con `PdfDocument`, que ya trae el sistema, así
  que no suma librerías ni peso.
  - Hojas A4.
  - Si una tabla no entra, sigue en la hoja siguiente y repite el encabezado.
  - Si un texto no entra en su celda, se corta con "…".
- **Formato:** las fechas van como las usa el contador (13/10/2026) y los montos con "S/".

**Verificación.**

- **Tests:** 5 nuevos en `ReportePdfTest`:
  - título, régimen y aviso;
  - ganancia o pérdida;
  - ventas por día en orden;
  - gastos por categoría y en detalle;
  - mes sin movimientos.

  Pasan los 113 tests.
- **Teléfono:** con la copia de prueba y la bodega de demostración, "En PDF" abrió el menú de
  compartir. Se bajó el archivo y se revisaron sus 2 hojas A4:
  - resumen con ganancia de S/ 4,397.72;
  - categorías;
  - 13 días de ventas (766 ventas, S/ 6,481.10);
  - detalle de 7 gastos.

**Después (Q7):** la Ayuda decía "Mándale el resumen del mes en Excel"; ahora dice "en PDF o Excel".

**Archivos.**

- Nuevos: `ReportePdf.kt`, `GeneradorPdf.kt` y `GeneradorPdfAndroid.kt`.
- `CompartidorArchivos.kt` y `CompartidorArchivosAndroid.kt`, que ahora también comparten archivos
  binarios.
- `ReporteViewModel.kt`, `ReporteScreen.kt`, `Formato.kt` (usa los nombres de mes del dominio),
  `ModuloAndroid.kt` y `ModulosTest.kt`.

---

# Ronda 3 · Lo que pidieron los usuarios (reunión del 10 de octubre)

**Fuente:** `recomendaciones de cliente/` en el repositorio de documentos. Es una reunión con tres
usuarios (David, Jemina y Ronal) a los que se les mostró la app.

**Ya estaba hecho antes de esta ronda:**

- la separación entre lo personal y el negocio (paso 10);
- la exportación a Excel (y ahora también a PDF, paso 26).

**No se programa:** el registro automático, es decir, leer los correos de Yape o de la tarjeta, la foto
de la boleta con OCR y el QR.

- **Por qué:** cambia la promesa de la app de no usar internet y no leer nada fuera de ella, y la
  reunión lo dejó como propuesta para la siguiente reunión.
- **Dónde quedó:** en `CONTINUAR.md` como pendiente.

Todo lo de esta ronda va en la **base de datos versión 5**, con migración automática desde la 4.

## Paso 27 · Gráfico "Este mes" con su lectura

**Pedido.** David: "cuando abro la banca me sale un cuadro de cuánto entró y cuánto gasté, pero no
sé qué significa" y "lo mezcla todo".

**Cambio.**

- Arriba en la pestaña "Hoy" de Inicio, la tarjeta **"Este mes"** muestra dos barras:
  - *Vendiste* (o *Te entró*), en verde;
  - *Gastaste*, en gris.
- Debajo va una frase: "Gastaste el 32 % de lo que vendiste: te quedan S/ 4,397.72." Si se gastó más,
  dice "Gastaste S/ X más de lo que te entró."
- Solo muestra lo del modo, así que no mezcla lo personal con el negocio.
- La frase la arma `ResumenPeriodo.lecturaDelMes`.

## Paso 28 · Presupuesto con plantilla

**Pedido.**

- James propuso una plantilla base, y David y Ronal pidieron marcarla "con checks".
- Jemina y Ronal pidieron que el presupuesto cambie según el mes ("enero no es igual que octubre").
- Jemina pidió que, al registrar un gasto, salgan **sus** partidas y no "las 50,000 categorías que existen".

**Cambio.**

- **"Arma tu presupuesto":** una lista con checks según el modo. Las partidas comunes vienen marcadas.
  - **Personal:** alimentación, alquiler, luz, transporte, celular, pensiones, salud, deudas, ropa y
    salidas; y de ingresos, sueldo, comisiones, clases, cachuelos y ganancia de inversiones.
  - **Negocio:** mercadería, alquiler del local, luz, transporte y delivery, sueldos, bolsas,
    mantenimiento, publicidad e impuestos.
  - El monto es opcional y se pueden agregar partidas propias ("Agregar otra").
- **"Tu presupuesto"** (pantalla nueva):
  - **Arriba:** selector de mes y un resumen ("Gastaste S/ X de S/ Y") con barra y frase ("Te
    pasaste S/ 200; sobre todo en Alimentación").
  - **Partidas:** cada una con lo real frente a lo presupuestado, en verde, ámbar desde el 90 % o rojo
    si se pasó, y "Te quedan…" o "Te pasaste…".
  - **Editar:** al tocar una partida se cambia su monto **desde ese mes en adelante**, sin tocar los
    meses anteriores. También se puede renombrar o quitar.
  - **Lo que sobra:** también se muestran lo que no cae en ninguna partida ("Sin partida") y, en lo
    personal, "De tu negocio".
- **Registrar un gasto:** si hay presupuesto, "¿En qué?" muestra las partidas propias. Cada partida
  guarda su subcategoría de siempre, así que la dona, el tope y los reportes no cambian.
- **Gastos de antes:** los anotados antes de armar el presupuesto caen en la partida de su misma
  subcategoría.
- **Inicio:** en "Mi negocio" o "Mi mes", la tarjeta **"Tu presupuesto"** invita a armarlo o muestra
  cuánto lleva.
- **Datos:**
  - tablas `partidas` y `montos_presupuesto` (un monto vale desde su mes hasta que se cambia);
  - columna `partidaId` en gastos e ingresos.

## Paso 29 · Fuentes de ingreso

**Pedido.** Jemina: "no tengo sueldo, tengo ingresos variables: comisiones, clases de matemáticas,
asesorías… quiero anotar cada uno con su actividad y su fecha". También preguntó por la ganancia de sus
inversiones, como fuente de ingreso.

**Cambio.**

- **Al anotar un ingreso personal:** si se armó el presupuesto, aparece **"¿De dónde vino?"** con las
  fuentes (Sueldo, Comisiones, Clases y asesorías, Ganancia de inversiones…). Sin descripción, el
  ingreso toma el nombre de su fuente en las listas.
- **Reportes › Mi plata:** tarjeta **"De dónde vino"**, con el total de cada fuente en el mes (y lo que
  vino del negocio).
- **No incluye** un módulo de inversiones (capital, retiros, rentabilidad); solo la ganancia como
  ingreso.

## Paso 30 · Metas de ahorro

**Pedido.** David: metas como "un viaje en diciembre" o "un estudio", para lo personal y también para
el negocio, con abonos cuando se pueda (quincenal o "si me salió algo"), con el porcentaje y con un aviso
cada vez que se abona.

**Cambio.**

- **"Tus metas"** (pantalla nueva): cada meta tiene un nombre, un objetivo y un plazo opcional (sin
  fecha, en 3 o 6 meses, en 1 año o fin de año).
- **Cada tarjeta** muestra cuánto lleva, el % y **cuánto separar por semana** para llegar a tiempo
  ("Separa S/ 85.00 por semana para llegar al 13 de abril de 2027").
- **"Separar plata":** registra un abono con montos rápidos.
  - El aviso dice "Separaste S/ 700.00. Llevas 26 %." y tiene **Deshacer**.
  - Separar no es un gasto: no cambia la caja ni el tope.
- **Notificaciones:** al cruzar el 25, 50, 75 y 100 % llega un aviso, una sola vez por hito; el último
  dice "¡Cumpliste tu meta…!".
- **Inicio:** en "Mi negocio" o "Mi mes", la tarjeta **"Tus metas"** muestra las dos primeras con su
  barra.
- **Datos:** tablas `metas` y `abonos_meta`.

## Verificación de la ronda 3

- **Tests:** 9 nuevos en `PresupuestoYMetasTest`.
  - Presupuesto:
    - el monto vale para los meses siguientes y cambia en enero;
    - cada gasto va a su partida o a la de su subcategoría;
    - lo que no cae en ninguna partida, las fuentes y lo que vino del negocio;
    - la frase "te pasaste";
    - armarlo desde la plantilla sin repetir partidas;
    - agregar y quitar partidas;
    - el presupuesto del mes solo mira el modo.
  - Metas:
    - cuánto separar por semana;
    - los avisos por hito, una sola vez cada uno;
    - lo que se valida al crear una meta.
  - Además, la exportación de datos incluye el presupuesto y las metas, y todas las pantallas nuevas
    se arman con sus dependencias. Pasan los 122 tests.
- **Migración en el teléfono:**
  1. Se instaló la versión anterior (base v4) como copia de prueba, con la bodega de demostración.
  2. Después se actualizó a la nueva.
  3. La base quedó en la versión 5 con las 4,800 ventas, los 91 gastos y los 5 cobros intactos, y las 4
     tablas nuevas.
- **Recorrido en el teléfono:**
  - "Este mes" en Inicio;
  - armar el presupuesto del negocio;
  - cambiar un monto y agregar "Bolsas";
  - un gasto en "Bolsas", que se sumó a su partida (S/ 12 de S/ 50);
  - una meta de S/ 3,000 a 6 meses con dos abonos (llegó al 26 % y dio el monto semanal);
  - cambiar a lo personal, armar el presupuesto con fuentes y anotar ingresos con fuente;
  - "De dónde vino" en Reportes.
- **Corregido durante la prueba:**
  - el resumen decía "S/ 302.76 de S/ 0.00" cuando aún no había montos; ahora muestra solo lo gastado;
  - un ingreso con fuente y sin descripción salía como "Ingreso"; ahora sale con el nombre de su
    fuente.
- **Archivos:**
  - nuevos `Presupuesto.kt`, `Meta.kt`, `PresupuestoEntities.kt`, `PresupuestoRepository.kt`,
    `PresupuestoRepositoryImpl.kt`, `PresupuestoUseCases.kt` y `MetaUseCases.kt`;
  - nuevas pantallas `presupuesto/` (armar y ver) y `metas/`;
  - nuevos componentes `TarjetaEsteMes.kt`, `BarraAvance.kt` y `TarjetasPlanes.kt`;
  - cambios en Gasto, Venta, Inicio, Reportes, Ayuda, exportar y borrar datos, la navegación y la
    inyección de dependencias.

---

## Paso 31 · Menos texto (ronda 3)

**Problema.** Los usuarios volvieron a decir que hay mucho texto. Con el presupuesto, las metas y el
gráfico "Este mes" sumaron tarjetas y frases. Antes de este paso había **2,050 palabras** de interfaz
en el código de las pantallas.

**Lo que se repetía y se quitó o se juntó.**

| Dónde | Antes | Ahora |
|---|---|---|
| Inicio › Hoy | Tarjeta "Este mes" con barras y una frase, y debajo "Tu caja" con los mismos números en texto | Una sola tarjeta: **"Tu caja"** con barras de lo que entró y lo que se gastó en el periodo elegido (Hoy, Semana o Mes) y "Te quedó". La frase repetía "Te quedó" y se quitó |
| Tu caja | "Gastaste en el negocio", "Te quedó (ganancia)", "¡Bien! Anotaste 7 de los últimos 7 días." | "Gastaste", "Te quedó", "¡Bien! Anotaste 7 de 7 días." |
| Inicio › Mi negocio / Mi mes | Seis tarjetas, entre ellas "Ahorro del mes", que se cruzaba con las metas | Sale "Ahorro del mes". La meta de ahorro sigue en Ajustes, y para juntar están las metas |
| Tarjetas vacías de presupuesto y metas | Una oración cada una | Una fila: el título y "Armar" o "Nueva" |
| Tarjeta "Tu presupuesto" | Siempre una frase | La frase solo aparece si te pasaste, en rojo |
| Tu presupuesto | Debajo de cada partida, "Te quedan S/ X" o "Toca para ponerle monto", y una raya | Solo "Te pasaste S/ X" cuando corresponde. Una indicación al final: "Toca una partida para cambiar su monto." |
| Metas | "Separa S/ 85.00 por semana para llegar al 13 de abril de 2027." | "S/ 85.00 por semana hasta el 13 de abril de 2027" |
| Registrar venta | "Guardaste S/ 3.50 (Gaseosa). Ya puedes anotar la siguiente." y la ayuda "Con nombre, la próxima vez es un toque." | "Guardaste S/ 3.50 · Gaseosa", sin la ayuda |
| Ajustes › Tu actividad | Título y una explicación | Solo el título |
| Ajustes › indicadores de uso | Seis filas a la vista: son para el equipo en el piloto, no para el comerciante | Plegados en "Ver tus números de uso" |
| Ajustes › Tus datos | Título, tres viñetas y "No se copia a la nube. Si cambias de celular, exporta…" | "Todo queda en tu celular. No pedimos claves del banco ni enviamos nada a SUNAT." y "Si cambias de celular, expórtalos antes." |
| Reportes › gráfico | Tres frases (mejor mes, mes más flojo y comparación con el mes pasado) | Solo el mejor mes. La comparación con el mes pasado engañaba: con el mes a medias siempre decía "vendes menos" |

**Resultado.**

- Las palabras de interfaz en el código bajan de 2,050 a **1,857**. Esa cuenta incluye los textos
  plegados, los diálogos y los errores, que no se ven de entrada.
- Lo visible bajó más: Inicio tiene una tarjeta menos en cada pestaña y las tarjetas vacías ya no tienen
  frase.
- No se quitó ninguna función. La meta de ahorro sigue en Ajustes, y los indicadores de uso están a un
  toque.
- No se tocó el aviso de privacidad (texto legal).

**Verificación.**

- Pasan los 122 tests.
- En el teléfono, con la copia de prueba y la bodega de demostración, en tema claro:
  - Inicio, en las dos pestañas;
  - armar el presupuesto, con una partida pasada que sale en rojo;
  - una meta con un abono;
  - Ajustes;
  - Reportes.
- Además, `scripts/demo.py` se actualizó a la base v5: nombra las columnas al insertar y limpia el
  presupuesto y las metas.

**Archivos.**

- `InicioScreen.kt` y `InicioViewModel.kt` (sin el cálculo del ahorro que ya no se muestra).
- `TarjetasPlanes.kt`, `PresupuestoScreen.kt`, `MetasScreen.kt`, `VentaScreen.kt`, `AjustesScreen.kt`,
  `ReporteScreen.kt` y `ResumenPeriodo.kt`.
- Se borró `TarjetaEsteMes.kt`.
- `scripts/demo.py`.

---

# Ronda 4 · Los documentos del 19 de octubre

**Fuente:** `Centavot_01_Proyecto.md` a `Centavot_04_Presupuesto.md`, en el repositorio de documentos.
Lo que se podía programar sin esperar a nadie eran los requerimientos de presupuesto RP-01 a RP-10
(documento 03 §3) y los indicadores K6 y K7 (documento 01 §11.2).

**No se programa:**

- **Registro automático (RF-26):** espera que los usuarios aprueben la propuesta.
- **RMT y "sin RUC" (P4):** esperan al contador.
- **Videos (RF-27):** no son código.
- **Ficha por trabajador, web Pro, iOS y "Ruta del Negocio" (RF-28 a RF-31):** quedan para más adelante.

## Paso 32 · Presupuesto según la guía

**Base de datos versión 6.** Las partidas suman tres columnas: `tipoGasto`, `semanal` y `deuda`. La
migración 5 → 6 es automática.

| Req. | Qué hace la app ahora |
|---|---|
| RP-01 | Cada partida de gasto es **ahorro, fijo, variable o prescindible**. Se elige al agregarla o editarla. Las partidas que ya existían toman el tipo de su subcategoría (alquiler y servicios: fijo; comida, mercadería y transporte: variable; otros: prescindible) |
| RP-02 | "Tu presupuesto" agrupa los gastos por tipo, en el orden de la guía, con el subtotal de cada grupo ("S/ X de S/ Y") |
| RP-03 | **Balance del plan** = lo que entra − ahorro − gastos. "Tu plan deja S/ 240 sin destino: pásalos a ahorro" o, en rojo, "Tu plan gasta S/ 580 más de lo que entra: recorta prescindibles". En "Arma tu presupuesto" se ve en vivo, abajo |
| RP-04 | El **ahorro va primero**: grupo propio en la plantilla y en el presupuesto, con el 10 % sugerido de lo que entra. Lo real es lo que se separó para las metas en el mes: cada abono va a la partida de ahorro con el mismo nombre de la meta, o a la primera. No cuenta como gasto |
| RP-05 | En lo personal, la tarjeta **"Tu reparto frente a 50/30/20"**: necesidades (fijos + variables), estilo de vida (prescindibles) y ahorro, en porcentaje de lo que entra |
| RP-06 | **"De menos a más":** si el ahorro está bajo el 20 %, la tarjeta compara con el mes anterior ("Ahorras 14 %; el mes pasado, 10 %. Vas bien") o anima a subir de a pocos |
| RP-07 | **Cierre de mes:** "Usar lo real en noviembre" deja cada partida con gastos con lo que de verdad se gastó (en una semanal, el promedio por semana). Pide confirmar. Los ingresos no se copian, porque la guía pide presupuestarlos con el mes más bajo |
| RP-08 | **Partidas por semana** ("Gastos menores"): el monto es por semana, se reinicia el lunes y se ve "S/ 25 de S/ 40 esta semana". Al mes cuentan cuatro semanas |
| RP-09 | Al crear una meta, la app sugiere el **fondo de emergencia** (3 o 6 meses de gastos fijos) o, en el negocio, el **colchón** (2 semanas de gastos fijos), ya calculados |
| RP-10 | Una partida fija puede marcarse **"Es pago de una deuda"**. Si las cuotas pasan el 30 % de lo que entra, aviso en rojo: "Tus cuotas son el 33 % de lo que entra. Lo sano es hasta el 30 %" |

**Además:**

- **Plantilla nueva (documento 04 §14.3):**
  - **Personal:** Sueldo, Comisiones, Clases o asesorías, Ganancia de inversiones y Otros ingresos.
    Ahorro del mes, Fondo de emergencia e Inversión. Alquiler, Luz, Pago de deudas, Seguros y
    Pensión escolar. Comida, Transporte, Salud y Educación. Salidas, Streaming, Ropa y Gastos menores
    (por semana).
  - **Negocio:** Ventas, Colchón del negocio, Alquiler del local, Luz, Sueldos, Cuota del Nuevo RUS,
    Cuota del préstamo, Mercadería, Transporte y Otros.
- **Flujo de primer uso (§14.4):** "Arma tu presupuesto" sigue el orden de la guía (lo que entra, el
  ahorro primero, fijos, variables y prescindibles) con el balance en vivo. Es una sola pantalla, no
  varios pasos, para no sumar toques.
- **Ventas en el negocio:** se presupuesta cuánto se espera vender, y las ventas registradas caen solas
  en esa fuente.
- **K6 y K7:** en Ajustes › "Ver tus números de uso" aparecen "Gastos del mes con partida: N %" y
  "Metas con plata este mes: X de Y".
- **Exportar todos mis datos:** el presupuesto sale con el tipo de cada partida.
- **Al registrar un gasto** no se ofrecen las partidas de ahorro.
- **Corregido durante la prueba:**
  - el resumen decía "te quedan -S/ 458" cuando el exceso venía de gastos sin partida;
  - en los diálogos altos (partida y meta) el teclado tapaba "Guardar"; ahora la tecla "listo"
    guarda.

**Verificación.**

- **Tests:** 12 nuevos en `GuiaPresupuestoTest`, uno por requerimiento, con los ejemplos del documento
  04: S/ 3,000 de ingreso que reparten 50/30/20 exacto, los gastos menores de la semana desde el
  lunes, el fondo de 3 y 6 meses, las ventas en el negocio, K6 y K7, y la lectura con gastos sin
  partida. Pasan los 134 tests.
- **Migración en el teléfono:**
  1. Copia de prueba con la versión anterior (base 5), la bodega de demostración y un presupuesto
     armado con la plantilla vieja.
  2. Actualizada a la nueva: la base pasó a la 6 con las 5,248 ventas, los 94 gastos y las 4 partidas.
     Las partidas toman su tipo de la subcategoría y salen agrupadas.
- **Recorrido en el teléfono:**
  - cierre de mes: noviembre quedó con lo real de octubre en 4 partidas;
  - plantilla personal llenada con el ejemplo del documento 04 §10: balance en vivo "Te sobran
    S/ 240.00", que coincide con la cuenta a mano, y reparto 50 / 22 / 20;
  - un gasto en "Gastos menores": "S/ 25.00 de S/ 40.00 esta semana";
  - "Pago de deudas" subido a S/ 1,000: balance negativo y aviso del 33 %;
  - meta "Fondo de emergencia" con la sugerencia de S/ 5,550 y un abono que cayó en su partida de
    ahorro;
  - K6 y K7 en Ajustes.

**Archivos.**

- `Presupuesto.kt`, `PresupuestoEntities.kt`, `CentavotDatabase.kt` (v6), `PresupuestoRepositoryImpl.kt`
  y `PresupuestoUseCases.kt` (`UsarRealComoPresupuestoUseCase`, `ObservarIndicadoresPlanesUseCase`).
- `ArmarPresupuesto*`, `Presupuesto*`, `Metas*`, `Ajustes*`, `GastoViewModel.kt`, `ReporteCsv.kt` y
  `Modulos.kt`.
- Esquema `6.json`.
- Nuevo `scripts/captura.sh`, que guarda una captura recortada.

## Paso 33 · Presupuesto en bloques y menos texto

**Pedido:** "Se ve mucho texto", y que en "Arma tu presupuesto" haya una división entre los grupos. Se
hizo con la skill de diseño `impeccable` en modo *distill*: quitar lo que no hace falta y separar con
espacio y bordes, no con más palabras.

**Bloques.** Nuevo `BloqueGrupo.kt`: cada grupo (lo que entra, ahorro, fijos, variables, prescindibles)
va en un bloque blanco con borde fino, con el título y su total arriba y una línea suave entre filas.
El fondo de la app es verde muy claro, así que el blanco separa un grupo de otro sin más color.

| Pantalla | Antes | Ahora |
|---|---|---|
| Arma tu presupuesto | Un párrafo arriba; grupos solo con un título; cajas de monto con borde; "Agregar otra" al final, con un diálogo de 5 chips para elegir el grupo | Sin párrafo; un bloque por grupo con su total ("S/ 1,030"); el monto en un campo de fondo suave, a la derecha; "Agregar" dentro de cada grupo, y el diálogo solo pide el nombre; abajo, "Te sobran S/ 240 · pásalos a tu ahorro" |
| Tu presupuesto | Tarjeta del gasto, una frase suelta del balance, tarjeta 50/30/20, grupos con título y "Toca una partida para cambiarla." | Un resumen con lo gastado en grande, la lectura y el balance del plan dentro; "Tu reparto" en filas ("50 % de 50 %"); un bloque por grupo con lo real resaltado y el plan en gris; sin la pista; en el negocio el grupo de ingresos se llama "Lo que entra" (antes "Ventas" salía dos veces) |
| Montos | "S/ 2,000.00" | "S/ 2,000" en el presupuesto y en las metas (`formatearCorto()`); si hay céntimos se ven |
| Metas | Chips "Fondo de emergencia · 3 meses: S/ 3,090.00", que se partían en dos líneas; "S/ 57.00 por semana hasta el 20 de octubre de 2027" | "Fondo de emergencia" encima y chips "3 meses · S/ 3,090"; "S/ 57 por semana · hasta octubre 2027" |
| Cobros | Dos líneas: "Total S/ 300.00 · adelantó S/ 100.00" y "Desde ayer" | Una: "Desde ayer · total S/ 300.00 · adelantó S/ 100.00"; un cobro antiguo dice "Hace 38 días" en ámbar |
| Ajustes | "Lo que ganas en un mes normal." bajo el ingreso; "No pedimos claves del banco ni enviamos nada a SUNAT." | Sin la ayuda (solo aparece si hay error); "Todo queda en tu celular. Sin banco ni SUNAT." |

La ayuda ya estaba corta desde el paso 31 y no se tocó.

**Verificación.**

- **Tests:** compila y pasan los 134 tests.
- **Copia de prueba en el teléfono**, con la bodega de demostración, armando los dos presupuestos:
  - en el personal, el ejemplo del documento 04 §10, que cuadra en "Cada sol tiene destino";
  - en el negocio, con S/ 7,000 de ventas, sobran S/ 3,260;
  - "Agregar" en prescindibles abre "Agregar en Gastos prescindibles";
  - la partida semanal se ve "S/ 8 de S/ 40 /sem";
  - siguen funcionando el cierre de mes, la edición con tipo y deuda, la meta sugerida y los cobros.
- **Ajuste durante la prueba:** la línea del campo de monto quedaba encima del separador de la fila.
  Se cambió por un campo con fondo suave y sin línea.
- **Capturas:** se rehicieron 16 en el repositorio de documentos: 12, 23, 24, 35 a 37 y 39 a 48.

## Paso 34 · Marca Andina en la app

**Pedido:** pasar la app al pack de marca (`pach de marca/` en el repositorio de documentos: manual,
logotipos, símbolo e ícono) y rehacer las capturas.

| Qué | Antes | Ahora |
|---|---|---|
| Colores (`Tema.kt`) | Verde y menta | Paleta Andina. **Tinta** #1B1410: texto, botones principales y barras en orden. **Lana** #FAF6EF: fondos. **Maíz** #F4B400: lo elegido y los botones tonales. **Ámbar oscuro**: avisos de tope y cobros antiguos. **Carmín** #C1121F: pasarse o borrar |
| Letra | La del sistema (Roboto) | **Sora**, incluida en la app (`composeResources/font`, cinco pesos; licencia SIL OFL en `licencias/`). Semibold en títulos y regular en textos, como pide el manual |
| Logo (`LogoCentavot.kt`) | La mano con la moneda, teñida, girando, y como marca de agua en la bienvenida | El **logotipo** oficial en su versión Tinta, sin teñir ni girar: el manual prohíbe girarlo y cambiarle los colores. Entra con un fundido corto. En la bienvenida va arriba del saludo |
| Ícono | La mano sobre fondo blanco | La **moneda quipu** sobre fondo Tinta, como el ícono "Principal" del manual. Tiene capa monocromática (la moneda con las cuerdas caladas) para el ícono temático de Android 13+, y versiones cuadrada y redonda para Android 7 o menos |
| Gráfico "En qué se fue tu plata" | Nueve colores genéricos | Primero Tinta, Maíz y Carmín; después tonos que se distinguen sobre Lana |
| PDF para el contador | Títulos verdes | Títulos en Tinta y la raya del encabezado en Maíz |

**Verificación.**

- **Tests y tamaño:** compila y pasan los 134 tests. La APK de release pesa 7.5 MB (antes 7.7 MB): el
  logotipo nuevo pesa mucho menos que el dibujo de la mano, aunque se sumaron los cinco pesos de Sora.
- **Teléfono:** se recorrió toda la app en la copia de prueba, con la bodega de demostración y los dos
  presupuestos. No se cortó ningún texto por el ancho de Sora.
- **Capturas:** se rehicieron las 48 de `capturas-app/`, y la presentación usa ahora las de la marca
  nueva.

## Verificación general · 20 de octubre de 2026 (después del paso 34)

Prueba completa de la app, sin cambios de código.

**Sin teléfono:**

- 134 tests: 0 fallas, 0 errores y 0 saltados.
- Lint de debug y revisión de la versión final: sin problemas.
- APK de release de 7.51 MB.
- La app no pide permiso de internet; solo tiene `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, que agrega
  Android.
- `minSdk` 24, es decir, **Android 7.0 o superior**; los documentos dicen "Android 8+".

**En el teléfono:** copia de prueba, con `scripts/qa.py`. Salieron **28 de 28 casos bien**.

| Área | Casos |
|---|---|
| Primer uso | Aviso, nombre, modo y régimen |
| Negocio sin datos (montos exactos) | Venta de S/ 20; deshacer otra (avisa "Quitaste la venta de S/ 5.00"); gasto de S/ 12.50; S/ 10 para la casa. La caja da S/ 7.50 y "falta en caja" S/ 2.50 |
| Cobros | Estado vacío; contacto nuevo; fiado de S/ 50; abono de S/ 20 (quedan S/ 30); el recordatorio abre WhatsApp; "Cobrar todo" |
| Movimientos y reportes | Filtro por tipo. El resumen del contador da ventas S/ 70 (incluye el fiado, que se suma a las ventas), gastos S/ 12.50 y ganancia S/ 57.50; lo sacado para la casa no cuenta. El PDF (83 KB) y el CSV se generan con los montos correctos, y también "Exportar todos mis datos" |
| Con 6 meses de datos | El aviso del 80 % del tope sale al registrar la venta que lo cruza (al importar datos no se genera). Presupuesto del negocio: "Te sobran S/ 3,260". Presupuesto personal del documento 04: S/ 240 y reparto 50/30/20. El cierre de mes pide confirmar. Meta de S/ 3,090 con S/ 100 separados |
| Robustez | Girar la pantalla a mitad de un registro (no se pierde el monto). Android cierra la app en segundo plano y al volver sigue igual. **6,500 toques al azar** (monkey, 3 semillas): 0 cierres y 0 ANR. "Borrar todos mis datos" vuelve al aviso y limpia `cache/reportes` |
| Rendimiento (versión de depuración) | Arranque en frío ≈ 1.2 s con los 6 meses de datos. Memoria PSS de 246 MB después de las rondas al azar |

**Para revisar:**

- **Memoria:** en la versión de depuración, después de las rondas al azar, la memoria (PSS) llegó a
  246 MB. Hay que medirla en la versión de release, que antes rondaba los 104 MB, sobre todo pensando
  en teléfonos de 2 GB (Q5).
- **"Android 8+":** los documentos dicen eso, pero la app instala desde Android 7.0. O se ajustan los
  documentos o se sube el `minSdk`.
- **"En Excel":** genera un archivo CSV, que Excel abre bien.

## Paso 35 · Ámbar en vez de amarillo claro

**Pedido:** el amarillo claro `#FCE7A8` del botón Gasto y de otros elementos pasa a ser ámbar.

| Qué | Antes | Ahora |
|---|---|---|
| `secondaryContainer`: botón Gasto y botones tonales (Registrar cobro, Cobrar), chips y segmentos elegidos, pestaña activa abajo, etiqueta "Para la casa", resumen de "Mi plata" | `#FCE7A8` | **`#F4B400`**, el Maíz del manual de marca, con texto Tinta (contraste de 9.6:1) |
| `tertiaryContainer`: aviso de tope del 80 % | `#FCE7A8` con texto café | **`#F4B400`** con texto Tinta |
| Etiqueta de categoría "Personal" | Amarillo claro | **Tinta** con texto Lana. Si fuera ámbar, no se distinguiría de "Negocio", que ya es ámbar |

**Verificación.**

- **Tests:** compila y pasan los 134.
- **Teléfono:** la copia de prueba, con los datos de demostración, muestra el ámbar en inicio,
  cobros, movimientos, gasto y "Mi plata". Ningún texto queda sin leerse.
- **Capturas e instalación:** el mismo día se instaló en el teléfono del equipo (arrancó sin errores), se
  rehicieron las 48 capturas de `capturas-app/` y se cambiaron las 4 de la presentación.

## Paso 36 · Ícono sin el fondo negro

**Pedido:** al usuario no le gusta el fondo negro (Tinta) alrededor del ícono en el teléfono.

- **Versión:** el ícono pasa a la versión **"Alternativo"** del manual de marca, la moneda quipu sobre
  fondo **Lana** `#FAF6EF`.
- **Tamaño:** la moneda crece del 46 % al 52 % del lienzo adaptativo, para que llene mejor el círculo o
  el cuadrado del lanzador.
- **Para Android 7 o menos:** el ícono cuadrado y el redondo también van sobre Lana.
- **Sin cambio:** la capa monocromática de Android 13+ (la moneda con las cuerdas caladas).
- **Archivos:**
  - `values/colores_icono.xml`;
  - `mipmap-*/ic_launcher_foreground.png`, `ic_launcher.png` e `ic_launcher_round.png`;
  - el comentario de `mipmap-anydpi-v26/*.xml`.
- **Verificación:** compila. Una vista previa con las máscaras de círculo, cuadrado redondeado y
  cuadrado muestra la moneda centrada, sin bordes negros.

## Paso 37 · La moneda ocupa todo el ícono

**Pedido:** que el logo ocupe todo el ícono, sin el borde claro del paso 36.

- **Capa de adelante:** es un cuadrado Maíz `#F4B400` entero con el quipu encima, y el fondo
  adaptativo también es Maíz. El ícono se ve como la moneda en cualquier forma (círculo, cuadrado
  redondeado o cuadrado) y no queda la orilla del círculo.
- **Escala:** el quipu mantiene la proporción de la moneda. Si la moneda midiera los 72 dp visibles del
  lienzo de 108 dp, el símbolo entero mediría 72 × 200/184 = 78.3 dp, el 72.5 % del lienzo.
- **Capa monocromática de Android 13+:** la moneda con las cuerdas caladas, a la misma escala.
- **Para Android 7 o menos:** el ícono cuadrado y el redondo van enteros en Maíz con el quipu.
- **Nota de marca:** el manual pide no poner el *logotipo* (la palabra) sobre fondo maíz. Aquí no hay
  logotipo: el ícono es la cara de la moneda.
- **Verificación:** compila. La vista previa con las tres máscaras no muestra bordes ni uniones.

## Paso 38 · Quitar lo agregado por error y partidas al registrar

**Pedido:**

1. En "Arma tu presupuesto" no se podía borrar una partida agregada por error.
2. Al registrar un ingreso o un gasto tienen que aparecer las partidas del presupuesto, incluidas las
   agregadas a mano, cada una donde corresponde y con criterio.

**Antes:**

- **Gasto:** ya mostraba las partidas de gasto, pero mezcladas, en el orden en que se crearon.
- **Ingreso personal:** ya preguntaba "¿De dónde vino?" con las fuentes.
- **Venta del negocio:** nunca preguntaba la fuente; todo caía en la primera, "Ventas".

**El criterio**, escrito como funciones del dominio en `Presupuesto.kt`:

| Registro | Qué partidas ofrece | Regla |
|---|---|---|
| Gasto (`paraRegistrarGasto`) | Las de gasto: fijos, luego variables, luego prescindibles; dentro de cada grupo, en el orden del presupuesto | El **ahorro no aparece**, porque se separa desde Metas. Las fuentes de ingreso tampoco |
| Ingreso personal (`fuentesDeIngreso`) | Las fuentes de ingreso, en su orden | Opcional; sin elegir no se marca ninguna |
| Venta del negocio (`fuentesParaVenta`) | Las fuentes de ingreso, **solo si hay más de una**, bajo "¿Qué tipo de venta?" | Con una sola fuente no se pregunta nada, para no sumar toques. Con varias, la primera ("Ventas") ya viene marcada y vuelve a marcarse después de cada venta |
| Retiro para la casa | Ninguna | No es ingreso ni gasto del presupuesto |

**Lo que agrega el usuario aparece solo:** una partida propia, agregada al armar o desde "Agregar
partida", sale en el registro que le toca según su tipo: "Regalos y otros" en Gasto, "Propinas" en
Ingreso y "Servicios" en Venta.

**Quitar en "Arma tu presupuesto":** las partidas que agrega el usuario llevan un botón **✕ "Quitar"**
(`Iconos.Quitar`) que las borra de la lista al instante. No pide confirmar, porque todavía no se guardó
nada. Las de la plantilla no lo llevan: se desmarcan con el check. Una partida ya guardada se quita
como antes: en "Tu presupuesto", tocarla → "Quitar del presupuesto".

**Verificación.**

- **Tests:** 6 nuevos en `PartidasAlRegistrarTest` (orden de la guía, sin ahorro, tipo deducido de la
  subcategoría, fuentes del ingreso, venta con una y con dos fuentes). Pasan los **140**.
- **Teléfono:** copia de prueba, con `scripts/qa.py`. Salieron 9 de 9 casos bien:
  - "Me equivoque" se agrega y se quita con ✕; las partidas de la plantilla no tienen ✕;
  - en Gasto, las 7 partidas en orden fijos → variables → prescindibles, con "Regalos y otros" y sin
    ahorro ni ingresos;
  - en Ingreso salen Sueldo, Comisiones y Propinas, y un ingreso en Propinas cae ahí ("S/ 50 de
    S/ 150");
  - en el negocio con solo "Ventas", la venta no pregunta nada;
  - con "Servicios" agregada aparece "¿Qué tipo de venta?" con "Ventas" marcada: S/ 20 caen en
    Servicios y S/ 10 en Ventas.

## Paso 39 · Fechas de Movimientos sin descuadre

**Pedido:** "En Movimientos la opción para poner fecha descuadra la vista." Se usó la skill de diseño
`impeccable` en modo *layout*.

**Lo que se encontró en el teléfono:**

1. **El selector de rango de fechas se desarmaba:**
   - el título por defecto, "Fecha de inicio – Fecha de finalización", no cabía con Sora y se partía
     letra por letra ("Fe / ch / a / de fin / aliz / aci / ón");
   - el calendario quedaba cortado en un diálogo de 500 dp, con solo dos semanas y media a la vista;
   - el segundo toque no siempre tomaba el día.
2. **"Elegir fechas" quedaba fuera de la pantalla,** al final de una segunda fila de chips deslizable;
   había que adivinar que estaba ahí.
3. **Con un rango elegido,** el chip decía "5 de octubre – 12 de octubre", empujaba la fila y quedaba
   cortado.
4. **Arriba había dos filas de chips** y un resumen de dos renglones ("Vendiste S/ … · Gastaste S/ … ·
   Para la casa S/ …").

**El arreglo** (`MovimientosScreen.kt`):

| Antes | Ahora |
|---|---|
| Segunda fila de chips: Todo / Hoy / Esta semana / Este mes / Elegir fechas | **Un solo botón de período** ("📅 Todas las fechas ▾") con menú: Todas las fechas, Hoy, Esta semana, Este mes y "Elegir fechas…". El elegido lleva ✓; el botón queda en ámbar cuando hay filtro |
| Rango largo en el chip | **Rango corto** con `etiquetaRango`: "5 oct", "5 – 12 oct", "27 sep – 3 oct"; el año solo si no es el actual |
| Selector en diálogo, con el título partido | **Pantalla completa**, el patrón de Material 3 para rangos: barra con ✕ y "Listo", título corto que guía ("Toca el primer día" → "5 oct · toca el último día" → "5 – 12 oct"), sin el cambio a modo texto, fondo Lana único y el rango anterior ya marcado al volver a abrirlo |
| Fila de tipos con margen dentro del deslizamiento (el último chip se cortaba sin aire) | `LazyRow` con margen de 16 dp a los lados |
| Resumen en una frase de dos renglones | **Columnas** sobre una superficie suave: Vendiste · Gastaste · Para la casa (o Te entró · Gastaste en lo personal) |

**Verificación.**

- **Tests:** 1 nuevo en `FiltroMovimientosTest` (las etiquetas cortas de rango, incluido el cambio de
  mes y de año). Pasan los **141**, y el lint sigue sin problemas.
- **Teléfono:** copia de prueba con 6 meses de datos.
  - El menú del período abre y marca el elegido.
  - Un rango del 5 al 12 de octubre filtra: vendiste S/ 2,559.10.
  - Un rango del 27 de septiembre al 3 de octubre se ve completo en el botón.
  - En el modo personal salen dos columnas.
  - Ya no hay texto partido ni elementos fuera de la pantalla.

