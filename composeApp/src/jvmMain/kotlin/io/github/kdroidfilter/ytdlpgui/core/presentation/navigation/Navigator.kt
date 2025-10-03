package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

interface Navigator {
    val startDestination: Destination
    val navigationActions: Flow<NavigationAction>
    val currentDestination: StateFlow<Destination>

    suspend fun navigate(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit = {}
    )

    suspend fun navigateUp()

    val canGoBack: StateFlow<Boolean>

    fun setCanGoBack(value: Boolean)
    fun setCurrentDestination(destination: Destination)
}

class DefaultNavigator(
    override val startDestination: Destination
): Navigator {

    private val _navigationActions =
        MutableSharedFlow<NavigationAction>(extraBufferCapacity = 1)
    override val navigationActions = _navigationActions.asSharedFlow()

    // Internal stack of destinations to mirror what we ask NavController to do
    private val stack = ArrayDeque<Destination>().apply { addLast(startDestination) }

    private val _currentDestination = MutableStateFlow<Destination>(startDestination)
    override val currentDestination: StateFlow<Destination> = _currentDestination.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    override suspend fun navigate(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit
    ) {
        // Update our own stack first (source of truth for header)
        stack.addLast(destination)
        _currentDestination.value = destination
        _canGoBack.value = stack.size > 1

        _navigationActions.emit(
            NavigationAction.Navigate(destination, navOptions)
        )
    }

    override suspend fun navigateUp() {
        // Mirror a back action on our stack (if possible)
        if (stack.size > 1) {
            stack.removeLast() // pop current
            _currentDestination.value = stack.last()
        }
        _canGoBack.value = stack.size > 1

        _navigationActions.emit(NavigationAction.NavigateUp)
    }

    override fun setCanGoBack(value: Boolean) { _canGoBack.value = value }

    override fun setCurrentDestination(destination: Destination) {
        // Keep the stack consistent if someone forces a destination
        if (stack.isEmpty()) {
            stack.addLast(destination)
        } else {
            stack.removeLast()
            stack.addLast(destination)
        }
        _currentDestination.value = destination
        _canGoBack.value = stack.size > 1
    }
}
