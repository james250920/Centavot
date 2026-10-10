package com.app.lucka.domain.model

enum class Rubro(val etiqueta: String) {
    COMERCIO("Comercio o bodega"),
    ALIMENTOS("Comida y restaurantes"),
    SERVICIOS("Servicios profesionales"),
    OFICIO("Oficio técnico"),
    TRANSPORTE("Transporte"),
    OTRO("Otro"),
}

/**
 * Tasa de ahorro en décimas de porcentaje (125 = 12.5 %) para no usar Double.
 */
@kotlin.jvm.JvmInline
value class TasaAhorro(val decimas: Int) {
    init {
        require(decimas in 0..MAXIMA) { "La tasa de ahorro debe estar entre 0 y 100 %" }
    }

    companion object {
        const val MAXIMA = 1_000
        val CERO = TasaAhorro(0)
    }
}

/** Datos del usuario de este teléfono. */
data class Perfil(
    val nombre: String,
    val rubro: Rubro?,
    val ingresoMensual: Monto = Monto.CERO,
    val tasaAhorro: TasaAhorro = TasaAhorro.CERO,
) {
    /** Lo que el usuario quiere separar cada mes según su tasa de ahorro. */
    val metaAhorro: Monto get() = Monto(ingresoMensual.centimos * tasaAhorro.decimas / TasaAhorro.MAXIMA)
}
