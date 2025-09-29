package io.github.kdroidfilter.ytdlpgui.features.screens.home

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(navigator: Navigator) : ViewModel() {

    private var _link = MutableStateFlow("")
    val link = _link.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun onEvents(event: HomeEvents) {
        when (event) {
            HomeEvents.Download -> TODO()
        }
    }

}