package com.app.centavot.domain.usecase

import com.app.centavot.domain.model.MAX_ACTIVIDADES_VISIBLES
import com.app.centavot.fakes.FakeActividadRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ActividadUseCasesTest {

    @Test
    fun laVistaMuestraSoloLasRecientesSinBorrarElHistorial() = runTest {
        val repositorio = FakeActividadRepository()
        repeat(20) { repositorio.registrar("Actividad $it", LocalDateTime(2026, 9, 29, 10, it)) }
        val observar = ObservarActividadesUseCase(repositorio)

        val recientes = observar(completo = false).first()
        assertEquals(MAX_ACTIVIDADES_VISIBLES, recientes.size)
        assertEquals("Actividad 19", recientes.first().descripcion)
        assertEquals(20, observar(completo = true).first().size)
    }
}
