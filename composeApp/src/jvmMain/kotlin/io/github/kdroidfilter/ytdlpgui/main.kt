package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil3.SingletonImageLoader
import com.russhwolf.settings.Settings
import dev.nucleusframework.application.SingleInstanceRestoreEffect
import dev.nucleusframework.application.aotTraining
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.autolaunch.AutoLaunch
import dev.nucleusframework.composenativetray.trayapp.TrayApp
import dev.nucleusframework.composenativetray.trayapp.rememberTrayAppState
import dev.nucleusframework.composenativetray.utils.allowComposeNativeTrayLogging
import dev.nucleusframework.composenativetray.utils.isMenuBarInDarkMode
import dev.nucleusframework.core.runtime.NucleusApp
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.core.runtime.SingleInstanceManager
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode
import dev.nucleusframework.energymanager.EnergyManager
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.component.NativeBackground
import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnlyRtl
import io.github.kdroidfilter.ytdlpgui.di.AppGraph
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import io.github.kdroidfilter.ytdlpgui.di.TrayAppStateHolder
import io.github.kdroidfilter.ytdlpgui.di.rememberWindowViewModelStoreOwner
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsEvents
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.path
import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.*
import java.io.File
import kotlin.time.Duration.Companion.seconds


fun main(args: Array<String>) {
    initializeSentry()

    // Configure Skiko render API based on platform (respect pre-set -D flag)
    if (System.getProperty("skiko.renderApi") == null) {
        when (Platform.Current) {
            Platform.Windows -> {
                if (isWindows10()) {
                    System.setProperty("skiko.renderApi", "OPENGL")
                } else {
                    System.setProperty("skiko.renderApi", "DIRECT3D")
                }
            }
            Platform.Linux -> if (isNvidiaGpuPresent()) {
                System.setProperty("skiko.renderApi", "SOFTWARE")
            }
            else -> { /* Use default render API */ }
        }
    }

    allowComposeNativeTrayLogging = LoggerConfig.enabled
    SingleInstanceManager.configuration = SingleInstanceManager.Configuration(
        lockIdentifier = "aerodl"
    )

    FileKit.init(appId = "ada57c09-11e1-4d56-9d5d-0c480f6968ec")

    val cleanInstall = System.getProperty("cleanInstall", "false").toBoolean()
    if (cleanInstall) {
        clearAppData()
    }

    nucleusApplication(args, dockIconFollowsWindows = true) {
        aotTraining(duration = 30.seconds)

        val nucleusScope = this
        val windowViewModelOwner = rememberWindowViewModelStoreOwner()
        val appGraph = remember { createGraph<AppGraph>() }

        CompositionLocalProvider(
            LocalWindowViewModelStoreOwner provides windowViewModelOwner,
            LocalViewModelStoreOwner provides windowViewModelOwner,
            LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
        ) {
            val startedAtLogin = remember { AutoLaunch.wasStartedAtLogin(args) }
            val trayAppState = rememberTrayAppState(
                initialWindowSize = DpSize(350.dp, 500.dp),
                initiallyVisible = !startedAtLogin,
            )
            TrayAppStateHolder.set(trayAppState)

            if (startedAtLogin) {
                infoln { "Launched from autostart: starting in tray" }
            }

            // macOS delivers the login AppleEvent after NSApplication.run();
            // wasStartedAtLogin caches positives, so poll once the Compose loop is up.
            LaunchedEffect(trayAppState) {
                if (startedAtLogin || Platform.Current != Platform.MacOS) return@LaunchedEffect
                repeat(20) {
                    delay(100)
                    if (AutoLaunch.wasStartedAtLogin(args)) {
                        infoln { "Launched from autostart: hiding window" }
                        trayAppState.hide()
                        return@LaunchedEffect
                    }
                }
            }

            LaunchedEffect(trayAppState) {
                trayAppState.isVisible.collect { visible ->
                    applyEnergyEfficiencyForVisibility(visible)
                    if (!visible) {
                        delay(300)
                        infoln { "Window hidden: hinting GC" }
                        try {
                            System.gc()
                        } catch (_: Throwable) {
                            // ignore
                        }
                    }
                }
            }

            SingleInstanceRestoreEffect {
                trayAppState.show()
            }

            // Eagerly instantiate clipboard monitoring once, as a side effect
            LaunchedEffect(appGraph) {
                appGraph.clipboardMonitorManager
            }

            // Initialize Coil with native trusted roots
            val imageLoader = appGraph.imageLoader
            SingletonImageLoader.setSafe { imageLoader }

            if (cleanInstall) {
                remember {
                    clearSettings(appGraph.settings)
                    true
                }
            }

            val downloadManager = appGraph.downloadManager
            val isDownloading by downloadManager.isDownloading.collectAsState()

            val settingsVm: SettingsViewModel = metroViewModel(
                viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
            )
            val autoStartEnabled by settingsVm.autoLaunchEnabled.collectAsState()
            val clipboardEnabled by settingsVm.clipboardMonitoring.collectAsState()

            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

            nucleusScope.TrayApp(
                state = trayAppState,
                iconContent = {
                    Icon(
                        if (!isRtl) AeroDlLogoOnly else AeroDlLogoOnlyRtl,
                        null,
                        modifier = Modifier
                            .padding(if (Platform.Current != Platform.Windows) 12.dp else 2.dp)
                            .fillMaxSize(),
                        tint = if (isDownloading) {
                            Color(0xFF0E8420)
                        } else if (isMenuBarInDarkMode()) {
                            Color.White
                        } else {
                            Color.Black
                        },
                    )
                },
                tooltip = runBlocking { getString(Res.string.app_name) } + if (isDownloading) runBlocking {
                    getString(
                        Res.string.tray_downloading_suffix
                    )
                } else "",
                menu = {
                    if (!trayAppState.isVisible.value) Item(
                        label = runBlocking { getString(Res.string.menu_show_window) },
                    ) { trayAppState.show() } else Item(
                        label = runBlocking { getString(Res.string.menu_hide_window) },
                    ) { trayAppState.hide() }
                    Divider()
                    CheckableItem(
                        label = runBlocking { getString(Res.string.settings_auto_launch_title) },
                        checked = autoStartEnabled,
                        onCheckedChange = { checked ->
                            settingsVm.handleEvent(SettingsEvents.SetAutoLaunchEnabled(checked))
                        },
                    )
                    CheckableItem(
                        label = runBlocking { getString(Res.string.settings_clipboard_monitoring_title) },
                        checked = clipboardEnabled,
                        onCheckedChange = { checked ->
                            settingsVm.handleEvent(SettingsEvents.SetClipboardMonitoring(checked))
                        },
                    )
                    Divider()
                    Item(
                        label = runBlocking { getString(Res.string.quit) },
                        onClick = { exitApplication() },
                    )
                    Item(
                        label = runBlocking { getString(Res.string.app_version_label, NucleusApp.version.orEmpty()) },
                        isEnabled = false,
                    )
                }
            ) {
                NativeTheme(darkTheme = isSystemInDarkMode()) {
                    val windowShape = RoundedCornerShape(12.dp)
                    NativeBackground(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (NativeTheme.drawsWindowChrome) {
                                    Modifier
                                } else {
                                    Modifier
                                        .clip(windowShape)
                                        .border(
                                            1.dp,
                                            if (isSystemInDarkMode()) Color.DarkGray else Color.LightGray,
                                            windowShape,
                                        )
                                },
                            )
                    ) {
                        CompositionLocalProvider(LocalAppGraph provides appGraph) {
                            App()
                        }
                    }
                }
            }
        }
    }
}

private fun initializeSentry() {
    val sentryEnvironment = System.getenv("SENTRY_ENVIRONMENT")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "development"

    Sentry.init { options ->
        options.dsn = "https://e77a755df2930d297caf9d6d0fd07deb@o4510855773093888.ingest.de.sentry.io/4510855774797904"
        options.environment = sentryEnvironment
        options.release = NucleusApp.version
        options.isDebug = LoggerConfig.enabled
    }
    infoln { "Sentry initialized for environment '$sentryEnvironment'." }
}

fun clearAppData() {
    // Clear only AeroDL's data directory (yt-dlp/FFmpeg binaries)
    // Do NOT clear the shared java.io.tmpdir as it affects other applications
    val dataDir = File(FileKit.databasesDir.path)
    if (dataDir.exists()) {
        dataDir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) file.deleteRecursively()
                else file.delete()
            } catch (e: Exception) {
                errorln(e) { "Failed to delete ${file.absolutePath}: ${e.message}" }
            }
        }
    }
    infoln { "App data cleared: ${dataDir.absolutePath}" }
}

private fun clearSettings(settings: Settings) {
    settings.clear()
    infoln { "Settings cleared" }
}

/**
 * Pins the process to efficiency cores (EcoQoS / PRIO_DARWIN_BG / nice)
 * while the window is hidden, and restores default scheduling when shown.
 */
private fun applyEnergyEfficiencyForVisibility(visible: Boolean) {
    if (!EnergyManager.isAvailable()) return
    val result = if (visible) {
        EnergyManager.disableEfficiencyMode()
    } else {
        EnergyManager.enableEfficiencyMode()
    }
    if (result.success) {
        infoln {
            if (visible) {
                "Window visible: restored default CPU scheduling"
            } else {
                "Window hidden: using efficiency cores"
            }
        }
    } else if (result.message.isNotEmpty()) {
        infoln { "EnergyManager: ${result.message}" }
    }
}

private fun isWindows10(): Boolean {
    val osName = System.getProperty("os.name", "").lowercase()
    return osName.contains("windows 10")
}

private fun isNvidiaGpuPresent(): Boolean {
    // Check if NVIDIA driver is loaded by looking for the driver version file
    val nvidiaDriverFile = File("/proc/driver/nvidia/version")
    if (nvidiaDriverFile.exists()) return true

    // Fallback: try running nvidia-smi
    return try {
        val process = ProcessBuilder("nvidia-smi", "-L")
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        exitCode == 0
    } catch (_: Exception) {
        false
    }
}
