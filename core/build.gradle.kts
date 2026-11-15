import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM logic (counts, name matching, lookups). No Android dependencies,
// so it can be unit-tested on the PC without a phone.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
