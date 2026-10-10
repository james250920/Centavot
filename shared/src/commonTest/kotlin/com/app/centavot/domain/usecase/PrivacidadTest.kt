package com.app.centavot.domain.usecase

import com.app.centavot.core.util.CompartidorArchivos
import com.app.centavot.core.util.Reloj
import com.app.centavot.domain.model.Categoria
import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EventoUso
import com.app.centavot.domain.model.Ingreso
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.PeriodoTope
import com.app.centavot.domain.model.Perfil
import com.app.centavot.domain.model.RegimenTributario
import com.app.centavot.domain.model.Rubro
import com.app.centavot.domain.model.TasaAhorro
import com.app.centavot.domain.model.TipoCobro
import com.app.centavot.domain.model.TipoEventoUso
import com.app.centavot.domain.model.TipoRegimen
import com.app.centavot.domain.model.VERSION_AVISO_PRIVACIDAD
import com.app.centavot.domain.repository.DatosRepository
import com.app.centavot.domain.repository.PerfilRepository
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeCobroRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeRegimenRepository
import com.app.centavot.fakes.FakeUsoRepository
import com.app.centavot.presentation.EstadoApp
import com.app.centavot.presentation.estadoApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P1 (aviso, consentimiento y borrado) y P3 (exportar todos los datos). */
class PrivacidadTest {

    private val ahora = LocalDateTime(2026, 10, 9, 10, 30)
    private val reloj = object : Reloj {
        override fun hoy() = ahora.date
        override fun ahora() = ahora
    }
    private val regimen = RegimenTributario(TipoRegimen.RUS, Monto.soles(8_000), PeriodoTope.MENSUAL, "RUS · Categoría 2")
    private val perfil = Perfil("Rosa Quispe", Rubro.COMERCIO, Monto.CERO, TasaAhorro(100))

    @Test
    fun sinConsentimientoLaAppMuestraPrimeroElAviso() {
        assertEquals(EstadoApp.SIN_CONSENTIMIENTO, estadoApp(consentido = false, perfil = perfil, regimen = regimen))
        assertEquals(EstadoApp.SIN_PERFIL, estadoApp(consentido = true, perfil = null, regimen = null))
        assertEquals(EstadoApp.SIN_REGIMEN, estadoApp(consentido = true, perfil = perfil, regimen = null))
        assertEquals(EstadoApp.LISTA, estadoApp(consentido = true, perfil = perfil, regimen = regimen))
    }

    @Test
    fun aceptarGuardaLaVersionVigenteDelAviso() = runTest {
        val uso = FakeUsoRepository()
        val observar = ObservarConsentimientoUseCase(uso)
        assertFalse(observar().first())

        AceptarAvisoPrivacidadUseCase(uso, reloj)()

        assertTrue(observar().first())
        val evento = uso.eventos.value.single()
        assertEquals(TipoEventoUso.CONSENTIMIENTO_PRIVACIDAD, evento.tipo)
        assertEquals(VERSION_AVISO_PRIVACIDAD, evento.valor)
        assertEquals(ahora, evento.fechaHora)
    }

    @Test
    fun unConsentimientoDeOtraVersionNoBasta() = runTest {
        val uso = FakeUsoRepository()
        uso.registrar(EventoUso(TipoEventoUso.CONSENTIMIENTO_PRIVACIDAD, ahora, "2020-01"))

        assertFalse(ObservarConsentimientoUseCase(uso)().first())
    }

    @Test
    fun borrarTodoBorraLosDatosYLosArchivosExportados() = runTest {
        var datosBorrados = false
        var archivosBorrados = false
        val compartidor = object : CompartidorArchivos {
            override fun compartir(nombreArchivo: String, contenido: String, tipoMime: String) = Unit
            override fun borrarArchivos() {
                archivosBorrados = true
            }
        }

        BorrarTodosLosDatosUseCase(DatosRepository { datosBorrados = true }, compartidor)()

        assertTrue(datosBorrados)
        assertTrue(archivosBorrados)
    }

    @Test
    fun exportarTodoIncluyeCadaSeccionConSusDatos() = runTest {
        val juana = Contacto("c1", "Señora Juana", "987654321")
        val cobros = FakeCobroRepository().apply {
            contactos.value = listOf(juana)
            this.cobros.value = listOf(
                Cobro("k1", juana, "Abarrotes", Monto.soles(85), LocalDate(2026, 10, 3), tipo = TipoCobro.FIADO, abonado = Monto.soles(30)),
            )
        }
        val actividades = FakeActividadRepository().apply { registrar("Registraste una venta \"Pan\" de S/ 5.00.", ahora) }
        val exportar = ExportarTodosLosDatosUseCase(
            perfiles = object : PerfilRepository {
                override fun observarPerfil(): Flow<Perfil?> = MutableStateFlow(perfil)
                override suspend fun guardar(perfil: Perfil) = Unit
            },
            regimenes = FakeRegimenRepository(regimen),
            ingresos = FakeIngresoRepository(listOf(Ingreso("i1", Monto(500), ahora.date, Categoria.NEGOCIO, "Pan"))),
            gastos = FakeGastoRepository(),
            cobros = cobros,
            actividades = actividades,
            reloj = reloj,
        )

        val archivo = exportar()
        val lineas = archivo.contenido.removePrefix("﻿").lines()

        assertEquals("lucka-mis-datos-2026-10-09.csv", archivo.nombre)
        assertTrue("Rosa Quispe,Comercio o bodega,0.00,10.0,RUS · Categoría 2,8000.00" in lineas)
        assertTrue("2026-10-09,Venta,Pan,5.00" in lineas)
        assertTrue("Señora Juana,987654321" in lineas)
        assertTrue("2026-10-03,Señora Juana,Fiado o préstamo,Abarrotes,85.00,0.00,30.00,55.00,Pendiente," in lineas)
        listOf("PERFIL", "VENTAS E INGRESOS (1)", "GASTOS (0)", "CONTACTOS (1)", "COBROS (1)", "HISTORIAL DE ACTIVIDAD (1)").forEach {
            assertTrue(it in lineas, "Falta la sección $it")
        }
    }
}
