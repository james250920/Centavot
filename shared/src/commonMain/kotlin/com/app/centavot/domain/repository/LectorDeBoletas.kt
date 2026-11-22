package com.app.centavot.domain.repository

import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.SubcategoriaGasto
import kotlinx.datetime.LocalDate

/** Lee la foto de una boleta, factura o constancia de Yape/Plin (OCR en el backend) y devuelve un borrador para revisar. */
fun interface LectorDeBoletas {
    suspend fun leer(jpeg: ByteArray): LecturaBoleta
}

sealed interface LecturaBoleta {
    /** Lo que se pudo leer (cualquier campo puede faltar) y qué debe revisar la persona. */
    data class Leida(
        val monto: Monto?,
        val fecha: LocalDate?,
        val subcategoria: SubcategoriaGasto?,
        val descripcion: String?,
        val revisar: List<String>,
        /** true si le pagaron ("Te yapearon"): va como venta o ingreso; false si pagó: va como gasto. */
        val esIngreso: Boolean = false,
    ) : LecturaBoleta

    data class Error(val mensaje: String) : LecturaBoleta
}
