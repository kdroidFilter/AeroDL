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
            // Exclude signed BouncyCastle jars to avoid digest errors after ProGuard.
            implementation("org.jetbrains.nativecerts:jvm-native-trusted-roots:${libs.versions.jvmNativeTrustedRoots.get()}") {
                exclude(group = "org.bouncycastle", module = "bcpkix-jdk18on")
                exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
            }

            // Re-introduce BouncyCastle via local, de-signed jars placed in this module's `libs/` folder.
            // See `network/libs/README-bouncycastle.txt` for instructions to strip signatures.
            implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

            // Internal logging module for shared logging config
            implementation(project(":logging"))
        }
    }
}
