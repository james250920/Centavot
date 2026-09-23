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
