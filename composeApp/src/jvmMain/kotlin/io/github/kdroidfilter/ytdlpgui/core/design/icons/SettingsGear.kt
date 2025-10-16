package io.github.kdroidfilter.ytdlpgui.core.design.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SettingsGear: ImageVector
    get() {
        if (_SettingsGear != null) return _SettingsGear!!
        
        _SettingsGear = ImageVector.Builder(
            name = "SettingsGear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(19.85f, 8.75f)
                lineToRelative(4.15f, 0.83f)
                verticalLineToRelative(4.84f)
                lineToRelative(-4.15f, 0.83f)
                lineToRelative(2.35f, 3.52f)
                lineToRelative(-3.43f, 3.43f)
                lineToRelative(-3.52f, -2.35f)
                lineToRelative(-0.83f, 4.15f)
                horizontalLineTo(9.58f)
                lineToRelative(-0.83f, -4.15f)
                lineToRelative(-3.52f, 2.35f)
                lineToRelative(-3.43f, -3.43f)
                lineToRelative(2.35f, -3.52f)
                lineTo(0f, 14.42f)
                verticalLineTo(9.58f)
                lineToRelative(4.15f, -0.83f)
                lineTo(1.8f, 5.23f)
                lineTo(5.23f, 1.8f)
                lineToRelative(3.52f, 2.35f)
                lineTo(9.58f, 0f)
                horizontalLineToRelative(4.84f)
                lineToRelative(0.83f, 4.15f)
                lineToRelative(3.52f, -2.35f)
                lineToRelative(3.43f, 3.43f)
                lineToRelative(-2.35f, 3.52f)
                close()
                moveToRelative(-1.57f, 5.07f)
                lineToRelative(4f, -0.81f)
                verticalLineToRelative(-2f)
                lineToRelative(-4f, -0.81f)
                lineToRelative(-0.54f, -1.3f)
                lineToRelative(2.29f, -3.43f)
                lineToRelative(-1.43f, -1.43f)
                lineToRelative(-3.43f, 2.29f)
                lineToRelative(-1.3f, -0.54f)
                lineToRelative(-0.81f, -4f)
                horizontalLineToRelative(-2f)
                lineToRelative(-0.81f, 4f)
                lineToRelative(-1.3f, 0.54f)
                lineToRelative(-3.43f, -2.29f)
                lineToRelative(-1.43f, 1.43f)
                lineTo(6.38f, 8.9f)
                lineToRelative(-0.54f, 1.3f)
                lineToRelative(-4f, 0.81f)
                verticalLineToRelative(2f)
                lineToRelative(4f, 0.81f)
                lineToRelative(0.54f, 1.3f)
                lineToRelative(-2.29f, 3.43f)
                lineToRelative(1.43f, 1.43f)
                lineToRelative(3.43f, -2.29f)
                lineToRelative(1.3f, 0.54f)
                lineToRelative(0.81f, 4f)
                horizontalLineToRelative(2f)
                lineToRelative(0.81f, -4f)
                lineToRelative(1.3f, -0.54f)
                lineToRelative(3.43f, 2.29f)
                lineToRelative(1.43f, -1.43f)
                lineToRelative(-2.29f, -3.43f)
                lineToRelative(0.54f, -1.3f)
                close()
                moveToRelative(-8.186f, -4.672f)
                arcTo(3.43f, 3.43f, 0f, false, true, 12f, 8.57f)
                arcTo(3.44f, 3.44f, 0f, false, true, 15.43f, 12f)
                arcToRelative(3.43f, 3.43f, 0f, true, true, -5.336f, -2.852f)
                close()
                moveToRelative(0.956f, 4.274f)
                curveToRelative(0.281f, 0.188f, 0.612f, 0.288f, 0.95f, 0.288f)
                arcTo(1.7f, 1.7f, 0f, false, false, 13.71f, 12f)
                arcToRelative(1.71f, 1.71f, 0f, true, false, -2.66f, 1.422f)
                close()
            }
        }.build()
        
        return _SettingsGear!!
    }

private var _SettingsGear: ImageVector? = null

