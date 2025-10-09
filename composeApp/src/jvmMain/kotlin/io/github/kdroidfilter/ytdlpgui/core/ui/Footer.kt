package io.github.kdroidfilter.ytdlpgui.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text
import io.github.composefluent.component.TooltipBox
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.Heart
import io.github.kdroidfilter.ytdlpgui.core.design.icons.Github
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.tooltip_github
import ytdlpgui.composeapp.generated.resources.tooltip_support_kofi

@OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)
@Composable
fun Footer(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_github)) }) {
            SubtleButton(
                iconOnly = true,
                onClick = { openUrlInBrowser("https://github.com/kdroidFilter/ytdlpgui") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(Github, contentDescription = null)
            }
        }
        Spacer(Modifier.width(8.dp))
        TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_support_kofi)) }) {
            SubtleButton(
                iconOnly = true,
                onClick = { openUrlInBrowser("https://ko-fi.com/lomityaesh") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(Icons.Filled.Heart, contentDescription = null)
            }
        }
    }
}
