package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.component.Icon
import io.github.kdroidfilter.ytdlpgui.ui.component.Text
import io.github.kdroidfilter.ytdlpgui.ui.icons.Icons

@Composable
fun ExpandableDescription(
    description: String,
    modifier: Modifier = Modifier,
    maxLinesCollapsed: Int = 2
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember(description) { mutableStateOf(false) }
    val degrees by animateFloatAsState(if (expanded) -90f else 90f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(NativeTheme.shapes.control)
                .clickable(
                    enabled = hasOverflow,
                    indication = null,
                    interactionSource = null
                ) { expanded = !expanded }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = description,
                style = NativeTheme.typography.body,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesCollapsed,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                onTextLayout = { textLayoutResult ->
                    if (!expanded && textLayoutResult.hasVisualOverflow) {
                        hasOverflow = true
                    }
                }
            )
            if (hasOverflow) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Regular.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees)
                )
            }
        }
    }
}