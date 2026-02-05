package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> androidx.compose.material.Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (tint == Color.Unspecified) io.github.composefluent.FluentTheme.colors.text.text.primary else tint,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (tint == Color.Unspecified) io.github.kdroidfilter.darwinui.theme.LocalDarwinContentColor.current else tint,
        )
    }
}
