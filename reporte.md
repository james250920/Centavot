# Reporte de mejoras UX/UI

Mejoras que salieron de la revisión UX/UI del 10 de octubre de 2026 (heurísticas de Nielsen, puntaje
inicial **28/40**). Se aplican una por una en la rama `feat/mejoras-ux`; cada paso queda registrado
aquí al terminarlo. Las pruebas en teléfono se hacen con una copia aparte (`com.app.centavot.prueba`)
y datos de demostración, en un Samsung Galaxy A15 con Android 16.

| # | Paso | Problema de la revisión | Estado |
|---|---|---|---|
| 1 | Registrar venta más simple | [P1] Demasiadas decisiones para la acción más frecuente | ✅ |
| 2 | Cobros sin acciones tapadas | [P1] El botón flotante tapa acciones; 5 acciones por cobro | ⏳ |
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
