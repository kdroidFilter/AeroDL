import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.NativeImageMarch
import dev.nucleusframework.desktop.application.dsl.ReleaseChannel
import dev.nucleusframework.desktop.application.dsl.ReleaseType
import dev.nucleusframework.desktop.application.dsl.TargetFormat

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

val releaseVersion =
    System.getenv("RELEASE_VERSION")
        ?.removePrefix("v")
        ?.takeIf { it.isNotBlank() && it.first().isDigit() }
        ?: "1.0.0"

val nativePackageVersion = releaseVersion.substringBefore("-")

sentry {
    includeSourceContext = true
    org = System.getenv("SENTRY_ORG") ?: "kdroidfilter"
    projectName = System.getenv("SENTRY_PROJECT") ?: "kotlin"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
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
            implementation(project(":ui"))
            implementation(libs.composemediaplayer)

            // Coil
            implementation(libs.coil)
            implementation(libs.coil.network)

            // DI - Metro
            implementation(libs.metro.runtime)
            implementation(libs.metro.viewmodel)
            implementation(libs.metro.viewmodel.compose)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)

            // Nucleus
            implementation(libs.nucleus.application)
            implementation(libs.nucleus.core.runtime)
            implementation(libs.nucleus.aot.runtime)
            implementation(libs.nucleus.darkmode.detector)
            implementation(libs.nucleus.updater.runtime)
            implementation(libs.nucleus.native.http)
            implementation(libs.nucleus.graalvm.runtime)
            implementation(libs.nucleus.decorated.window.tao)
            implementation(libs.nucleus.notification.common)
            implementation(libs.nucleus.autolaunch)
            implementation(libs.nucleus.energy.manager)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)

            // Settings
            implementation(libs.multiplatform.settings)

            implementation(libs.confettikit)

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            // Desktop specific
            implementation(compose.desktop.currentOs)
            implementation(libs.composenativetray)
            implementation(libs.composenativetray.app)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.cardiologist)
            implementation(libs.sentry.core)

            // no external markdown UI renderer; using lightweight parser

            // Project dependencies
            implementation(project(":ytdlp"))
            implementation(project(":ffmpeg"))
            implementation(project(":network"))
            implementation(project(":logging"))
            implementation(project(":youtube-playlist-extractor"))

            // SQLDelight driver
            implementation(libs.sqlDelight.driver.sqlite)
        }
    }
}

nucleus.application {
    mainClass = "io.github.kdroidfilter.ytdlpgui.MainKt"

    graalvm {
        isEnabled = true
    }

    val cleanInstall = project.findProperty("cleanInstall")?.toString()?.toBoolean() ?: false
    val debugLogs = project.findProperty("debugLogs")?.toString()?.toBoolean() ?: false
    jvmArgs += listOf("-DcleanInstall=$cleanInstall", "-DdebugLogs=$debugLogs")


    nativeDistributions {
        vendor = "KDroidFilter"
        targetFormats(
            TargetFormat.Dmg,
            TargetFormat.Nsis,
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.Portable,
            TargetFormat.AppImage,
            TargetFormat.Zip,
        )
        packageName = "AeroDl"
        packageVersion = releaseVersion
        description = "AeroDl"
        homepage = "https://github.com/kdroidFilter/AeroDL"
        cleanupNativeLibs = true
        enableAotCache = true
        compressionLevel = CompressionLevel.Ultra

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
            packageVersion = nativePackageVersion
            portable {
                compressionLevel = CompressionLevel.Normal
            }

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
            packageVersion = nativePackageVersion
        }
        linux {
            packageName = "aerodl"
            iconFile.set(project.file("icons/logo.png"))
            packageVersion = releaseVersion
            debPackageVersion = releaseVersion
            debMaintainer = "kdroidfilter@gmail.com"
            appImage {
                compressionLevel = CompressionLevel.Normal
            }

            signing {
                enabled.set(true)
                silentUpdate.set(true)
                val localSigning = file("packaging/linux-signing.local.properties")
                if (localSigning.isFile) {
                    val props =
                        localSigning
                            .readLines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                            .associate { line ->
                                val i = line.indexOf('=')
                                line.substring(0, i).trim() to line.substring(i + 1).trim()
                            }

                    fun local(name: String): String? = props[name]?.takeIf { it.isNotEmpty() }
                    local("compose.desktop.linux.signing.keyId")?.let { keyId.set(it) }
                    local("compose.desktop.linux.signing.keyFile")?.let { keyFile.set(file(it)) }
                    local("compose.desktop.linux.signing.passphrase")?.let { passphrase.set(it) }
                }
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

tasks.withType<Jar> {
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.EC")
}
