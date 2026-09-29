package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.Cobro
import com.app.centavot.domain.model.Contacto
import com.app.centavot.domain.model.EstadoCobro
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.ResumenCobros
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeCobroRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CobroUseCasesTest {

    private val hoy = LocalDate(2026, 9, 29)
    private val repositorio = FakeCobroRepository()
    private val actividades = FakeActividadRepository()
    private var siguienteId = 0
    private val generarId = { "id-${++siguienteId}" }

    private val agregarContacto = AgregarContactoUseCase(repositorio, actividades, { hoy }, generarId)
    private val registrar = RegistrarCobroUseCase(repositorio, actividades, { hoy }, generarId)
    private val marcarCobrado = MarcarCobradoUseCase(repositorio, actividades) { hoy }
    private val eliminarCobro = EliminarCobroUseCase(repositorio, actividades) { hoy }
    private val eliminarContacto = EliminarContactoUseCase(repositorio, actividades) { hoy }

    private val rosa = Contacto("rosa", "Rosa")

    @Test
    fun agregaContactoSinRepetirNombre() = runTest {
        assertIs<AgregarContactoUseCase.Resultado.Agregado>(agregarContacto("  Rosa  ", " "))
        assertEquals(AgregarContactoUseCase.Resultado.Repetido, agregarContacto("rosa", null))
        assertEquals(AgregarContactoUseCase.Resultado.NombreVacio, agregarContacto("   ", null))

        val contacto = repositorio.contactos.value.single()
        assertEquals("Rosa", contacto.nombre)
        assertEquals(null, contacto.telefono)
    }

    @Test
    fun registraYCobraDejandoRastroEnActividad() = runTest {
        val cobro = assertIs<RegistrarCobroUseCase.Resultado.Registrado>(
            registrar(rosa, " Fiado de arroz ", Monto.soles(12), hoy),
        ).cobro

        marcarCobrado(cobro.id)

        val guardado = repositorio.cobros.value.single()
        assertEquals(EstadoCobro.COBRADO, guardado.estado)
        assertEquals(hoy, guardado.fechaCobrado)
        assertEquals(
            listOf(
                "Rosa te pagó S/ 12.00 (\"Fiado de arroz\").",
                "Registraste un cobro a Rosa por S/ 12.00 (\"Fiado de arroz\").",
            ),
            actividades.actividades.value.sortedByDescending { it.id }.map { it.descripcion },
        )
    }

    @Test
    fun validaElCobro() = runTest {
        assertEquals(RegistrarCobroUseCase.Resultado.MotivoVacio, registrar(rosa, " ", Monto.soles(1), hoy))
        assertEquals(RegistrarCobroUseCase.Resultado.MontoInvalido, registrar(rosa, "Pan", Monto.CERO, hoy))
        assertEquals(RegistrarCobroUseCase.Resultado.FechaFutura, registrar(rosa, "Pan", Monto.soles(1), LocalDate(2026, 9, 30)))
        assertTrue(repositorio.cobros.value.isEmpty())
    }

    @Test
    fun eliminarUnCobroQuedaAnotado() = runTest {
        repositorio.cobros.value = listOf(Cobro("c1", rosa, "Préstamo", Monto.soles(50), hoy))

        eliminarCobro("c1")

        assertTrue(repositorio.cobros.value.isEmpty())
        assertEquals("Eliminaste el cobro \"Préstamo\" a Rosa de S/ 50.00.", actividades.actividades.value.single().descripcion)
    }

    @Test
    fun noEliminaContactosConCobros() = runTest {
        repositorio.contactos.value = listOf(rosa)
        repositorio.cobros.value = listOf(Cobro("c1", rosa, "Préstamo", Monto.soles(50), hoy, EstadoCobro.COBRADO))

        assertEquals(EliminarContactoUseCase.Resultado.TieneCobros, eliminarContacto(rosa))
        assertEquals(listOf(rosa), repositorio.contactos.value)
    }

    @Test
    fun resumenSumaSoloPendientesPorContacto() {
        val mario = Contacto("mario", "Mario")
        val resumen = ResumenCobros.de(
            listOf(
                Cobro("1", rosa, "a", Monto.soles(10), hoy),
                Cobro("2", rosa, "b", Monto.soles(5), hoy),
                Cobro("3", mario, "c", Monto.soles(30), hoy),
                Cobro("4", mario, "d", Monto.soles(100), hoy, EstadoCobro.COBRADO),
            ),
        )

        assertEquals(Monto.soles(45), resumen.totalPendiente)
        assertEquals(listOf("Mario" to Monto.soles(30), "Rosa" to Monto.soles(15)), resumen.porContacto.map { it.contacto.nombre to it.total })
        assertEquals(2, resumen.porContacto.last().cantidad)
    }
}
