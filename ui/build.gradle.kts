plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

/**
 * Compile-time UI backend. This is a JVM-only module: both backends live in
 * extra source directories that are added to `jvmMain`. Override with
 * `-PuiBackend=linux|fluent` (CI / packaging on a non-target OS).
 */
val uiBackend: String =
    (findProperty("uiBackend") as String?)?.lowercase()
        ?: when {
            System.getProperty("os.name").lowercase().contains("linux") -> "linux"
            else -> "fluent"
        }

kotlin {
    jvm()
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        val jvmMain by getting {
            kotlin.srcDir("src/${uiBackend}Main/kotlin")
            dependencies {
                implementation(libs.nucleus.system.color)
                when (uiBackend) {
                    "linux" -> implementation(libs.yaru)
                    else -> {
                        implementation(libs.compose.fluent)
                        implementation(libs.compose.fluent.icons.extended)
                    }
                }
            }
        }
    }
}
