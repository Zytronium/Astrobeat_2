import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.zytronium.astrobeat2"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.zytronium.astrobeat2"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // -------- API config --------
        val secrets = Properties().apply {
            rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.let { load(it) }
        }

        buildConfigField("String", "API_BASE_URL", "\"${secrets.getProperty("API_BASE_URL") ?: ""}\"")
        buildConfigField("String", "API_SECRET",   "\"${secrets.getProperty("API_SECRET")   ?: ""}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // -------- networking --------
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // -------- room (local db) --------
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // -------- media3 / exoplayer --------
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // -------- workmanager --------
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)

    // -------- hilt (dependency injection) --------
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.hilt.work.compiler)

    // -------- kotlin coroutines --------
    implementation(libs.coroutines.android)

    // -------- standard android --------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
}
