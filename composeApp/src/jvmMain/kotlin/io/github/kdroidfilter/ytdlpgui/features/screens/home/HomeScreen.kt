package io.github.kdroidfilter.ytdlpgui.features.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.ClipboardPaste
import io.github.composefluent.icons.regular.ArrowRight
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import ytdlpgui.composeapp.generated.resources.AeroDl
import ytdlpgui.composeapp.generated.resources.AeroDlDark
import ytdlpgui.composeapp.generated.resources.Res

@Composable
fun HomeScreen() {
    val viewModel = koinInject<HomeViewModel>()
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
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AeroDL", style = FluentTheme.typography.title)
            Image(
                painter = painterResource(if (isSystemInDarkMode()) Res.drawable.AeroDlDark else Res.drawable.AeroDl),
                contentDescription = "Logo",
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = state.link,
                onValueChange = { onEvent(HomeEvents.OnLinkChanged(it)) },
                placeholder = { Text("https://youtu.be/XXXXXXXXX", maxLines = 1) },
                header = {
                    val modifier = Modifier.fillMaxWidth(0.85f)
                    val textAlign = TextAlign.Center
                    val style =  FluentTheme.typography.caption
                    if (state.errorMessage == null) {
                        Text(
                            text = "Paste a Video link",
                            style = style,
                            textAlign = textAlign,
                            color = FluentTheme.colors.text.text.disabled,
                            modifier = modifier
                        )
                    } else {
                        Text(
                            text = state.errorMessage,
                            style = style,
                            textAlign = textAlign,
                            color = FluentTheme.colors.system.critical,
                            modifier = modifier
                        )
                    }
                }
            )
            Button(
                modifier = Modifier.size(33.dp),
                onClick = { onEvent(HomeEvents.OnClipBoardClicked) },
                iconOnly = true
            ) {
                Icon(Icons.Filled.ClipboardPaste, contentDescription = "Paste Link")
            }
        }
        AccentButton(
            onClick = { onEvent(HomeEvents.OnNextClicked) }
        ) {
            Text("Next")
            Icon(Icons.Default.ArrowRight, contentDescription = null)
        }
    }


}