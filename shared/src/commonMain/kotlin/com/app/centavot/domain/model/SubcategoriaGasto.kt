package com.app.centavot.domain.model

/** En qué se fue el dinero. Sirve para el gráfico de distribución y el reporte. */
enum class SubcategoriaGasto(val etiqueta: String) {
    MERCADERIA("Mercadería e insumos"),
    ALQUILER("Alquiler"),
    TRANSPORTE("Transporte"),
    SERVICIOS("Luz, agua, internet"),
    ALIMENTACION("Comida"),
    CASA("Casa y familia"),
    SALUD("Salud"),
    EDUCACION("Educación"),
    OTROS("Otros"),
}
