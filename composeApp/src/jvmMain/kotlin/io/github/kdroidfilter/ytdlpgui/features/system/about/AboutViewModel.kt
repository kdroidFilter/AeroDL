package io.github.kdroidfilter.ytdlpgui.features.system.about

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AboutViewModel(
    navigator: Navigator,
    ytDlpWrapper: YtDlpWrapper,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = AboutState(
                appVersion = getAppVersion(),
                ytdlpVersion = ytDlpWrapper.version()
            )
        }
    }

    fun onEvents(event: AboutEvents) {
        when (event) {
            AboutEvents.Refresh -> { /* TODO: Implement if needed */ }
        }
    }
}
