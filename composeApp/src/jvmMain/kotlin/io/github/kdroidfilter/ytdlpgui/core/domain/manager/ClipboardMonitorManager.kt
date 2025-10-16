@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.core.domain.manager

import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import dev.zacsweers.metro.Inject
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardContent
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardListener
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardMonitor
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardMonitorFactory
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.compose.builder.notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name
import ytdlpgui.composeapp.generated.resources.clipboard_ignore
import ytdlpgui.composeapp.generated.resources.clipboard_link_detected_message
import ytdlpgui.composeapp.generated.resources.clipboard_link_detected_title
import ytdlpgui.composeapp.generated.resources.clipboard_open_in_app
import androidx.compose.runtime.*
import io.github.kdroidfilter.ytdlpgui.core.platform.notifications.NotificationThumbUtils
import io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination

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
    private val supportedSitesRepository: SupportedSitesRepository,
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
        val listener = object : ClipboardListener {
            override fun onClipboardChange(content: ClipboardContent) {
                scope.launch { handleContent(content) }
            }
        }
        monitor = ClipboardMonitorFactory.create(listener).also { m ->
            m.start()
            // Initial check
            runCatching { m.getCurrentContent().let { scope.launch { handleContent(it) } } }
        }
    }

    private fun stop() {
        monitor?.stop()
        monitor = null
    }

    @OptIn(ExperimentalNotificationsApi::class)
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
        val isKnown = supportedSitesRepository.matchesKnownSite(url)
        if (!isYouTube && !isKnown) return
        val isPlaylist = if (isYouTube) (lower.contains("list=") || lower.contains("/playlist")) else false
        val isChannel = if (isYouTube) (
            lower.contains("/channel/") || lower.contains("/c/") || lower.contains("youtube.com/@")
        ) else false

        // For YouTube, avoid bulk (playlist/channel) prompts which can be noisy.
        if (isYouTube && (isPlaylist || isChannel)) return

        // Mark as handled to avoid repeated prompts for the same URL
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
                // Request navigation to the Single Download screen for this URL
                navigationEventBus.navigateTo(Destination.Download.Single(url))
                trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
            }
        }

        // Show a localized notification asking user consent to open in the app
        val thumbUrl = NotificationThumbUtils.resolveThumbnailUrl(null, url)
        val largeIconContent: (@Composable () -> Unit)? = NotificationThumbUtils.buildLargeIcon(thumbUrl)

        val notif = notification(
            title = title,
            message = message,
            largeIcon = largeIconContent,
            onActivated = { action() },
            onDismissed = { /* no-op */ },
            onFailed = { /* no-op */ }
        ) {
            button(title = openBtn) { action() }
            button(title = ignoreBtn) {
                // Do nothing, simply dismiss
            }
        }
        notif.send()
    }
}
