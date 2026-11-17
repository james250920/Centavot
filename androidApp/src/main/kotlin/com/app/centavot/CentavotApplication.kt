package com.app.centavot

import com.app.centavot.data.sync.ConfigApi
import com.app.centavot.data.sync.Sincronizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.GlobalContext
import android.app.Application
import com.app.centavot.di.moduloAndroid
import com.app.centavot.di.modulosComunes
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.app.centavot.core.util.Sesion

class CentavotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CentavotApplication)
            // Va al final: reemplaza la SesionLocal de los módulos comunes por la de Firebase.
            modules(
                modulosComunes + moduloAndroid + module {
                    single<Sesion> { SesionFirebase(this@CentavotApplication) }
                    // Backend de Centavot: centavot.apiUrl en local.properties (vacío = sin sincronizar).
                    single { ConfigApi(BuildConfig.CENTAVOT_API_URL) }
                },
            )
        }
        // Sincroniza en segundo plano mientras la app vive; sin URL o sin sesión no hace nada.
        GlobalContext.get().get<Sincronizador>().iniciar(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }
}
