package io.github.kdroidfilter.ytdlpgui.features.download.manager

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.core.util.infoln
import io.github.kdroidfilter.ytdlpgui.core.util.warnln
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class DownloadViewModel(
    private val navController: NavHostController,
    private val downloadManager: DownloadManager,
    private val historyRepository: DownloadHistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorDialogItem = MutableStateFlow<DownloadManager.DownloadItem?>(null)
    val errorDialogItem = _errorDialogItem.asStateFlow()

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
            is DownloadEvents.ShowErrorDialog -> {
                val item = items.value.firstOrNull { it.id == event.id }
                if (item != null) {
                    infoln { "[DownloadViewModel] Showing error dialog for item ${item.id}" }
                    infoln { "[DownloadViewModel] Error message: ${item.message ?: "null"}" }
                } else {
                    warnln { "[DownloadViewModel] Item not found for id ${event.id}" }
                }
                _errorDialogItem.value = item
            }
            DownloadEvents.DismissErrorDialog -> {
                _errorDialogItem.value = null
            }
            is DownloadEvents.DismissFailed -> {
                // Clear dialog if it targets the same item
                if (_errorDialogItem.value?.id == event.id) {
                    _errorDialogItem.value = null
                }
                downloadManager.remove(event.id)
            }
        }
    }

    private fun openDirectoryFor(historyId: String) {
        val item = historyRepository.history.value.firstOrNull { it.id == historyId } ?: return
        val path = item.outputPath ?: return
        FileExplorerUtils.openDirectoryForPath(path)
    }
}
