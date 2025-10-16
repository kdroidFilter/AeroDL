package io.github.kdroidfilter.ytdlpgui.features.download.manager

import androidx.lifecycle.viewModelScope
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.warnln
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.util.Locale

class DownloadViewModel(
    private val downloadManager: DownloadManager,
    private val historyRepository: DownloadHistoryRepository,
    private val initViewModel: io.github.kdroidfilter.ytdlpgui.features.init.InitViewModel,
) : MVIViewModel<DownloadState, DownloadEvents>() {


    override fun initialState(): DownloadState = DownloadState.emptyState

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorDialogItem = MutableStateFlow<DownloadManager.DownloadItem?>(null)
    val errorDialogItem = _errorDialogItem.asStateFlow()

    val items: StateFlow<List<DownloadManager.DownloadItem>> = downloadManager.items
    val history: StateFlow<List<DownloadHistoryRepository.HistoryItem>> = historyRepository.history

    // Search query for history filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Availability of output directories by history id (derived from history)
    val directoryAvailability = history.map { list ->
        list.associate { h ->
            val exists = h.outputPath?.let { p ->
                val f = File(p)
                if (h.isSplit) {
                    f.parentFile?.exists() == true
                } else {
                    f.exists()
                }
            } == true
            h.id to exists
        }
    }

    // Combined UI state
    private data class Base(
        val loading: Boolean,
        val items: List<DownloadManager.DownloadItem>,
        val history: List<DownloadHistoryRepository.HistoryItem>,
        val dirAvail: Map<String, Boolean>,
        val errorItem: DownloadManager.DownloadItem?,
        val hasAnyHistory: Boolean,
        val searchQuery: String,
    )

    // Note: This ViewModel uses a combined state from multiple sources, so we override uiState
    override val uiState = combine(
        isLoading,
        items,
        history,
        directoryAvailability,
        errorDialogItem,
    ) { loading, itemsList, historyList, dirAvail, errorItem ->
        Base(
            loading = loading,
            items = itemsList,
            history = historyList,
            dirAvail = dirAvail,
            errorItem = errorItem,
            hasAnyHistory = historyList.isNotEmpty(),
            searchQuery = "",
        )
    }.combine(searchQuery) { base, query ->
        val q = query.trim().lowercase(Locale.getDefault())
        val filteredHistory = if (q.isEmpty()) base.history else base.history.filter { h ->
            val title = h.videoInfo?.title?.lowercase(Locale.getDefault()) ?: ""
            val url = h.url.lowercase(Locale.getDefault())
            val path = h.outputPath?.lowercase(Locale.getDefault()) ?: ""
            title.contains(q) || url.contains(q) || path.contains(q)
        }
        base.copy(history = filteredHistory, searchQuery = query)
    }.combine(initViewModel.uiState) { base, initState ->
        DownloadState(
            isLoading = base.loading,
            items = base.items,
            history = base.history,
            directoryAvailability = base.dirAvail,
            errorDialogItem = base.errorItem,
            searchQuery = base.searchQuery,
            hasAnyHistory = base.hasAnyHistory,
            updateAvailable = initState.updateAvailable && !initState.updateDismissed && initState.latestVersion != null && initState.downloadUrl != null,
            updateVersion = initState.latestVersion,
            updateUrl = initState.downloadUrl,
            updateBody = initState.releaseBody,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadState.emptyState,
    )

    override fun handleEvent(event: DownloadEvents) {
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
            DownloadEvents.DismissUpdateInfoBar -> {
                initViewModel.dismissUpdateInfo()
            }
            is DownloadEvents.UpdateSearchQuery -> {
                _searchQuery.value = event.query
            }
        }
    }

    private fun openDirectoryFor(historyId: String) {
        val item = historyRepository.history.value.firstOrNull { it.id == historyId } ?: return
        val path = item.outputPath ?: return
        val toOpen = if (item.isSplit) {
            val parent = File(path).parentFile
            parent?.absolutePath ?: path
        } else path
        FileExplorerUtils.openDirectoryForPath(toOpen)
    }
}
