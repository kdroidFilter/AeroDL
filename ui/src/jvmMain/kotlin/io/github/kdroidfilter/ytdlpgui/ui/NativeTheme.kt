package io.github.kdroidfilter.ytdlpgui.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import io.github.kdroidfilter.ytdlpgui.ui.component.NativeThemeImpl

val LocalNativeColors = staticCompositionLocalOf<NativeColors> {
    error("NativeTheme was not applied")
}

val LocalNativeTypography = staticCompositionLocalOf<NativeTypography> {
    error("NativeTheme was not applied")
}

val LocalNativeShapes = staticCompositionLocalOf<NativeShapes> {
    error("NativeTheme was not applied")
}

val LocalNativeContentColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun NativeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    NativeThemeImpl(darkTheme, content)
}

object NativeTheme {
    val colors: NativeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNativeColors.current

    val typography: NativeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNativeTypography.current

    val shapes: NativeShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalNativeShapes.current
}

class NativeColors(
    val system: System,
    val text: TextPalette,
    val fillAccent: FillAccent,
    val stroke: StrokePalette,
    val background: BackgroundPalette,
    val control: ControlPalette,
    val controlStrong: ControlStrongPalette,
) {
    class System(
        val success: Color,
        val critical: Color,
        val caution: Color,
        val neutral: Color,
        val attention: Color,
    )

    class TextPalette(
        val text: TextLevels,
        val onAccent: OnAccent,
    )

    class TextLevels(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val disabled: Color,
    )

    class OnAccent(
        val primary: Color,
        val disabled: Color,
    )

    class FillAccent(val default: Color)

    class StrokePalette(val control: StrokeControl) {
        class StrokeControl(val default: Color)
    }

    class BackgroundPalette(val layer: Layer) {
        class Layer(val default: Color)
    }

    class ControlPalette(val secondary: Color)

    class ControlStrongPalette(val default: Color)
}

class NativeTypography(
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
)

class NativeShapes(
    val control: Shape,
)

internal fun Color.contrastingOnColor(): Color =
    if (luminance() > 0.5f) Color.Black else Color.White
