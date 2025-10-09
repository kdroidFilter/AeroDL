plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            // Ktor client - using API so they're exposed to dependent modules
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.client.serialization)
            api(libs.ktor.client.logging)
            api(libs.ktor.client.cio)

            // Kotlinx serialization - using API so it's exposed to dependent modules
            api(libs.kotlinx.serialization.json)
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")

            // Coil (OkHttp for network fetching)
            implementation(libs.coil)
            implementation(libs.coil.network)

            // Security - Native trusted roots
            implementation("org.jetbrains.nativecerts:jvm-native-trusted-roots:1.1.7")
        }
    }
}