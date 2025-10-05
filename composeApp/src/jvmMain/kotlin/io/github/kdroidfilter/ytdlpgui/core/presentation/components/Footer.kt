package io.github.kdroidfilter.ytdlpgui.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import io.github.composefluent.component.SubtleButton
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.Github
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalFluentApi::class)
@Composable
fun Footer(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SubtleButton(
            iconOnly = true,
            onClick = {
                val url = "https://github.com/kdroidFilter/ytdlpgui"
                try {
                    val desktop = Desktop.getDesktop()
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(URI(url))
                    }
                } catch (_: Exception) {
                    // no-op: ignore failures to open browser
                }
            },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon(Github, contentDescription = null)
        }
    }
}
