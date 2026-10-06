package com.app.centavot.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.minusMonth
import kotlinx.datetime.yearMonth

/** Periodos del "cierre de caja": el comerciante quiere ver el día, la semana y el mes. */
enum class Periodo(val etiqueta: String) {
    HOY("Hoy"),
    SEMANA("Esta semana"),
    MES("Este mes"),
}

/** Desde el inicio del periodo (la semana empieza el lunes) hasta [hoy]. */
fun Periodo.rango(hoy: LocalDate): ClosedRange<LocalDate> = when (this) {
    Periodo.HOY -> hoy..hoy
    Periodo.SEMANA -> hoy.minus(hoy.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)..hoy
    Periodo.MES -> hoy.yearMonth.firstDay..hoy
}

/** Cuánto vendió, gastó y ganó el negocio en un periodo. */
data class ResumenPeriodo(
    val ventas: Monto,
    val ingresosPersonales: Monto,
    val gastosNegocio: Monto,
    val gastosPersonales: Monto,
    val cantidadMovimientos: Int,
) {
    /** Ganancia del negocio: lo vendido menos lo gastado en el negocio. Lo personal no entra. */
    val ganancia: Monto get() = ventas - gastosNegocio

    companion object {
        val VACIO = ResumenPeriodo(Monto.CERO, Monto.CERO, Monto.CERO, Monto.CERO, 0)

        fun de(ingresos: List<Ingreso>, gastos: List<Gasto>) = ResumenPeriodo(
            ventas = ingresos.filter { it.esDeNegocio }.map { it.monto }.sumar(),
            ingresosPersonales = ingresos.filterNot { it.esDeNegocio }.map { it.monto }.sumar(),
            gastosNegocio = gastos.filter { it.categoria == Categoria.NEGOCIO }.map { it.monto }.sumar(),
            gastosPersonales = gastos.filter { it.categoria == Categoria.PERSONAL }.map { it.monto }.sumar(),
            cantidadMovimientos = ingresos.size + gastos.size,
        )
    }
}

data class MesHistorial(val mes: YearMonth, val ventas: Monto, val gastosNegocio: Monto) {
    val ganancia: Monto get() = ventas - gastosNegocio
}

/**
 * Ventas y gastos de negocio mes a mes, del más antiguo al más reciente. Sirve para el gráfico
 * de líneas y para que el comerciante vea en números lo que ya intuye (meses buenos y flojos).
 */
data class Historial(val meses: List<MesHistorial>) {
    private val conVentas get() = meses.filter { it.ventas > Monto.CERO }

    val hayDatos: Boolean get() = meses.any { it.ventas > Monto.CERO || it.gastosNegocio > Monto.CERO }

    /** Mes con más ventas; null si ningún mes tiene ventas. */
    val mejorMes: MesHistorial? get() = conVentas.maxByOrNull { it.ventas }

    /** Mes con menos ventas; solo tiene sentido con al menos dos meses con ventas. */
    val peorMes: MesHistorial? get() = conVentas.takeIf { it.size >= 2 }?.minByOrNull { it.ventas }

    /** Cuánto subieron (o bajaron) las ventas del último mes frente al anterior, en %; null sin base. */
    val variacionUltimoMes: Int?
        get() {
            val actual = meses.lastOrNull() ?: return null
            val anterior = meses.getOrNull(meses.size - 2)?.takeIf { it.ventas > Monto.CERO } ?: return null
            return ((actual.ventas.centimos - anterior.ventas.centimos) * 100 / anterior.ventas.centimos).toInt()
        }

    companion object {
        fun de(hasta: YearMonth, cantidadMeses: Int, ingresos: List<Ingreso>, gastos: List<Gasto>): Historial {
            val meses = generateSequence(hasta) { it.minusMonth() }.take(cantidadMeses).toList().reversed()
            return Historial(
                meses.map { mes ->
                    MesHistorial(
                        mes = mes,
                        ventas = ingresos.filter { it.esDeNegocio && it.fecha.yearMonth == mes }.map { it.monto }.sumar(),
                        gastosNegocio = gastos.filter { it.esDeNegocio && it.fecha.yearMonth == mes }.map { it.monto }.sumar(),
                    )
                },
            )
        }
    }
}
