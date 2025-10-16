package io.github.kdroidfilter.ytdlpgui.features.system.about

import androidx.lifecycle.viewModelScope
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository

@Inject
class AboutViewModel(
    private val settingsRepository: SettingsRepository,
    private val ytDlpWrapper: YtDlpWrapper,
) : MVIViewModel<AboutState, AboutEvents>() {


    override fun initialState(): AboutState = AboutState()

    init {
        viewModelScope.launch {
            val ytdlpVersion = ytDlpWrapper.version()
            val ffmpegVersion = ytDlpWrapper.ffmpegVersion()
            update {
                copy(
                    appVersion = getAppVersion(),
                    ytdlpVersion = ytdlpVersion,
                    ffmpegVersion = ffmpegVersion
                )
            }
        }
    }

    override fun handleEvent(event: AboutEvents) {
        when (event) {
            AboutEvents.Refresh -> { /* TODO: Implement if needed */ }
        }
    }
}
