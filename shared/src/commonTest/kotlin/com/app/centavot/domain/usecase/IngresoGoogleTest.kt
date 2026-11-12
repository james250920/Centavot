package com.app.centavot.domain.usecase

import com.app.centavot.core.util.ResultadoIngreso
import com.app.centavot.core.util.SesionLocal
import com.app.centavot.domain.model.Modo
import com.app.centavot.domain.model.Perfil
import com.app.centavot.presentation.EstadoApp
import com.app.centavot.presentation.estadoApp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Ingreso con Google (11 nov 2026): va después del aviso y antes de la bienvenida. */
class IngresoGoogleTest {

    private val perfil = Perfil("Rosa", null, modo = Modo.PERSONAL)

    @Test
    fun sinSesionSePideEntrarDespuesDelAvisoYAntesDeLaBienvenida() {
        assertEquals(EstadoApp.SIN_CONSENTIMIENTO, estadoApp(consentido = false, perfil = null, regimen = null, sinSesion = true))
        assertEquals(EstadoApp.SIN_SESION, estadoApp(consentido = true, perfil = null, regimen = null, sinSesion = true))
        // Quien ya usaba la app también entra antes de seguir.
        assertEquals(EstadoApp.SIN_SESION, estadoApp(consentido = true, perfil = perfil, regimen = null, sinSesion = true))
        assertEquals(EstadoApp.LISTA, estadoApp(consentido = true, perfil = perfil, regimen = null, sinSesion = false))
    }

    @Test
    fun sinFirebaseLaAppNoPideIngreso() = runTest {
        val sesion = SesionLocal()
        assertFalse(sesion.disponible)
        assertNull(sesion.usuario.value)
        assertIs<ResultadoIngreso.Error>(sesion.entrarConGoogle())
    }
}
