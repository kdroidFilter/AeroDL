package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.util.concurrent.atomic.AtomicBoolean

data class ClipboardContent(val text: String?)

fun interface ClipboardListener {
    fun onClipboardChange(content: ClipboardContent)
}

/**
 * Polls the system clipboard for plain text.
 *
 * Uses the same AWT clipboard that Compose Desktop exposes via [androidx.compose.ui.platform.LocalClipboard].
 */
class PollingClipboardMonitor(
    private val listener: ClipboardListener,
    private val intervalMs: Long = 500,
) {
    private val running = AtomicBoolean(false)
    @Volatile private var worker: Thread? = null
    private var lastText: String? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        worker = Thread({
            while (running.get()) {
                val text = readClipboardText()
                if (text != lastText) {
                    lastText = text
                    runCatching { listener.onClipboardChange(ClipboardContent(text)) }
                }
                runCatching { Thread.sleep(intervalMs) }
            }
        }, "clipboard-monitor").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        worker?.interrupt()
        worker = null
        lastText = null
    }

    fun getCurrentContent(): ClipboardContent = ClipboardContent(readClipboardText())

    private fun readClipboardText(): String? = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) return@runCatching null
        clipboard.getData(DataFlavor.stringFlavor) as? String
    }.getOrNull()
}
