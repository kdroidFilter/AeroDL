package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.component.Text
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.converter_trim
import ytdlpgui.composeapp.generated.resources.converter_trim_duration

/**
 * Reusable trim/cut slider component for selecting a time range.
 * Used in both Converter and Download screens.
 */
@Composable
fun TrimSlider(
    trimStartMs: Long,
    trimEndMs: Long,
    totalDurationMs: Long,
    isTrimmed: Boolean,
    onTrimRangeChange: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.converter_trim),
            style = NativeTheme.typography.bodyStrong,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(trimStartMs),
                style = NativeTheme.typography.caption,
                color = NativeTheme.colors.text.text.secondary
            )
            Text(
                text = formatDuration(trimEndMs),
                style = NativeTheme.typography.caption,
                color = NativeTheme.colors.text.text.secondary
            )
        }

        // RangeSlider
        RangeSlider(
            value = trimStartMs.toFloat()..trimEndMs.toFloat(),
            onValueChange = { range ->
                onTrimRangeChange(range.start.toLong(), range.endInclusive.toLong())
            },
            valueRange = 0f..totalDurationMs.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = NativeTheme.colors.fillAccent.default,
                activeTrackColor = NativeTheme.colors.fillAccent.default,
                inactiveTrackColor = NativeTheme.colors.stroke.control.default
            )
        )

        // Duration info
        if (isTrimmed) {
            Text(
                text = stringResource(
                    Res.string.converter_trim_duration,
                    formatDuration(trimEndMs - trimStartMs)
                ),
                style = NativeTheme.typography.caption,
                color = NativeTheme.colors.text.text.secondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Formats milliseconds to a human-readable duration string (H:MM:SS or M:SS).
 */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
