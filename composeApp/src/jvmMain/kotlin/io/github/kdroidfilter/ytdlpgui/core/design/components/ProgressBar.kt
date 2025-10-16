package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.animation.FluentDuration

/**
 * A determinate progress bar that displays progress from 0f to 1f.
 *
 * RTL support: in RTL layout direction, the fill grows from right to left.
 *
 * @param progress Current progress, clamped to [0f, 1f].
 * @param modifier Modifier for styling and layout.
 * @param color Color of the progress track.
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = FluentTheme.colors.fillAccent.default
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = modifier.defaultMinSize(minWidth = 130.dp, minHeight = 3.dp),
        propagateMinConstraints = true,
        contentAlignment = Alignment.CenterStart
    ) {
        Rail()
        Box(Modifier.matchParentSize()) {
            Track(
                progress = progress.coerceIn(0f, 1f),
                color = color,
                // Mirror the canvas in RTL so computations stay identical
                rtlMirror = isRtl
            )
        }
    }
}

@Composable
private fun Rail() {
    Box(
        Modifier
            .requiredHeight(1.dp)
            .background(FluentTheme.colors.controlStrong.default, CircleShape)
    )
}

private val TrackWidth = 3.dp

@Composable
private fun Track(
    progress: Float,
    color: Color,
    rtlMirror: Boolean
) {
    val canvasMod = Modifier
        .fillMaxSize()
        .let {
            if (rtlMirror) {
                it.graphicsLayer {
                    // Mirror horizontally around center to flip direction in RTL
                    scaleX = -1f
                    transformOrigin = TransformOrigin.Center
                }
            } else it
        }

    Canvas(canvasMod) {
        if (progress > 0f) {
            val half = (TrackWidth / 2).toPx()
            // Same math for both LTR and RTL thanks to the canvas mirroring
            drawLine(
                color = color,
                start = Offset(half, half),
                end = Offset(progress * (size.width - half), half),
                strokeWidth = TrackWidth.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private val LongWidth = 100.dp
private val ShortWidth = 50.dp
private val Easing = CubicBezierEasing(0.5f, 0f, 0.5f, 1.0f)

/**
 * An indeterminate progress bar with a continuous looping animation.
 *
 * RTL support: in RTL layout direction, the animation runs right-to-left.
 *
 * @param modifier Modifier to apply.
 * @param color Color of the moving segments.
 */
@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    color: Color = FluentTheme.colors.fillAccent.default
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier.defaultMinSize(minWidth = 130.dp, minHeight = 3.dp),
        contentAlignment = Alignment.CenterStart,
        propagateMinConstraints = true
    ) {
        // NOTE: Fluent specs show a rail for indeterminate, but WinUI3 Gallery often omits it.
        // If you want it, uncomment:
        // Rail()

        Box(Modifier.matchParentSize()) {
            val infinite = rememberInfiniteTransition()
            val progress by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = InfiniteRepeatableSpec(
                    animation = tween(
                        durationMillis = FluentDuration.VeryLongDuration * 3,
                        easing = Easing
                    )
                )
            )

            val canvasMod = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .let {
                    if (isRtl) {
                        it.graphicsLayer {
                            // Mirror horizontally to reverse travel direction in RTL
                            scaleX = -1f
                            transformOrigin = TransformOrigin.Center
                        }
                    } else it
                }

            /*
                |               totalWidth                 |
                |          preWidth         |  size.width  |
                |  long  | size.width |short|  size.width  |
                 --------[            ]-----[ display area ]
                |          preWidth         |  size.width  |  long  | size.width |short|
                                            [ display area ]--------[            ]-----
             */
            Canvas(canvasMod) {
                val trackPx = TrackWidth.toPx()
                val half = trackPx / 2

                val shortWidthPx = ShortWidth.toPx()
                val longWidthPx = LongWidth.toPx()

                val preWidth = shortWidthPx + size.width + longWidthPx
                val totalWidth = size.width + preWidth

                val shortOffset = (progress * totalWidth + longWidthPx + size.width) - preWidth
                val shortStart = half + shortOffset
                val shortEnd = shortStart + shortWidthPx - half

                val longOffset = (progress * totalWidth) - preWidth
                val longStart = half + longOffset
                val longEnd = longStart + longWidthPx - half

                // Short segment
                drawLine(
                    color = color,
                    start = Offset(shortStart, half),
                    end = Offset(shortEnd, half),
                    strokeWidth = trackPx,
                    cap = StrokeCap.Round
                )

                // Long segment
                drawLine(
                    color = color,
                    start = Offset(longStart, half),
                    end = Offset(longEnd, half),
                    strokeWidth = trackPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
