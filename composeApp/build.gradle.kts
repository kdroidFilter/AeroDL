import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)

}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Androidx
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // UI libraries
            implementation(libs.compose.fluent)
            implementation(libs.compose.fluent.icons.extended)
            implementation(libs.composemediaplayer)

            // Coil
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Platform tools
            implementation(libs.platformtools.darkmodedetector)
            implementation(libs.platformtools.clipboardmanager)
            implementation("io.github.vinceglb:auto-launch:0.7.0")
            implementation("io.github.vinceglb:filekit-core:0.11.0")
            implementation("io.github.vinceglb:filekit-dialogs:0.11.0")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.11.0")
            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            // Desktop specific
            implementation(compose.desktop.currentOs)
            implementation(libs.composenativetray)
            implementation(libs.kotlinx.coroutinesSwing)

            // Project dependencies
            implementation(project(":ytdlp"))
        }
    }
}


compose.desktop {
    application {
        mainClass = "io.github.kdroidfilter.ytdlpgui.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.kdroidfilter.ytdlpgui"
            packageVersion = "1.0.0"
        }
    }
}
