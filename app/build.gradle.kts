plugins {
    alias(libs.plugins.android.application)
}

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
    // Room: persistencia del borrador de "Publicar artículo" (Punto 5).
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    // Retrofit: alta de publicaciones y pausar/reactivar contra la API_Rest del TPO.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    // ViewModel + LiveData: estado compartido entre los pasos del wizard de publicar.
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}