# mosaico_compose

Kotlin Compose Multiplatform app targeting Android and Windows desktop.

## Modules
- Android app
- Desktop app (Windows)

## Prerequisites
- JDK 17+
- Gradle (wrapper included)
- Android Studio (for Android SDK and emulator)
- (Optional) Visual Studio C++ Build Tools are not required for Compose Desktop.

## Run
- Android: use Android Studio to run the `androidApp` module on a device/emulator.
- Desktop: run via Gradle task `:desktopApp:run`.

## Build
- Android: `:androidApp:assembleDebug`
- Desktop: `:desktopApp:packageReleaseDistributionForCurrentOS`
