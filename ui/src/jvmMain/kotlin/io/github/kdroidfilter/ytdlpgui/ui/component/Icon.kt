package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toolingGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeContentColor
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.icons.NativeIcon

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = tint.takeOrElse {
        LocalNativeContentColor.current.takeOrElse { NativeTheme.colors.text.text.primary }
    }
    Icon(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = resolvedTint,
    )
}

@Composable
fun Icon(
    icon: NativeIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    when {
        icon.glyph != null || !icon.sfSymbolName.isNullOrEmpty() ->
            GlyphIcon(icon, contentDescription, modifier, tint)
        icon.imageVector != null ->
            Icon(icon.imageVector, contentDescription, modifier, tint)
    }
}

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = tint.takeOrElse {
        LocalNativeContentColor.current.takeOrElse { NativeTheme.colors.text.text.primary }
    }
    val colorFilter = if (resolvedTint == Color.Unspecified) null else ColorFilter.tint(resolvedTint)
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .toolingGraphicsLayer()
            .then(
                if (painter.intrinsicSize == Size.Unspecified ||
                    (painter.intrinsicSize.width.isInfinite() && painter.intrinsicSize.height.isInfinite())
                ) {
                    Modifier.size(16.dp)
                } else {
                    Modifier
                },
            )
            .paint(painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
            .then(semantics),
    )
}
