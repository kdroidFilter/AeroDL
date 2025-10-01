package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SingleDownloadViewModel(
    private val navigator: Navigator,
    private val savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper
) : ViewModel() {

    val videoUrl = savedStateHandle.toRoute<Destination.SingleDownloadScreen>().videoLink
    private var _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo = _videoInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()


    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            println("Getting video info for $videoUrl")
            ytDlpWrapper.getVideoInfo(videoUrl)
                .onSuccess {
                    _videoInfo.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    println("Error getting video info: ${it.message}")
                }

        }
    }


    fun onEvents(event: SingleDownloadEvents) {
        when (event) {
            SingleDownloadEvents.Refresh -> { /* TODO */
            }
        }
    }
}
