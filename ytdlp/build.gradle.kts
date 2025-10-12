plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            // Network module (provides Ktor, SSL/TLS configuration)
            implementation(project(":network"))

            // Coroutines
            implementation(libs.kotlinx.coroutinesSwing)

            // Platform tools
            implementation(libs.platformtools.releasefetcher)
            implementation(libs.platformtools.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.testJunit)
        }
    }
}
