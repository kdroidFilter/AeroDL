package io.github.kdroidfilter.ytdlpgui.core.ui.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica

@Composable
fun PreviewContainer(content: @Composable () -> Unit) {
    FluentTheme {
        Mica(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}