package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

import io.github.kdroidfilter.logging.warnln
import io.github.kdroidfilter.ytdlpgui.nativeclipboard.WindowsClipboardWatcher

/**
 * Windows clipboard monitor backed by a Kotlin/Native Win32 format listener
 * (`AddClipboardFormatListener` / `WM_CLIPBOARDUPDATE`). Falls back to polling
 * if the native listener cannot be started.
 */
class WindowsEventClipboardMonitor(
    private val listener: ClipboardListener,
) : ClipboardMonitor {
    private var watcher: WindowsClipboardWatcher? = null
    private var fallback: PollingClipboardMonitor? = null
    private var lastText: String? = null

    override fun start() {
        try {
            val native = WindowsClipboardWatcher()
            watcher = native
            lastText = native.readText().ifEmpty { null }
            native.start { text ->
                val normalized = text.ifEmpty { null }
                if (normalized != lastText) {
                    lastText = normalized
                    runCatching { listener.onClipboardChange(ClipboardContent(normalized)) }
                }
            }
        } catch (error: Throwable) {
            warnln(error) { "Native clipboard listener failed, falling back to polling" }
            runCatching { watcher?.close() }
            watcher = null
            fallback = PollingClipboardMonitor(listener).also { it.start() }
        }
    }

    override fun stop() {
        val native = watcher
        watcher = null
        if (native != null) {
            runCatching { native.stop() }
            runCatching { native.close() }
        }
        fallback?.stop()
        fallback = null
        lastText = null
    }
}
