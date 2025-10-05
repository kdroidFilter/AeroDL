@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.core.clipboard

import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardContent
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardListener
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardMonitor
import io.github.kdroidfilter.platformtools.clipboardmanager.ClipboardMonitorFactory
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.compose.builder.notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App-level clipboard monitor, independent of any screen lifecycle.
 * When enabled in settings, it listens to clipboard changes and navigates
 * to SingleDownloadScreen if a YouTube link is detected.
 */
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name
import ytdlpgui.composeapp.generated.resources.clipboard_ignore
import ytdlpgui.composeapp.generated.resources.clipboard_link_detected_message
import ytdlpgui.composeapp.generated.resources.clipboard_link_detected_title
import ytdlpgui.composeapp.generated.resources.clipboard_open_in_app
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import org.jetbrains.skia.Image as SkiaImage

class ClipboardMonitorManager(
    private val navigator: Navigator,
    private val settings: Settings,
    private val trayAppState: TrayAppState,
    private val supportedSitesRepository: io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository,
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
            scope.launch {
                trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
                runCatching { trayAppState.show() }
                navigator.navigate(Destination.SingleDownloadScreen(url))
                trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
            }
        }

        // Show a localized notification asking user consent to open in the app
        val videoId: String? = if (isYouTube) YouTubeThumbnailHelper.extractVideoId(url) else null
        val largeIconContent: (@Composable () -> Unit)? =
            if (videoId != null) {
                {
                    val thumbUrl = YouTubeThumbnailHelper.getThumbnailUrl(
                        videoId,
                        YouTubeThumbnailHelper.ThumbnailQuality.MEDIUM
                    )
                    var imageBitmap by remember(thumbUrl) { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(thumbUrl) {
                        runCatching {
                            val bytes = java.net.URL(thumbUrl).readBytes()
                            val skiaImage = SkiaImage.makeFromEncoded(bytes)
                            imageBitmap = skiaImage.toComposeImageBitmap()
                        }
                    }
                    imageBitmap?.let { img ->
                        Image(
                            bitmap = img,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else null

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
