@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import io.github.kdroidfilter.ytdlpgui.di.AppScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import java.io.File

/**
 * Navigation state for ConverterInputScreen
 */
sealed interface ConverterInputNavigationState {
    data object None : ConverterInputNavigationState
    data class NavigateToOptions(val filePath: String) : ConverterInputNavigationState
}

/**
 * UI State for the Converter Input screen (file selection)
 */
data class ConverterInputState(
    val isAnalyzing: Boolean = false,
    val isDragging: Boolean = false,
    val errorMessage: String? = null,
    val navigationState: ConverterInputNavigationState = ConverterInputNavigationState.None
)

/**
 * Events for the Converter Input screen
 */
sealed class ConverterInputEvents {
    data object OpenFilePicker : ConverterInputEvents()
    data class FilesDropped(val files: List<File>) : ConverterInputEvents()
    data object DragEntered : ConverterInputEvents()
    data object DragExited : ConverterInputEvents()
    data object OnNavigationConsumed : ConverterInputEvents()
    data object ClearError : ConverterInputEvents()
    data object ScreenEntered : ConverterInputEvents()
    data object ScreenExited : ConverterInputEvents()
}

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey(ConverterInputViewModel::class)
@Inject
class ConverterInputViewModel(
    private val trayAppState: TrayAppState
) : MVIViewModel<ConverterInputState, ConverterInputEvents>() {

    override fun initialState(): ConverterInputState = ConverterInputState()

    override fun handleEvent(event: ConverterInputEvents) {
        when (event) {
            ConverterInputEvents.OpenFilePicker -> openFilePicker()
            is ConverterInputEvents.FilesDropped -> {
                update { copy(isDragging = false) }
                event.files.firstOrNull()?.let { navigateToOptions(it) }
            }
            ConverterInputEvents.DragEntered -> update { copy(isDragging = true) }
            ConverterInputEvents.DragExited -> update { copy(isDragging = false) }
            ConverterInputEvents.OnNavigationConsumed -> update { copy(navigationState = ConverterInputNavigationState.None) }
            ConverterInputEvents.ClearError -> update { copy(errorMessage = null) }
            ConverterInputEvents.ScreenEntered -> trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
            ConverterInputEvents.ScreenExited -> trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
        }
    }

    private fun openFilePicker() {
        viewModelScope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(
                    extensions = listOf(
                        "mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v",
                        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus"
                    )
                )
            )
            file?.path?.let { path ->
                navigateToOptions(File(path))
            }
        }
    }

    private fun navigateToOptions(file: File) {
        if (!file.exists()) {
            update { copy(errorMessage = "File not found: ${file.name}") }
            return
        }
        update { copy(navigationState = ConverterInputNavigationState.NavigateToOptions(file.absolutePath)) }
    }
}
