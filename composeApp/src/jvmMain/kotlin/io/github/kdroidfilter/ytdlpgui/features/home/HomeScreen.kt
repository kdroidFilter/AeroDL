package io.github.kdroidfilter.ytdlpgui.features.home

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.component.*
import io.github.kdroidfilter.ytdlpgui.ui.icons.Icons
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.*
import java.awt.datatransfer.DataFlavor
import java.io.File

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: HomeViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()
    
    // Handle navigation
    LaunchedEffect(state.navigationState) {
        when (val navigationState = state.navigationState) {
            is HomeNavigationState.NavigateToDownload -> {
                navController.navigate(navigationState.destination)
                viewModel.handleEvent(HomeEvents.OnNavigationConsumed)
            }
            HomeNavigationState.None -> {
                // No navigation needed
            }
        }
    }
    
    HomeView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeView(
    state: HomeState,
    onEvent: (HomeEvents) -> Unit,
) {
    val currentLayoutDirection = LocalLayoutDirection.current
    val isRtl = (currentLayoutDirection == LayoutDirection.Rtl)
    var isDragOver by remember { mutableStateOf(false) }
    val onEventState = rememberUpdatedState(onEvent)
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isDragOver = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver = false
                val text = droppedText(event) ?: return false
                onEventState.value(HomeEvents.OnLinkChanged(text))
                return true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = AeroDlLogoOnly,
                contentDescription = stringResource(Res.string.logo_content_desc),
                modifier = Modifier.height(150.dp),
                tint = NativeTheme.colors.system.neutral
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                modifier = Modifier
                    .weight(1f)
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { true },
                        target = dropTarget,
                    ),
                large = true,
                value = state.link,
                enabled = !state.isLoading,
                onValueChange = { onEvent(HomeEvents.OnLinkChanged(it)) },
                placeholder = { Text(stringResource(Res.string.placeholder_link_example), maxLines = 1) },
                singleLine = true,
                header = {
                    val (headerText, headerColor) = when {
                        state.isLoading -> stringResource(Res.string.loading) to NativeTheme.colors.text.text.tertiary
                        isDragOver -> stringResource(Res.string.drop_video_link_header) to NativeTheme.colors.fillAccent.default
                        state.errorMessage != null -> {
                            val msg = when (state.errorMessage) {
                                HomeError.SingleValidUrl -> stringResource(Res.string.error_single_valid_url)
                                HomeError.InvalidUrlFormat -> stringResource(Res.string.error_invalid_url_format)
                                HomeError.UrlRequired -> stringResource(Res.string.error_url_required)
                            }
                            msg to NativeTheme.colors.system.critical
                        }
                        else -> stringResource(Res.string.paste_video_link_header) to NativeTheme.colors.text.text.secondary
                    }
                    Text(
                        text = headerText,
                        style = NativeTheme.typography.caption,
                        textAlign = TextAlign.Center,
                        color = headerColor,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                modifier = Modifier.size(NativeTheme.sizes.control),
                onClick = { onEvent(HomeEvents.OnClipBoardClicked) },
                iconOnly = true,
                large = true,
                disabled = state.isLoading,
            ) {
                Icon(
                    Icons.Filled.ClipboardPaste,
                    contentDescription = stringResource(Res.string.paste_link_content_desc)
                )
            }
        }
        AccentButton(
            onClick = { onEvent(HomeEvents.OnNextClicked) },
            disabled = state.isLoading
        ) {
            Text(stringResource(Res.string.next))
            Icon(if (isRtl) Icons.Default.ArrowLeft else Icons.Default.ArrowRight, contentDescription = null)
        }
    }

}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeView(state = HomeState.emptyState, onEvent = {})
}

@Preview
@Composable
fun HomeScreenPreviewLoading() {
    HomeView(state = HomeState.loadingState, onEvent = {})
}

@OptIn(ExperimentalComposeUiApi::class)
private fun droppedText(event: DragAndDropEvent): String? {
    val transferable = runCatching { event.awtTransferable }.getOrNull() ?: return null
    if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        val text = runCatching {
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        }.getOrNull()?.trim()
        if (!text.isNullOrEmpty()) return text
    }
    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        @Suppress("UNCHECKED_CAST")
        val files = runCatching {
            transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
        }.getOrNull().orEmpty()
        files.firstNotNullOfOrNull { file ->
            file.path.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
        }?.let { return it }
    }
    return null
}
