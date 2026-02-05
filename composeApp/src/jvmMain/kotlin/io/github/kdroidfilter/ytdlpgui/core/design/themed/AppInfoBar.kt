package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

enum class AppInfoBarSeverity {
    Info,
    Success,
    Warning,
    Error,
}

@Composable
fun AppInfoBar(
    title: String,
    message: String,
    severity: AppInfoBarSeverity = AppInfoBarSeverity.Info,
    modifier: Modifier = Modifier,
    isOpen: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
) {
    if (!isOpen) return

    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> {
            val fluentSeverity = when (severity) {
                AppInfoBarSeverity.Info -> io.github.composefluent.component.InfoBarSeverity.Informational
                AppInfoBarSeverity.Success -> io.github.composefluent.component.InfoBarSeverity.Success
                AppInfoBarSeverity.Warning -> io.github.composefluent.component.InfoBarSeverity.Warning
                AppInfoBarSeverity.Error -> io.github.composefluent.component.InfoBarSeverity.Critical
            }
            val closeActionContent: @Composable (() -> Unit)? = if (onDismiss != null) {
                {
                    io.github.composefluent.component.InfoBarDefaults.CloseActionButton(
                        onClick = onDismiss
                    )
                }
            } else null
            io.github.composefluent.component.InfoBar(
                title = { AppText(title) },
                message = { AppText(message) },
                severity = fluentSeverity,
                modifier = modifier,
                action = action,
                icon = icon,
                closeAction = closeActionContent,
            )
        }
        AppTheme.DARWIN -> {
            val darwinType = when (severity) {
                AppInfoBarSeverity.Info -> io.github.kdroidfilter.darwinui.components.AlertType.Info
                AppInfoBarSeverity.Success -> io.github.kdroidfilter.darwinui.components.AlertType.Success
                AppInfoBarSeverity.Warning -> io.github.kdroidfilter.darwinui.components.AlertType.Warning
                AppInfoBarSeverity.Error -> io.github.kdroidfilter.darwinui.components.AlertType.Error
            }
            io.github.kdroidfilter.darwinui.components.AlertBanner(
                message = message,
                title = title,
                type = darwinType,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
    }
}
