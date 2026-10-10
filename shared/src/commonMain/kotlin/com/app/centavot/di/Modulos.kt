package com.app.centavot.di

import com.app.centavot.presentation.Avisos
import com.app.centavot.domain.usecase.RestaurarCobroUseCase
import com.app.centavot.core.util.Reloj
import com.app.centavot.core.util.RelojSistema
import com.app.centavot.data.local.CentavotDatabase
import com.app.centavot.data.repository.ActividadRepositoryImpl
import com.app.centavot.data.repository.DatosRepositoryImpl
import com.app.centavot.data.repository.CobroRepositoryImpl
import com.app.centavot.data.repository.GastoRepositoryImpl
import com.app.centavot.data.repository.IngresoRepositoryImpl
import com.app.centavot.data.repository.UsoRepositoryImpl
import com.app.centavot.data.repository.NotificacionRepositoryImpl
import com.app.centavot.data.repository.PerfilRepositoryImpl
import com.app.centavot.data.repository.RegimenRepositoryImpl
import com.app.centavot.domain.repository.ActividadRepository
import com.app.centavot.domain.repository.CobroRepository
import com.app.centavot.domain.repository.DatosRepository
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.IngresoRepository
import com.app.centavot.domain.repository.UsoRepository
import com.app.centavot.domain.repository.NotificacionRepository
import com.app.centavot.domain.repository.PerfilRepository
import com.app.centavot.domain.repository.RegimenRepository
import com.app.centavot.domain.usecase.AceptarAvisoPrivacidadUseCase
import com.app.centavot.domain.usecase.AgregarContactoUseCase
import com.app.centavot.domain.usecase.BorrarTodosLosDatosUseCase
import com.app.centavot.domain.usecase.ExportarTodosLosDatosUseCase
import com.app.centavot.domain.usecase.ObservarConsentimientoUseCase
import com.app.centavot.domain.usecase.EliminarCobroUseCase
import com.app.centavot.domain.usecase.EliminarContactoUseCase
import com.app.centavot.domain.usecase.EliminarGastoUseCase
import com.app.centavot.domain.usecase.GuardarGastoUseCase
import com.app.centavot.domain.usecase.GuardarPerfilUseCase
import com.app.centavot.domain.usecase.GuardarRegimenUseCase
import com.app.centavot.domain.usecase.MarcarCobradoUseCase
import com.app.centavot.domain.usecase.ObtenerCobroUseCase
import com.app.centavot.domain.usecase.RegistrarAbonoUseCase
import com.app.centavot.domain.usecase.RegistrarCobroYVentaUseCase
import com.app.centavot.domain.usecase.MarcarNotificacionesLeidasUseCase
import com.app.centavot.domain.usecase.ObservarActividadesUseCase
import com.app.centavot.domain.usecase.ObservarCobrosUseCase
import com.app.centavot.domain.usecase.ObservarContactosUseCase
import com.app.centavot.domain.usecase.ObservarGastosUseCase
import com.app.centavot.domain.usecase.ObservarNoLeidasUseCase
import com.app.centavot.domain.usecase.ObservarNotificacionesUseCase
import com.app.centavot.domain.usecase.ObservarPerfilUseCase
import com.app.centavot.domain.usecase.ObservarEstadoTopeUseCase
import com.app.centavot.domain.usecase.EliminarIngresoUseCase
import com.app.centavot.domain.usecase.GuardarIngresoUseCase
import com.app.centavot.domain.usecase.ObservarGastosDelMesUseCase
import com.app.centavot.domain.usecase.ObservarHistorialUseCase
import com.app.centavot.domain.usecase.ObservarIngresosUseCase
import com.app.centavot.domain.usecase.ObservarResumenPeriodoUseCase
import com.app.centavot.domain.usecase.ObservarResumenUsoUseCase
import com.app.centavot.domain.usecase.ObservarVentasFrecuentesUseCase
import com.app.centavot.domain.usecase.ObtenerIngresoUseCase
import com.app.centavot.domain.usecase.RegistrarAperturaUseCase
import com.app.centavot.domain.usecase.ResponderEncuestaCuadernoUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.ObservarReporteUseCase
import com.app.centavot.domain.usecase.ObservarResumenCobrosUseCase
import com.app.centavot.domain.usecase.ObservarResumenMesUseCase
import com.app.centavot.domain.usecase.ObtenerGastoUseCase
import com.app.centavot.domain.usecase.ObtenerOpcionesRegimenUseCase
import com.app.centavot.domain.usecase.RegistrarCobroUseCase
import com.app.centavot.domain.usecase.RevisarAlertaTopeUseCase
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
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// La base de datos (CentavotDatabase), el CompartidorArchivos y el AbridorEnlaces los registra cada plataforma,
// p. ej. moduloAndroid.

val moduloData = module {
    single { get<CentavotDatabase>().gastoDao() }
    single { get<CentavotDatabase>().regimenDao() }
    single { get<CentavotDatabase>().perfilDao() }
    single { get<CentavotDatabase>().cobroDao() }
    single { get<CentavotDatabase>().actividadDao() }
    single { get<CentavotDatabase>().notificacionDao() }
    single { get<CentavotDatabase>().ingresoDao() }
    single { get<CentavotDatabase>().eventoUsoDao() }
    single { get<CentavotDatabase>().datosDao() }
    single<GastoRepository> { GastoRepositoryImpl(get()) }
    single<RegimenRepository> { RegimenRepositoryImpl(get()) }
    single<PerfilRepository> { PerfilRepositoryImpl(get()) }
    single<CobroRepository> { CobroRepositoryImpl(get()) }
    single<ActividadRepository> { ActividadRepositoryImpl(get()) }
    single<NotificacionRepository> { NotificacionRepositoryImpl(get()) }
    single<IngresoRepository> { IngresoRepositoryImpl(get()) }
    single<UsoRepository> { UsoRepositoryImpl(get()) }
    single<DatosRepository> { DatosRepositoryImpl(get()) }
}

@OptIn(ExperimentalUuidApi::class)
private val generarId: () -> String = { Uuid.random().toString() }

val moduloDomain = module {
    single<Reloj> { RelojSistema }
    single { Avisos() }
    factory { GuardarGastoUseCase(get(), get(), generarId, get(), get(), get()) }
    factory { GuardarIngresoUseCase(get(), get(), generarId, get(), get(), get()) }
    factoryOf(::ObtenerIngresoUseCase)
    factoryOf(::EliminarIngresoUseCase)
    factoryOf(::ObservarIngresosUseCase)
    factoryOf(::ObservarVentasFrecuentesUseCase)
    factoryOf(::ObservarResumenPeriodoUseCase)
    factoryOf(::ObservarHistorialUseCase)
    factoryOf(::ObservarGastosDelMesUseCase)
    factoryOf(::RegistrarAperturaUseCase)
    factoryOf(::ObservarResumenUsoUseCase)
    factoryOf(::ResponderEncuestaCuadernoUseCase)
    factoryOf(::ObservarConsentimientoUseCase)
    factoryOf(::AceptarAvisoPrivacidadUseCase)
    factoryOf(::ExportarTodosLosDatosUseCase)
    factoryOf(::BorrarTodosLosDatosUseCase)
    factoryOf(::ObtenerGastoUseCase)
    factoryOf(::EliminarGastoUseCase)
    factoryOf(::ObservarGastosUseCase)
    factoryOf(::ObservarRegimenUseCase)
    factoryOf(::GuardarRegimenUseCase)
    factoryOf(::ObtenerOpcionesRegimenUseCase)
    factoryOf(::ObservarEstadoTopeUseCase)
    factoryOf(::ObservarResumenMesUseCase)
    factoryOf(::ObservarReporteUseCase)
    factoryOf(::RevisarAlertaTopeUseCase)
    factoryOf(::ObservarPerfilUseCase)
    factoryOf(::GuardarPerfilUseCase)
    factoryOf(::ObservarContactosUseCase)
    factoryOf(::ObservarCobrosUseCase)
    factoryOf(::ObservarResumenCobrosUseCase)
    factory { AgregarContactoUseCase(get(), get(), get(), generarId) }
    factoryOf(::EliminarContactoUseCase)
    factory { RegistrarCobroUseCase(get(), get(), get(), generarId) }
    factoryOf(::MarcarCobradoUseCase)
    factoryOf(::RegistrarAbonoUseCase)
    factoryOf(::RegistrarCobroYVentaUseCase)
    factoryOf(::RestaurarCobroUseCase)
    factoryOf(::ObtenerCobroUseCase)
    factoryOf(::EliminarCobroUseCase)
    factoryOf(::ObservarActividadesUseCase)
    factoryOf(::ObservarNotificacionesUseCase)
    factoryOf(::ObservarNoLeidasUseCase)
    factoryOf(::MarcarNotificacionesLeidasUseCase)
}

val moduloPresentation = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::RegimenViewModel)
    viewModelOf(::InicioViewModel)
    viewModelOf(::MovimientosViewModel)
    viewModelOf(::ReporteViewModel)
    viewModelOf(::AjustesViewModel)
    viewModelOf(::CobrosViewModel)
    viewModelOf(::ContactosViewModel)
    viewModelOf(::ActividadViewModel)
    viewModelOf(::NotificacionesViewModel)
    viewModel { (id: String?) -> GastoViewModel(id, get(), get(), get(), get(), get()) }
    viewModel { (id: String?) -> VentaViewModel(id, get(), get(), get(), get(), get(), get()) }
    viewModel { (id: String?) -> CobroViewModel(id, get(), get(), get(), get(), get(), get(), get(), get()) }
}

val modulosComunes = listOf(moduloData, moduloDomain, moduloPresentation)
