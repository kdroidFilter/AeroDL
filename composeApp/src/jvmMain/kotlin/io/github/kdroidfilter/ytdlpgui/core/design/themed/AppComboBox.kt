package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppComboBox(
    items: List<String>,
    selectedIndex: Int?,
    onSelectionChange: (index: Int, item: String) -> Unit,
    modifier: Modifier = Modifier,
    header: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.ComboBox(
            items = items,
            selected = selectedIndex,
            onSelectionChange = onSelectionChange,
            modifier = modifier,
            header = header,
            placeholder = placeholder,
            disabled = !enabled,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.ComboBox(
            items = items,
            selected = selectedIndex,
            onSelectionChange = onSelectionChange,
            modifier = modifier,
            header = header,
            placeholder = placeholder,
            disabled = !enabled,
        )
    }
}
