package com.app.centavot.di

import com.app.centavot.core.util.AbridorEnlaces
import com.app.centavot.core.util.CompartidorArchivos
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.CobroRepository
import com.app.centavot.domain.repository.DatosRepository
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.NotificacionRepository
import com.app.centavot.domain.repository.PerfilRepository
import com.app.centavot.domain.repository.RegimenRepository
import com.app.centavot.domain.repository.UsoRepository
import com.app.centavot.fakes.FakeActividadRepository
import com.app.centavot.fakes.FakeCobroRepository
import com.app.centavot.fakes.FakeGastoRepository
import com.app.centavot.fakes.FakeIngresoRepository
import com.app.centavot.fakes.FakeNotificacionRepository
import com.app.centavot.fakes.FakeRegimenRepository
import com.app.centavot.fakes.FakeUsoRepository
import com.app.centavot.domain.model.Perfil
import com.app.centavot.presentation.AppViewModel
import com.app.centavot.presentation.screens.actividad.ActividadViewModel
import com.app.centavot.presentation.screens.ajustes.AjustesViewModel
import com.app.centavot.presentation.screens.cobros.CobroViewModel
import com.app.centavot.presentation.screens.cobros.CobrosViewModel
import com.app.centavot.presentation.screens.cobros.ContactosViewModel
import com.app.centavot.presentation.screens.gasto.GastoViewModel
import com.app.centavot.presentation.screens.inicio.InicioViewModel
import com.app.centavot.presentation.screens.movimientos.MovimientosViewModel
import com.app.centavot.presentation.screens.notificaciones.NotificacionesViewModel
import com.app.centavot.presentation.screens.regimen.RegimenViewModel
import com.app.centavot.presentation.screens.reporte.ReporteViewModel
import com.app.centavot.presentation.screens.venta.VentaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Un error de Koin solo aparece al abrir la pantalla. Este test arma cada ViewModel con los
 * módulos reales de dominio y presentación (los repositorios son fakes, sin base de datos).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModulosTest {

    private val moduloFakes = module {
        single<GastoRepository> { FakeGastoRepository() }
        single<IngresoRepository> { FakeIngresoRepository() }
        single<RegimenRepository> { FakeRegimenRepository() }
        single<PerfilRepository> {
            object : PerfilRepository {
                override fun observarPerfil(): Flow<Perfil?> = MutableStateFlow(null)
                override suspend fun guardar(perfil: Perfil) = Unit
            }
        }
        single<CobroRepository> { FakeCobroRepository() }
        single<ActividadRepository> { FakeActividadRepository() }
        single<NotificacionRepository> { FakeNotificacionRepository() }
        single<UsoRepository> { FakeUsoRepository() }
        single<DatosRepository> { DatosRepository { } }
        single<CompartidorArchivos> { CompartidorArchivos { _, _, _ -> } }
        single<AbridorEnlaces> { AbridorEnlaces { } }
    }

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun todasLasPantallasSeArmanConSusDependencias() {
        val koin = startKoin { modules(moduloFakes, moduloDomain, moduloPresentation) }.koin

        koin.get<AppViewModel>()
        koin.get<RegimenViewModel>()
        koin.get<InicioViewModel>()
        koin.get<MovimientosViewModel>()
        koin.get<ReporteViewModel>()
        koin.get<AjustesViewModel>()
        koin.get<CobrosViewModel>()
        koin.get<CobroViewModel> { parametersOf(null) }
        koin.get<CobroViewModel> { parametersOf("cobro-1") }
        koin.get<ContactosViewModel>()
        koin.get<ActividadViewModel>()
        koin.get<NotificacionesViewModel>()
        koin.get<GastoViewModel> { parametersOf(null) }
        koin.get<VentaViewModel> { parametersOf(null, false) }
        koin.get<VentaViewModel> { parametersOf(null, true) }
        koin.get<VentaViewModel> { parametersOf("venta-1", false) }
    }
}
