package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class BulkDownloadViewModel(navController: NavHostController) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Single UI state for the screen
    val state = isLoading
        .map { loading -> BulkDownloadState(isLoading = loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BulkDownloadState()
        )

    fun onEvents(event: BulkDownloadEvents) {
        when (event) {
            BulkDownloadEvents.Refresh -> { /* TODO */ }
        }
    }
}
