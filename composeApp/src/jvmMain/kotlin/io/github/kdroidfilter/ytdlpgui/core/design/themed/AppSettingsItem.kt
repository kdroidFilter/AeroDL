package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppCardExpanderItem(
    heading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    caption: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.CardExpanderItem(
            heading = heading,
            caption = caption ?: {},
            icon = icon,
            trailing = trailing ?: {},
            modifier = modifier,
        )
        AppTheme.DARWIN -> {
            val colors = io.github.kdroidfilter.darwinui.theme.DarwinTheme.colors
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.card, shape)
                    .border(1.dp, colors.border, shape)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    heading()
                    if (caption != null) {
                        caption()
                    }
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    trailing()
                }
            }
        }
    }
}
