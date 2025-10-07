package io.github.kdroidfilter.ytdlpgui.features.system.about

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AboutViewModel(navigator: Navigator) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun onEvents(event: AboutEvents) {
        when (event) {
            AboutEvents.Refresh -> { /* TODO: Implement if needed */ }
        }
    }
}
