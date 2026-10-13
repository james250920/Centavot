package com.app.centavot.domain.model

import kotlinx.datetime.YearMonth

/**
 * Presupuesto del usuario, pedido en la reunión con usuarios del 10 de octubre: cada uno arma el suyo a
 * partir de una plantilla (marca con check lo que le sirve), lo cambia mes a mes y, al registrar, elige
 * entre **sus** partidas en vez de una lista larga.
 */
enum class TipoPartida { GASTO, INGRESO }

/**
 * Una línea del presupuesto: "Alimentación", "Pensión del colegio", "Clases de matemáticas"...
 * [subcategoria] la conecta con la clasificación de siempre (dona, tope, reportes); las que el usuario
 * crea desde cero quedan en [SubcategoriaGasto.OTROS].
 */
data class Partida(
    val id: String,
    val nombre: String,
    val tipo: TipoPartida,
    val categoria: Categoria,
    val subcategoria: SubcategoriaGasto? = null,
    val orden: Int = 0,
)

/** Cuánto se piensa gastar (o recibir) en una partida desde [mes]; vale para los meses siguientes hasta que se cambie. */
data class MontoPresupuesto(val partidaId: String, val mes: YearMonth, val monto: Monto)

/** El monto de [partidaId] para [mes]: el último que se fijó en ese mes o antes. Enero puede ser distinto de octubre. */
fun List<MontoPresupuesto>.montoPara(partidaId: String, mes: YearMonth): Monto =
    filter { it.partidaId == partidaId && it.mes <= mes }.maxByOrNull { it.mes }?.monto ?: Monto.CERO

enum class EstadoLinea {
    /** Sin monto presupuestado: solo se muestra lo real. */
    SIN_MONTO,
    BIEN,

    /** Desde el 90 %. */
    CERCA,
    PASADO,
}

data class LineaPresupuesto(val partida: Partida, val presupuesto: Monto, val real: Monto) {
    val porcentaje: Int get() = if (presupuesto <= Monto.CERO) 0 else (real.centimos * 100 / presupuesto.centimos).toInt()

    /** Lo que queda (positivo) o lo que se pasó (negativo). */
    val diferencia: Monto get() = presupuesto - real

    val estado: EstadoLinea
        get() = when {
            presupuesto <= Monto.CERO -> EstadoLinea.SIN_MONTO
            real > presupuesto -> EstadoLinea.PASADO
            porcentaje >= UMBRAL_CERCA -> EstadoLinea.CERCA
            else -> EstadoLinea.BIEN
        }
}

private const val UMBRAL_CERCA = 90

/** El presupuesto de un mes frente a lo que de verdad pasó, para un modo. */
data class PresupuestoMes(
    val mes: YearMonth,
    val gastos: List<LineaPresupuesto>,
    val ingresos: List<LineaPresupuesto>,
    /** Gastos del mes que no caen en ninguna partida. */
    val gastoSinPartida: Monto,
    /** Ingresos del mes sin fuente elegida. */
    val ingresoSinFuente: Monto,
    /** En lo personal: lo que vino de la caja del negocio ("Saqué para la casa"). */
    val deTuNegocio: Monto = Monto.CERO,
) {
    val hayPartidas: Boolean get() = gastos.isNotEmpty() || ingresos.isNotEmpty()
    val presupuestoGastos: Monto get() = gastos.map { it.presupuesto }.sumar()
    val realGastos: Monto get() = gastos.map { it.real }.sumar() + gastoSinPartida
    val presupuestoIngresos: Monto get() = ingresos.map { it.presupuesto }.sumar()
    val realIngresos: Monto get() = ingresos.map { it.real }.sumar() + ingresoSinFuente + deTuNegocio
    val pasadas: List<LineaPresupuesto> get() = gastos.filter { it.estado == EstadoLinea.PASADO }

    /** Una frase que resume el mes, para quien no quiere leer la tabla. */
    val lectura: String?
        get() {
            if (presupuestoGastos <= Monto.CERO) return null
            val diferencia = presupuestoGastos - realGastos
            val mayor = pasadas.maxByOrNull { it.real - it.presupuesto }
            return when {
                mayor != null && diferencia < Monto.CERO ->
                    "Te pasaste ${Monto(-diferencia.centimos).enSoles()} del presupuesto; sobre todo en ${mayor.partida.nombre}."
                mayor != null ->
                    "Vas dentro del presupuesto, pero en ${mayor.partida.nombre} ya te pasaste."
                else -> "Vas dentro del presupuesto: te quedan ${diferencia.enSoles()} por gastar."
            }
        }

    companion object {
        /**
         * Junta partidas, montos y movimientos del mes. Un gasto va a su partida; si no eligió ninguna (por
         * ejemplo, uno anotado antes de armar el presupuesto), a la primera partida con su misma subcategoría.
         */
        fun de(
            mes: YearMonth,
            partidas: List<Partida>,
            montos: List<MontoPresupuesto>,
            gastos: List<Gasto>,
            ingresos: List<Ingreso>,
        ): PresupuestoMes {
            val deGasto = partidas.filter { it.tipo == TipoPartida.GASTO }.sortedBy { it.orden }
            val deIngreso = partidas.filter { it.tipo == TipoPartida.INGRESO }.sortedBy { it.orden }
            val idsGasto = deGasto.map { it.id }.toSet()
            val idsIngreso = deIngreso.map { it.id }.toSet()

            val realPorPartida = mutableMapOf<String, Monto>()
            var sinPartida = Monto.CERO
            gastos.forEach { gasto ->
                val destino = gasto.partidaId?.takeIf { it in idsGasto }
                    ?: deGasto.firstOrNull { it.subcategoria != null && it.subcategoria == gasto.subcategoria }?.id
                if (destino == null) {
                    sinPartida += gasto.monto
                } else {
                    realPorPartida[destino] = (realPorPartida[destino] ?: Monto.CERO) + gasto.monto
                }
            }
            var sinFuente = Monto.CERO
            var deTuNegocio = Monto.CERO
            ingresos.forEach { ingreso ->
                val destino = ingreso.partidaId?.takeIf { it in idsIngreso }
                when {
                    ingreso.retiroDelNegocio -> deTuNegocio += ingreso.monto
                    destino == null -> sinFuente += ingreso.monto
                    else -> realPorPartida[destino] = (realPorPartida[destino] ?: Monto.CERO) + ingreso.monto
                }
            }
            fun lineas(lista: List<Partida>) = lista.map { partida ->
                LineaPresupuesto(partida, montos.montoPara(partida.id, mes), realPorPartida[partida.id] ?: Monto.CERO)
            }
            return PresupuestoMes(mes, lineas(deGasto), lineas(deIngreso), sinPartida, sinFuente, deTuNegocio)
        }
    }
}

/** Un ítem de la plantilla: se marca con check y se le pone monto (o se deja en blanco). */
data class ItemPlantilla(
    val nombre: String,
    val tipo: TipoPartida,
    val subcategoria: SubcategoriaGasto?,
    /** Viene marcado: lo que casi todos tienen. */
    val marcado: Boolean,
)

/** Plantilla base para armar el presupuesto sin escribir, según el modo. */
fun plantillaPresupuesto(modo: Modo): List<ItemPlantilla> = when (modo) {
    Modo.PERSONAL -> listOf(
        ItemPlantilla("Sueldo", TipoPartida.INGRESO, null, marcado = true),
        ItemPlantilla("Comisiones", TipoPartida.INGRESO, null, marcado = false),
        ItemPlantilla("Clases y asesorías", TipoPartida.INGRESO, null, marcado = false),
        ItemPlantilla("Cachuelos", TipoPartida.INGRESO, null, marcado = false),
        ItemPlantilla("Ganancia de inversiones", TipoPartida.INGRESO, null, marcado = false),
        ItemPlantilla("Alimentación", TipoPartida.GASTO, SubcategoriaGasto.ALIMENTACION, marcado = true),
        ItemPlantilla("Alquiler o vivienda", TipoPartida.GASTO, SubcategoriaGasto.CASA, marcado = true),
        ItemPlantilla("Luz, agua e internet", TipoPartida.GASTO, SubcategoriaGasto.SERVICIOS, marcado = true),
        ItemPlantilla("Transporte", TipoPartida.GASTO, SubcategoriaGasto.TRANSPORTE, marcado = true),
        ItemPlantilla("Celular", TipoPartida.GASTO, null, marcado = false),
        ItemPlantilla("Educación y pensiones", TipoPartida.GASTO, SubcategoriaGasto.EDUCACION, marcado = false),
        ItemPlantilla("Salud", TipoPartida.GASTO, SubcategoriaGasto.SALUD, marcado = false),
        ItemPlantilla("Deudas y cuotas", TipoPartida.GASTO, null, marcado = false),
        ItemPlantilla("Ropa", TipoPartida.GASTO, null, marcado = false),
        ItemPlantilla("Salidas y entretenimiento", TipoPartida.GASTO, null, marcado = false),
    )
    Modo.NEGOCIO -> listOf(
        ItemPlantilla("Mercadería e insumos", TipoPartida.GASTO, SubcategoriaGasto.MERCADERIA, marcado = true),
        ItemPlantilla("Alquiler del local", TipoPartida.GASTO, SubcategoriaGasto.ALQUILER, marcado = true),
        ItemPlantilla("Luz, agua e internet", TipoPartida.GASTO, SubcategoriaGasto.SERVICIOS, marcado = true),
        ItemPlantilla("Transporte y delivery", TipoPartida.GASTO, SubcategoriaGasto.TRANSPORTE, marcado = true),
        ItemPlantilla("Sueldos y pagos al personal", TipoPartida.GASTO, SubcategoriaGasto.PERSONAL_NEGOCIO, marcado = false),
        ItemPlantilla("Bolsas y empaques", TipoPartida.GASTO, null, marcado = false),
        ItemPlantilla("Mantenimiento y reparaciones", TipoPartida.GASTO, null, marcado = false),
        ItemPlantilla("Publicidad", TipoPartida.GASTO, null, marcado = false),
        ItemPlantilla("Impuestos y trámites", TipoPartida.GASTO, null, marcado = false),
    )
}
