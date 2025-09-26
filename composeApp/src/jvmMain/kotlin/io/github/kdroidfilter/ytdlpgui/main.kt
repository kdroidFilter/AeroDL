package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.component.Button
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Window

@OptIn(ExperimentalTrayAppApi::class)
fun main() = application {
    TrayApp(
        icon = Icons.Default.Window,
        tooltip = "YTDLP GUI",
        windowSize = DpSize(300.dp, 500.dp),
    ) {
        FluentTheme {
            Mica(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    Column {
                        Text("Bonjour ! Ceci est une fenêtre d'exemple.")
                        Button(
                            onClick = { exitApplication() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Quitter")
                        }
                    }
                }
            }
        }
    }
}