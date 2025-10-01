package io.github.kdroidfilter.ytdlpgui.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.animation.FluentDuration
import io.github.composefluent.animation.FluentEasing
import io.github.composefluent.component.SwitcherDefaults
import io.github.composefluent.component.SwitcherStyleScheme
import io.github.composefluent.component.Text
import io.github.composefluent.scheme.collectVisualState

@Composable
fun Switcher(
    checked: Boolean,
    onCheckStateChange: (checked: Boolean) -> Unit,
    text: String? = null,
    // Interpreted as “text on the START side” so it’s mirrored in RTL automatically
    textBefore: Boolean = false,
    enabled: Boolean = true,
    styles: SwitcherStyleScheme = if (checked) {
        SwitcherDefaults.selectedSwitcherStyle()
    } else {
        SwitcherDefaults.defaultSwitcherStyle()
    },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    // --- Layout direction (RTL / LTR) ---
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Visual state & style
    val transition = updateTransition(targetState = checked, label = "switch-checked")
    val style = styles.schemeFor(interactionSource.collectVisualState(!enabled))

    // Row is toggleable for proper accessibility semantics (role = Switch)
    Row(
        modifier = Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckStateChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Place the label on the START side. In RTL, START is right.
        if (textBefore) {
            text?.let {
                Text(
                    modifier = Modifier.offset(y = (-1).dp),
                    text = it,
                    color = style.labelColor,
                )
                Spacer(Modifier.width(12.dp))
            }
        }

        val fillColor by animateColorAsState(
            targetValue = style.fillColor,
            animationSpec = tween(FluentDuration.QuickDuration, easing = FluentEasing.FastInvokeEasing),
            label = "fill-color"
        )

        // Track
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 20.dp)
                .border(1.dp, style.borderBrush, CircleShape)
                .clip(CircleShape)
                .background(fillColor)
                .padding(horizontal = 4.dp),
            // Start means left in LTR and right in RTL – exactly what we want
            contentAlignment = Alignment.CenterStart
        ) {
            // Thumb size animations
            val height by animateDpAsState(
                targetValue = style.controlSize.height,
                animationSpec = tween(FluentDuration.QuickDuration, easing = FluentEasing.FastInvokeEasing),
                label = "thumb-height"
            )
            val width by animateDpAsState(
                targetValue = style.controlSize.width,
                animationSpec = tween(FluentDuration.QuickDuration, easing = FluentEasing.FastInvokeEasing),
                label = "thumb-width"
            )

            // Distance to travel from START to END within the padded track.
            // Using the same magnitude as your original logic for visual parity.
            val travel by transition.animateDp(
                transitionSpec = {
                    tween(FluentDuration.QuickDuration, easing = FluentEasing.PointToPointEasing)
                },
                label = "thumb-offset",
                targetValueByState = { isOn ->
                    if (isOn) 26.dp - (width / 2) else 0.dp
                }
            )

            // Convert to px and mirror in RTL by flipping the sign
            val density = LocalDensity.current
            val offsetX by remember(density, travel) {
                derivedStateOf { with(density) { travel.toPx() } }
            }
            val mirroredOffsetX = if (isRtl) -offsetX else offsetX

            // Thumb
            Box(
                Modifier
                    .size(width = width, height = height)
                    .graphicsLayer {
                        translationX = mirroredOffsetX
                        transformOrigin = TransformOrigin.Center
                    }
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            if (!enabled) FluentTheme.colors.text.onAccent.disabled
                            else FluentTheme.colors.text.onAccent.primary
                        } else {
                            if (!enabled) FluentTheme.colors.text.text.disabled
                            else FluentTheme.colors.text.text.secondary
                        }
                    )
            )
        }

        // Trailing label (END side)
        if (!textBefore) {
            text?.let {
                Spacer(Modifier.width(12.dp))
                Text(
                    modifier = Modifier.offset(y = (-1).dp),
                    text = it,
                    style = FluentTheme.typography.body,
                    color = style.labelColor
                )
            }
        }
    }
}
