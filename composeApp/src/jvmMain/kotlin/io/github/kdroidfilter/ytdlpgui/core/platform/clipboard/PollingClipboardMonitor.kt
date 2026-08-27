package io.github.kdroidfilter.ytdlpgui.core.platform.clipboard

import dev.nucleusframework.energymanager.EnergyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
 * The poll loop runs on an efficiency-core thread (EcoQoS / QOS_CLASS_BACKGROUND / nice)
 * so it does not compete with the UI or downloads on performance cores.
 */
class PollingClipboardMonitor(
    private val listener: ClipboardListener,
    private val intervalMs: Long = 500,
) {
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null
    private var lastText: String? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        job = scope.launch {
            EnergyManager.withEfficiencyMode {
                // Snapshot current clipboard so existing content is not treated as a change
                // (e.g. a URL already copied before the app launched).
                lastText = readClipboardText()
                while (isActive && running.get()) {
                    val text = readClipboardText()
                    if (text != lastText) {
                        lastText = text
                        runCatching { listener.onClipboardChange(ClipboardContent(text)) }
                    }
                    delay(intervalMs)
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        job = null
        scope.cancel()
        lastText = null
    }

    private fun readClipboardText(): String? = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) return@runCatching null
        clipboard.getData(DataFlavor.stringFlavor) as? String
    }.getOrNull()
}
