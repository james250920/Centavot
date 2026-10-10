package com.app.lucka.di

import com.app.lucka.core.util.AbridorEnlaces
import com.app.lucka.core.util.AbridorEnlacesAndroid
import com.app.lucka.core.util.CompartidorArchivos
import com.app.lucka.core.util.CompartidorArchivosAndroid
import com.app.lucka.data.local.crearDatabase
import com.app.lucka.data.local.crearDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val moduloAndroid = module {
    single { crearDatabase(crearDatabaseBuilder(androidContext())) }
    single<CompartidorArchivos> { CompartidorArchivosAndroid(androidContext()) }
    single<AbridorEnlaces> { AbridorEnlacesAndroid(androidContext()) }
}
