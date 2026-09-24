# MVP Android — Centavot

**Fecha:** 23 de septiembre de 2026
**Estado:** compila y pasa 23 de 23 tests. Falta probarlo en un emulador o teléfono.

El MVP funciona **sin backend**: todo se guarda en el teléfono con Room.

## Pantallas

1. **Elegir régimen** — es lo primero que aparece. Opciones:
   - RUS Categoría 1 — S/ 5,000 al mes
   - RUS Categoría 2 — S/ 8,000 al mes
   - RER — S/ 525,000 al año

   Con una nota de que los montos son referenciales.
2. **Inicio**
   - Tarjeta con el % del tope usado, barra de progreso y un mensaje según el nivel:
     "vas bien", aviso al 80 % y 90 %, y rojo al llegar al tope.
   - Totales del mes: negocio y personal.
   - Últimos 5 gastos.
   - Botón "Registrar gasto".
3. **Registrar o editar gasto** — monto con teclado numérico, "¿Para qué fue?"
   (Negocio o Personal), descripción opcional y fecha (Hoy, Ayer u otra con calendario).
   Al editar se puede eliminar, con confirmación.
4. **Movimientos** — gastos agrupados por día, con el total de cada día y filtros
   Todos / Negocio / Personal.
5. **Reporte SUNAT** — se elige el mes con flechas y se ve el total de gastos de
   negocio y su lista, con la recomendación de revisarlo con el contador.

## Criterios de UX aplicados

- Lenguaje simple y cercano, sin términos técnicos.
- Las alertas usan color, ícono y texto a la vez, no solo color.
- Botones grandes, fáciles de tocar.
- Mensajes útiles cuando una lista está vacía, con un botón para actuar.
- Un error al lado de cada campo que falte llenar.
- El campo de monto no acepta letras ni más de 2 decimales.
- Soporte de modo oscuro y lectores de pantalla.

## Cómo está armado

| Capa | Qué contiene |
|---|---|
| Datos (`data/`) | Room guarda los gastos y el régimen: entidades, DAOs, mappers y repositorios. |
| Reglas del negocio (`domain/`) | Modelos y 10 casos de uso: guardar y eliminar un gasto, calcular cuánto falta para el tope, resumen del mes y reporte, entre otros. |
| Pantallas (`presentation/`) | Cada pantalla tiene su ViewModel, con navegación tipada entre pantallas. |
| Inyección (`di/`) | Koin conecta las capas. La base de datos de Android se registra en `shared/src/androidMain`. |

- Los topes están en un solo archivo (`data/local/TopesReferenciales.kt`). Cuando
  haya backend, se cambia solo ese archivo.
- Tests: 13 nuevos para los casos de uso y el formato de montos y fechas.

## Cómo probarlo

Abrir el proyecto en Android Studio y ejecutar `androidApp` en el emulador Pixel_7
(Run ▶), o por terminal:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
```

## Pendientes

- **Probarlo en un emulador o teléfono** — todavía nadie lo ha visto funcionando.
- **iOS:** no tiene configurada la base de datos, así que se cierra al iniciar.
  Queda así mientras el foco sea Android.
- **Tope por gastos o por ingresos:** la alerta suma gastos de negocio, como indica
  el documento de arquitectura. Pero SUNAT mide el tope del RUS por ingresos o
  compras. Si debe medirse por ingresos, la app tendrá que registrar ingresos también.
  Hay que confirmarlo con alguien que sepa del tema tributario.
- **Fuera de este MVP:** foto de boleta (OCR), registro por voz y exportar el
  reporte para el contador.
