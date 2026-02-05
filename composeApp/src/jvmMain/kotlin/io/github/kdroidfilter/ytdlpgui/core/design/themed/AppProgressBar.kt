package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

/**
 * Determinate linear progress bar.
 */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.kdroidfilter.ytdlpgui.core.design.components.ProgressBar(
            progress = progress,
            modifier = modifier,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.LinearProgress(
            value = progress * 100f,
            modifier = modifier,
        )
    }
}

/**
 * Indeterminate linear progress bar.
 */
@Composable
fun AppIndeterminateProgressBar(
    modifier: Modifier = Modifier,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.kdroidfilter.ytdlpgui.core.design.components.ProgressBar(
            modifier = modifier,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.LinearProgress(
            indeterminate = true,
            modifier = modifier,
        )
    }
}
