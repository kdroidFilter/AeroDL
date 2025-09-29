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

    // The last destination requested via Navigator. Used to restore UI state
    // (including destinations that are not part of the top tabs).
    val currentDestination: StateFlow<Destination>

    suspend fun navigate(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit = {}
    )

    suspend fun navigateUp()

    val canGoBack: StateFlow<Boolean>

    fun setCanGoBack(value: Boolean)
}

class DefaultNavigator(
    override val startDestination: Destination
): Navigator {
    private val _navigationActions =
        MutableSharedFlow<NavigationAction>(extraBufferCapacity = 1)
    override val navigationActions = _navigationActions.asSharedFlow()

    private val _currentDestination = MutableStateFlow<Destination>(Destination.HomeScreen)
    override val currentDestination: StateFlow<Destination> = _currentDestination.asStateFlow()

    override suspend fun navigate(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit
    ) {
        _currentDestination.value = destination
        _navigationActions.emit(
            NavigationAction.Navigate(destination, navOptions)
        )
    }

    override suspend fun navigateUp() =
        _navigationActions.emit(NavigationAction.NavigateUp)

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    override fun setCanGoBack(value: Boolean) { _canGoBack.value = value }
}