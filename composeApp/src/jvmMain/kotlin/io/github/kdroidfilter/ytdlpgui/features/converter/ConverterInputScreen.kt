package io.github.kdroidfilter.ytdlpgui.features.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.DocumentAdd
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import java.awt.datatransfer.DataFlavor
import java.io.File

@Composable
fun ConverterInputScreen(navController: NavHostController) {
    val viewModel: ConverterInputViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()

    // Keep tray window open while on this screen
    DisposableEffect(Unit) {
        viewModel.handleEvent(ConverterInputEvents.ScreenEntered)
        onDispose {
            viewModel.handleEvent(ConverterInputEvents.ScreenExited)
        }
    }

    // Handle navigation to Options screen
    LaunchedEffect(state.navigationState) {
        when (val navState = state.navigationState) {
            is ConverterInputNavigationState.NavigateToOptions -> {
                navController.navigate(Destination.Converter.Options(navState.filePath))
                viewModel.handleEvent(ConverterInputEvents.OnNavigationConsumed)
            }
            ConverterInputNavigationState.None -> {
                // No navigation needed
            }
        }
    }

    ConverterInputView(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ConverterInputView(
    state: ConverterInputState,
    onEvent: (ConverterInputEvents) -> Unit
) {
    val borderColor = if (state.isDragging) {
        FluentTheme.colors.fillAccent.default
    } else {
        FluentTheme.colors.stroke.control.default
    }

    val backgroundColor = if (state.isDragging) {
        FluentTheme.colors.fillAccent.default.copy(alpha = 0.1f)
    } else {
        FluentTheme.colors.background.layer.default
    }

    // Create drag and drop target
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                onEvent(ConverterInputEvents.DragEntered)
            }

            override fun onEnded(event: DragAndDropEvent) {
                onEvent(ConverterInputEvents.DragExited)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                return try {
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        if (files.isNotEmpty()) {
                            onEvent(ConverterInputEvents.FilesDropped(files))
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header section
        Text(
            text = stringResource(Res.string.converter_subtitle),
            style = FluentTheme.typography.body,
            color = FluentTheme.colors.text.text.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Spacer(Modifier.weight(1f))

        // Drop zone
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dragAndDropTarget
                )
                .clickable {
                    onEvent(ConverterInputEvents.OpenFilePicker)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                if (state.isAnalyzing) {
                    ProgressRing()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.converter_analyzing),
                        style = FluentTheme.typography.body
                    )
                } else {
                    Icon(
                        imageVector = Icons.Regular.DocumentAdd,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (state.isDragging) FluentTheme.colors.fillAccent.default else FluentTheme.colors.text.text.secondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.converter_drop_zone),
                        style = FluentTheme.typography.bodyStrong,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.converter_or),
                            style = FluentTheme.typography.body,
                            color = FluentTheme.colors.text.text.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.converter_select_file),
                            style = FluentTheme.typography.body,
                            color = FluentTheme.colors.fillAccent.default
                        )
                    }
                }
            }
        }

        // Supported formats
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.converter_supported_formats),
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.tertiary,
            textAlign = TextAlign.Center
        )

        // Error message
        if (state.errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.errorMessage,
                color = FluentTheme.colors.system.critical,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))
    }
}
