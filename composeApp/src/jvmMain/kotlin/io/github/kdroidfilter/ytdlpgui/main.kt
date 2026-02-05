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
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import io.github.kdroidfilter.ytdlpgui.di.rememberWindowViewModelStoreOwner
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsViewModel
import coil3.SingletonImageLoader
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import com.kdroid.composetray.tray.api.rememberTrayAppState
import com.kdroid.composetray.utils.SingleInstanceManager
import com.kdroid.composetray.utils.allowComposeNativeTrayLogging
import com.kdroid.composetray.utils.isMenuBarInDarkMode
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.createGraph
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors
import io.github.kdroidfilter.darwinui.theme.DarwinTheme
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme
import androidx.compose.foundation.background
import io.github.kdroidfilter.knotify.builder.AppConfig
import io.github.kdroidfilter.knotify.builder.NotificationInitializer
import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnlyRtl
import io.github.kdroidfilter.ytdlpgui.di.AppGraph
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import io.github.kdroidfilter.ytdlpgui.di.TrayAppStateHolder
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsEvents
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.*
import java.io.File

@OptIn(ExperimentalTrayAppApi::class, ExperimentalFluentApi::class)
fun main() {
    // AOT training: auto-exit after the specified duration so JVM shutdown hooks
    // (which write .aotconf) run reliably on all platforms — including Windows where
    // external signals (taskkill, Process.destroy) cannot trigger them.
    System.getProperty("aot.training.autoExit")?.toLongOrNull()?.let { seconds ->
        Thread({
            Thread.sleep(seconds * 1000)
            System.exit(0)
        }, "aot-training-timer").apply { isDaemon = true; start() }
    }

    // Configure Skiko render API based on platform (respect pre-set -D flag)
    if (System.getProperty("skiko.renderApi") == null) {
        when (getOperatingSystem()) {
            OperatingSystem.WINDOWS -> {
                if (isWindows10()) {
                    System.setProperty("skiko.renderApi", "OPENGL")
                } else {
                    System.setProperty("skiko.renderApi", "DIRECT3D")
                }
            }
            OperatingSystem.LINUX -> if (isNvidiaGpuPresent()) {
                System.setProperty("skiko.renderApi", "SOFTWARE")
            }
            else -> { /* Use default render API */ }
        }
    }

    application {
        allowComposeNativeTrayLogging = LoggerConfig.enabled
        val cleanInstall = System.getProperty("cleanInstall", "false").toBoolean()
        SingleInstanceManager.configuration = SingleInstanceManager.Configuration(
            lockIdentifier = "aerodl"
        )

        FileKit.init(appId = "ada57c09-11e1-4d56-9d5d-0c480f6968ec")

        if (cleanInstall) {
            clearAppData()
        }

//    Locale.setDefault(Locale("en"))
        val appGraph = remember { createGraph<AppGraph>() }
        run {
            val windowViewModelOwner = rememberWindowViewModelStoreOwner()
            CompositionLocalProvider(
                LocalWindowViewModelStoreOwner provides windowViewModelOwner,
                LocalViewModelStoreOwner provides windowViewModelOwner,
                LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
            ) {
                NotificationInitializer.configure(
                    AppConfig(
                        appName = runBlocking { getString(Res.string.app_name) },
                    )
                )
                val autoLaunch = appGraph.autoLaunch
                val trayAppState = rememberTrayAppState(
                    initialWindowSize = DpSize(350.dp, 500.dp),
                    initiallyVisible = !autoLaunch.isStartedViaAutostart()
                )
                TrayAppStateHolder.set(trayAppState)

                // Eagerly instantiate clipboard monitoring once, as a side effect
                LaunchedEffect(appGraph) {
                    appGraph.clipboardMonitorManager
                }

                // Initialize Coil with native trusted roots
                val imageLoader = appGraph.imageLoader
                SingletonImageLoader.setSafe { imageLoader }

                if (cleanInstall) {
                    clearSettings(appGraph.settings)
                }


                val isSingleInstance = SingleInstanceManager.isSingleInstance(
                    onRestoreRequest = {
                        trayAppState.show()
                    }
                )
                if (!isSingleInstance) exitApplication()


                val downloadManager = appGraph.downloadManager
                val isDownloading by downloadManager.isDownloading.collectAsState()

                val settingsVm: SettingsViewModel = metroViewModel(
                    viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
                )
                val autoStartEnabled by settingsVm.autoLaunchEnabled.collectAsState()
                val clipboardEnabled by settingsVm.clipboardMonitoring.collectAsState()
                val appTheme by settingsVm.appTheme.collectAsState()

                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                TrayApp(
                    state = trayAppState,
                    iconContent = {

                        Icon(
                            if (!isRtl) AeroDlLogoOnly else AeroDlLogoOnlyRtl,
                            null,
                            modifier = Modifier
                                .padding(if (getOperatingSystem() != OperatingSystem.WINDOWS) 12.dp else 2.dp)
                                .fillMaxSize(),
                            tint = if (isDownloading) Color(0xFF0F7B0F) else {
                                if (isMenuBarInDarkMode()) Color.White else Color.Black
                            }
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
                            label = runBlocking { getString(Res.string.app_version_label, getAppVersion()) },
                            isEnabled = false,
                        )
                    }
                ) {

                   println(this.window.renderApi)
                    CompositionLocalProvider(LocalAppTheme provides appTheme) {
                        when (appTheme) {
                            AppTheme.FLUENT -> {
                                FluentTheme(colors = if (isSystemInDarkMode()) darkColors() else lightColors()) {
                                    Mica(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                if (isSystemInDarkMode()) Color.DarkGray else Color.LightGray,
                                                RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        CompositionLocalProvider(LocalAppGraph provides appGraph) {
                                            App()
                                        }
                                    }
                                }
                            }
                            AppTheme.DARWIN -> {
                                DarwinTheme(darkTheme = isSystemInDarkMode()) {
                                    androidx.compose.foundation.layout.Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarwinTheme.colors.background)
                                            .border(
                                                1.dp,
                                                DarwinTheme.colors.border,
                                                RoundedCornerShape(12.dp)
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
            }

        }
    }
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
                errorln { "Failed to delete ${file.absolutePath}: ${e.message}" }
            }
        }
    }
    infoln { "App data cleared: ${dataDir.absolutePath}" }
}

private fun clearSettings(settings: Settings) {
    settings.clear()
    infoln { "Settings cleared" }
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
