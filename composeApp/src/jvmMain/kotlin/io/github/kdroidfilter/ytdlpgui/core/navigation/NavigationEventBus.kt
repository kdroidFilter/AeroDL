package io.github.kdroidfilter.ytdlpgui.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A very small, DI-backed event bus to request app-level navigation from non-UI layers
 * (e.g., managers triggered by system events/notifications).
 *
 * UI layer (App.kt) collects [events] and applies them to the NavController.
 */
class NavigationEventBus {
    private val _events = MutableSharedFlow<Destination>(extraBufferCapacity = 1)
    val events: SharedFlow<Destination> = _events.asSharedFlow()

    fun navigateTo(destination: Destination) {
        _events.tryEmit(destination)
    }
}
