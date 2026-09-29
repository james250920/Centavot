package com.app.centavot.di

import com.app.centavot.core.util.CompartidorArchivos
import com.app.centavot.core.util.CompartidorArchivosAndroid
import com.app.centavot.data.local.crearDatabase
import com.app.centavot.data.local.crearDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val moduloAndroid = module {
    single { crearDatabase(crearDatabaseBuilder(androidContext())) }
    single<CompartidorArchivos> { CompartidorArchivosAndroid(androidContext()) }
}
