package com.app.centavot.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import com.app.centavot.core.util.AbridorEnlaces
import com.app.centavot.core.util.AbridorEnlacesAndroid
import com.app.centavot.core.util.CompartidorArchivos
import com.app.centavot.core.util.CompartidorArchivosAndroid
import com.app.centavot.core.util.GeneradorPdf
import com.app.centavot.core.util.GeneradorPdfAndroid
import com.app.centavot.data.local.crearDatabase
import com.app.centavot.data.local.crearDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val moduloAndroid = module {
    single { crearDatabase(crearDatabaseBuilder(androidContext())) }
    single<CompartidorArchivos> { CompartidorArchivosAndroid(androidContext()) }
    single<AbridorEnlaces> { AbridorEnlacesAndroid(androidContext()) }
    single<GeneradorPdf> { GeneradorPdfAndroid() }
    single { HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000 } } }
}
