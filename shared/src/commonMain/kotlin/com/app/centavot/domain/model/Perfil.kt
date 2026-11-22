package com.app.centavot.domain.model

enum class Rubro(val etiqueta: String) {
    COMERCIO("Comercio o bodega"),
    ALIMENTOS("Comida y restaurantes"),
    // Con ejemplos de negocios con local: "profesionales" atraía a quien da recibos por honorarios (4.ª categoría), fuera del alcance.
    SERVICIOS("Servicios (peluquería, lavandería)"),
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
    val modo: Modo = Modo.NEGOCIO,
    /** Celular en formato internacional (+51987654321), sin verificar. No sirve para entrar. */
    val celular: String? = null,
    /** Aceptó que el equipo de Centavot le escriba por WhatsApp (consentimiento propio, Ley 29733). */
    val aceptaWhatsapp: Boolean = false,
) {
    /** Lo que el usuario quiere separar cada mes según su tasa de ahorro. */
    val metaAhorro: Monto get() = Monto(ingresoMensual.centimos * tasaAhorro.decimas / TasaAhorro.MAXIMA)
}

/**
 * Celular al formato de wa.me (igual que el backend y la web): 987 654 321 → +51987654321.
 * Vacío → null. Lanza [IllegalArgumentException] si no es un número válido.
 */
fun normalizarCelular(texto: String?): String? {
    val limpio = texto.orEmpty().filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
    return when {
        limpio.isEmpty() -> null
        Regex("^9\\d{8}$").matches(limpio) -> "+51$limpio"
        Regex("^519\\d{8}$").matches(limpio) -> "+$limpio"
        Regex("^\\+[1-9]\\d{7,14}$").matches(limpio) -> limpio
        else -> throw IllegalArgumentException("Celular no válido")
    }
}
