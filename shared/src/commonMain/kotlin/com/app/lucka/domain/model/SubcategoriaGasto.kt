package com.app.lucka.domain.model

/** En qué se fue el dinero. Sirve para el gráfico de distribución y el reporte. */
enum class SubcategoriaGasto(val etiqueta: String) {
    MERCADERIA("Mercadería e insumos"),
    ALQUILER("Alquiler"),
    TRANSPORTE("Transporte"),
    SERVICIOS("Luz, agua, internet"),
    PERSONAL_NEGOCIO("Sueldos y pagos al personal"),
    ALIMENTACION("Comida"),
    CASA("Casa y familia"),
    SALUD("Salud"),
    EDUCACION("Educación"),
    OTROS("Otros"),
    ;

    companion object
}

private val SOLO_NEGOCIO = setOf(SubcategoriaGasto.MERCADERIA, SubcategoriaGasto.ALQUILER, SubcategoriaGasto.PERSONAL_NEGOCIO)
private val SOLO_PERSONAL = setOf(
    SubcategoriaGasto.ALIMENTACION,
    SubcategoriaGasto.CASA,
    SubcategoriaGasto.SALUD,
    SubcategoriaGasto.EDUCACION,
)

/** Subcategorías que tienen sentido para un gasto de negocio o personal; sin categoría, todas. */
fun SubcategoriaGasto.Companion.para(categoria: Categoria?): List<SubcategoriaGasto> = when (categoria) {
    Categoria.NEGOCIO -> SubcategoriaGasto.entries.filterNot { it in SOLO_PERSONAL }
    Categoria.PERSONAL -> SubcategoriaGasto.entries.filterNot { it in SOLO_NEGOCIO }
    null -> SubcategoriaGasto.entries
}

fun SubcategoriaGasto.aplicaA(categoria: Categoria): Boolean = this in SubcategoriaGasto.para(categoria)
