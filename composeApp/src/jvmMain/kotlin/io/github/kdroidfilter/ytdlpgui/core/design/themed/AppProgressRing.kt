package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

/**
 * Indeterminate progress ring.
 */
@Composable
fun AppProgressRing(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.ProgressRing(
            modifier = modifier,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.ProgressRing(
            modifier = modifier,
            size = size,
        )
    }
}

/**
 * Determinate progress ring.
 */
@Composable
fun AppProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.ProgressRing(
            progress = progress,
            modifier = modifier,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.ProgressRing(
            progress = progress,
            modifier = modifier,
            size = size,
        )
    }
}
