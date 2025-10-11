package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BulkDownloadViewModel(navController: NavHostController) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun onEvents(event: BulkDownloadEvents) {
        when (event) {
            BulkDownloadEvents.Refresh -> { /* TODO */ }
        }
    }
}
