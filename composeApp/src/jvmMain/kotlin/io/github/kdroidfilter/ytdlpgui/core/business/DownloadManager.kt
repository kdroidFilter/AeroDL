package io.github.kdroidfilter.ytdlpgui.features.screens.download

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.SubtitleOptions
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

    private val pendingQueue: ArrayDeque<String> = ArrayDeque()

    private fun maxParallel(): Int = settings.getInt("parallel_downloads", 2).coerceIn(1, 10)
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

    private fun maybeStartPending() {
        while (runningCount() < maxParallel() && pendingQueue.isNotEmpty()) {
            launchDownload(pendingQueue.removeFirst())
        }
    }

    private fun launchDownload(id: String) {
        val item = _items.value.find { it.id == id } ?: return
        if (item.status != DownloadItem.Status.Pending) return

        var lastDestPath: String? = null
        val destRegex = Regex("Destination: (.+)")
        val mergeRegex = Regex("Merging formats into \"(.+)\"")

        val eventHandler = createEventHandler(id, item, { lastDestPath }) { line ->
            destRegex.find(line)?.let { lastDestPath = it.groupValues[1] }
            mergeRegex.find(line)?.let { lastDestPath = it.groupValues[1] }
        }

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
        onLog: (String) -> Unit
    ): (Event) -> Unit = { event ->
        when (event) {
            is Event.Started ->
                update(id) { it.copy(status = DownloadItem.Status.Running, message = null) }

            is Event.Progress -> {
                val pct = (event.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
                update(id) { it.copy(progress = pct, message = null) }
            }

            is Event.Completed -> {
                val status = if (event.success) DownloadItem.Status.Completed else DownloadItem.Status.Failed
                update(id) { it.copy(status = status, message = null) }
                if (event.success) {
                    val detectedPath = getOutputPath()
                    val absolutePath = detectedPath?.let { p ->
                        val f = File(p)
                        if (f.isAbsolute) f.absolutePath else ytDlpWrapper.downloadDir?.let { dir -> File(dir, p).absolutePath } ?: f.absolutePath
                    }
                    saveToHistory(id, item, absolutePath)
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

            is Event.Log -> onLog(event.line)

            is Event.NetworkProblem -> {
                update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.detail) }
                maybeStartPending()
            }
        }
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

    private fun downloadVideoWithSubtitles(item: DownloadItem, onEvent: (Event) -> Unit): Handle =
        ytDlpWrapper.downloadMp4At(
            url = item.url,
            preset = item.preset ?: YtDlpWrapper.Preset.P720,
            outputTemplate = buildOutputTemplate(item.preset),
            subtitles = SubtitleOptions(
                languages = item.subtitleLanguages ?: emptyList(),
                writeAutoSubtitles = true,
                embedSubtitles = true,
                writeSubtitles = false,
                subFormat = "srt"
            ),
            onEvent = onEvent
        )

    private fun buildOutputTemplate(preset: YtDlpWrapper.Preset?): String {
        val includePreset = settings.getBoolean("include_preset_in_filename", true)
        return if (includePreset && preset != null) {
            "%(title)s_${preset.height}p.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }
    }

    private fun saveToHistory(id: String, item: DownloadItem, outputFilePath: String?) {
        val finalPath = outputFilePath ?: ytDlpWrapper.downloadDir?.absolutePath
        historyRepository.add(
            id = id,
            url = item.url,
            videoInfo = item.videoInfo,
            outputPath = finalPath,
            isAudio = item.preset == null,
            presetHeight = item.preset?.height,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map { if (it.id == id) transform(it) else it }
    }
}