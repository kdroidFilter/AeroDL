package io.github.kdroidfilter.ytdlpgui.core.config

import androidx.compose.runtime.compositionLocalOf

enum class AppTheme {
    FLUENT,
    DARWIN;
}

val LocalAppTheme = compositionLocalOf { AppTheme.FLUENT }
