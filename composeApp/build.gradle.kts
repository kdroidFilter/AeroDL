import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Locale

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.hydraulicConveyor)
}

val ref = System.getenv("GITHUB_REF") ?: ""
val version = if (ref.startsWith("refs/tags/")) {
    val tag = ref.removePrefix("refs/tags/")
    if (tag.startsWith("v")) tag.substring(1) else tag
} else "1.0.0"

kotlin {
    jvm()
    jvmToolchain(21)

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
            implementation(libs.coil)
            implementation(libs.coil.network)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Platform tools
            implementation(libs.platformtools.core)
            implementation(libs.platformtools.darkmodedetector)
            implementation(libs.platformtools.clipboardmanager)
            implementation(libs.autolaunch)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Settings & Notifications
            implementation(libs.multiplatform.settings)
            implementation(libs.knotify)
            implementation(libs.knotify.compose)

            implementation(libs.confettikit)

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
            implementation(project(":network"))

            // SQLDelight driver
            implementation(libs.sqlDelight.driver.sqlite)
        }
    }
}



compose.desktop {
    application {
        mainClass = "io.github.kdroidfilter.ytdlpgui.MainKt"

        nativeDistributions {
            vendor = "KDroidFilter"
            targetFormats(TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AeroDl"
            packageVersion = version
            description = "An awesome GUI for yt-dlp!"
            modules("jdk.accessibility", "java.sql", "jdk.security.auth")
            windows {
                dirChooser = true
                menuGroup = "start-menu-group"
                iconFile.set(project.file("icons/logo.ico"))
                shortcut = true
            }
            macOS {
                bundleID = "io.github.kdroidfilter.ytdlpgui"
                dockName = "AeroDl"
                iconFile.set(project.file("icons/logo.icns"))
            }
            linux {
                iconFile.set(project.file("icons/logo.png"))
            }
            buildTypes.release.proguard {
                isEnabled = true
                obfuscate.set(false)
                optimize.set(true)
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            // Database configuration here.
            // https://cashapp.github.io/sqldelight
            packageName.set("io.github.kdroidfilter.ytdlpgui.db")
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:${libs.versions.sqlDelight.get()}")
        }
    }
}


