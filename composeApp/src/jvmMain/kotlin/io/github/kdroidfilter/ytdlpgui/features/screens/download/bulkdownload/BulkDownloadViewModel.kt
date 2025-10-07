package io.github.kdroidfilter.ytdlpgui.features.screens.download.bulkdownload

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BulkDownloadViewModel(navigator: Navigator) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun onEvents(event: BulkDownloadEvents) {
        when (event) {
            BulkDownloadEvents.Refresh -> { /* TODO */ }
        }
    }
}
