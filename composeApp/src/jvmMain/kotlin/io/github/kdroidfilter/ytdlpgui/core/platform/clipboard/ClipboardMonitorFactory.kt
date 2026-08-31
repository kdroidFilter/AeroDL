package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

import dev.nucleusframework.core.runtime.Platform

internal fun createClipboardMonitor(listener: ClipboardListener): ClipboardMonitor =
    if (Platform.Current == Platform.Windows) {
        WindowsEventClipboardMonitor(listener)
    } else {
        PollingClipboardMonitor(listener)
    }
