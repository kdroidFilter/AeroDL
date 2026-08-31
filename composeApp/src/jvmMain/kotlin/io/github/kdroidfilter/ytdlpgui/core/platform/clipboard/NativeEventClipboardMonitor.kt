package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

import io.github.kdroidfilter.logging.warnln
import io.github.kdroidfilter.ytdlpgui.nativeclipboard.NativeClipboardWatcher

/**
 * Clipboard monitor backed by a Kotlin/Native OS listener (Win32
 * `WM_CLIPBOARDUPDATE` or macOS pasteboard cache invalidation). Falls back to
 * polling if the native listener cannot be started.
 */
class NativeEventClipboardMonitor(
    private val listener: ClipboardListener,
) : ClipboardMonitor {
    private var watcher: NativeClipboardWatcher? = null
    private var fallback: PollingClipboardMonitor? = null
    private var lastText: String? = null

    override fun start() {
        try {
            val native = NativeClipboardWatcher()
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
