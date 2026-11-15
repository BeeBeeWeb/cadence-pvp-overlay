import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.beebeeweb.cadence"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beebeeweb.cadence"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The Great League data (and the PvPoke licence next to it) is bundled as app assets.
    sourceSets["main"].assets.srcDir("../data")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    // On-device text recognition (Latin script). Runs offline, no network permission needed.
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
