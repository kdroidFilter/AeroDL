package io.github.kdroidfilter.ytdlpgui.features.screens.home

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(){
    val viewModel = koinViewModel<HomeViewModel>()
    val state = collectHomeState(viewModel)
    HomeView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun HomeView(
    state: HomeState,
    onEvent: (HomeEvents) -> Unit,
) {
    Text("Home Screen")
}