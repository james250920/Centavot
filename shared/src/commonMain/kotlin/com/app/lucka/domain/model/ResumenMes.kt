package com.app.lucka.domain.model

data class ResumenMes(
    val totalNegocio: Monto,
    val totalPersonal: Monto,
    val cantidadGastos: Int,
    /** Total por subcategoría, de mayor a menor; la clave null agrupa los gastos sin subcategoría. */
    val porSubcategoria: List<Pair<SubcategoriaGasto?, Monto>> = emptyList(),
) {
    val total: Monto get() = totalNegocio + totalPersonal
}
