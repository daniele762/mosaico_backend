plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    // JavaFX WebView embedding (Windows)
    val jfxVersion = "21.0.3"
    implementation("org.openjfx:javafx-base:$jfxVersion:win")
    implementation("org.openjfx:javafx-graphics:$jfxVersion:win")
    implementation("org.openjfx:javafx-controls:$jfxVersion:win")
    implementation("org.openjfx:javafx-swing:$jfxVersion:win")
    implementation("org.openjfx:javafx-web:$jfxVersion:win")
}

compose.desktop {
    application {
        mainClass = "com.example.mosaico.DesktopMainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "mosaico_compose"
            packageVersion = "0.1.0"
        }
    }
}

kotlin {
    jvmToolchain(17)
}
