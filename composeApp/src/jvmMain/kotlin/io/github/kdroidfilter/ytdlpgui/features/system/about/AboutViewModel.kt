package io.github.kdroidfilter.ytdlpgui.features.system.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import kotlinx.coroutines.launch
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.kdroidfilter.ytdlpgui.di.AppScope

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey(AboutViewModel::class)
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
            val denoVersion = ytDlpWrapper.denoVersion()
            update {
                copy(
                    appVersion = getAppVersion(),
                    ytdlpVersion = ytdlpVersion,
                    ffmpegVersion = ffmpegVersion,
                    denoVersion = denoVersion
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
