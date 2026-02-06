plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)

}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("io.github.kdroidfilter:composewebview:1.0.0-alpha-10")
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil)
            implementation(libs.coil.network)
        }
    }
}

compose.desktop {
    application {
        mainClass = "WebViewDemoKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
    }
}
