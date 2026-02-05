package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

enum class AppDialogButton {
    Primary,
    Secondary,
    Close,
}

@Composable
fun AppContentDialog(
    title: String,
    visible: Boolean,
    content: @Composable () -> Unit,
    primaryButtonText: String,
    secondaryButtonText: String? = null,
    closeButtonText: String? = null,
    onButtonClick: (AppDialogButton) -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.ContentDialog(
            title = title,
            visible = visible,
            content = content,
            primaryButtonText = primaryButtonText,
            secondaryButtonText = secondaryButtonText,
            closeButtonText = closeButtonText,
            onButtonClick = { fluentButton ->
                val mapped = when (fluentButton) {
                    io.github.composefluent.component.ContentDialogButton.Primary -> AppDialogButton.Primary
                    io.github.composefluent.component.ContentDialogButton.Secondary -> AppDialogButton.Secondary
                    io.github.composefluent.component.ContentDialogButton.Close -> AppDialogButton.Close
                }
                onButtonClick(mapped)
            },
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.ContentDialog(
            title = title,
            visible = visible,
            content = content,
            primaryButtonText = primaryButtonText,
            secondaryButtonText = secondaryButtonText,
            closeButtonText = closeButtonText,
            onButtonClick = { darwinButton ->
                val mapped = when (darwinButton) {
                    io.github.kdroidfilter.darwinui.components.ContentDialogButton.Primary -> AppDialogButton.Primary
                    io.github.kdroidfilter.darwinui.components.ContentDialogButton.Secondary -> AppDialogButton.Secondary
                    io.github.kdroidfilter.darwinui.components.ContentDialogButton.Close -> AppDialogButton.Close
                }
                onButtonClick(mapped)
            },
        )
    }
}
