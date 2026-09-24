package com.app.centavot.di

import com.app.centavot.core.util.Reloj
import com.app.centavot.core.util.RelojSistema
import com.app.centavot.data.local.CentavotDatabase
import com.app.centavot.data.repository.GastoRepositoryImpl
import com.app.centavot.data.repository.RegimenRepositoryImpl
import com.app.centavot.domain.repository.GastoRepository
import com.app.centavot.domain.repository.RegimenRepository
import com.app.centavot.domain.usecase.EliminarGastoUseCase
import com.app.centavot.domain.usecase.GuardarGastoUseCase
import com.app.centavot.domain.usecase.GuardarRegimenUseCase
import com.app.centavot.domain.usecase.ObservarGastosUseCase
import com.app.centavot.domain.usecase.ObservarProximidadTopeUseCase
import com.app.centavot.domain.usecase.ObservarRegimenUseCase
import com.app.centavot.domain.usecase.ObservarReporteUseCase
import com.app.centavot.domain.usecase.ObservarResumenMesUseCase
import com.app.centavot.domain.usecase.ObtenerGastoUseCase
import com.app.centavot.domain.usecase.ObtenerOpcionesRegimenUseCase
import com.app.centavot.presentation.AppViewModel
import com.app.centavot.presentation.screens.gasto.GastoViewModel
import com.app.centavot.presentation.screens.inicio.InicioViewModel
import com.app.centavot.presentation.screens.movimientos.MovimientosViewModel
import com.app.centavot.presentation.screens.regimen.RegimenViewModel
import com.app.centavot.presentation.screens.reporte.ReporteViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// La base de datos (CentavotDatabase) la registra cada plataforma, p. ej. moduloAndroid.

val moduloData = module {
    single { get<CentavotDatabase>().gastoDao() }
    single { get<CentavotDatabase>().regimenDao() }
    single<GastoRepository> { GastoRepositoryImpl(get()) }
    single<RegimenRepository> { RegimenRepositoryImpl(get()) }
}

@OptIn(ExperimentalUuidApi::class)
val moduloDomain = module {
    single<Reloj> { RelojSistema }
    factory { GuardarGastoUseCase(get(), get(), generarId = { Uuid.random().toString() }) }
    factoryOf(::ObtenerGastoUseCase)
    factoryOf(::EliminarGastoUseCase)
    factoryOf(::ObservarGastosUseCase)
    factoryOf(::ObservarRegimenUseCase)
    factoryOf(::GuardarRegimenUseCase)
    factoryOf(::ObtenerOpcionesRegimenUseCase)
    factoryOf(::ObservarProximidadTopeUseCase)
    factoryOf(::ObservarResumenMesUseCase)
    factoryOf(::ObservarReporteUseCase)
}

val moduloPresentation = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::RegimenViewModel)
    viewModelOf(::InicioViewModel)
    viewModelOf(::MovimientosViewModel)
    viewModelOf(::ReporteViewModel)
    viewModel { (id: String?) -> GastoViewModel(id, get(), get(), get(), get()) }
}

val modulosComunes = listOf(moduloData, moduloDomain, moduloPresentation)
