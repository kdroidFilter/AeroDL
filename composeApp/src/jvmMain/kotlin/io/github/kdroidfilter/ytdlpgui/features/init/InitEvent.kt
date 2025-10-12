package io.github.kdroidfilter.ytdlpgui.features.init

sealed class InitEvent {
    data object IgnoreUpdate : InitEvent()
    data object DismissUpdateInfo : InitEvent()
    data object StartInitialization : InitEvent()
}