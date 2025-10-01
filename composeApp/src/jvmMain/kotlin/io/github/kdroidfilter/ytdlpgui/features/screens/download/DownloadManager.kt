package io.github.kdroidfilter.ytdlpgui.features.screens.download

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import io.github.kdroidfilter.ytdlp.core.Handle
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import java.io.File

/**
 * Simple manager to handle multiple concurrent downloads and expose their state.
 */

class DownloadManager(
    private val ytDlpWrapper: YtDlpWrapper,
    private val settings: Settings,
    private val historyRepository: DownloadHistoryRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val url: String,
        val videoInfo: VideoInfo? = null,
        val preset: YtDlpWrapper.Preset? = null, // null -> audio
        val progress: Float = 0f,
        val status: Status = Status.Pending,
        val message: String? = null,
        val handle: Handle? = null,
    ) {
        enum class Status { Pending, Running, Completed, Failed, Cancelled }
    }

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    // Simple FIFO queue of pending downloads (by id)
    private val pendingQueue: ArrayDeque<String> = ArrayDeque()

    private fun maxParallel(): Int = settings.getInt("parallel_downloads", 2).coerceIn(1, 10)

    private fun runningCount(): Int = _items.value.count { it.status == DownloadItem.Status.Running }

    /** Enqueue a background video (MP4) download for the given url. */
    fun start(url: String, videoInfo: VideoInfo? = null, preset: YtDlpWrapper.Preset? = null) : String {
        val usedPreset = preset ?: YtDlpWrapper.Preset.P720
        val item = DownloadItem(url = url, videoInfo = videoInfo, preset = usedPreset, status = DownloadItem.Status.Pending)
        _items.value += item
        pendingQueue.addLast(item.id)
        maybeStartPending()
        return item.id
    }

    /** Enqueue an audio-only (MP3) download for the given url. */
    fun startAudio(url: String, videoInfo: VideoInfo? = null): String {
        val item = DownloadItem(url = url, videoInfo = videoInfo, preset = null, status = DownloadItem.Status.Pending)
        _items.value += item
        pendingQueue.addLast(item.id)
        maybeStartPending()
        return item.id
    }

    fun cancel(id: String) {
        // If it's still pending, remove from queue
        pendingQueue.remove(id)
        val current = _items.value.find { it.id == id }
        current?.handle?.cancel()
        update(id) { it.copy(status = DownloadItem.Status.Cancelled) }
        // Free a slot if something was running and try to start next
        maybeStartPending()
    }

    private fun maybeStartPending() {
        // Start as many as allowed
        while (runningCount() < maxParallel() && pendingQueue.isNotEmpty()) {
            val nextId = pendingQueue.removeFirst()
            launchDownload(nextId)
        }
    }

    private fun launchDownload(id: String) {
        val item = _items.value.find { it.id == id } ?: return
        if (item.status != DownloadItem.Status.Pending) return

        // Track destination path from yt-dlp logs
        var lastDestPath: String? = null
        val destRegex = Regex("Destination: (.+)")
        val mergeRegex = Regex("Merging formats into \"(.+)\"")

        fun resolveAbsolutePath(path: String?): String? {
            if (path.isNullOrBlank()) return null
            val file = File(path)
            return if (file.isAbsolute) file.absolutePath else File(ytDlpWrapper.downloadDir ?: File("."), path).absolutePath
        }

        if (item.preset == null) {
            // Audio download
            val handle = ytDlpWrapper.downloadAudioMp3WithPreset(
                url = item.url,
                preset = YtDlpWrapper.AudioQualityPreset.HIGH,
                outputTemplate = "%(title)s.%(ext)s",
                onEvent = { event ->
                    when (event) {
                        is Event.Started -> update(id) { it.copy(status = DownloadItem.Status.Running, message = null) }
                        is Event.Progress -> {
                            val pct = (event.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
                            update(id) { it.copy(progress = pct, message = null) }
                        }
                        is Event.Completed -> {
                            val status = if (event.success) DownloadItem.Status.Completed else DownloadItem.Status.Failed
                            update(id) { it.copy(status = status, message = null) }
                            if (event.success) {
                                historyRepository.add(
                                    id = id,
                                    url = item.url,
                                    videoInfo = item.videoInfo,
                                    outputPath = ytDlpWrapper.downloadDir?.absolutePath,
                                    isAudio = true,
                                    presetHeight = null,
                                    createdAt = System.currentTimeMillis()
                                )
                            }
                            maybeStartPending()
                        }
                        is Event.Error -> {
                            update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.message) }
                            maybeStartPending()
                        }
                        is Event.Cancelled -> {
                            update(id) { it.copy(status = DownloadItem.Status.Cancelled, message = null) }
                            maybeStartPending()
                        }
                        is Event.Log -> {
                            val line = event.line
                            destRegex.find(line)?.let { m -> lastDestPath = m.groupValues[1] }
                            mergeRegex.find(line)?.let { m -> lastDestPath = m.groupValues[1] }
                        }
                        is Event.NetworkProblem -> {
                            update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.detail) }
                            maybeStartPending()
                        }
                    }
                }
            )
            update(id) { it.copy(handle = handle) }
        } else {
            // Video download
            val usedPreset = item.preset
            val includePreset = settings.getBoolean("include_preset_in_filename", true)
            val outputTemplate = if (includePreset && usedPreset != null) {
                "%(title)s_${usedPreset.height}p.%(ext)s"
            } else {
                "%(title)s.%(ext)s"
            }
            val handle = ytDlpWrapper.downloadMp4At(
                url = item.url,
                preset = usedPreset ?: YtDlpWrapper.Preset.P720,
                outputTemplate = outputTemplate,
                onEvent = { event ->
                    when (event) {
                        is Event.Started -> update(id) { it.copy(status = DownloadItem.Status.Running, message = null) }
                        is Event.Progress -> {
                            val pct = (event.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
                            update(id) { it.copy(progress = pct, message = null) }
                        }
                        is Event.Completed -> {
                            val status = if (event.success) DownloadItem.Status.Completed else DownloadItem.Status.Failed
                            update(id) { it.copy(status = status, message = null) }
                            if (event.success) {
                                historyRepository.add(
                                    id = id,
                                    url = item.url,
                                    videoInfo = item.videoInfo,
                                    outputPath = ytDlpWrapper.downloadDir?.absolutePath,
                                    isAudio = false,
                                    presetHeight = usedPreset?.height,
                                    createdAt = System.currentTimeMillis()
                                )
                            }
                            maybeStartPending()
                        }
                        is Event.Error -> {
                            update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.message) }
                            maybeStartPending()
                        }
                        is Event.Cancelled -> {
                            update(id) { it.copy(status = DownloadItem.Status.Cancelled, message = null) }
                            maybeStartPending()
                        }
                        is Event.Log -> {
                            val line = event.line
                            destRegex.find(line)?.let { m -> lastDestPath = m.groupValues[1] }
                            mergeRegex.find(line)?.let { m -> lastDestPath = m.groupValues[1] }
                        }
                        is Event.NetworkProblem -> {
                            update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.detail) }
                            maybeStartPending()
                        }
                    }
                }
            )
            update(id) { it.copy(handle = handle) }
        }
    }

    private fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map { if (it.id == id) transform(it) else it }
    }
}
