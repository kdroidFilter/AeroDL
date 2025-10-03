package io.github.kdroidfilter.ytdlpgui.core.clipboard

import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardContent
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardListener
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardMonitor
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardMonitorFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App-level clipboard monitor, independent of any screen lifecycle.
 * When enabled in settings, it listens to clipboard changes and navigates
 * to SingleDownloadScreen if a YouTube link is detected.
 */
class ClipboardMonitorManager(
    private val navigator: Navigator,
    private val settings: Settings,
) {
    companion object {
        private const val KEY_CLIPBOARD_MONITORING = "clipboard_monitoring_enabled"
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private var monitor: ClipboardMonitor? = null
    private var lastHandled: String? = null

    init {
        // Start immediately if enabled in persisted settings
        val enabled = settings.getBoolean(KEY_CLIPBOARD_MONITORING, false)
        if (enabled) start()
    }

    fun onSettingChanged(enabled: Boolean) {
        settings.putBoolean(KEY_CLIPBOARD_MONITORING, enabled)
        if (enabled) start() else stop()
    }

    private fun start() {
        if (monitor != null) return
        val listener = object : ClipboardListener {
            override fun onClipboardChange(content: ClipboardContent) {
                scope.launch { handleContent(content) }
            }
        }
        monitor = ClipboardMonitorFactory.create(listener).also { m ->
            m.start()
            // Initial check
            runCatching { m.getCurrentContent()?.let { scope.launch { handleContent(it) } } }
        }
    }

    private fun stop() {
        monitor?.stop()
        monitor = null
    }

    private fun handleContent(content: ClipboardContent) {
        val text = content.text?.trim().orEmpty()
        if (text.isEmpty()) return
        // Extract a single URL from the text
        val urlRegex = Regex("""https?://\S+""")
        val matches = urlRegex.findAll(text).toList()
        if (matches.size != 1) return
        val url = matches.first().value
        if (url == lastHandled) return

        val lower = url.lowercase()
        val isYouTube = listOf("youtube.com", "youtu.be").any { lower.contains(it) }
        if (!isYouTube) return
        val isPlaylist = lower.contains("list=") || lower.contains("/playlist")
        val isChannel = lower.contains("/channel/") || lower.contains("/c/") || (isYouTube && lower.contains("youtube.com/@"))

        // We only auto-open single video links; bulk (playlist/channel) can be noisy.
        if (isPlaylist || isChannel) return

        lastHandled = url
        scope.launch { navigator.navigate(Destination.SingleDownloadScreen(url)) }
    }
}
