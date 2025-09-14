plugins {
    // Kotlin Android plugin for the Android app module
    kotlin("android") version "2.0.20" apply false
    // Kotlin Compose compiler plugin required for Compose Multiplatform with Kotlin 2.x
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    // JetBrains Compose plugin for both Android and Desktop modules
    id("org.jetbrains.compose") version "1.7.0" apply false
    // Android Gradle Plugin for the Android app module
    id("com.android.application") version "8.7.1" apply false
}

allprojects {
    group = "com.example.mosaico"
    version = "0.1.0"
}
