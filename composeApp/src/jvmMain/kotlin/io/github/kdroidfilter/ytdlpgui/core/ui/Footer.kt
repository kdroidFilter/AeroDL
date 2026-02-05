package io.github.kdroidfilter.ytdlpgui.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.design.icons.Github
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSubtleButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTooltip
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.tooltip_github
import ytdlpgui.composeapp.generated.resources.tooltip_support_kofi

@Composable
fun Footer(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AppTooltip(tooltip = stringResource(Res.string.tooltip_github)) {
            AppSubtleButton(
                onClick = { openUrlInBrowser("https://github.com/kdroidFilter/ytdlpgui") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                AppIcon(Github, contentDescription = null)
            }
        }
        Spacer(Modifier.width(8.dp))
        AppTooltip(tooltip = stringResource(Res.string.tooltip_support_kofi)) {
            AppSubtleButton(
                onClick = { openUrlInBrowser("https://ko-fi.com/lomityaesh") },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                AppIcon(AppIcons.Heart, contentDescription = null)
            }
        }
    }
}
