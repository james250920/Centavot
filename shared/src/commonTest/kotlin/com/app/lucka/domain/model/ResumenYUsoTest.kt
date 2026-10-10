package com.app.lucka.domain.model

import com.app.lucka.core.util.codificarParaUrl
import com.app.lucka.core.util.enlaceWhatsApp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResumenYUsoTest {

    // Lunes 5 de octubre de 2026.
    private val hoy = LocalDate(2026, 10, 5)

    private fun gasto(soles: Long, fecha: LocalDate, categoria: Categoria) =
        Gasto("g-$soles-$fecha", Monto.soles(soles), fecha, OrigenGasto.TEXTO, EstadoGasto.CONFIRMADO, categoria)

    @Test
    fun laSemanaEmpiezaElLunesYElMesElDiaUno() {
        val domingo = LocalDate(2026, 10, 11)
        assertEquals(LocalDate(2026, 10, 5)..domingo, Periodo.SEMANA.rango(domingo))
        assertEquals(LocalDate(2026, 10, 1)..hoy, Periodo.MES.rango(hoy))
        assertEquals(hoy..hoy, Periodo.HOY.rango(hoy))
    }

    @Test
    fun gananciaEsVentasMenosGastosDelNegocio() {
        val resumen = ResumenPeriodo.de(
            ingresos = listOf(Ingreso("1", Monto.soles(400), hoy), Ingreso("2", Monto.soles(1_000), hoy, Categoria.PERSONAL)),
            gastos = listOf(gasto(150, hoy, Categoria.NEGOCIO), gasto(80, hoy, Categoria.PERSONAL)),
        )

        assertEquals(Monto.soles(400), resumen.ventas)
        assertEquals(Monto.soles(250), resumen.ganancia)
        assertEquals(Monto.soles(80), resumen.gastosPersonales)
        assertEquals(Monto.soles(1_000), resumen.ingresosPersonales)
        assertEquals(4, resumen.cantidadMovimientos)
    }

    @Test
    fun historialEncuentraMejorYPeorMesYLaVariacion() {
        val ingresos = listOf(
            Ingreso("ago", Monto.soles(1_000), LocalDate(2026, 8, 10)),
            Ingreso("sep", Monto.soles(2_000), LocalDate(2026, 9, 10)),
            Ingreso("oct", Monto.soles(1_500), LocalDate(2026, 10, 2)),
        )
        val historial = Historial.de(YearMonth(2026, 10), 6, ingresos, emptyList())

        assertEquals(6, historial.meses.size)
        assertEquals(YearMonth(2026, 5), historial.meses.first().mes)
        assertEquals(YearMonth(2026, 9), historial.mejorMes!!.mes)
        assertEquals(YearMonth(2026, 8), historial.peorMes!!.mes)
        assertEquals(-25, historial.variacionUltimoMes)
    }

    @Test
    fun historialSinVentasNoInventaLecturas() {
        val historial = Historial.de(YearMonth(2026, 10), 6, emptyList(), emptyList())

        assertFalse(historial.hayDatos)
        assertNull(historial.mejorMes)
        assertNull(historial.variacionUltimoMes)
    }

    @Test
    fun resumenDeUsoCuentaDiasConRegistros() {
        fun evento(tipo: TipoEventoUso, dia: Int) = EventoUso(tipo, LocalDateTime(2026, 10, dia, 9, 0))
        val eventos = listOf(
            evento(TipoEventoUso.APERTURA, 1),
            evento(TipoEventoUso.REGISTRO, 1),
            evento(TipoEventoUso.REGISTRO, 1),
            evento(TipoEventoUso.REGISTRO, 3),
            evento(TipoEventoUso.APERTURA, 5),
            evento(TipoEventoUso.APERTURA, 5),
            evento(TipoEventoUso.REGISTRO, 5),
        )

        val uso = ResumenUso.de(eventos, hoy)

        assertEquals(2, uso.aperturasHoy)
        assertEquals(1, uso.registrosHoy)
        assertEquals(3, uso.diasConRegistroUltimos7)
        assertEquals(1, uso.registrosPorDiaActivo)
        assertEquals(4, uso.diasDesdePrimerUso)
        assertFalse(uso.debePreguntarCuaderno)
    }

    @Test
    fun preguntaPorElCuadernoALos7YA30Dias() {
        val inicio = EventoUso(TipoEventoUso.APERTURA, LocalDateTime(2026, 9, 1, 8, 0))
        val respuesta = EventoUso(TipoEventoUso.ENCUESTA_CUADERNO, LocalDateTime(2026, 9, 8, 8, 0), ResumenUso.RESPUESTA_SI)

        assertTrue(ResumenUso.de(listOf(inicio), LocalDate(2026, 9, 8)).debePreguntarCuaderno)
        assertFalse(ResumenUso.de(listOf(inicio, respuesta), LocalDate(2026, 9, 20)).debePreguntarCuaderno)
        assertTrue(ResumenUso.de(listOf(inicio, respuesta), LocalDate(2026, 10, 1)).debePreguntarCuaderno)
        assertEquals(listOf(true), ResumenUso.de(listOf(inicio, respuesta), hoy).respuestasCuaderno)
    }

    @Test
    fun pedidoConAdelantoDebeSoloElSaldo() {
        val pedido = Cobro(
            "p1", Contacto("c1", "Lucho", "987654321"), "Arreglo de zapatos", Monto.soles(60), hoy,
            tipo = TipoCobro.PEDIDO, adelanto = Monto.soles(20),
        )

        assertEquals(Monto.soles(40), pedido.saldo)
        assertEquals(Monto.soles(40), ResumenCobros.de(listOf(pedido)).totalPendiente)
        assertEquals(
            "Hola Lucho, te escribo para recordarte que tu pedido \"Arreglo de zapatos\" tiene un saldo de S/ 40.00. Saludos, Rosa.",
            pedido.mensajeRecordatorio("Rosa"),
        )
    }

    @Test
    fun enlaceDeWhatsAppAgregaElCodigoDePeru() {
        assertEquals("https://wa.me/51987654321?text=Hola%20t%C3%BA", enlaceWhatsApp("987 654 321", "Hola tú"))
        assertEquals("https://wa.me/51987654321?text=x", enlaceWhatsApp("+51 987654321", "x"))
        assertNull(enlaceWhatsApp("12345", "x"))
        assertEquals("S%2F%2040.00", codificarParaUrl("S/ 40.00"))
    }

    @Test
    fun csvDeMeDebenSoloLlevaPendientes() {
        val rosa = Contacto("c1", "Rosa")
        val csv = reporteMeDebenCsv(
            listOf(
                Cobro("1", rosa, "Fiado", Monto.soles(30), hoy),
                Cobro("2", rosa, "Pagado", Monto.soles(10), hoy, estado = EstadoCobro.COBRADO),
            ),
        ).removePrefix("﻿").lines()

        assertEquals("2026-10-05,Rosa,Fiado o préstamo,Fiado,30.00,0.00,0.00,30.00", csv[3])
        assertEquals(",,,,,,Total,30.00", csv[4])
    }

    @Test
    fun ventasFrecuentesNoDistinguenMayusculas() {
        val ingresos = listOf(
            Ingreso("3", Monto(350), hoy, descripcion = "Gaseosa"),
            Ingreso("2", Monto(350), hoy, descripcion = " gaseosa "),
            Ingreso("1", Monto(350), hoy, descripcion = "GASEOSA"),
            Ingreso("0", Monto(1_000), hoy, descripcion = "Menú"),
        )

        assertEquals(
            listOf(VentaFrecuente("Gaseosa", Monto(350)), VentaFrecuente("Menú", Monto(1_000))),
            ingresos.ventasFrecuentes(),
        )
    }

    @Test
    fun elSaldoDescuentaAdelantoYAbonos() {
        val cobro = Cobro(
            "p1", Contacto("c1", "Lucho"), "Zapatos", Monto.soles(60), hoy,
            tipo = TipoCobro.PEDIDO, adelanto = Monto.soles(20), abonado = Monto.soles(15),
        )

        assertEquals(Monto.soles(35), cobro.pagado)
        assertEquals(Monto.soles(25), cobro.saldo)
    }
}
