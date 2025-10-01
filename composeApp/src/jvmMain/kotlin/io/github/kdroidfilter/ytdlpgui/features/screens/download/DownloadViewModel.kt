package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadViewModel(navigator: Navigator) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun onEvents(event: DownloadEvents) {
        when (event) {
            DownloadEvents.Refresh -> { /* TODO */ }
        }
    }
}
