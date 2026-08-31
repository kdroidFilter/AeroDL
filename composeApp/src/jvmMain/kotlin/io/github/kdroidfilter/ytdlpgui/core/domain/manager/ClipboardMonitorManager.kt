package io.github.kdroidfilter.ytdlpgui.core.domain.manager

import dev.zacsweers.metro.Inject
import io.github.kdroidfilter.ytdlpgui.core.platform.clipboard.ClipboardContent
import io.github.kdroidfilter.ytdlpgui.core.platform.clipboard.ClipboardListener
import io.github.kdroidfilter.ytdlpgui.core.platform.clipboard.ClipboardMonitor
import io.github.kdroidfilter.ytdlpgui.core.platform.clipboard.createClipboardMonitor
import dev.nucleusframework.notification.common.notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.nucleusframework.composenativetray.trayapp.TrayAppState
import dev.nucleusframework.composenativetray.trayapp.TrayWindowDismissMode
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name
import ytdlpgui.composeapp.generated.resources.clipboard_ignore
import ytdlpgui.composeapp.generated.resources.clipboard_link_detected_message
import ytdlpgui.composeapp.generated.resources.clipboard_link_detected_title
import ytdlpgui.composeapp.generated.resources.clipboard_open_in_app
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.di.applyDefaultDismissMode

/**
 * Manages clipboard monitoring functionality, allowing detection and handling
 * of specific clipboard content such as URLs. The class listens for changes
 * in the clipboard and processes the content according to certain rules.
 *
 * This manager integrates with various components like settings, navigation,
 * and supported sites repository to provide user prompts or actions based on
 * recognized content (e.g., links to known or YouTube sites).
 *
 * The monitoring can be enabled or disabled via settings and includes
 * handling of corner cases such as bulk links (e.g., playlists or channels)
 * for YouTube URLs.
 *
 * Primary responsibilities:
 * - Starting and stopping clipboard monitoring based on user-configured settings.
 * - Processing clipboard changes to extract and validate URL content.
 * - Coordinating user interaction through notifications for recognized URLs.
 *
 * @constructor Initializes the manager with the required dependencies.
 * @param settingsRepository Provides access to user-configurable settings.
 * @param trayAppState Manages the application tray state for notifications and UI interaction.
 * @param supportedSitesRepository Repository containing information about recognized or supported sites.
 */
@Inject
class ClipboardMonitorManager(
    private val settingsRepository: io.github.kdroidfilter.ytdlpgui.data.SettingsRepository,
    private val trayAppState: TrayAppState,
    private val navigationEventBus: io.github.kdroidfilter.ytdlpgui.core.navigation.NavigationEventBus,
) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private var monitor: ClipboardMonitor? = null
    private var lastHandled: String? = null

    init {
        // Start immediately if enabled in persisted settings
        val enabled = settingsRepository.clipboardMonitoringEnabled.value
        if (enabled) start()
    }

    fun onSettingChanged(enabled: Boolean) {
        if (enabled) start() else stop()
    }

    private fun start() {
        if (monitor != null) return
        val listener = ClipboardListener { content ->
            scope.launch { handleContent(content) }
        }
        monitor = createClipboardMonitor(listener).also { it.start() }
    }

    private fun stop() {
        monitor?.stop()
        monitor = null
    }

    private suspend fun handleContent(content: ClipboardContent) {
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

        // Only accept YouTube links for clipboard monitoring
        if (!isYouTube) return

        val isPlaylist = lower.contains("list=") || lower.contains("/playlist")
        val isChannel = lower.contains("/channel/") || lower.contains("/c/") || lower.contains("youtube.com/@")

        // Avoid bulk (playlist/channel) prompts which can be noisy.
        if (isPlaylist || isChannel) return

        if (!settingsRepository.isOnboardingCompleted()) return

        lastHandled = url

        val appName = getString(Res.string.app_name)
        val title = getString(Res.string.clipboard_link_detected_title)
        val message = getString(Res.string.clipboard_link_detected_message, appName, url)
        val openBtn = getString(Res.string.clipboard_open_in_app, appName)
        val ignoreBtn = getString(Res.string.clipboard_ignore)

        fun action() {
            scope.launch(Dispatchers.Main) {
                trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
                runCatching { trayAppState.show() }
                navigationEventBus.navigateTo(Destination.Download.Single(url))
                trayAppState.applyDefaultDismissMode(settingsRepository.disableTrayAutoHide.value)
            }
        }

        notification(
            title = title,
            message = message,
            onActivated = { action() },
        ) {
            button(title = openBtn) { action() }
            button(title = ignoreBtn) { }
        }.send()
    }
}
