package com.app.lucka.di

import com.app.lucka.core.util.AbridorEnlaces
import com.app.lucka.core.util.CompartidorArchivos
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.CobroRepository
import com.app.lucka.domain.repository.DatosRepository
import com.app.lucka.domain.repository.GastoRepository
import com.app.lucka.domain.repository.IngresoRepository
import com.app.lucka.domain.repository.NotificacionRepository
import com.app.lucka.domain.repository.PerfilRepository
import com.app.lucka.domain.repository.RegimenRepository
import com.app.lucka.domain.repository.UsoRepository
import com.app.lucka.fakes.FakeActividadRepository
import com.app.lucka.fakes.FakeCobroRepository
import com.app.lucka.fakes.FakeGastoRepository
import com.app.lucka.fakes.FakeIngresoRepository
import com.app.lucka.fakes.FakeNotificacionRepository
import com.app.lucka.fakes.FakeRegimenRepository
import com.app.lucka.fakes.FakeUsoRepository
import com.app.lucka.domain.model.Perfil
import com.app.lucka.presentation.AppViewModel
import com.app.lucka.presentation.screens.actividad.ActividadViewModel
import com.app.lucka.presentation.screens.ajustes.AjustesViewModel
import com.app.lucka.presentation.screens.cobros.CobroViewModel
import com.app.lucka.presentation.screens.cobros.CobrosViewModel
import com.app.lucka.presentation.screens.cobros.ContactosViewModel
import com.app.lucka.presentation.screens.gasto.GastoViewModel
import com.app.lucka.presentation.screens.inicio.InicioViewModel
import com.app.lucka.presentation.screens.movimientos.MovimientosViewModel
import com.app.lucka.presentation.screens.notificaciones.NotificacionesViewModel
import com.app.lucka.presentation.screens.regimen.RegimenViewModel
import com.app.lucka.presentation.screens.reporte.ReporteViewModel
import com.app.lucka.presentation.screens.venta.VentaViewModel
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
        koin.get<VentaViewModel> { parametersOf(null) }
        koin.get<VentaViewModel> { parametersOf("venta-1") }
    }
}
