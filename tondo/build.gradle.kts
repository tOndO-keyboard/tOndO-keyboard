plugins {
    kotlin("multiplatform") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.3.21" apply false
    kotlin("android") version "2.3.21" apply false
    id("com.android.library") version "8.13.0" apply false
}

allprojects {
    group = "com.foschia.tondo"
    version = "1.0.0"
}
