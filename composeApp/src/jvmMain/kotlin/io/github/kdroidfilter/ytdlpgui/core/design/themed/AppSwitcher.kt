package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppSwitcher(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.Switcher(
            checked = checked,
            onCheckStateChange = onCheckedChange,
            text = label,
            enabled = enabled,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.Switcher(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            label = label,
            enabled = enabled,
        )
    }
}
