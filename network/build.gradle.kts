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
            implementation(libs.ktor.serialization.kotlinx.json)

            // Coil (OkHttp for network fetching)
            implementation(libs.coil)
            implementation(libs.coil.network)

            // Security - Native trusted roots
            implementation(libs.jvm.native.trusted.roots)

            // Internal logging module for shared logging config
            implementation(project(":logging"))
        }
    }
}
