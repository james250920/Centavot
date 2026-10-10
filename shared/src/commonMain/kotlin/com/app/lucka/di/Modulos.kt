package com.app.lucka.di

import com.app.lucka.core.util.Reloj
import com.app.lucka.core.util.RelojSistema
import com.app.lucka.data.local.LuckaDatabase
import com.app.lucka.data.repository.ActividadRepositoryImpl
import com.app.lucka.data.repository.DatosRepositoryImpl
import com.app.lucka.data.repository.CobroRepositoryImpl
import com.app.lucka.data.repository.GastoRepositoryImpl
import com.app.lucka.data.repository.IngresoRepositoryImpl
import com.app.lucka.data.repository.UsoRepositoryImpl
import com.app.lucka.data.repository.NotificacionRepositoryImpl
import com.app.lucka.data.repository.PerfilRepositoryImpl
import com.app.lucka.data.repository.RegimenRepositoryImpl
import com.app.lucka.domain.repository.ActividadRepository
import com.app.lucka.domain.repository.CobroRepository
import com.app.lucka.domain.repository.DatosRepository
import com.app.lucka.domain.repository.GastoRepository
import com.app.lucka.domain.repository.IngresoRepository
import com.app.lucka.domain.repository.UsoRepository
import com.app.lucka.domain.repository.NotificacionRepository
import com.app.lucka.domain.repository.PerfilRepository
import com.app.lucka.domain.repository.RegimenRepository
import com.app.lucka.domain.usecase.AceptarAvisoPrivacidadUseCase
import com.app.lucka.domain.usecase.AgregarContactoUseCase
import com.app.lucka.domain.usecase.BorrarTodosLosDatosUseCase
import com.app.lucka.domain.usecase.ExportarTodosLosDatosUseCase
import com.app.lucka.domain.usecase.ObservarConsentimientoUseCase
import com.app.lucka.domain.usecase.EliminarCobroUseCase
import com.app.lucka.domain.usecase.EliminarContactoUseCase
import com.app.lucka.domain.usecase.EliminarGastoUseCase
import com.app.lucka.domain.usecase.GuardarGastoUseCase
import com.app.lucka.domain.usecase.GuardarPerfilUseCase
import com.app.lucka.domain.usecase.GuardarRegimenUseCase
import com.app.lucka.domain.usecase.MarcarCobradoUseCase
import com.app.lucka.domain.usecase.ObtenerCobroUseCase
import com.app.lucka.domain.usecase.RegistrarAbonoUseCase
import com.app.lucka.domain.usecase.RegistrarCobroYVentaUseCase
import com.app.lucka.domain.usecase.MarcarNotificacionesLeidasUseCase
import com.app.lucka.domain.usecase.ObservarActividadesUseCase
import com.app.lucka.domain.usecase.ObservarCobrosUseCase
import com.app.lucka.domain.usecase.ObservarContactosUseCase
import com.app.lucka.domain.usecase.ObservarGastosUseCase
import com.app.lucka.domain.usecase.ObservarNoLeidasUseCase
import com.app.lucka.domain.usecase.ObservarNotificacionesUseCase
import com.app.lucka.domain.usecase.ObservarPerfilUseCase
import com.app.lucka.domain.usecase.ObservarEstadoTopeUseCase
import com.app.lucka.domain.usecase.EliminarIngresoUseCase
import com.app.lucka.domain.usecase.GuardarIngresoUseCase
import com.app.lucka.domain.usecase.ObservarGastosDelMesUseCase
import com.app.lucka.domain.usecase.ObservarHistorialUseCase
import com.app.lucka.domain.usecase.ObservarIngresosUseCase
import com.app.lucka.domain.usecase.ObservarResumenPeriodoUseCase
import com.app.lucka.domain.usecase.ObservarResumenUsoUseCase
import com.app.lucka.domain.usecase.ObservarVentasFrecuentesUseCase
import com.app.lucka.domain.usecase.ObtenerIngresoUseCase
import com.app.lucka.domain.usecase.RegistrarAperturaUseCase
import com.app.lucka.domain.usecase.ResponderEncuestaCuadernoUseCase
import com.app.lucka.domain.usecase.ObservarRegimenUseCase
import com.app.lucka.domain.usecase.ObservarReporteUseCase
import com.app.lucka.domain.usecase.ObservarResumenCobrosUseCase
import com.app.lucka.domain.usecase.ObservarResumenMesUseCase
import com.app.lucka.domain.usecase.ObtenerGastoUseCase
import com.app.lucka.domain.usecase.ObtenerOpcionesRegimenUseCase
import com.app.lucka.domain.usecase.RegistrarCobroUseCase
import com.app.lucka.domain.usecase.RevisarAlertaTopeUseCase
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
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// La base de datos (LuckaDatabase), el CompartidorArchivos y el AbridorEnlaces los registra cada plataforma,
// p. ej. moduloAndroid.

val moduloData = module {
    single { get<LuckaDatabase>().gastoDao() }
    single { get<LuckaDatabase>().regimenDao() }
    single { get<LuckaDatabase>().perfilDao() }
    single { get<LuckaDatabase>().cobroDao() }
    single { get<LuckaDatabase>().actividadDao() }
    single { get<LuckaDatabase>().notificacionDao() }
    single { get<LuckaDatabase>().ingresoDao() }
    single { get<LuckaDatabase>().eventoUsoDao() }
    single { get<LuckaDatabase>().datosDao() }
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
    viewModel { (id: String?) -> GastoViewModel(id, get(), get(), get(), get()) }
    viewModel { (id: String?) -> VentaViewModel(id, get(), get(), get(), get(), get()) }
    viewModel { (id: String?) -> CobroViewModel(id, get(), get(), get(), get(), get()) }
}

val modulosComunes = listOf(moduloData, moduloDomain, moduloPresentation)
