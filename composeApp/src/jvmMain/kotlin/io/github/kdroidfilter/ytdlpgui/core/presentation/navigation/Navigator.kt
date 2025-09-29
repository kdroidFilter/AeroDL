package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

interface Navigator {
    val startDestination: Destination
    val navigationActions: Flow<NavigationAction>

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

    override suspend fun navigate(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit
    ) = _navigationActions.emit(
        NavigationAction.Navigate(destination, navOptions)
    )

    override suspend fun navigateUp() =
        _navigationActions.emit(NavigationAction.NavigateUp)

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

     override fun setCanGoBack(value: Boolean) { _canGoBack.value = value }
}