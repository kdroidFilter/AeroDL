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

/**
 * Simple manager to handle multiple concurrent downloads and expose their state.
 */

class DownloadManager(
    private val ytDlpWrapper: YtDlpWrapper,
    private val settings: Settings,
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
    ) {
        enum class Status { Pending, Running, Completed, Failed, Cancelled }
    }

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    /** Start a background download for the given url. */
    fun start(url: String, videoInfo: VideoInfo? = null, preset: YtDlpWrapper.Preset? = null) : String {
        val usedPreset = preset ?: YtDlpWrapper.Preset.P720
        val item = DownloadItem(url = url, videoInfo = videoInfo, preset = usedPreset, status = DownloadItem.Status.Running)
        _items.value = _items.value + item

        val includePreset = settings.getBoolean("include_preset_in_filename", true)
        val outputTemplate = if (includePreset) "%(title)s_${usedPreset.height}p.%(ext)s" else "%(title)s.%(ext)s"

        val handle = ytDlpWrapper.downloadMp4At(
            url = url,
            preset = usedPreset,
            outputTemplate = outputTemplate,
            onEvent = { event ->
                when (event) {
                    is Event.Started -> update(item.id) { it.copy(status = DownloadItem.Status.Running, message = null) }
                    is Event.Progress -> {
                        val pct = (event.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
                        update(item.id) { it.copy(progress = pct, message = null) }
                    }
                    is Event.Completed -> {
                        val status = if (event.success) DownloadItem.Status.Completed else DownloadItem.Status.Failed
                        update(item.id) { it.copy(status = status, message = null) }
                    }
                    is Event.Error -> update(item.id) { it.copy(status = DownloadItem.Status.Failed, message = event.message) }
                    is Event.Cancelled -> update(item.id) { it.copy(status = DownloadItem.Status.Cancelled, message = null) }
                    is Event.Log -> {}
                    is Event.NetworkProblem -> update(item.id) { it.copy(status = DownloadItem.Status.Failed, message = event.detail) }
                }
            }
        )
        // Save handle in the item for cancellation
        update(item.id) { it.copy(handle = handle) }
        return item.id
    }

    fun cancel(id: String) {
        val current = _items.value.find { it.id == id }
        current?.handle?.cancel()
        update(id) { it.copy(status = DownloadItem.Status.Cancelled) }
    }

    private fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map { if (it.id == id) transform(it) else it }
    }
}
