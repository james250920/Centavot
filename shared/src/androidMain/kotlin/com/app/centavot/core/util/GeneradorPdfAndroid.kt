package com.app.centavot.core.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.app.centavot.domain.model.BloqueReporte
import com.app.centavot.domain.model.DocumentoReporte
import java.io.ByteArrayOutputStream

/** Dibuja el resumen del contador en hojas A4 con `PdfDocument`, que ya trae Android: no suma librerías. */
class GeneradorPdfAndroid : GeneradorPdf {
    override fun generar(documento: DocumentoReporte): ByteArray {
        val pdf = PdfDocument()
        try {
            Hojas(pdf, documento.pie).apply {
                encabezado(documento.titulo, documento.subtitulo)
                documento.bloques.forEach { bloque ->
                    when (bloque) {
                        is BloqueReporte.Totales -> totales(bloque)
                        is BloqueReporte.Tabla -> tabla(bloque)
                    }
                }
                terminar()
            }
            return ByteArrayOutputStream().also { pdf.writeTo(it) }.toByteArray()
        } finally {
            pdf.close()
        }
    }
}

// A4 en puntos (1/72 de pulgada).
private const val ANCHO = 595
private const val ALTO = 842
private const val MARGEN = 40f
private const val ALTO_FILA = 18f
private const val ESPACIO_PIE = 56f

private val VERDE = Color.rgb(0x0B, 0x6E, 0x4F)
private val GRIS = Color.rgb(0x5A, 0x66, 0x60)
private val LINEA = Color.rgb(0xD6, 0xE1, 0xDA)
private val FONDO_ENCABEZADO = Color.rgb(0xE8, 0xF3, 0xED)

private fun pincel(tamano: Float, color: Int = Color.BLACK, negrita: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = tamano
    this.color = color
    typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
}

/** Lleva la página actual, la posición vertical y el salto de página. */
private class Hojas(private val pdf: PdfDocument, private val pie: String) {
    private var numero = 0
    private var pagina: PdfDocument.Page? = null
    private lateinit var lienzo: Canvas
    private var y = 0f
    private val anchoUtil = ANCHO - 2 * MARGEN
    private val limite = ALTO - MARGEN - ESPACIO_PIE

    private val titulo = pincel(20f, VERDE, negrita = true)
    private val subtitulo = pincel(12f, GRIS)
    private val seccion = pincel(13f, VERDE, negrita = true)
    private val texto = pincel(10f)
    private val textoNegrita = pincel(10f, negrita = true)
    private val encabezadoTabla = pincel(9f, GRIS, negrita = true)
    private val chico = pincel(8f, GRIS)
    private val raya = Paint().apply { color = LINEA; strokeWidth = 0.8f }
    private val fondo = Paint().apply { color = FONDO_ENCABEZADO }

    init {
        nuevaPagina()
    }

    fun encabezado(textoTitulo: String, textoSubtitulo: String) {
        y += 20f
        lienzo.drawText(textoTitulo, MARGEN, y, titulo)
        y += 18f
        lienzo.drawText(textoSubtitulo, MARGEN, y, subtitulo)
        y += 10f
        lienzo.drawLine(MARGEN, y, ANCHO - MARGEN, y, Paint().apply { color = VERDE; strokeWidth = 1.5f })
        y += 14f
    }

    fun totales(bloque: BloqueReporte.Totales) {
        asegurar(28f + bloque.filas.size * (ALTO_FILA + 2))
        tituloSeccion(bloque.titulo)
        bloque.filas.forEach { fila ->
            val pincelFila = if (fila.destacada) pincel(12f, negrita = true) else pincel(11f)
            y += ALTO_FILA + 2
            lienzo.drawText(fila.etiqueta, MARGEN, y, pincelFila)
            lienzo.drawText(fila.valor, ANCHO - MARGEN - pincelFila.measureText(fila.valor), y, pincelFila)
            if (fila.destacada) lienzo.drawLine(MARGEN, y - ALTO_FILA + 2, ANCHO - MARGEN, y - ALTO_FILA + 2, raya)
        }
        y += 18f
    }

    fun tabla(bloque: BloqueReporte.Tabla) {
        val pesoTotal = bloque.columnas.sumOf { it.peso.toDouble() }.toFloat()
        val anchos = bloque.columnas.map { anchoUtil * it.peso / pesoTotal }
        // El título no se queda solo al final de una hoja.
        asegurar(28f + ALTO_FILA * 3)
        tituloSeccion(bloque.titulo)
        encabezadoDeTabla(bloque, anchos)
        bloque.filas.forEach { celdas ->
            if (y + ALTO_FILA > limite) {
                nuevaPagina()
                encabezadoDeTabla(bloque, anchos)
            }
            fila(bloque, anchos, celdas, texto)
        }
        bloque.total?.let { total ->
            if (y + ALTO_FILA > limite) nuevaPagina()
            lienzo.drawLine(MARGEN, y + 4, ANCHO - MARGEN, y + 4, Paint().apply { color = GRIS; strokeWidth = 1f })
            fila(bloque, anchos, total, textoNegrita)
        }
        y += 22f
    }

    fun terminar() {
        cerrarPagina()
    }

    private fun tituloSeccion(textoSeccion: String) {
        y += 16f
        lienzo.drawText(textoSeccion, MARGEN, y, seccion)
        y += 6f
    }

    private fun encabezadoDeTabla(bloque: BloqueReporte.Tabla, anchos: List<Float>) {
        lienzo.drawRect(MARGEN, y + 3, ANCHO - MARGEN, y + ALTO_FILA + 3, fondo)
        fila(bloque, anchos, bloque.columnas.map { it.titulo.uppercase() }, encabezadoTabla, conRaya = false)
    }

    private fun fila(bloque: BloqueReporte.Tabla, anchos: List<Float>, celdas: List<String>, pincelFila: Paint, conRaya: Boolean = true) {
        y += ALTO_FILA
        var x = MARGEN
        celdas.forEachIndexed { i, celda ->
            val ancho = anchos[i]
            val cabe = recortar(celda, ancho - 8f, pincelFila)
            val columna = bloque.columnas[i]
            val xTexto = if (columna.alDerecha) x + ancho - 4f - pincelFila.measureText(cabe) else x + 4f
            lienzo.drawText(cabe, xTexto, y - 1, pincelFila)
            x += ancho
        }
        if (conRaya) lienzo.drawLine(MARGEN, y + 4, ANCHO - MARGEN, y + 4, raya)
    }

    private fun asegurar(alto: Float) {
        if (y + alto > limite) nuevaPagina()
    }

    private fun nuevaPagina() {
        cerrarPagina()
        numero += 1
        val nueva = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
        pagina = nueva
        lienzo = nueva.canvas
        y = MARGEN
    }

    /** Cada hoja lleva el aviso de SUNAT y su número abajo. */
    private fun cerrarPagina() {
        val actual = pagina ?: return
        var yPie = ALTO - MARGEN - ESPACIO_PIE + 22f
        lienzo.drawLine(MARGEN, yPie - 12f, ANCHO - MARGEN, yPie - 12f, raya)
        envolver(pie, anchoUtil, chico).forEach { linea ->
            lienzo.drawText(linea, MARGEN, yPie, chico)
            yPie += 11f
        }
        val numeroPagina = "Página $numero"
        lienzo.drawText(numeroPagina, ANCHO - MARGEN - chico.measureText(numeroPagina), ALTO - MARGEN, chico)
        pdf.finishPage(actual)
        pagina = null
    }
}

/** Corta el texto con "…" si no entra en la celda. */
private fun recortar(texto: String, ancho: Float, pincel: Paint): String {
    if (pincel.measureText(texto) <= ancho) return texto
    var fin = texto.length
    while (fin > 0 && pincel.measureText(texto.substring(0, fin) + "…") > ancho) fin--
    return texto.substring(0, fin).trimEnd() + "…"
}

/** Parte un texto largo en líneas que entran en [ancho]. */
private fun envolver(texto: String, ancho: Float, pincel: Paint): List<String> {
    val lineas = mutableListOf<String>()
    var actual = ""
    texto.split(" ").forEach { palabra ->
        val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
        if (pincel.measureText(prueba) <= ancho) {
            actual = prueba
        } else {
            lineas += actual
            actual = palabra
        }
    }
    if (actual.isNotEmpty()) lineas += actual
    return lineas
}
