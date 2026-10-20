package com.app.centavot.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.yearMonth

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
    /** v6 (RP-01): fijo, variable, prescindible o ahorro. Sin elegir, se deduce de la subcategoría. */
    val tipoGasto: TipoGasto? = null,
    /** v6 (RP-08): el monto es por semana y se reinicia el lunes ("Gastos menores"). */
    val semanal: Boolean = false,
    /** v6 (RP-10): es el pago de una deuda (cuota de préstamo, tarjeta). */
    val deuda: Boolean = false,
) {
    /** El tipo con que se agrupa en el presupuesto. */
    val clase: TipoGasto get() = tipoGasto ?: tipoGastoPorDefecto(subcategoria)
}

/**
 * Los grupos de gasto de la guía de finanzas (documento 04 §5): se separa primero el ahorro; luego lo
 * que se paga sí o sí, lo necesario cuyo monto se puede optimizar y lo que se puede recortar.
 */
enum class TipoGasto(val etiqueta: String, val bloque: Bloque503020) {
    AHORRO("Ahorro", Bloque503020.AHORRO),
    FIJO("Gastos fijos", Bloque503020.NECESIDADES),
    VARIABLE("Gastos variables", Bloque503020.NECESIDADES),
    PRESCINDIBLE("Gastos prescindibles", Bloque503020.ESTILO_DE_VIDA),
}

/** La regla 50/30/20 (documento 04 §8): una referencia, no una obligación. */
enum class Bloque503020(val etiqueta: String, val referencia: Int) {
    NECESIDADES("Necesidades", 50),
    ESTILO_DE_VIDA("Estilo de vida", 30),
    AHORRO("Ahorro", 20),
}

/** Para las partidas que el usuario no clasificó: la subcategoría dice si se paga sí o sí. */
fun tipoGastoPorDefecto(subcategoria: SubcategoriaGasto?): TipoGasto = when (subcategoria) {
    SubcategoriaGasto.ALQUILER, SubcategoriaGasto.SERVICIOS, SubcategoriaGasto.PERSONAL_NEGOCIO,
    SubcategoriaGasto.CASA, SubcategoriaGasto.EDUCACION -> TipoGasto.FIJO
    SubcategoriaGasto.MERCADERIA, SubcategoriaGasto.ALIMENTACION, SubcategoriaGasto.TRANSPORTE,
    SubcategoriaGasto.SALUD -> TipoGasto.VARIABLE
    SubcategoriaGasto.OTROS, null -> TipoGasto.PRESCINDIBLE
}

/** Un mes se cuenta como cuatro semanas, como en la guía ("S/ 40 por semana = S/ 160 al mes"). */
const val SEMANAS_POR_MES = 4

/** Límite de la guía para las cuotas de deudas: el 30 % del ingreso (documento 04 §6). */
const val LIMITE_DEUDAS_PORCENTAJE = 30

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

/**
 * Una partida en un mes. [presupuesto] y [real] son del mes; en una partida semanal también van los de la
 * semana en curso ([montoSemanal], [realSemana]), que es lo que manda en su estado.
 */
data class LineaPresupuesto(
    val partida: Partida,
    val presupuesto: Monto,
    val real: Monto,
    val montoSemanal: Monto? = null,
    val realSemana: Monto? = null,
) {
    private val base: Monto get() = montoSemanal?.takeIf { realSemana != null } ?: presupuesto
    private val gastado: Monto get() = realSemana?.takeIf { montoSemanal != null } ?: real

    val porcentaje: Int get() = if (base <= Monto.CERO) 0 else (gastado.centimos * 100 / base.centimos).toInt()

    /** Lo que queda (positivo) o lo que se pasó (negativo), de la semana en una partida semanal. */
    val diferencia: Monto get() = base - gastado

    val estado: EstadoLinea
        get() = when {
            base <= Monto.CERO -> EstadoLinea.SIN_MONTO
            gastado > base -> EstadoLinea.PASADO
            porcentaje >= UMBRAL_CERCA -> EstadoLinea.CERCA
            else -> EstadoLinea.BIEN
        }
}

private const val UMBRAL_CERCA = 90

/** Cuánto del ingreso presupuestado va a cada bloque de la regla 50/30/20, en porcentaje. */
data class Reparto503020(val necesidades: Int, val estiloDeVida: Int, val ahorro: Int) {
    fun de(bloque: Bloque503020): Int = when (bloque) {
        Bloque503020.NECESIDADES -> necesidades
        Bloque503020.ESTILO_DE_VIDA -> estiloDeVida
        Bloque503020.AHORRO -> ahorro
    }
}

/** El presupuesto de un mes frente a lo que de verdad pasó, para un modo. */
data class PresupuestoMes(
    val mes: YearMonth,
    /** Gastos sin el ahorro, en el orden de la guía: fijos, variables, prescindibles. */
    val gastos: List<LineaPresupuesto>,
    val ingresos: List<LineaPresupuesto>,
    /** Gastos del mes que no caen en ninguna partida. */
    val gastoSinPartida: Monto,
    /** Ingresos del mes sin fuente elegida. */
    val ingresoSinFuente: Monto,
    /** En lo personal: lo que vino de la caja del negocio ("Saqué para la casa"). */
    val deTuNegocio: Monto = Monto.CERO,
    /** RP-04: las partidas de ahorro; lo real es lo que se separó para las metas en el mes. */
    val ahorro: List<LineaPresupuesto> = emptyList(),
    /** Lo separado para metas cuando no hay ninguna partida de ahorro. */
    val separadoSinPartida: Monto = Monto.CERO,
) {
    val hayPartidas: Boolean get() = gastos.isNotEmpty() || ingresos.isNotEmpty() || ahorro.isNotEmpty()
    val presupuestoGastos: Monto get() = gastos.map { it.presupuesto }.sumar()
    val realGastos: Monto get() = gastos.map { it.real }.sumar() + gastoSinPartida
    val presupuestoIngresos: Monto get() = ingresos.map { it.presupuesto }.sumar()
    val realIngresos: Monto get() = ingresos.map { it.real }.sumar() + ingresoSinFuente + deTuNegocio
    val presupuestoAhorro: Monto get() = ahorro.map { it.presupuesto }.sumar()
    val realAhorro: Monto get() = ahorro.map { it.real }.sumar() + separadoSinPartida
    val pasadas: List<LineaPresupuesto> get() = gastos.filter { it.estado == EstadoLinea.PASADO }

    /** RP-02: los gastos agrupados por tipo, en el orden de la guía, sin grupos vacíos. */
    val grupos: List<Pair<TipoGasto, List<LineaPresupuesto>>>
        get() = listOf(TipoGasto.FIJO, TipoGasto.VARIABLE, TipoGasto.PRESCINDIBLE)
            .map { tipo -> tipo to gastos.filter { it.partida.clase == tipo } }
            .filter { it.second.isNotEmpty() }

    /** RP-03: ingresos presupuestados − ahorro − gastos. Null si no se presupuestó lo que entra. */
    val balance: Monto?
        get() = if (presupuestoIngresos <= Monto.CERO) null else presupuestoIngresos - presupuestoAhorro - presupuestoGastos

    /** RP-10: qué parte del ingreso presupuestado se va en cuotas de deudas. */
    val porcentajeDeudas: Int?
        get() {
            if (presupuestoIngresos <= Monto.CERO) return null
            val deudas = gastos.filter { it.partida.deuda }.map { it.presupuesto }.sumar()
            if (deudas <= Monto.CERO) return null
            return (deudas.centimos * 100 / presupuestoIngresos.centimos).toInt()
        }

    val deudasAltas: Boolean get() = (porcentajeDeudas ?: 0) > LIMITE_DEUDAS_PORCENTAJE

    /** RP-05: el reparto del presupuesto frente a 50/30/20. Null si no se presupuestó lo que entra. */
    val reparto: Reparto503020?
        get() {
            val ingresos = presupuestoIngresos.centimos
            if (ingresos <= 0) return null
            fun pct(monto: Monto) = (monto.centimos * 100 / ingresos).toInt()
            val necesidades = gastos.filter { it.partida.clase.bloque == Bloque503020.NECESIDADES }.map { it.presupuesto }.sumar()
            val estilo = gastos.filter { it.partida.clase.bloque == Bloque503020.ESTILO_DE_VIDA }.map { it.presupuesto }.sumar()
            return Reparto503020(pct(necesidades), pct(estilo), pct(presupuestoAhorro))
        }

    /** Una frase que resume el mes, para quien no quiere leer la tabla. */
    val lectura: String?
        get() {
            if (presupuestoGastos <= Monto.CERO) return null
            val diferencia = presupuestoGastos - realGastos
            val mayor = pasadas.maxByOrNull { it.real - it.presupuesto }
            return when {
                mayor != null && diferencia < Monto.CERO ->
                    "Te pasaste ${Monto(-diferencia.centimos).enSoles()} del presupuesto; sobre todo en ${mayor.partida.nombre}."
                // Ninguna partida se pasó, pero sí el total: el exceso viene de gastos sin partida.
                diferencia < Monto.CERO -> "Te pasaste ${Monto(-diferencia.centimos).enSoles()} del presupuesto con gastos sin partida."
                mayor != null ->
                    "Vas dentro del presupuesto, pero en ${mayor.partida.nombre} ya te pasaste."
                else -> "Vas dentro del presupuesto: te quedan ${diferencia.enSoles()} por gastar."
            }
        }

    /**
     * RP-07, cierre de mes: el presupuesto del mes siguiente con lo que de verdad se gastó en cada partida
     * ("si presupuestaste S/ 500 en comida pero gastas S/ 700, lo realista es S/ 700"). Solo las partidas con
     * gasto; en una semanal, el promedio por semana. Los ingresos no se copian: la guía pide presupuestarlos
     * con el mes más bajo.
     */
    fun realComoPresupuesto(): List<Pair<Partida, Monto>> = gastos.filter { it.real > Monto.CERO }.map { linea ->
        val monto = if (linea.partida.semanal) Monto(linea.real.centimos / SEMANAS_POR_MES) else linea.real
        linea.partida to monto
    }

    companion object {
        /**
         * Junta partidas, montos y movimientos del mes.
         *
         * - Un gasto va a su partida; si no eligió ninguna (por ejemplo, uno anotado antes de armar el
         *   presupuesto), a la primera partida con su misma subcategoría.
         * - Un ingreso va a su fuente. En el negocio, una venta sin fuente va a la primera fuente ("Ventas").
         * - Lo separado para metas ([separado]: nombre de la meta y monto) va a la partida de ahorro con el
         *   mismo nombre, o si no a la primera partida de ahorro.
         * - Con [hoy] dentro del mes, las partidas semanales también llevan lo de la semana (desde el lunes).
         */
        fun de(
            mes: YearMonth,
            partidas: List<Partida>,
            montos: List<MontoPresupuesto>,
            gastos: List<Gasto>,
            ingresos: List<Ingreso>,
            separado: List<Pair<String, Monto>> = emptyList(),
            hoy: LocalDate? = null,
            ventasALaPrimeraFuente: Boolean = false,
        ): PresupuestoMes {
            val ordenadas = partidas.sortedBy { it.orden }
            val deAhorro = ordenadas.filter { it.tipo == TipoPartida.GASTO && it.clase == TipoGasto.AHORRO }
            val deGasto = ordenadas.filter { it.tipo == TipoPartida.GASTO && it.clase != TipoGasto.AHORRO }
            val deIngreso = ordenadas.filter { it.tipo == TipoPartida.INGRESO }
            val idsGasto = deGasto.map { it.id }.toSet()
            val idsIngreso = deIngreso.map { it.id }.toSet()

            val realPorPartida = mutableMapOf<String, Monto>()
            fun sumar(id: String, monto: Monto) {
                realPorPartida[id] = (realPorPartida[id] ?: Monto.CERO) + monto
            }
            val destinoDe = mutableMapOf<String, String?>()
            var sinPartida = Monto.CERO
            gastos.forEach { gasto ->
                val destino = gasto.partidaId?.takeIf { it in idsGasto }
                    // "Otros" no dice nada: no se adivina su partida.
                    ?: deGasto.firstOrNull {
                        it.subcategoria != null && it.subcategoria != SubcategoriaGasto.OTROS && it.subcategoria == gasto.subcategoria
                    }?.id
                destinoDe[gasto.id] = destino
                if (destino == null) sinPartida += gasto.monto else sumar(destino, gasto.monto)
            }
            var sinFuente = Monto.CERO
            var deTuNegocio = Monto.CERO
            ingresos.forEach { ingreso ->
                val destino = ingreso.partidaId?.takeIf { it in idsIngreso }
                    ?: deIngreso.firstOrNull()?.id?.takeIf { ventasALaPrimeraFuente && !ingreso.retiroDelNegocio }
                when {
                    ingreso.retiroDelNegocio -> deTuNegocio += ingreso.monto
                    destino == null -> sinFuente += ingreso.monto
                    else -> sumar(destino, ingreso.monto)
                }
            }
            var separadoSinPartida = Monto.CERO
            separado.forEach { (meta, monto) ->
                val destino = deAhorro.firstOrNull { it.nombre.equals(meta, ignoreCase = true) } ?: deAhorro.firstOrNull()
                if (destino == null) separadoSinPartida += monto else sumar(destino.id, monto)
            }

            // La semana en curso, desde el lunes, solo si se está mirando el mes de hoy.
            val lunes = hoy?.takeIf { it.yearMonth == mes }?.let { it.minus(it.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY) }
            fun linea(partida: Partida): LineaPresupuesto {
                val monto = montos.montoPara(partida.id, mes)
                if (!partida.semanal) return LineaPresupuesto(partida, monto, realPorPartida[partida.id] ?: Monto.CERO)
                val semana = lunes?.let { desde ->
                    gastos.filter { destinoDe[it.id] == partida.id && it.fecha >= desde && it.fecha <= hoy }.map { it.monto }.sumar()
                }
                return LineaPresupuesto(
                    partida = partida,
                    presupuesto = Monto(monto.centimos * SEMANAS_POR_MES),
                    real = realPorPartida[partida.id] ?: Monto.CERO,
                    montoSemanal = monto,
                    realSemana = semana,
                )
            }
            val lineasGasto = listOf(TipoGasto.FIJO, TipoGasto.VARIABLE, TipoGasto.PRESCINDIBLE)
                .flatMap { tipo -> deGasto.filter { it.clase == tipo } }
                .map(::linea)
            return PresupuestoMes(
                mes = mes,
                gastos = lineasGasto,
                ingresos = deIngreso.map(::linea),
                gastoSinPartida = sinPartida,
                ingresoSinFuente = sinFuente,
                deTuNegocio = deTuNegocio,
                ahorro = deAhorro.map(::linea),
                separadoSinPartida = separadoSinPartida,
            )
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
    val tipoGasto: TipoGasto? = null,
    val semanal: Boolean = false,
    val deuda: Boolean = false,
)

private fun ingreso(nombre: String, marcado: Boolean = false) = ItemPlantilla(nombre, TipoPartida.INGRESO, null, marcado)

private fun gasto(
    nombre: String,
    tipo: TipoGasto,
    subcategoria: SubcategoriaGasto?,
    marcado: Boolean,
    semanal: Boolean = false,
    deuda: Boolean = false,
) = ItemPlantilla(nombre, TipoPartida.GASTO, subcategoria, marcado, tipo, semanal, deuda)

/**
 * Plantilla base para armar el presupuesto sin escribir, según el modo (documento 04 §14.3), en el orden
 * de la guía: lo que entra, el ahorro primero, los fijos, los variables y los prescindibles.
 */
fun plantillaPresupuesto(modo: Modo): List<ItemPlantilla> = when (modo) {
    Modo.PERSONAL -> listOf(
        ingreso("Sueldo"),
        ingreso("Comisiones"),
        ingreso("Clases o asesorías"),
        ingreso("Ganancia de inversiones"),
        ingreso("Otros ingresos"),
        gasto("Ahorro del mes", TipoGasto.AHORRO, null, marcado = true),
        gasto("Fondo de emergencia", TipoGasto.AHORRO, null, marcado = true),
        gasto("Inversión", TipoGasto.AHORRO, null, marcado = false),
        gasto("Alquiler o hipoteca", TipoGasto.FIJO, SubcategoriaGasto.CASA, marcado = true),
        gasto("Luz, agua, internet", TipoGasto.FIJO, SubcategoriaGasto.SERVICIOS, marcado = true),
        gasto("Pago de deudas", TipoGasto.FIJO, SubcategoriaGasto.OTROS, marcado = false, deuda = true),
        gasto("Seguros", TipoGasto.FIJO, SubcategoriaGasto.OTROS, marcado = false),
        gasto("Pensión escolar", TipoGasto.FIJO, SubcategoriaGasto.EDUCACION, marcado = false),
        gasto("Comida y mercado", TipoGasto.VARIABLE, SubcategoriaGasto.ALIMENTACION, marcado = true),
        gasto("Transporte", TipoGasto.VARIABLE, SubcategoriaGasto.TRANSPORTE, marcado = true),
        gasto("Salud", TipoGasto.VARIABLE, SubcategoriaGasto.SALUD, marcado = false),
        gasto("Educación", TipoGasto.VARIABLE, SubcategoriaGasto.EDUCACION, marcado = false),
        gasto("Salidas y restaurantes", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, marcado = false),
        gasto("Streaming y suscripciones", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, marcado = false),
        gasto("Ropa", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, marcado = false),
        gasto("Gastos menores", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, marcado = true, semanal = true),
    )
    Modo.NEGOCIO -> listOf(
        // Las ventas salen solas de lo que se registra; aquí solo se pone cuánto se espera vender.
        ingreso("Ventas", marcado = true),
        gasto("Colchón del negocio", TipoGasto.AHORRO, null, marcado = true),
        gasto("Alquiler del local", TipoGasto.FIJO, SubcategoriaGasto.ALQUILER, marcado = true),
        gasto("Luz, agua, internet", TipoGasto.FIJO, SubcategoriaGasto.SERVICIOS, marcado = true),
        gasto("Sueldos y pagos al personal", TipoGasto.FIJO, SubcategoriaGasto.PERSONAL_NEGOCIO, marcado = false),
        gasto("Cuota del Nuevo RUS", TipoGasto.FIJO, SubcategoriaGasto.OTROS, marcado = true),
        gasto("Cuota del préstamo", TipoGasto.FIJO, SubcategoriaGasto.OTROS, marcado = false, deuda = true),
        gasto("Mercadería e insumos", TipoGasto.VARIABLE, SubcategoriaGasto.MERCADERIA, marcado = true),
        gasto("Transporte", TipoGasto.VARIABLE, SubcategoriaGasto.TRANSPORTE, marcado = false),
        gasto("Otros", TipoGasto.PRESCINDIBLE, SubcategoriaGasto.OTROS, marcado = false),
    )
}

/**
 * RP-09: cuánto juntar para el fondo de emergencia (personal: [meses] de gastos fijos, 3 a 6) o para el
 * colchón del negocio (2 semanas de gastos fijos). Null si no hay gastos fijos presupuestados.
 */
fun PresupuestoMes.fondoSugerido(modo: Modo, meses: Int = 3): Monto? {
    val fijos = gastos.filter { it.partida.clase == TipoGasto.FIJO }.map { it.presupuesto }.sumar()
    if (fijos <= Monto.CERO) return null
    return when (modo) {
        Modo.PERSONAL -> Monto(fijos.centimos * meses)
        Modo.NEGOCIO -> Monto(fijos.centimos / 2)
    }
}
