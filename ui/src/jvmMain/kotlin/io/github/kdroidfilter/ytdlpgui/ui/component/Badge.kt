package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeContentColor
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.contrastingOnColor

object BadgeDefaults {
    @Composable
    fun color(status: BadgeStatus): Color = when (status) {
        BadgeStatus.Informational, BadgeStatus.Attention -> NativeTheme.colors.system.attention
        BadgeStatus.InformationalSafe, BadgeStatus.Success -> NativeTheme.colors.system.success
        BadgeStatus.Caution -> NativeTheme.colors.system.caution
        BadgeStatus.Critical -> NativeTheme.colors.system.critical
    }

    @Composable
    fun contentColor(status: BadgeStatus): Color = color(status).contrastingOnColor()

    @Composable
    fun Icon(status: BadgeStatus, modifier: Modifier = Modifier) {
        Box(
            modifier
                .size(8.dp)
                .background(contentColor(status), CircleShape),
        )
    }
}

@Composable
fun Badge(
    status: BadgeStatus,
    backgroundColor: Color = BadgeDefaults.color(status),
    contentColor: Color = BadgeDefaults.contentColor(status),
    modifier: Modifier = Modifier,
    content: (@Composable (status: BadgeStatus) -> Unit)? = null,
) {
    Badge(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        modifier = modifier,
        content = content?.let { { it(status) } },
    )
}

@Composable
fun Badge(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = backgroundColor.contrastingOnColor(),
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier
            .defaultMinSize(if (content != null) 16.dp else 8.dp)
            .background(backgroundColor, CircleShape)
            .padding(if (content != null) 4.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            CompositionLocalProvider(LocalNativeContentColor provides contentColor) {
                content()
            }
        }
    }
}
