package io.github.kdroidfilter.ytdlpgui.features.screens.initscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InitViewModel(
    private val ytDlpWrapper: YtDlpWrapper,
    private val navigator: Navigator,
) : ViewModel() {
    private val _state = MutableStateFlow(InitState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ytDlpWrapper.apply {
                noCheckCertificate = true
                cookiesFromBrowser = "firefox"
            }.initialize { event ->
                when (event) {
                    YtDlpWrapper.InitEvent.CheckingYtDlp -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = true,
                            downloadingYtDlp = false,
                            downloadYtDlpProgress = null,
                            checkingFFmpeg = false,
                            downloadingFFmpeg = false,
                            downloadFfmpegProgress = null,
                            updatingYtdlp = false,
                            updatingFFmpeg = false,
                            errorMessage = null,
                            initCompleted = false,
                        )
                    }
                    YtDlpWrapper.InitEvent.DownloadingYtDlp -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = false,
                            downloadingYtDlp = true,
                            downloadYtDlpProgress = null,
                            errorMessage = null
                        )
                    }
                    YtDlpWrapper.InitEvent.UpdatingYtDlp -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = false,
                            updatingYtdlp = true,
                            downloadingYtDlp = false,
                            downloadYtDlpProgress = null,
                            errorMessage = null
                        )
                    }
                    YtDlpWrapper.InitEvent.EnsuringFfmpeg -> {
                        _state.value = _state.value.copy(
                            checkingFFmpeg = true,
                            downloadingFFmpeg = false,
                            downloadFfmpegProgress = null,
                            errorMessage = null
                        )
                    }
                    is YtDlpWrapper.InitEvent.YtDlpProgress -> {
                        _state.value = _state.value.copy(
                            downloadingYtDlp = true,
                            downloadYtDlpProgress = (event.percent ?: 0.0).toFloat()
                        )
                    }
                    is YtDlpWrapper.InitEvent.FfmpegProgress -> {
                        _state.value = _state.value.copy(
                            checkingFFmpeg = false,
                            downloadingFFmpeg = true,
                            downloadFfmpegProgress = (event.percent ?: 0.0).toFloat()
                        )
                    }
                    is YtDlpWrapper.InitEvent.Error -> {
                        _state.value = _state.value.copy(
                            errorMessage = event.message,
                            checkingYtDlp = false,
                            checkingFFmpeg = false,
                            downloadingYtDlp = false,
                            downloadingFFmpeg = false,
                            updatingYtdlp = false,
                            updatingFFmpeg = false,
                            initCompleted = false
                        )
                    }
                    is YtDlpWrapper.InitEvent.Completed -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = false,
                            checkingFFmpeg = false,
                            downloadingYtDlp = false,
                            downloadingFFmpeg = false,
                            updatingYtdlp = false,
                            updatingFFmpeg = false,
                            initCompleted = event.success
                        )
                        viewModelScope.launch {
                            navigator.navigate(Destination.HomeScreen)
                        }
                    }
                }
            }
        }
    }
}