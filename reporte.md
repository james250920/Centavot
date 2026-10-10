# Reporte de mejoras UX/UI

Mejoras que salieron de la revisión UX/UI del 10 de octubre de 2026 (heurísticas de Nielsen, puntaje
inicial **28/40**). Se aplican una por una en la rama `feat/mejoras-ux`; cada paso queda registrado
aquí al terminarlo. Las pruebas en teléfono se hacen con una copia aparte (`com.app.centavot.prueba`)
y datos de demostración, en un Samsung Galaxy A15 con Android 16.

| # | Paso | Problema de la revisión | Estado |
|---|---|---|---|
| 1 | Registrar venta más simple | [P1] Demasiadas decisiones para la acción más frecuente | ✅ |
| 2 | Cobros sin acciones tapadas | [P1] El botón flotante tapa acciones; 5 acciones por cobro | ✅ |
| 3 | Confirmación y deshacer al guardar | [P2] Gasto, cobro y abono se cierran sin decir nada | ⏳ |
| 4 | Colores con un solo significado | [P2] Ámbar para alertas y para cosas neutras | ⏳ |
| 5 | Barra superior de Inicio | [P2] 4 íconos sin texto | ⏳ |
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
