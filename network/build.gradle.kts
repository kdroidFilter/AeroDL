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

            // Nucleus native HTTPS modules (replaces jvm-native-trusted-roots + BouncyCastle)
            implementation(libs.nucleus.native.http.ktor)    // KtorConfig + native SSL
            implementation(libs.nucleus.native.http.okhttp)  // CoilConfig OkHttp client

            // Internal logging module for shared logging config
            implementation(project(":logging"))
        }
    }
}
