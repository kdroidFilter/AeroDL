package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

enum class AppBadgeStatus {
    Success,
    Critical,
    Warning,
    Info,
    Default,
}

@Composable
fun AppBadge(
    status: AppBadgeStatus = AppBadgeStatus.Default,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> {
            val fluentStatus = when (status) {
                AppBadgeStatus.Success -> io.github.composefluent.component.BadgeStatus.Success
                AppBadgeStatus.Critical -> io.github.composefluent.component.BadgeStatus.Critical
                AppBadgeStatus.Warning -> io.github.composefluent.component.BadgeStatus.Caution
                AppBadgeStatus.Info -> io.github.composefluent.component.BadgeStatus.Attention
                AppBadgeStatus.Default -> io.github.composefluent.component.BadgeStatus.Attention
            }
            io.github.composefluent.component.Badge(
                status = fluentStatus,
                modifier = modifier,
            ) { _ ->
                content()
            }
        }
        AppTheme.DARWIN -> {
            val darwinVariant = when (status) {
                AppBadgeStatus.Success -> io.github.kdroidfilter.darwinui.components.BadgeVariant.Success
                AppBadgeStatus.Critical -> io.github.kdroidfilter.darwinui.components.BadgeVariant.Destructive
                AppBadgeStatus.Warning -> io.github.kdroidfilter.darwinui.components.BadgeVariant.Warning
                AppBadgeStatus.Info -> io.github.kdroidfilter.darwinui.components.BadgeVariant.Info
                AppBadgeStatus.Default -> io.github.kdroidfilter.darwinui.components.BadgeVariant.Default
            }
            io.github.kdroidfilter.darwinui.components.Badge(
                variant = darwinVariant,
                modifier = modifier,
                content = content,
            )
        }
    }
}
