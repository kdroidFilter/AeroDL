package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme

internal val LocalTooltipTextSink = staticCompositionLocalOf<MutableState<String>?> { null }

@Composable
fun TooltipBox(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBoxImpl(tooltip, modifier, content)
}

@Composable
internal fun DefaultTooltipBox(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val density = LocalDensity.current
    val positionProvider = remember(density) { OutsideAnchorPositionProvider(density) }
    Box(modifier = modifier.hoverable(interactionSource)) {
        content()
        if (hovered) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                val shape = RoundedCornerShape(6.dp)
                Box(
                    Modifier
                        .shadow(4.dp, shape)
                        .background(NativeTheme.colors.background.layer.default, shape)
                        .border(1.dp, NativeTheme.colors.stroke.control.default, shape)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    tooltip()
                }
            }
        }
    }
}

/**
 * Places the popup fully outside the anchor so it cannot sit under the cursor
 * and retrigger hover Enter/Exit on desktop.
 */
private class OutsideAnchorPositionProvider(
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = with(density) { 8.dp.roundToPx() }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(0, maxX)
        val yBelow = anchorBounds.bottom + gap
        val y = if (yBelow + popupContentSize.height <= windowSize.height) {
            yBelow
        } else {
            (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}
