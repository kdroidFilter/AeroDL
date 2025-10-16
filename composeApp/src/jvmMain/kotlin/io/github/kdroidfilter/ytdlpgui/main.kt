package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.rememberTrayAppState
import com.kdroid.composetray.utils.SingleInstanceManager
import com.kdroid.composetray.utils.allowComposeNativeTrayLogging
import com.kdroid.composetray.utils.isMenuBarInDarkMode
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors
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
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.createGraph
import io.github.kdroidfilter.ytdlpgui.di.AppGraph
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import io.github.kdroidfilter.ytdlpgui.di.TrayAppStateHolder
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsEvents
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.*
import java.io.File

@OptIn(ExperimentalTrayAppApi::class, ExperimentalFluentApi::class)
fun main() = application {
    allowComposeNativeTrayLogging = LoggerConfig.enabled
    val cleanInstall = System.getProperty("cleanInstall", "false").toBoolean()

    if (cleanInstall) {
        clearJavaTempDir()
    }
//    Locale.setDefault(Locale("en"))
    val appGraph = remember { createGraph<AppGraph>() }
    run {
        val vmStore = remember { ViewModelStore() }
        val viewModelStoreOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = vmStore
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
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

            val settingsVm = appGraph.settingsViewModel
            val autoStartEnabled by settingsVm.autoLaunchEnabled.collectAsState()
            val clipboardEnabled by settingsVm.clipboardMonitoring.collectAsState()

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
                        tint = if (isDownloading) FluentTheme.colors.system.success else {
                            if (isMenuBarInDarkMode()) Color.White else Color.Black
                        }
                    )
                },
                tooltip = runBlocking { getString(Res.string.app_name) } + if (isDownloading) " - Downloading..." else "",
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
        }

    }
}

fun clearJavaTempDir() {
    val tmpDir = System.getProperty("java.io.tmpdir")
    val dir = File(tmpDir)
    dir.listFiles()?.forEach { file ->
        try {
            if (file.isDirectory) file.deleteRecursively()
            else file.delete()
        } catch (e: Exception) {
            errorln { "Failed to delete ${file.absolutePath}: ${e.message}" }
        }
    }
    infoln { "Cache cleared: $tmpDir" }
}

private fun clearSettings(settings: Settings) {
    settings.clear()
    infoln { "Settings cleared" }
}
