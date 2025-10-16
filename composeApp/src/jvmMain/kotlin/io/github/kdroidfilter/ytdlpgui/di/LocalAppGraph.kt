package io.github.kdroidfilter.ytdlpgui.di

import androidx.compose.runtime.compositionLocalOf

val LocalAppGraph = compositionLocalOf<AppGraph> {
    error("No AppGraph provided")
}