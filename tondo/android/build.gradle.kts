plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.foschia.tondo.android"
    compileSdk = 36
    defaultConfig { minSdk = 21 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Icons, Noto emoji artwork (and optional fonts) shared with the iOS extension.
    sourceSets["main"].assets.srcDirs("../assets")
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    api(project(":core"))
}
