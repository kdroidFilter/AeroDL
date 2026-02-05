package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> {
            val headerContent: @Composable (() -> Unit)? = if (label != null) {
                { AppText(label) }
            } else null
            val placeholderContent: @Composable (() -> Unit)? = if (placeholder.isNotEmpty()) {
                { AppText(placeholder) }
            } else null
            io.github.composefluent.component.TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                header = headerContent,
                placeholder = placeholderContent,
                enabled = enabled,
                singleLine = singleLine,
                leadingIcon = leadingIcon,
                trailing = trailingIcon?.let { icon -> { icon() } },
            )
        }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            placeholder = placeholder,
            label = label,
            enabled = enabled,
            singleLine = singleLine,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }
}
