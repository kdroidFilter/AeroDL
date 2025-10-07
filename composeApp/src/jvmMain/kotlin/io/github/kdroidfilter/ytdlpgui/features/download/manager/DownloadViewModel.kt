package io.github.kdroidfilter.ytdlpgui.features.download.manager

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class DownloadViewModel(
    private val navigator: Navigator,
    private val downloadManager: DownloadManager,
    private val historyRepository: DownloadHistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val items: StateFlow<List<DownloadManager.DownloadItem>> = downloadManager.items
    val history: StateFlow<List<DownloadHistoryRepository.HistoryItem>> = historyRepository.history

    // Availability of output directories by history id (derived from history)
    val directoryAvailability = history.map { list ->
        list.associate { it.id to (it.outputPath?.let { path -> File(path).exists() } == true) }
    }

    fun onEvents(event: DownloadEvents) {
        when (event) {
            is DownloadEvents.Cancel -> downloadManager.cancel(event.id)
            DownloadEvents.Refresh -> { /* no-op for now */
            }

            DownloadEvents.ClearHistory -> historyRepository.clear()
            is DownloadEvents.DeleteHistory -> historyRepository.delete(event.id)
            is DownloadEvents.OpenDirectory -> openDirectoryFor(event.id)
        }
    }

    private fun openDirectoryFor(historyId: String) {
        val item = historyRepository.history.value.firstOrNull { it.id == historyId } ?: return
        val path = item.outputPath ?: return
        FileExplorerUtils.openDirectoryForPath(path)
    }
}
