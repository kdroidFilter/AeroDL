package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.Button(
            onClick = onClick,
            modifier = modifier,
            disabled = !enabled,
        ) { content() }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
    }
}

@Composable
fun AppAccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.AccentButton(
            onClick = onClick,
            modifier = modifier,
            disabled = !enabled,
        ) { content() }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.PrimaryButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
    }
}

@Composable
fun AppSubtleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.SubtleButton(
            onClick = onClick,
            modifier = modifier,
            disabled = !enabled,
        ) { content() }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.SubtleButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
    }
}

@Composable
fun AppHyperlinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.SubtleButton(
            onClick = onClick,
            modifier = modifier,
            disabled = !enabled,
        ) { content() }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.HyperlinkButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
    }
}
