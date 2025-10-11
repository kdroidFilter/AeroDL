@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.core.domain.manager

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.compose.builder.notification
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.Handle
import io.github.kdroidfilter.ytdlp.core.SubtitleOptions
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.core.platform.notifications.NotificationThumbUtils
import io.github.kdroidfilter.ytdlpgui.core.util.errorln
import io.github.kdroidfilter.ytdlpgui.core.util.infoln
import io.github.kdroidfilter.ytdlpgui.core.util.warnln
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.download_completed_message
import ytdlpgui.composeapp.generated.resources.download_completed_title
import ytdlpgui.composeapp.generated.resources.open_directory
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.collections.ArrayDeque

/**
 * Manages downloading of videos and audio from URLs using the `YtDlpWrapper`. It keeps track of
 * download progress, handles download-related events, and maintains a queue for pending downloads.
 *
 * @constructor Creates an instance of `DownloadManager`.
 * @param ytDlpWrapper The YtDlpWrapper instance used for performing downloads.
 * @param settings Provides settings for configuring the maximum parallel downloads and other options.
 * @param historyRepository Stores the history of completed downloads.
 * @param trayAppState The tray application state for managing window visibility.
 */
class DownloadManager(
    private val getNavController: () -> NavHostController,
    private val ytDlpWrapper: YtDlpWrapper,
    private val settings: Settings,
    private val historyRepository: DownloadHistoryRepository,
    private val trayAppState: TrayAppState,
) {
    private val scope = CoroutineScope(Dispatchers.IO)


    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val url: String,
        val videoInfo: VideoInfo? = null,
        val preset: YtDlpWrapper.Preset? = null,
        val progress: Float = 0f,
        val status: Status = Status.Pending,
        val message: String? = null,
        val handle: Handle? = null,
        val subtitleLanguages: List<String>? = null,
    ) {
        enum class Status { Pending, Running, Completed, Failed, Cancelled }
    }

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    // Reactive global state: true if at least one download is currently running
    val isDownloading: StateFlow<Boolean> = _items
        .map { list -> list.any { it.status == DownloadItem.Status.Running } }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val pendingQueue: ArrayDeque<String> = ArrayDeque()

    private fun maxParallel(): Int = settings.getInt(SettingsKeys.PARALLEL_DOWNLOADS, 2).coerceIn(1, 10)
    private fun runningCount(): Int = _items.value.count { it.status == DownloadItem.Status.Running }

    fun start(url: String, videoInfo: VideoInfo? = null, preset: YtDlpWrapper.Preset? = null): String =
        enqueueDownload(url, videoInfo, preset ?: YtDlpWrapper.Preset.P720, null)

    fun startWithSubtitles(
        url: String,
        videoInfo: VideoInfo? = null,
        preset: YtDlpWrapper.Preset? = null,
        languages: List<String>
    ): String = enqueueDownload(url, videoInfo, preset ?: YtDlpWrapper.Preset.P720, languages.filter { it.isNotBlank() })

    fun startAudio(url: String, videoInfo: VideoInfo? = null): String =
        enqueueDownload(url, videoInfo, null, null)

    private fun enqueueDownload(
        url: String,
        videoInfo: VideoInfo?,
        preset: YtDlpWrapper.Preset?,
        subtitles: List<String>?
    ): String {
        val item = DownloadItem(
            url = url,
            videoInfo = videoInfo,
            preset = preset,
            subtitleLanguages = subtitles
        )
        _items.value += item
        pendingQueue.addLast(item.id)
        maybeStartPending()
        return item.id
    }

    fun cancel(id: String) {
        pendingQueue.remove(id)
        _items.value.find { it.id == id }?.handle?.cancel()
        update(id) { it.copy(status = DownloadItem.Status.Cancelled) }
        maybeStartPending()
    }

    /**
     * Removes a download item from the in-memory list. If the item is currently running,
     * its handle is cancelled first. Also ensures it is removed from the pending queue.
     */
    fun remove(id: String) {
        pendingQueue.remove(id)
        // Cancel if still running
        _items.value.find { it.id == id }?.handle?.cancel()
        // Drop the item from the list
        _items.value = _items.value.filterNot { it.id == id }
        maybeStartPending()
    }

    private fun maybeStartPending() {
        while (runningCount() < maxParallel() && pendingQueue.isNotEmpty()) {
            launchDownload(pendingQueue.removeFirst())
        }
    }


    private fun launchDownload(id: String) {
        val item = _items.value.find { it.id == id } ?: return
        if (item.status != DownloadItem.Status.Pending) return

        // Fallback trackers from logs (kept, but strengthened)
        var lastDestPath: String? = null
        val rDest = Regex("(?i)^\\[download]\\s+Destination:\\s+(.+)$")
        val rMerging = Regex("(?i)^\\[(?:Merger|Fixup.*)]\\s+Merging formats into\\s+\"(.+)\"$")
        val rMoving  = Regex("(?i)^\\[(?:download|move)]\\s+Moving to\\s+\"(.+)\"$")

        // NEW: predictable temp sink based on URL MD5 (must match buildCommand)
        val sinkFile = File(System.getProperty("java.io.tmpdir"), "ytdlp-finalpath-${md5(item.url)}.txt")
        // Clean any stale file from previous run
        runCatching { if (sinkFile.exists()) sinkFile.delete() }

        val eventHandler = createEventHandler(
            id = id,
            item = item,
            getOutputPath = { lastDestPath }, // logs fallback (kept)
            onLog = { line ->
                rDest.find(line)?.let { lastDestPath = it.groupValues[1].trim('"') }
                rMerging.find(line)?.let { lastDestPath = it.groupValues[1].trim('"') }
                rMoving.find(line)?.let  { lastDestPath = it.groupValues[1].trim('"') }
            },
            finalPathSink = sinkFile // pass to completion to read file
        )

        val handle = when {
            item.preset == null -> downloadAudio(item, eventHandler)
            !item.subtitleLanguages.isNullOrEmpty() -> downloadVideoWithSubtitles(item, eventHandler)
            else -> downloadVideo(item, eventHandler)
        }

        update(id) { it.copy(handle = handle) }
    }

    private fun createEventHandler(
        id: String,
        item: DownloadItem,
        getOutputPath: () -> String?,
        onLog: (String) -> Unit,
        finalPathSink: File
    ): (Event) -> Unit = { event ->
        when (event) {
            is Event.Started -> {
                infoln { "[DownloadManager] Download started for item $id" }
                update(id) { it.copy(status = DownloadItem.Status.Running, message = null) }
            }

            is Event.Progress -> {
                val pct = (event.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
                update(id) { it.copy(progress = pct, message = null) }
            }

            is Event.Log -> onLog(event.line)

            is Event.Completed -> {
                val status = if (event.success) DownloadItem.Status.Completed else DownloadItem.Status.Failed
                infoln { "[DownloadManager] Download completed for item $id, success=${event.success}" }
                // Only clear message on success - preserve error message on failure
                update(id) {
                    if (event.success) {
                        it.copy(status = status, message = null)
                    } else {
                        it.copy(status = status)
                    }
                }

                if (event.success) {
                    infoln { "[DownloadManager] Reading final path from sink: ${finalPathSink.absolutePath}" }
                    val absolutePath =
                        readFinalPathFromSink(finalPathSink)
                            ?: computeAbsoluteFromFallbacks(item, getOutputPath())

                    if (absolutePath != null) {
                        infoln { "[DownloadManager] Final output path: $absolutePath" }
                    } else {
                        warnln { "[DownloadManager] Could not determine final output path for item $id" }
                    }

                    saveToHistory(id, item, absolutePath)

                    // Notify when window hidden or not on downloader screen (using type-safe hasRoute())
                    val currentDestination = getNavController().currentBackStackEntry?.destination
                    val isOnDownloaderScreen = currentDestination?.hierarchy?.any {
                        it.hasRoute(Destination.MainNavigation.Downloader::class)
                    } == true

                    if ((settings.getBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, true) && !trayAppState.isVisible.value) ||
                        (settings.getBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, true) && trayAppState.isVisible.value && !isOnDownloaderScreen)) {
                        scope.launch { sendCompletionNotification(item, absolutePath) }
                    }
                }

                // Cleanup sink whatever the result
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }

                // Only remove from memory when the download actually succeeded.
                // Keep failed items so the user can see the error and dismiss manually.
                if (event.success) {
                    // Drop completed items to prevent list growth (history persists them)
                    remove(id)
                } else {
                    // Ensure pending queue advances after a failure as well
                    maybeStartPending()
                }
            }

            is Event.Error -> {
                errorln { "[DownloadManager] Download error for item $id: ${event.message}" }
                event.cause?.let { errorln { "[DownloadManager] Error cause: ${it.message}" } }
                update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.message) }
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }
                maybeStartPending()
            }

            is Event.Cancelled -> {
                infoln { "[DownloadManager] Download cancelled for item $id" }
                update(id) { it.copy(status = DownloadItem.Status.Cancelled, message = null) }
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }
                // Remove cancelled items to avoid leaking them in memory
                remove(id)
            }

            is Event.NetworkProblem -> {
                errorln { "[DownloadManager] Network problem for item $id: ${event.detail}" }
                update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.detail) }
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }
                maybeStartPending()
            }
        }
    }

    private fun readFinalPathFromSink(file: File): String? {
        return try {
            if (!file.exists()) {
                null
            } else {
                // Keep last non-blank line (handles playlists / multiple outputs appending)
                file.readLines()
                    .asReversed()
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?.trim('"')
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun computeAbsoluteFromFallbacks(item: DownloadItem, loggedPath: String?): String? {
        loggedPath?.let {
            val p = File(it.trim('"'))
            if (p.isAbsolute) return p.absolutePath
            ytDlpWrapper.downloadDir?.let { dir -> return File(dir, p.path).absolutePath }
            return p.absolutePath
        }

        // Final fallback: synthesize from title + preset (kept from your logic)
        val downloadDir = ytDlpWrapper.downloadDir ?: return null
        val videoTitle = item.videoInfo?.title ?: return null
        val safeTitle = sanitizeFilename(videoTitle)
        val ext = if (item.preset == null) "mp3" else "mp4"
        return File(downloadDir.absolutePath, "$safeTitle.$ext").absolutePath
    }

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }


    private fun downloadAudio(item: DownloadItem, onEvent: (Event) -> Unit): Handle =
        ytDlpWrapper.downloadAudioMp3WithPreset(
            url = item.url,
            preset = YtDlpWrapper.AudioQualityPreset.HIGH,
            outputTemplate = "%(title)s.%(ext)s",
            onEvent = onEvent
        )

    private fun downloadVideo(item: DownloadItem, onEvent: (Event) -> Unit): Handle =
        ytDlpWrapper.downloadMp4At(
            url = item.url,
            preset = item.preset ?: YtDlpWrapper.Preset.P720,
            outputTemplate = buildOutputTemplate(item.preset),
            onEvent = onEvent
        )

    private fun downloadVideoWithSubtitles(item: DownloadItem, onEvent: (Event) -> Unit): Handle {
        infoln { "[DownloadManager] Initiating video download with subtitles for: ${item.url}" }
        infoln { "[DownloadManager] Requested subtitle languages: ${item.subtitleLanguages?.joinToString(",") ?: "none"}" }
        infoln { "[DownloadManager] Preset: ${item.preset?.height}p" }

        val subtitleOptions = SubtitleOptions(
            languages = item.subtitleLanguages ?: emptyList(),
            writeAutoSubtitles = true,
            embedSubtitles = true,
            writeSubtitles = false,
            subFormat = "srt"
        )

        infoln { "[DownloadManager] SubtitleOptions: embed=${subtitleOptions.embedSubtitles}, writeAuto=${subtitleOptions.writeAutoSubtitles}, write=${subtitleOptions.writeSubtitles}" }

        return ytDlpWrapper.downloadMp4At(
            url = item.url,
            preset = item.preset ?: YtDlpWrapper.Preset.P720,
            outputTemplate = buildOutputTemplate(item.preset),
            subtitles = subtitleOptions,
            onEvent = onEvent
        )
    }

    private fun buildOutputTemplate(preset: YtDlpWrapper.Preset?): String {
        val includePreset = settings.getBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, true)
        return if (includePreset && preset != null) {
            "%(title)s_${preset.height}p.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }
    }

    private fun saveToHistory(id: String, item: DownloadItem, outputFilePath: String?) {
        historyRepository.add(
            id = id,
            url = item.url,
            videoInfo = item.videoInfo,
            outputPath = outputFilePath,
            isAudio = item.preset == null,
            presetHeight = item.preset?.height,
            createdAt = System.currentTimeMillis()
        )
    }

    @OptIn(ExperimentalNotificationsApi::class)
    private suspend fun sendCompletionNotification(item: DownloadItem, absolutePath: String?) {
        val title = getString(Res.string.download_completed_title)
        val nameOrUrl = item.videoInfo?.title?.ifBlank { null } ?: run {
            absolutePath?.let { File(it).nameWithoutExtension }
        } ?: item.url
        val message = getString(Res.string.download_completed_message, nameOrUrl)
        val openBtn = getString(Res.string.open_directory)

        fun openDirAction() { absolutePath?.let { FileExplorerUtils.openDirectoryForPath(it) } }

        val thumbUrl = NotificationThumbUtils.resolveThumbnailUrl(item.videoInfo?.thumbnail, item.url)
        val largeIconContent = NotificationThumbUtils.buildLargeIcon(thumbUrl)

        val notif = notification(
            title = title,
            message = message,
            largeIcon = largeIconContent,
            onActivated = { openDirAction() },
            onDismissed = { },
            onFailed = { }
        ) {
            button(title = openBtn) { openDirAction() }
        }
        notif.send()
    }

    private fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map {
            if (it.id == id) {
                val updated = transform(it)
                if (updated.status == DownloadItem.Status.Failed) {
                    infoln { "[DownloadManager] Updating item $id to Failed status with message: ${updated.message}" }
                }
                updated
            } else it
        }
    }

    private  fun sanitizeFilename(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
}
