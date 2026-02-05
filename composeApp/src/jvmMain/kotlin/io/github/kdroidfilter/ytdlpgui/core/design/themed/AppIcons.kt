@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*
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
            AppTheme.DARWIN -> Lucide.House
        }

    val Settings: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Settings
            AppTheme.DARWIN -> Lucide.Settings
        }

    val Info: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Info
            AppTheme.DARWIN -> Lucide.Info
        }

    val ArrowLeft: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ArrowLeft
            AppTheme.DARWIN -> Lucide.ChevronLeft
        }

    val ArrowRight: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ArrowRight
            AppTheme.DARWIN -> Lucide.ChevronRight
        }

    val ConvertRange: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ConvertRange
            AppTheme.DARWIN -> Lucide.ArrowLeftRight
        }

    val Heart: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.Heart
            AppTheme.DARWIN -> Lucide.Heart
        }

    val Download: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ArrowDownload
            AppTheme.DARWIN -> Lucide.Download
        }

    val Copy: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Copy
            AppTheme.DARWIN -> Lucide.Copy
        }

    val Folder: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.OpenFolder
            AppTheme.DARWIN -> Lucide.Folder
        }

    val Warning: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Warning
            AppTheme.DARWIN -> Lucide.TriangleAlert
        }

    val Search: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Search
            AppTheme.DARWIN -> Lucide.Search
        }

    val Close: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Dismiss
            AppTheme.DARWIN -> Lucide.X
        }

    val Check: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Checkmark
            AppTheme.DARWIN -> Lucide.Check
        }

    val Delete: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Delete
            AppTheme.DARWIN -> Lucide.Trash2
        }

    val History: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.History
            AppTheme.DARWIN -> Lucide.Clock
        }

    val MoreVertical: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.MoreVertical
            AppTheme.DARWIN -> Lucide.EllipsisVertical
        }

    val Cookies: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Cookies
            AppTheme.DARWIN -> Lucide.Cookie
        }

    val DocumentEdit: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.DocumentEdit
            AppTheme.DARWIN -> Lucide.Pencil
        }

    val MusicNote: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.MusicNote1
            AppTheme.DARWIN -> Lucide.Music
        }

    val Flash: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.Flash
            AppTheme.DARWIN -> Lucide.Zap
        }

    val TopSpeed: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.TopSpeed
            AppTheme.DARWIN -> Lucide.Gauge
        }

    val Power: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Power
            AppTheme.DARWIN -> Lucide.Power
        }

    val Globe: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Globe
            AppTheme.DARWIN -> Lucide.Globe
        }

    val LockShield: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.LockShield
            AppTheme.DARWIN -> Lucide.Shield
        }

    val Clipboard: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Clipboard
            AppTheme.DARWIN -> Lucide.Clipboard
        }

    val ClipboardPaste: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.ClipboardPaste
            AppTheme.DARWIN -> Lucide.ClipboardPaste
        }

    val CheckboxChecked: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.CheckboxChecked
            AppTheme.DARWIN -> Lucide.SquareCheck
        }

    val Play: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Play
            AppTheme.DARWIN -> Lucide.Play
        }

    val Pause: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.Pause
            AppTheme.DARWIN -> Lucide.Pause
        }

    val ChevronRight: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ChevronRight
            AppTheme.DARWIN -> Lucide.ChevronRight
        }

    val FolderOpen: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Filled.FolderOpen
            AppTheme.DARWIN -> Lucide.FolderOpen
        }

    val FolderProhibited: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.FolderProhibited
            AppTheme.DARWIN -> Lucide.FolderX
        }

    val ErrorCircle: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Default.ErrorCircle
            AppTheme.DARWIN -> Lucide.CircleX
        }

    val Video: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Video
            AppTheme.DARWIN -> Lucide.Video
        }

    val VideoClipMultiple: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.VideoClipMultiple
            AppTheme.DARWIN -> Lucide.ListVideo
        }

    val SelectAllOn: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.SelectAllOn
            AppTheme.DARWIN -> Lucide.ListChecks
        }

    val SelectAllOff: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.SelectAllOff
            AppTheme.DARWIN -> Lucide.List
        }

    val Person: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Person
            AppTheme.DARWIN -> Lucide.User
        }

    val DocumentAdd: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.DocumentAdd
            AppTheme.DARWIN -> Lucide.FilePlus
        }

    val Textbox: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.Textbox
            AppTheme.DARWIN -> Lucide.Type
        }

    val FilmstripPlay: ImageVector
        @Composable get() = when (LocalAppTheme.current) {
            AppTheme.FLUENT -> io.github.composefluent.icons.Icons.Regular.FilmstripPlay
            AppTheme.DARWIN -> Lucide.ListVideo
        }
}
