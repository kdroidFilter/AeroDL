package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

data class ClipboardContent(val text: String?)

fun interface ClipboardListener {
    fun onClipboardChange(content: ClipboardContent)
}

interface ClipboardMonitor {
    fun start()
    fun stop()
}
