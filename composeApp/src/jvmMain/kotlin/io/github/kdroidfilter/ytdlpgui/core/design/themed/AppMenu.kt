package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppMenuContainer(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    trigger: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> {
            Box(modifier = modifier) {
                trigger()
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = onDismissRequest,
                    content = content,
                )
            }
        }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            trigger = trigger,
            content = content,
        )
    }
}

@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> androidx.compose.material3.DropdownMenuItem(
            text = { AppText(text) },
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.DropdownMenuItem(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
        ) {
            AppText(text)
        }
    }
}

@Composable
fun AppMenuSeparator(modifier: Modifier = Modifier) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> HorizontalDivider(modifier = modifier)
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.DropdownMenuSeparator(modifier = modifier)
    }
}
