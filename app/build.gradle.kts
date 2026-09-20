import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

// URL del backend (FastAPI) leída de local.properties, que no se versiona:
// cada dev la apunta a su propio servidor (ej. la IP local mientras se
// desarrolla) sin tocar código ni pisarle la config a nadie más.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) {
        load(archivo.inputStream())
    }
}
val apiBaseUrl: String = (propiedadesLocales.getProperty("API_BASE_URL")
    ?: "https://ronda-api-production.up.railway.app/")

android {
    namespace = "com.example.tpo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.tpo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // Navigation Component: Single Activity + Fragments navegados por NavController (Clase 4).
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    // RecyclerView para el listado de publicaciones del Home.
    implementation(libs.recyclerview)
    // Swipe-to-refresh del Home para el modo offline.
    implementation(libs.swiperefreshlayout)
    // Desbloqueo por huella/cara/PIN del dispositivo (Punto 1).
    implementation(libs.biometric)
    // Hilt: inyeccion de dependencias (TokenManager, NetworkModule, RondaApp)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    // Room: persistencia del borrador de publicación (Punto 5)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    // Retrofit/OkHttp/Gson: consumo de la API (login, publicaciones, mis publicaciones)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}