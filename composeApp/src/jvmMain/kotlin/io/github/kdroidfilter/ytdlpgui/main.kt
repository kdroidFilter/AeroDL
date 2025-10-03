package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import com.kdroid.composetray.tray.api.rememberTrayAppState
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.darkColors
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.PictureInPictureExit
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.lightColors
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.di.appModule
import org.koin.compose.KoinApplication
import java.util.Locale

@OptIn(ExperimentalTrayAppApi::class, ExperimentalFluentApi::class)
fun main() = application {
//    Locale.setDefault(Locale("he", "US"))

    KoinApplication(application = {
        modules(appModule)
    }) {

        val trayAppState = rememberTrayAppState(
            initialWindowSize = DpSize(350.dp, 500.dp),
            initiallyVisible = true
        )
        val trayVisible by trayAppState.isVisible.collectAsState()


        TrayApp(
            state = trayAppState,
            icon = AeroDlLogoOnly,
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