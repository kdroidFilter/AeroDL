package io.github.kdroidfilter.ytdlpgui.ui.icons

import dev.nucleusframework.macoscompose.icons.Icons as MacosIcons
import dev.nucleusframework.macoscompose.icons.SystemIcon
import dev.nucleusframework.macoscompose.icons.extended.IconsExtended
import dev.nucleusframework.macoscompose.icons.extended.ArrowLeft
import dev.nucleusframework.macoscompose.icons.extended.ArrowRight
import dev.nucleusframework.macoscompose.icons.extended.Ban
import dev.nucleusframework.macoscompose.icons.extended.Clipboard
import dev.nucleusframework.macoscompose.icons.extended.Clock
import dev.nucleusframework.macoscompose.icons.extended.EllipsisVertical
import dev.nucleusframework.macoscompose.icons.extended.FilePlus
import dev.nucleusframework.macoscompose.icons.extended.FileText
import dev.nucleusframework.macoscompose.icons.extended.FolderOpen
import dev.nucleusframework.macoscompose.icons.extended.Gauge
import dev.nucleusframework.macoscompose.icons.extended.Globe
import dev.nucleusframework.macoscompose.icons.extended.Key
import dev.nucleusframework.macoscompose.icons.extended.Music
import dev.nucleusframework.macoscompose.icons.extended.Pause
import dev.nucleusframework.macoscompose.icons.extended.Play
import dev.nucleusframework.macoscompose.icons.extended.Power
import dev.nucleusframework.macoscompose.icons.extended.RefreshCw
import dev.nucleusframework.macoscompose.icons.extended.ShieldCheck
import dev.nucleusframework.macoscompose.icons.extended.Square
import dev.nucleusframework.macoscompose.icons.extended.Type
import dev.nucleusframework.macoscompose.icons.extended.Video
import dev.nucleusframework.macoscompose.icons.extended.Zap

object Icons {
    val Default get() = Regular

    object Regular {
        val Home = icon(MacosIcons.Home)
        val History = icon(IconsExtended.Clock)
        val ConvertRange = icon(MacosIcons.ArrowLeftRight)
        val Settings = icon(MacosIcons.Settings)
        val Info = icon(MacosIcons.Info)
        val ArrowLeft = icon(IconsExtended.ArrowLeft)
        val ArrowRight = icon(IconsExtended.ArrowRight)
        val Warning = icon(MacosIcons.TriangleAlert)
        val Clipboard = icon(IconsExtended.Clipboard)
        val Power = icon(IconsExtended.Power)
        val Copy = icon(MacosIcons.Copy)
        val ArrowDownload = icon(MacosIcons.Download)
        val ArrowSync = icon(IconsExtended.RefreshCw)
        val ErrorCircle = icon(MacosIcons.CircleX)
        val Search = icon(MacosIcons.Search)
        val Delete = icon(MacosIcons.Trash2)
        val Folder = icon(MacosIcons.Folder)
        val FolderProhibited = icon(IconsExtended.Ban)
        val Dismiss = icon(MacosIcons.X)
        val MusicNote1 = icon(IconsExtended.Music)
        val MusicNote2 = icon(IconsExtended.Music)
        val Video = icon(IconsExtended.Video)
        val FilmstripPlay = icon(IconsExtended.Play)
        val Textbox = icon(IconsExtended.Type)
        val ChevronLeft = icon(MacosIcons.ChevronLeft)
        val ChevronRight = icon(MacosIcons.ChevronRight)
        val Play = icon(IconsExtended.Play)
        val Pause = icon(IconsExtended.Pause)
        val VideoClipMultiple = icon(IconsExtended.Video)
        val SelectAllOn = icon(MacosIcons.Check)
        val SelectAllOff = icon(IconsExtended.Square)
        val DocumentAdd = icon(IconsExtended.FilePlus)
        val Cookies = icon(IconsExtended.Key)
        val CheckboxChecked = icon(MacosIcons.Check)
        val Globe = icon(IconsExtended.Globe)
        val LockShield = icon(IconsExtended.ShieldCheck)
    }

    object Filled {
        val MoreVertical = icon(IconsExtended.EllipsisVertical)
        val Heart = icon(MacosIcons.Heart)
        val ClipboardPaste = icon(IconsExtended.Clipboard)
        val FolderOpen = icon(IconsExtended.FolderOpen)
        val Cookies = icon(IconsExtended.Key)
        val DocumentEdit = icon(IconsExtended.FileText)
        val MusicNote1 = icon(IconsExtended.Music)
        val Flash = icon(IconsExtended.Zap)
        val TopSpeed = icon(IconsExtended.Gauge)
        val OpenFolder = icon(IconsExtended.FolderOpen)
    }
}

private fun icon(system: SystemIcon): NativeIcon = NativeIcon(
    imageVector = system.fallback,
    sfSymbolName = system.sfSymbolName.takeIf { it.isNotEmpty() },
)
