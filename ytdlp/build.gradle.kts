plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutinesSwing)
            // Platform tools
            implementation(libs.platformtools.releasefetcher)
            implementation(libs.platformtools.core)

            // Ktor client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.cio)

            implementation(libs.kotlinx.serialization.json)
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
            // Security
            implementation("org.jetbrains.nativecerts:jvm-native-trusted-roots:1.1.7")
        }
    }
}

