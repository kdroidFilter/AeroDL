package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

import dev.nucleusframework.core.runtime.Platform

internal fun createClipboardMonitor(listener: ClipboardListener): ClipboardMonitor =
    when (Platform.Current) {
        Platform.Windows, Platform.MacOS -> NativeEventClipboardMonitor(listener)
        else -> PollingClipboardMonitor(listener)
    }
