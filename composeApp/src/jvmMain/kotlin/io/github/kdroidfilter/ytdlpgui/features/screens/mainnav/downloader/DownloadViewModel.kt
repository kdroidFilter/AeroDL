package io.github.kdroidfilter.ytdlpgui.features.screens.mainnav.downloader

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.business.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.util.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

class DownloadViewModel(
    private val navigator: Navigator,
    private val downloadManager: DownloadManager,
    private val historyRepository: DownloadHistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val items: StateFlow<List<DownloadManager.DownloadItem>> = downloadManager.items
    val history: StateFlow<List<DownloadHistoryRepository.HistoryItem>> = historyRepository.history

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
