package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography

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
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    enabled = hasOverflow,
                    indication = null,
                    interactionSource = null
                ) { expanded = !expanded }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = description,
                style = AppTypography.body,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesCollapsed,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (hasOverflow) {
                Spacer(Modifier.width(8.dp))
                AppIcon(
                    AppIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees)
                )
            }
        }
    }
}
