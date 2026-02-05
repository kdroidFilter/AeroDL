package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppTooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.TooltipBox(
            tooltip = { AppText(tooltip) },
            modifier = modifier,
            content = content,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.Tooltip(
            text = tooltip,
            modifier = modifier,
            content = content,
        )
    }
}
