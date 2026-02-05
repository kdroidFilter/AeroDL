package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.composefluent.FluentTheme
import io.github.kdroidfilter.darwinui.theme.DarwinTheme
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

object AppTypography {

    val body: TextStyle
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.typography.body
            AppTheme.DARWIN -> DarwinTheme.typography.bodyMedium
        }

    val bodyStrong: TextStyle
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.typography.bodyStrong
            AppTheme.DARWIN -> DarwinTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        }

    val subtitle: TextStyle
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.typography.subtitle
            AppTheme.DARWIN -> DarwinTheme.typography.titleLarge
        }

    val caption: TextStyle
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.typography.caption
            AppTheme.DARWIN -> DarwinTheme.typography.caption
        }
}
