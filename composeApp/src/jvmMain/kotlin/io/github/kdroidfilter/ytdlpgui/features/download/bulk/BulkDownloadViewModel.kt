package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.kdroidfilter.ytdlpgui.di.AppScope

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey(BulkDownloadViewModel::class)
@Inject
class BulkDownloadViewModel : MVIViewModel<BulkDownloadState, BulkDownloadEvents>() {


    override fun initialState(): BulkDownloadState = BulkDownloadState()

    private val _isLoading = MutableStateFlow(false)

    // Single UI state for the screen - Note: This ViewModel uses a simple mapped state, so we override uiState
    override val uiState = _isLoading
        .map { loading -> BulkDownloadState(isLoading = loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BulkDownloadState()
        )

    override fun handleEvent(event: BulkDownloadEvents) {
        when (event) {
            BulkDownloadEvents.Refresh -> { /* TODO */ }
        }
    }
}
