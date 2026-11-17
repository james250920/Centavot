import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Firma para Google Play: la llave de subida y sus claves viven en keystore.properties (no se sube a git).
val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use(::load)
}

// URL del backend: https://cv.menfroyt-dev.com. Para probar con un backend local, en local.properties (no va a git):
// centavot.apiUrl=http://localhost:3001 (con adb reverse). Vacía = la app no sincroniza.
val apiUrl: String = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use(::load)
}.getProperty("centavot.apiUrl", "https://cv.menfroyt-dev.com")

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // Firebase: lee androidApp/google-services.json (proyecto centavot-67f60).
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    // Firebase con su BoM: las versiones de cada librería las fija el BoM. Auth es para "Continuar con Google".
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    // "Continuar con Google" con Credential Manager (la hoja de cuentas del sistema).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.app.centavot"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.app.centavot"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "CENTAVOT_API_URL", "\"$apiUrl\"")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (propiedadesFirma.isNotEmpty()) {
            create("release") {
                storeFile = file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // Sin keystore.properties la versión final sale sin firmar, como antes.
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}