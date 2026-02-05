package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.design.icons.SettingsGear
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import kotlin.math.max
import kotlin.math.min

@Composable
fun AnimatedGears(
    modifier: Modifier = Modifier,
    // Base design sizes (will be scaled to fit)
    largeSize: Dp = 140.dp,
    smallSize: Dp = 120.dp,
    meshGap: Dp = 2.dp,
    largeColor: Color = Color(0xFF2196F3),
    smallColor: Color = Color(0xFFFF9800),
    largeDurationMs: Int = 3000,
    clockwiseLarge: Boolean = true,
    allowUpscale: Boolean = true
) {
    // ---- Animations --------------------------------------------------------------
    val infinite = rememberInfiniteTransition(label = "gears_rotation")

    val largeSign = if (clockwiseLarge) 1f else -1f
    val largeRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f * largeSign,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = largeDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "large_gear_rotation"
    )

    // Angular speed ratio: w ~ 1/r -> small rotates faster by (large / small)
    val speedRatio = (largeSize / smallSize).coerceAtLeast(0.01f)
    val smallDurationMs = max(200, (largeDurationMs / speedRatio).toInt())

    val smallSign = -largeSign
    val smallRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f * smallSign,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = smallDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "small_gear_rotation"
    )

    // ---- Layout & responsive fit -------------------------------------------------
    BoxWithConstraints(modifier = modifier) {
        // Bounding box of both gears with a visual gap
        val designWidth = largeSize + smallSize + meshGap
        val designHeight = maxOf(largeSize, smallSize)

        // Use Dp arithmetic: Dp / Dp -> Float (density cancels out)
        val fitScaleW = maxWidth / designWidth
        val fitScaleH = maxHeight / designHeight
        val rawScale = min(fitScaleW, fitScaleH)
        val scale = if (allowUpscale) rawScale else min(1f, rawScale)

        val largeScaled = largeSize * scale
        val smallScaled = smallSize * scale
        val gapScaled = meshGap * scale

        // Center the *group* of gears
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.wrapContentSize(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gapScaled, Alignment.CenterHorizontally)
            ) {
                AppIcon(
                    imageVector = SettingsGear,
                    contentDescription = "Large gear",
                    modifier = Modifier
                        .size(largeScaled)
                        .rotate(largeRotation),
                    tint = largeColor
                )
                AppIcon(
                    imageVector = SettingsGear,
                    contentDescription = "Small gear",
                    modifier = Modifier
                        .size(smallScaled)
                        .rotate(smallRotation),
                    tint = smallColor
                )
            }
        }
    }
}
