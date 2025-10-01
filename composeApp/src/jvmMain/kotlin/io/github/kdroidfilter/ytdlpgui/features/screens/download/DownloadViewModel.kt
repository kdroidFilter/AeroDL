package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

class DownloadViewModel(
    private val navigator: Navigator,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val items: StateFlow<List<DownloadManager.DownloadItem>> = downloadManager.items

    fun onEvents(event: DownloadEvents) {
        when (event) {
            is DownloadEvents.Cancel -> downloadManager.cancel(event.id)
            DownloadEvents.Refresh -> { /* no-op for now */ }
        }
    }
}
