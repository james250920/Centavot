package com.app.centavot.core.util

import com.app.centavot.domain.model.DocumentoReporte

/** Dibuja un [DocumentoReporte] como PDF. Lo implementa cada plataforma (en Android, con PdfDocument). */
fun interface GeneradorPdf {
    fun generar(documento: DocumentoReporte): ByteArray
}
