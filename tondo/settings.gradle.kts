pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tondo"

// Shared keyboard: logic, layout and renderer-agnostic scene (Android, iOS, JVM).
include(":core")
// Android view that paints the shared scene on a Canvas (consumed by the "Android Studio" app).
include(":android")
