plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)

            // Serialization for JSON parsing
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            // WebView - exposed via api so consumers can use WebViewNavigator
            api(libs.composewebview)

            // Coroutines
            implementation(libs.kotlinx.coroutinesSwing)

            // Logging
            implementation(project(":logging"))
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
