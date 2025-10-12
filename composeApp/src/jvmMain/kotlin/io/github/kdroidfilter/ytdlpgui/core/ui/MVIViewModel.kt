package io.github.kdroidfilter.ytdlpgui.core.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base ViewModel class implementing the Model-View-Intent (MVI) pattern.
 *
 * @param State The type of the UI state
 * @param Event The type of the UI events
 * @param savedStateHandle Optional SavedStateHandle for process death/restoration
 */
abstract class MVIViewModel<State, Event>(
    protected val savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {

    private val _uiState by lazy { MutableStateFlow(initialState()) }
    open val uiState: StateFlow<State> get() = _uiState.asStateFlow()

    /**
     * Updates the UI state using the provided block.
     * @param block A lambda that receives the current state and returns the new state
     */
    protected fun update(block: State.() -> State) {
        _uiState.update(block)
    }

    /**
     * Returns the initial state of the ViewModel.
     */
    abstract fun initialState(): State

    /**
     * Handles UI events and updates the state accordingly.
     * @param event The event to handle
     */
    abstract fun handleEvent(event: Event)
}