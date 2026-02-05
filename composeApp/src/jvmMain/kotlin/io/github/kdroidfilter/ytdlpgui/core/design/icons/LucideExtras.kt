package io.github.kdroidfilter.ytdlpgui.core.design.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// =============================================================================
// Clock (for History)
// =============================================================================

val LucideClock: ImageVector
    get() {
        if (_LucideClock != null) return _LucideClock!!
        _LucideClock = ImageVector.Builder(
            name = "clock", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(22f, 12f); arcTo(10f, 10f, 0f, false, true, 12f, 22f); arcTo(10f, 10f, 0f, false, true, 2f, 12f); arcTo(10f, 10f, 0f, false, true, 22f, 12f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 6f); verticalLineToRelative(6f); lineToRelative(4f, 2f)
            }
        }.build()
        return _LucideClock!!
    }
private var _LucideClock: ImageVector? = null

// =============================================================================
// MoreVertical (for overflow menu)
// =============================================================================

val LucideMoreVertical: ImageVector
    get() {
        if (_LucideMoreVertical != null) return _LucideMoreVertical!!
        _LucideMoreVertical = ImageVector.Builder(
            name = "more-vertical", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(13f, 12f); arcTo(1f, 1f, 0f, false, true, 12f, 13f); arcTo(1f, 1f, 0f, false, true, 11f, 12f); arcTo(1f, 1f, 0f, false, true, 13f, 12f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(13f, 5f); arcTo(1f, 1f, 0f, false, true, 12f, 6f); arcTo(1f, 1f, 0f, false, true, 11f, 5f); arcTo(1f, 1f, 0f, false, true, 13f, 5f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(13f, 19f); arcTo(1f, 1f, 0f, false, true, 12f, 20f); arcTo(1f, 1f, 0f, false, true, 11f, 19f); arcTo(1f, 1f, 0f, false, true, 13f, 19f); close()
            }
        }.build()
        return _LucideMoreVertical!!
    }
private var _LucideMoreVertical: ImageVector? = null

// =============================================================================
// Cookie
// =============================================================================

val LucideCookie: ImageVector
    get() {
        if (_LucideCookie != null) return _LucideCookie!!
        _LucideCookie = ImageVector.Builder(
            name = "cookie", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2f)
                arcToRelative(10f, 10f, 0f, true, false, 10f, 10f)
                arcToRelative(4f, 4f, 0f, false, true, -4f, -4f)
                arcToRelative(4f, 4f, 0f, false, true, -4f, -4f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8.5f, 16.5f); horizontalLineToRelative(0.01f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 15.5f); horizontalLineToRelative(0.01f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 12f); horizontalLineToRelative(0.01f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(11f, 8f); horizontalLineToRelative(0.01f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(7.5f, 11f); horizontalLineToRelative(0.01f)
            }
        }.build()
        return _LucideCookie!!
    }
private var _LucideCookie: ImageVector? = null

// =============================================================================
// Pencil (for DocumentEdit)
// =============================================================================

val LucidePencil: ImageVector
    get() {
        if (_LucidePencil != null) return _LucidePencil!!
        _LucidePencil = ImageVector.Builder(
            name = "pencil", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21.174f, 6.812f)
                arcToRelative(1f, 1f, 0f, false, false, -3.986f, -3.987f)
                lineTo(3.842f, 16.174f)
                arcToRelative(2f, 2f, 0f, false, false, -0.5f, 0.83f)
                lineToRelative(-1.321f, 4.352f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0.623f, 0.622f)
                lineToRelative(4.353f, -1.32f)
                arcToRelative(2f, 2f, 0f, false, false, 0.83f, -0.497f)
                close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 5f); lineToRelative(4f, 4f)
            }
        }.build()
        return _LucidePencil!!
    }
private var _LucidePencil: ImageVector? = null

// =============================================================================
// Music (for MusicNote)
// =============================================================================

val LucideMusic: ImageVector
    get() {
        if (_LucideMusic != null) return _LucideMusic!!
        _LucideMusic = ImageVector.Builder(
            name = "music", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 18f); verticalLineTo(5f); lineToRelative(12f, -2f); verticalLineToRelative(13f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 18f); arcTo(3f, 3f, 0f, false, true, 6f, 21f); arcTo(3f, 3f, 0f, false, true, 3f, 18f); arcTo(3f, 3f, 0f, false, true, 9f, 18f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21f, 16f); arcTo(3f, 3f, 0f, false, true, 18f, 19f); arcTo(3f, 3f, 0f, false, true, 15f, 16f); arcTo(3f, 3f, 0f, false, true, 21f, 16f); close()
            }
        }.build()
        return _LucideMusic!!
    }
private var _LucideMusic: ImageVector? = null

// =============================================================================
// Zap (for Flash)
// =============================================================================

val LucideZap: ImageVector
    get() {
        if (_LucideZap != null) return _LucideZap!!
        _LucideZap = ImageVector.Builder(
            name = "zap", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 14f); arcToRelative(1f, 1f, 0f, false, true, -0.78f, -1.63f); lineToRelative(9.9f, -10.2f); arcToRelative(0.5f, 0.5f, 0f, false, true, 0.86f, 0.46f); lineToRelative(-1.92f, 6.02f); arcTo(1f, 1f, 0f, false, false, 13f, 10f); horizontalLineToRelative(7f); arcToRelative(1f, 1f, 0f, false, true, 0.78f, 1.63f); lineToRelative(-9.9f, 10.2f); arcToRelative(0.5f, 0.5f, 0f, false, true, -0.86f, -0.46f); lineToRelative(1.92f, -6.02f); arcTo(1f, 1f, 0f, false, false, 11f, 14f); close()
            }
        }.build()
        return _LucideZap!!
    }
private var _LucideZap: ImageVector? = null

// =============================================================================
// Gauge (for TopSpeed)
// =============================================================================

val LucideGauge: ImageVector
    get() {
        if (_LucideGauge != null) return _LucideGauge!!
        _LucideGauge = ImageVector.Builder(
            name = "gauge", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 16f); lineToRelative(3.5f, -6f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.34f, 19f)
                arcToRelative(10f, 10f, 0f, true, true, 17.32f, 0f)
            }
        }.build()
        return _LucideGauge!!
    }
private var _LucideGauge: ImageVector? = null

// =============================================================================
// Power
// =============================================================================

val LucidePower: ImageVector
    get() {
        if (_LucidePower != null) return _LucidePower!!
        _LucidePower = ImageVector.Builder(
            name = "power", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2f); verticalLineToRelative(10f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(18.4f, 6.6f)
                arcToRelative(9f, 9f, 0f, true, true, -12.77f, 0.04f)
            }
        }.build()
        return _LucidePower!!
    }
private var _LucidePower: ImageVector? = null

// =============================================================================
// Globe
// =============================================================================

val LucideGlobe: ImageVector
    get() {
        if (_LucideGlobe != null) return _LucideGlobe!!
        _LucideGlobe = ImageVector.Builder(
            name = "globe", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(22f, 12f); arcTo(10f, 10f, 0f, false, true, 12f, 22f); arcTo(10f, 10f, 0f, false, true, 2f, 12f); arcTo(10f, 10f, 0f, false, true, 22f, 12f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2f)
                arcToRelative(14.5f, 14.5f, 0f, false, false, 0f, 20f)
                arcToRelative(14.5f, 14.5f, 0f, false, false, 0f, -20f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(2f, 12f); horizontalLineToRelative(20f)
            }
        }.build()
        return _LucideGlobe!!
    }
private var _LucideGlobe: ImageVector? = null

// =============================================================================
// Shield (for LockShield)
// =============================================================================

val LucideShield: ImageVector
    get() {
        if (_LucideShield != null) return _LucideShield!!
        _LucideShield = ImageVector.Builder(
            name = "shield", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 13f)
                curveToRelative(0f, 5f, -3.5f, 7.5f, -7.66f, 8.95f)
                arcToRelative(1f, 1f, 0f, false, true, -0.67f, -0.01f)
                curveTo(7.5f, 20.5f, 4f, 18f, 4f, 13f)
                verticalLineTo(6f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                curveToRelative(2f, 0f, 4.5f, -1.2f, 6.24f, -2.72f)
                arcToRelative(1.17f, 1.17f, 0f, false, true, 1.52f, 0f)
                curveTo(14.51f, 3.81f, 17f, 5f, 19f, 5f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                close()
            }
        }.build()
        return _LucideShield!!
    }
private var _LucideShield: ImageVector? = null

// =============================================================================
// ClipboardPaste
// =============================================================================

val LucideClipboardPaste: ImageVector
    get() {
        if (_LucideClipboardPaste != null) return _LucideClipboardPaste!!
        _LucideClipboardPaste = ImageVector.Builder(
            name = "clipboard-paste", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 2f); horizontalLineTo(9f); arcToRelative(1f, 1f, 0f, false, false, -1f, 1f); verticalLineToRelative(2f); curveToRelative(0f, 0.6f, 0.4f, 1f, 1f, 1f); horizontalLineToRelative(6f); curveToRelative(0.6f, 0f, 1f, -0.4f, 1f, -1f); verticalLineTo(3f); curveToRelative(0f, -0.6f, -0.4f, -1f, -1f, -1f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 4f); horizontalLineToRelative(2f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); verticalLineToRelative(14f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); horizontalLineTo(6f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); verticalLineTo(6f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); horizontalLineToRelative(2f)
            }
        }.build()
        return _LucideClipboardPaste!!
    }
private var _LucideClipboardPaste: ImageVector? = null

// =============================================================================
// Clipboard
// =============================================================================

val LucideClipboard: ImageVector
    get() {
        if (_LucideClipboard != null) return _LucideClipboard!!
        _LucideClipboard = ImageVector.Builder(
            name = "clipboard", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 2f); horizontalLineTo(9f); arcToRelative(1f, 1f, 0f, false, false, -1f, 1f); verticalLineToRelative(2f); curveToRelative(0f, 0.6f, 0.4f, 1f, 1f, 1f); horizontalLineToRelative(6f); curveToRelative(0.6f, 0f, 1f, -0.4f, 1f, -1f); verticalLineTo(3f); curveToRelative(0f, -0.6f, -0.4f, -1f, -1f, -1f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 4f); horizontalLineToRelative(2f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); verticalLineToRelative(14f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); horizontalLineTo(6f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); verticalLineTo(6f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); horizontalLineToRelative(2f)
            }
        }.build()
        return _LucideClipboard!!
    }
private var _LucideClipboard: ImageVector? = null

// =============================================================================
// SquareCheck (for CheckboxChecked)
// =============================================================================

val LucideSquareCheck: ImageVector
    get() {
        if (_LucideSquareCheck != null) return _LucideSquareCheck!!
        _LucideSquareCheck = ImageVector.Builder(
            name = "square-check", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 11f); lineToRelative(3f, 3f); lineTo(21f, 4f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21f, 12f); verticalLineToRelative(7f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); horizontalLineTo(5f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); verticalLineTo(5f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); horizontalLineToRelative(11f)
            }
        }.build()
        return _LucideSquareCheck!!
    }
private var _LucideSquareCheck: ImageVector? = null

// =============================================================================
// Play
// =============================================================================

val LucidePlay: ImageVector
    get() {
        if (_LucidePlay != null) return _LucidePlay!!
        _LucidePlay = ImageVector.Builder(
            name = "play", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 3f); lineToRelative(14f, 9f); lineToRelative(-14f, 9f); close()
            }
        }.build()
        return _LucidePlay!!
    }
private var _LucidePlay: ImageVector? = null

// =============================================================================
// Pause
// =============================================================================

val LucidePause: ImageVector
    get() {
        if (_LucidePause != null) return _LucidePause!!
        _LucidePause = ImageVector.Builder(
            name = "pause", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 4f); horizontalLineToRelative(4f); verticalLineToRelative(16f); horizontalLineTo(6f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14f, 4f); horizontalLineToRelative(4f); verticalLineToRelative(16f); horizontalLineTo(14f); close()
            }
        }.build()
        return _LucidePause!!
    }
private var _LucidePause: ImageVector? = null

// =============================================================================
// Video
// =============================================================================

val LucideVideo: ImageVector
    get() {
        if (_LucideVideo != null) return _LucideVideo!!
        _LucideVideo = ImageVector.Builder(
            name = "video", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 5f); horizontalLineTo(4f); arcToRelative(2f, 2f, 0f, false, false, -2f, 2f); verticalLineToRelative(10f); arcToRelative(2f, 2f, 0f, false, false, 2f, 2f); horizontalLineToRelative(11f); arcToRelative(2f, 2f, 0f, false, false, 2f, -2f); verticalLineTo(7f); arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(17f, 9f); lineToRelative(5f, -3f); verticalLineToRelative(12f); lineToRelative(-5f, -3f)
            }
        }.build()
        return _LucideVideo!!
    }
private var _LucideVideo: ImageVector? = null

// =============================================================================
// FolderOpen
// =============================================================================

val LucideFolderOpen: ImageVector
    get() {
        if (_LucideFolderOpen != null) return _LucideFolderOpen!!
        _LucideFolderOpen = ImageVector.Builder(
            name = "folder-open", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 14f); lineToRelative(1.5f, -2.9f); arcTo(2f, 2f, 0f, false, true, 9.24f, 10f); horizontalLineTo(20f); arcToRelative(2f, 2f, 0f, false, true, 1.94f, 2.5f); lineToRelative(-1.54f, 6f); arcTo(2f, 2f, 0f, false, true, -1.95f, 1.5f); horizontalLineTo(4f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); verticalLineTo(5f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); horizontalLineToRelative(3.9f); arcToRelative(2f, 2f, 0f, false, true, 1.69f, 0.9f); lineToRelative(0.81f, 1.2f); arcToRelative(2f, 2f, 0f, false, false, 1.67f, 0.9f); horizontalLineTo(18f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); verticalLineToRelative(2f)
            }
        }.build()
        return _LucideFolderOpen!!
    }
private var _LucideFolderOpen: ImageVector? = null

// =============================================================================
// User (for Person)
// =============================================================================

val LucideUser: ImageVector
    get() {
        if (_LucideUser != null) return _LucideUser!!
        _LucideUser = ImageVector.Builder(
            name = "user", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(19f, 21f); verticalLineToRelative(-2f); arcToRelative(4f, 4f, 0f, false, false, -4f, -4f); horizontalLineTo(9f); arcToRelative(4f, 4f, 0f, false, false, -4f, 4f); verticalLineToRelative(2f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 7f); arcTo(4f, 4f, 0f, false, true, 12f, 11f); arcTo(4f, 4f, 0f, false, true, 8f, 7f); arcTo(4f, 4f, 0f, false, true, 16f, 7f); close()
            }
        }.build()
        return _LucideUser!!
    }
private var _LucideUser: ImageVector? = null

// =============================================================================
// FilePlus (for DocumentAdd)
// =============================================================================

val LucideFilePlus: ImageVector
    get() {
        if (_LucideFilePlus != null) return _LucideFilePlus!!
        _LucideFilePlus = ImageVector.Builder(
            name = "file-plus", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 2f); horizontalLineTo(6f); arcToRelative(2f, 2f, 0f, false, false, -2f, 2f); verticalLineToRelative(16f); arcToRelative(2f, 2f, 0f, false, false, 2f, 2f); horizontalLineToRelative(12f); arcToRelative(2f, 2f, 0f, false, false, 2f, -2f); verticalLineTo(7f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14f, 2f); verticalLineToRelative(4f); arcToRelative(2f, 2f, 0f, false, false, 2f, 2f); horizontalLineToRelative(4f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 12f); verticalLineToRelative(6f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 15f); horizontalLineToRelative(6f)
            }
        }.build()
        return _LucideFilePlus!!
    }
private var _LucideFilePlus: ImageVector? = null

// =============================================================================
// FolderX (for FolderProhibited)
// =============================================================================

val LucideFolderX: ImageVector
    get() {
        if (_LucideFolderX != null) return _LucideFolderX!!
        _LucideFolderX = ImageVector.Builder(
            name = "folder-x", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 20f); arcToRelative(2f, 2f, 0f, false, false, 2f, -2f); verticalLineTo(8f); arcToRelative(2f, 2f, 0f, false, false, -2f, -2f); horizontalLineToRelative(-7.9f); arcToRelative(2f, 2f, 0f, false, true, -1.69f, -0.9f); lineTo(9.6f, 3.9f); arcTo(2f, 2f, 0f, false, false, 7.93f, 3f); horizontalLineTo(4f); arcToRelative(2f, 2f, 0f, false, false, -2f, 2f); verticalLineToRelative(13f); arcToRelative(2f, 2f, 0f, false, false, 2f, 2f); close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.5f, 10.5f); lineTo(14.5f, 15.5f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14.5f, 10.5f); lineTo(9.5f, 15.5f)
            }
        }.build()
        return _LucideFolderX!!
    }
private var _LucideFolderX: ImageVector? = null

// =============================================================================
// ListChecks (for SelectAllOn)
// =============================================================================

val LucideListChecks: ImageVector
    get() {
        if (_LucideListChecks != null) return _LucideListChecks!!
        _LucideListChecks = ImageVector.Builder(
            name = "list-checks", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(10f, 6f); horizontalLineToRelative(11f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(10f, 12f); horizontalLineToRelative(11f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(10f, 18f); horizontalLineToRelative(11f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 6f); lineToRelative(1f, 1f); lineToRelative(2f, -2f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 12f); lineToRelative(1f, 1f); lineToRelative(2f, -2f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 18f); lineToRelative(1f, 1f); lineToRelative(2f, -2f)
            }
        }.build()
        return _LucideListChecks!!
    }
private var _LucideListChecks: ImageVector? = null

// =============================================================================
// List (for SelectAllOff)
// =============================================================================

val LucideList: ImageVector
    get() {
        if (_LucideList != null) return _LucideList!!
        _LucideList = ImageVector.Builder(
            name = "list", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8f, 6f); horizontalLineToRelative(13f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8f, 12f); horizontalLineToRelative(13f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8f, 18f); horizontalLineToRelative(13f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 6f); horizontalLineToRelative(0.01f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 12f); horizontalLineToRelative(0.01f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 18f); horizontalLineToRelative(0.01f)
            }
        }.build()
        return _LucideList!!
    }
private var _LucideList: ImageVector? = null

// =============================================================================
// Type (for Textbox)
// =============================================================================

val LucideType: ImageVector
    get() {
        if (_LucideType != null) return _LucideType!!
        _LucideType = ImageVector.Builder(
            name = "type", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 7f); verticalLineTo(4f); horizontalLineToRelative(16f); verticalLineToRelative(3f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 20f); horizontalLineToRelative(6f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 4f); verticalLineToRelative(16f)
            }
        }.build()
        return _LucideType!!
    }
private var _LucideType: ImageVector? = null

// =============================================================================
// ListVideo (for VideoClipMultiple / FilmstripPlay)
// =============================================================================

val LucideListVideo: ImageVector
    get() {
        if (_LucideListVideo != null) return _LucideListVideo!!
        _LucideListVideo = ImageVector.Builder(
            name = "list-video", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 12f); horizontalLineTo(3f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 6f); horizontalLineTo(3f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 18f); horizontalLineTo(3f)
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 12f); lineToRelative(5f, 3f); lineToRelative(-5f, 3f); verticalLineToRelative(-6f); close()
            }
        }.build()
        return _LucideListVideo!!
    }
private var _LucideListVideo: ImageVector? = null
