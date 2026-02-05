package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.composefluent.FluentTheme
import io.github.kdroidfilter.darwinui.theme.DarwinTheme
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

object AppColors {

    val textPrimary: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.text.text.primary
            AppTheme.DARWIN -> DarwinTheme.colors.textPrimary
        }

    val textSecondary: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.text.text.secondary
            AppTheme.DARWIN -> DarwinTheme.colors.textSecondary
        }

    val textTertiary: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.text.text.tertiary
            AppTheme.DARWIN -> DarwinTheme.colors.textTertiary
        }

    val textDisabled: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.text.text.disabled
            AppTheme.DARWIN -> DarwinTheme.colors.textQuaternary
        }

    val onAccentPrimary: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.text.onAccent.primary
            AppTheme.DARWIN -> DarwinTheme.colors.onAccent
        }

    val onAccentDisabled: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.text.onAccent.disabled
            AppTheme.DARWIN -> DarwinTheme.colors.textQuaternary
        }

    val success: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.system.success
            AppTheme.DARWIN -> DarwinTheme.colors.success
        }

    val critical: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.system.critical
            AppTheme.DARWIN -> DarwinTheme.colors.destructive
        }

    val neutral: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.system.neutral
            AppTheme.DARWIN -> DarwinTheme.colors.textPrimary
        }

    val fillAccentDefault: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.fillAccent.default
            AppTheme.DARWIN -> DarwinTheme.colors.accent
        }

    val controlSecondary: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.control.secondary
            AppTheme.DARWIN -> DarwinTheme.colors.secondary
        }

    val backgroundDefault: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.background.layer.default
            AppTheme.DARWIN -> DarwinTheme.colors.background
        }

    val strokeControlDefault: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.stroke.control.default
            AppTheme.DARWIN -> DarwinTheme.colors.border
        }

    val controlStrongDefault: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.controlStrong.default
            AppTheme.DARWIN -> DarwinTheme.colors.borderStrong
        }

    val warning: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.system.caution
            AppTheme.DARWIN -> DarwinTheme.colors.warning
        }

    val info: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.system.attention
            AppTheme.DARWIN -> DarwinTheme.colors.info
        }

    val border: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.stroke.control.default
            AppTheme.DARWIN -> DarwinTheme.colors.border
        }

    val card: Color
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> FluentTheme.colors.background.card.default
            AppTheme.DARWIN -> DarwinTheme.colors.card
        }
}
