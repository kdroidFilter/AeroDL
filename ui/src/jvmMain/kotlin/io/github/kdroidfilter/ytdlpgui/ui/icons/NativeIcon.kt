package io.github.kdroidfilter.ytdlpgui.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Opaque icon handle. Linux stores a Yaru glyph, Fluent stores an ImageVector.
 */
class NativeIcon internal constructor(
    internal val imageVector: ImageVector? = null,
    internal val glyph: Char? = null,
)
