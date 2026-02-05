package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.composefluent.icons.filled.ClipboardPaste
import io.github.composefluent.icons.filled.DocumentEdit
import io.github.composefluent.icons.filled.Flash
import io.github.composefluent.icons.filled.FolderOpen
import io.github.composefluent.icons.filled.Heart
import io.github.composefluent.icons.filled.MoreVertical
import io.github.composefluent.icons.filled.MusicNote1
import io.github.composefluent.icons.filled.OpenFolder
import io.github.composefluent.icons.filled.TopSpeed
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.icons.regular.ArrowLeft
import io.github.composefluent.icons.regular.ArrowRight
import io.github.composefluent.icons.regular.CheckboxChecked
import io.github.composefluent.icons.regular.Checkmark
import io.github.composefluent.icons.regular.ChevronRight
import io.github.composefluent.icons.regular.Clipboard
import io.github.composefluent.icons.regular.ConvertRange
import io.github.composefluent.icons.regular.Cookies
import io.github.composefluent.icons.regular.Copy
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.DocumentAdd
import io.github.composefluent.icons.regular.ErrorCircle
import io.github.composefluent.icons.regular.FilmstripPlay
import io.github.composefluent.icons.regular.FolderProhibited
import io.github.composefluent.icons.regular.Globe
import io.github.composefluent.icons.regular.History
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Info
import io.github.composefluent.icons.regular.LockShield
import io.github.composefluent.icons.regular.Pause
import io.github.composefluent.icons.regular.Person
import io.github.composefluent.icons.regular.Play
import io.github.composefluent.icons.regular.Power
import io.github.composefluent.icons.regular.Search
import io.github.composefluent.icons.regular.SelectAllOff
import io.github.composefluent.icons.regular.SelectAllOn
import io.github.composefluent.icons.regular.Settings
import io.github.composefluent.icons.regular.Textbox
import io.github.composefluent.icons.regular.Video
import io.github.composefluent.icons.regular.VideoClipMultiple
import io.github.composefluent.icons.regular.Warning
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

/**
 * Maps Fluent icons to their Lucide equivalents for Darwin theme.
 * Usage: AppIcons.Home returns the appropriate icon for the current theme.
 */
object AppIcons {

    val Home: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Home
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideHome
        }

    val Settings: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Settings
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideSettings
        }

    val Info: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Info
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideInfo
        }

    val ArrowLeft: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ArrowLeft
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideChevronLeft
        }

    val ArrowRight: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ArrowRight
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideChevronRight
        }

    val ConvertRange: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ConvertRange
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideArrowLeftRight
        }

    val Heart: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.Heart
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideHeart
        }

    val Download: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ArrowDownload
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideDownload
        }

    val Copy: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Copy
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideCopy
        }

    val Folder: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.OpenFolder
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideFolder
        }

    val Warning: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Warning
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideTriangleAlert
        }

    val Search: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Search
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideSearch
        }

    val Close: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Dismiss
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideX
        }

    val Check: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Checkmark
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideCheck
        }

    val Delete: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Delete
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideTrash2
        }

    val History: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.History
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideClock
        }

    val MoreVertical: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.MoreVertical
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideMoreVertical
        }

    val Cookies: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Cookies
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideCookie
        }

    val DocumentEdit: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.DocumentEdit
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucidePencil
        }

    val MusicNote: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.MusicNote1
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideMusic
        }

    val Flash: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.Flash
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideZap
        }

    val TopSpeed: ImageVector
        @Composable get() {
            val vector = when (LocalAppTheme.current) {
                AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.TopSpeed
                AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideGauge
            }
            return vector
        }

    val Power: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Power
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucidePower
        }

    val Globe: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Globe
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideGlobe
        }

    val LockShield: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.LockShield
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideShield
        }

    val Clipboard: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Clipboard
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideClipboard
        }

    val ClipboardPaste: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.ClipboardPaste
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideClipboardPaste
        }

    val CheckboxChecked: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.CheckboxChecked
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideSquareCheck
        }

    val Play: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Play
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucidePlay
        }

    val Pause: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Pause
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucidePause
        }

    val ChevronRight: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ChevronRight
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideChevronRight
        }

    val FolderOpen: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.FolderOpen
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideFolderOpen
        }

    val FolderProhibited: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.FolderProhibited
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideFolderX
        }

    val ErrorCircle: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ErrorCircle
            AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.icons.LucideCircleX
        }

    val Video: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Video
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideVideo
        }

    val VideoClipMultiple: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.VideoClipMultiple
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideListVideo
        }

    val SelectAllOn: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.SelectAllOn
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideListChecks
        }

    val SelectAllOff: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.SelectAllOff
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideList
        }

    val Person: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Person
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideUser
        }

    val DocumentAdd: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.DocumentAdd
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideFilePlus
        }

    val Textbox: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Textbox
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideType
        }

    val FilmstripPlay: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.FilmstripPlay
            AppTheme.DARWIN -> io.github.kdroidfilter.ytdlpgui.core.design.icons.LucideListVideo
        }
}
