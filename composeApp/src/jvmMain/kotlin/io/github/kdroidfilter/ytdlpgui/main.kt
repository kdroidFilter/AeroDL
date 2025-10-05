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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import com.kdroid.composetray.tray.api.rememberTrayAppState
import com.kdroid.composetray.utils.isMenuBarInDarkMode
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.darkColors
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.PictureInPictureExit
import io.github.composefluent.lightColors
import io.github.kdroidfilter.knotify.builder.AppConfig
import io.github.kdroidfilter.knotify.builder.NotificationInitializer
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.di.appModule
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name

@OptIn(ExperimentalTrayAppApi::class, ExperimentalFluentApi::class)
fun main() = application {
//    Locale.setDefault(Locale("he", "US"))

    KoinApplication(application = {
        modules(appModule)
    }) {
        NotificationInitializer.configure(
            AppConfig(
                appName = runBlocking { getString(Res.string.app_name) },
            )
        )
        val koin = getKoin()
        val existingTrayState = remember { runCatching { koin.get<TrayAppState>() }.getOrNull() }
        val trayAppState = existingTrayState ?: rememberTrayAppState(
            initialWindowSize = DpSize(350.dp, 500.dp),
            initiallyVisible = true
        )
        if (existingTrayState == null) {
            // Register as a singleton in Koin immediately to make it available to DI consumers
            runCatching { koin.declare(trayAppState) }
        }
        val trayVisible by trayAppState.isVisible.collectAsState()

        TrayApp(
            state = trayAppState,
            iconContent ={ Icon(AeroDlLogoOnly, "", modifier = Modifier.padding(12.dp).fillMaxSize(), tint = if (isMenuBarInDarkMode()) Color.White else Color.Black) },
            tooltip = "YTDLP GUI",
            menu = {
                CheckableItem("Ouvrir au démarrage", checked = true, onCheckedChange = {})
                if (!trayVisible) Item("Afficher la fenetre", onClick = { trayAppState.show() })
                else Item("Cacher la fenetre", onClick = { trayAppState.hide() })
                Item("Quitter", onClick = { exitApplication() }, icon = Icons.Filled.PictureInPictureExit)
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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