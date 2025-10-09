package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.CompositionLocalProvider
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
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Clipboard
import io.github.composefluent.icons.regular.Eye
import io.github.composefluent.icons.regular.EyeOff
import io.github.composefluent.icons.regular.Info
import io.github.composefluent.lightColors
import io.github.kdroidfilter.knotify.builder.AppConfig
import io.github.kdroidfilter.knotify.builder.NotificationInitializer
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.design.icons.Exit_to_app
import io.github.kdroidfilter.ytdlpgui.core.design.icons.Login
import io.github.kdroidfilter.ytdlpgui.di.appModule
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsEvents
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsViewModel
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name
import ytdlpgui.composeapp.generated.resources.menu_hide_window
import ytdlpgui.composeapp.generated.resources.menu_show_window
import ytdlpgui.composeapp.generated.resources.quit
import ytdlpgui.composeapp.generated.resources.settings_auto_launch_title
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_title
import ytdlpgui.composeapp.generated.resources.app_version_label
import java.io.File
import java.util.Locale
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnlyRtl
import io.github.kdroidfilter.ytdlpgui.core.util.errorln
import io.github.kdroidfilter.ytdlpgui.core.util.infoln

@OptIn(ExperimentalTrayAppApi::class, ExperimentalFluentApi::class)
fun main() = application {
    allowComposeNativeTrayLogging = true
    val cleanInstall = System.getProperty("cleanInstall", "false").toBoolean()

    if (cleanInstall) {
        clearJavaTempDir()
    }
//    Locale.setDefault(Locale("he"))
    KoinApplication(application = {
        modules(appModule)
    }) {
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
            val koin = getKoin()
            val autoLaunch = koinInject<AutoLaunch>()
            val existingTrayState = remember { runCatching { koin.get<TrayAppState>() }.getOrNull() }
            val trayAppState = existingTrayState ?: rememberTrayAppState(
                initialWindowSize = DpSize(350.dp, 500.dp),
                initiallyVisible = !autoLaunch.isStartedViaAutostart()
            )
            if (existingTrayState == null) {
                // Register as a singleton in Koin immediately to make it available to DI consumers
                runCatching { koin.declare(trayAppState) }
            }

            // Initialize Coil with native trusted roots
            val imageLoader = koinInject<ImageLoader>()
            SingletonImageLoader.setSafe { imageLoader }

            if (cleanInstall) {
                clearSettings(koin.get())
            }

            val isSingleInstance = SingleInstanceManager.isSingleInstance(
                onRestoreRequest = {
                    trayAppState.show()
                }
            )
            if (!isSingleInstance) exitApplication()

            val downloadManager = koinInject<DownloadManager>()
            val isDownloading by downloadManager.isDownloading.collectAsState()

            val settingsVm = koinViewModel<SettingsViewModel>()
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
                            settingsVm.onEvents(SettingsEvents.SetAutoLaunchEnabled(checked))
                        },
                    )
                    CheckableItem(
                        label = runBlocking { getString(Res.string.settings_clipboard_monitoring_title) },
                        checked = clipboardEnabled,
                        onCheckedChange = { checked ->
                            settingsVm.onEvents(SettingsEvents.SetClipboardMonitoring(checked))
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
                        App()
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
