package com.app.centavot.domain.model

enum class NivelAlerta(val umbralPorcentaje: Int) {
    NINGUNA(0),
    AVISO_80(80),
    AVISO_90(90),
    TOPE_ALCANZADO(100),
}

/** Cuánto le falta al usuario para llegar al tope de su régimen. */
data class ProximidadTope(
    val acumulado: Monto,
    val tope: Monto,
) {
    init {
        require(tope > Monto.CERO) { "El tope debe ser mayor que cero" }
    }

    val porcentaje: Int get() = (acumulado.centimos * 100 / tope.centimos).toInt()

    val restante: Monto get() = maxOf(tope - acumulado, Monto.CERO)

    val nivelAlerta: NivelAlerta
        get() = NivelAlerta.entries.last { porcentaje >= it.umbralPorcentaje }
}

/** Qué se compara con el tope del régimen. */
enum class MedidaTope(val etiqueta: String) {
    VENTAS("ventas"),
    COMPRAS("compras"),
}

/**
 * Situación del negocio frente al tope de su régimen. En el Nuevo RUS se vigilan las ventas
 * y las compras del mes; en el RER, las ventas del año.
 */
data class EstadoTope(
    val regimen: RegimenTributario,
    val ventas: ProximidadTope,
    /** null si el régimen no pone tope a las compras. */
    val compras: ProximidadTope? = null,
) {
    val medidas: List<Pair<MedidaTope, ProximidadTope>>
        get() = listOfNotNull(MedidaTope.VENTAS to ventas, compras?.let { MedidaTope.COMPRAS to it })

    /** La medida que está más cerca del tope: es la que decide el aviso. */
    val principal: Pair<MedidaTope, ProximidadTope> get() = medidas.maxBy { it.second.porcentaje }

    val nivelAlerta: NivelAlerta get() = principal.second.nivelAlerta
}
