import io.github.kdroidfilter.nucleus.desktop.application.dsl.CompressionLevel
import io.github.kdroidfilter.nucleus.desktop.application.dsl.ReleaseChannel
import io.github.kdroidfilter.nucleus.desktop.application.dsl.ReleaseType
import io.github.kdroidfilter.nucleus.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.metro)
    alias(libs.plugins.sentryJvmGradle)
    alias(libs.plugins.nucleus)
}

val version: String = System.getenv("GITHUB_REF")
    ?.takeIf { it.startsWith("refs/tags/") }
    ?.removePrefix("refs/tags/")?.removePrefix("v")
    ?: "1.0.0"

sentry {
    includeSourceContext = true
    org = System.getenv("SENTRY_ORG") ?: "kdroidfilter"
    projectName = System.getenv("SENTRY_PROJECT") ?: "kotlin"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

// Turn 0.x[.y] into 1.x[.y] for macOS (DMG/PKG require MAJOR > 0)
fun macSafeVersion(ver: String): String {
    // Strip prerelease/build metadata for packaging (e.g., 0.1.0-beta -> 0.1.0)
    val core = ver.substringBefore('-').substringBefore('+')
    val parts = core.split('.')

    return if (parts.isNotEmpty() && parts[0] == "0") {
        when (parts.size) {
            1 -> "1.0"                 // "0"      -> "1.0"
            2 -> "1.${parts[1]}"       // "0.1"    -> "1.1"
            else -> "1.${parts[1]}.${parts[2]}" // "0.1.2" -> "1.1.2"
        }
    } else {
        core // already >= 1.x or something else; leave as-is
    }
}

kotlin {
    jvm()
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
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

            // DI - Metro
            implementation(libs.metro.runtime)
            implementation(libs.metro.viewmodel)
            implementation(libs.metro.viewmodel.compose)

            // Platform tools
            implementation(libs.platformtools.core)
            implementation(libs.platformtools.clipboardmanager)
            implementation(libs.autolaunch)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)

            // Nucleus
            implementation(libs.nucleus.core.runtime)
            implementation(libs.nucleus.aot.runtime)
            implementation(libs.nucleus.darkmode.detector)
            implementation(libs.nucleus.updater.runtime)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)

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
            implementation(libs.cardiologist)
            implementation(libs.sentry.core)

            implementation(libs.platformtools.appmanager)
            // no external markdown UI renderer; using lightweight parser

            // Project dependencies
            implementation(project(":ytdlp"))
            implementation(project(":ffmpeg"))
            implementation(project(":network"))
            implementation(project(":logging"))
            implementation(project(":youtube-webview-extractor"))

            // SQLDelight driver
            implementation(libs.sqlDelight.driver.sqlite)
        }
    }
}

nucleus.application {
    mainClass = "io.github.kdroidfilter.ytdlpgui.MainKt"

    val cleanInstall = project.findProperty("cleanInstall")?.toString()?.toBoolean() ?: false
    val debugLogs = project.findProperty("debugLogs")?.toString()?.toBoolean() ?: false
    jvmArgs += listOf("-DcleanInstall=$cleanInstall", "-DdebugLogs=$debugLogs")

    buildTypes { release { proguard {
        version.set("7.8.1")
        isEnabled = true
        obfuscate.set(false)
        optimize.set(true)
        configurationFiles.from(project.file("proguard-rules.pro"))
    }}}

    nativeDistributions {
        vendor = "KDroidFilter"
        targetFormats(TargetFormat.Dmg, TargetFormat.Nsis, TargetFormat.Deb, TargetFormat.Rpm)
        packageName = "AeroDl"
        packageVersion = version
        description = "AeroDl"
        homepage = "https://github.com/kdroidFilter/AeroDL"
        cleanupNativeLibs = true
        enableAotCache = true
        compressionLevel = CompressionLevel.Maximum

        jvmArgs += listOf(
            "-XX:+UseCompactObjectHeaders",
            "-XX:+UseStringDeduplication",
            "-XX:MaxGCPauseMillis=50"
        )
        modules("jdk.accessibility", "java.net.http", "java.sql", "jdk.security.auth", "jdk.unsupported")

        publish {
            github {
                enabled = true
                owner = "kdroidFilter"
                repo = "AeroDL"
                channel = ReleaseChannel.Latest
                releaseType = ReleaseType.Release
            }
        }

        windows {
            iconFile.set(project.file("icons/logo.ico"))
            shortcut = true
            upgradeUuid = "ada57c09-11e1-4d56-9d5d-0c480f6968ec"
            perUserInstall = true
            packageVersion = version

            nsis {
                oneClick = true
                perMachine = false
                createDesktopShortcut = true
                createStartMenuShortcut = true
                runAfterFinish = true
            }
        }
        macOS {
            bundleID = "io.github.kdroidfilter.ytdlpgui"
            dockName = "AeroDl"
            iconFile.set(project.file("icons/logo.icns"))
            packageVersion = macSafeVersion(version)
        }
        linux {
            packageName = "aerodl"
            iconFile.set(project.file("icons/logo.png"))
            packageVersion = version
            debMaintainer = "kdroidfilter@gmail.com"
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

tasks.withType<Jar> {
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.EC")
}
