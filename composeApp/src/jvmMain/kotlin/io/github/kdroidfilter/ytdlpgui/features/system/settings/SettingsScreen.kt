package io.github.kdroidfilter.ytdlpgui.features.system.settings

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.design.components.BrowserSelector
import io.github.kdroidfilter.ytdlpgui.core.design.components.EllipsizedTextWithTooltip
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSwitcher
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppCardExpanderItem
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppComboBox
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppContentDialog
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppDialogButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTextField
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()
    SettingsView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
fun SettingsView(
    state: SettingsState,
    onEvent: (SettingsEvents) -> Unit,
) {
    val listState = rememberLazyListState()

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            item {
                ThemeSetting(
                    appTheme = state.appTheme,
                    onThemeChange = { onEvent(SettingsEvents.SetAppTheme(it)) },
                )
            }
            item {
                CookiesFromBrowserSetting(
                    currentBrowser = state.cookiesFromBrowser,
                    onBrowserSelected = { browser ->
                        onEvent(SettingsEvents.SetCookiesFromBrowser(browser))
                    },
                )
            }
            item {
                IncludePresetInFilenameSetting(
                    includePreset = state.includePresetInFilename,
                    onIncludePresetChange = { onEvent(SettingsEvents.SetIncludePresetInFilename(it)) },
                )
            }
            item {
                EmbedThumbnailInMp3Setting(
                    embedThumbnailInMp3 = state.embedThumbnailInMp3,
                    onEmbedThumbnailChange = { onEvent(SettingsEvents.SetEmbedThumbnailInMp3(it)) },
                )
            }
            item {
                ConcurrentFragmentsSetting(
                    concurrentFragments = state.concurrentFragments,
                    onConcurrentFragmentsSelected = { onEvent(SettingsEvents.SetConcurrentFragments(it)) },
                )
            }
            item {
                ParallelDownloadsSetting(
                    parallelDownloads = state.parallelDownloads,
                    onParallelDownloadsSelected = { onEvent(SettingsEvents.SetParallelDownloads(it)) },
                )
            }
            item {
                ValidateBulkUrlsSetting(
                    validateBulkUrls = state.validateBulkUrls,
                    onValidateBulkUrlsChange = { onEvent(SettingsEvents.SetValidateBulkUrls(it)) },
                )
            }
            item {
                DownloadDirectorySetting(
                    downloadDirPath = state.downloadDirPath,
                    onPickDownloadDir = { title -> onEvent(SettingsEvents.PickDownloadDir(title)) },
                )
            }
            item {
                ProxySetting(
                    proxy = state.proxy,
                    onProxyChange = { onEvent(SettingsEvents.SetProxy(it)) },
                )
            }
            item {
                NoCheckCertificateSetting(
                    noCheckCertificate = state.noCheckCertificate,
                    onNoCheckCertificateChange = { onEvent(SettingsEvents.SetNoCheckCertificate(it)) },
                )
            }
            item {
                ClipboardMonitoringSetting(
                    clipboardMonitoringEnabled = state.clipboardMonitoringEnabled,
                    onClipboardMonitoringChange = { onEvent(SettingsEvents.SetClipboardMonitoring(it)) },
                )
            }
            item {
                NotifyOnCompleteSetting(
                    notifyOnComplete = state.notifyOnComplete,
                    onNotifyOnCompleteChange = { onEvent(SettingsEvents.SetNotifyOnComplete(it)) },
                )
            }
            item {
                AutoLaunchSetting(
                    autoLaunchEnabled = state.autoLaunchEnabled,
                    onAutoLaunchChange = { onEvent(SettingsEvents.SetAutoLaunchEnabled(it)) },
                )
            }
            item {
                ResetToDefaultsSetting(
                    onResetClick = { onEvent(SettingsEvents.ResetToDefaults) }
                )
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
        )
    }
}

@Composable
private fun ThemeSetting(
    appTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
) {
    val themes = AppTheme.entries
    val items = themes.map { theme ->
        when (theme) {
            AppTheme.FLUENT -> stringResource(Res.string.settings_theme_fluent)
            AppTheme.DARWIN -> stringResource(Res.string.settings_theme_darwin)
        }
    }
    var selected by remember(appTheme) {
        mutableStateOf(themes.indexOf(appTheme))
    }

    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_theme_title),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_theme_caption),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        icon = { AppIcon(AppIcons.Settings, null) },
        trailing = {
            AppComboBox(
                modifier = Modifier.width(120.dp),
                header = null,
                placeholder = "",
                selectedIndex = selected,
                items = items,
                onSelectionChange = { index, _ ->
                    selected = index
                    onThemeChange(themes[index])
                }
            )
        }
    )
}

@Composable
private fun CookiesFromBrowserSetting(
    currentBrowser: String,
    onBrowserSelected: (String) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_cookies_from_browser_title),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_cookies_from_browser_label),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        icon = {
            AppIcon(
                imageVector = AppIcons.Cookies,
                contentDescription = stringResource(Res.string.settings_cookies_from_browser_title)
            )
        },
        trailing = {
            BrowserSelector(
                currentBrowser = currentBrowser,
                onBrowserSelected = onBrowserSelected,
                header = null,
                useDefaultPlaceholder = true
            )
        }
    )
}

@Preview
@Composable
fun CookiesFromBrowserSettingPreview() {
    CookiesFromBrowserSetting(currentBrowser = "firefox", onBrowserSelected = {})
}

@Composable
private fun IncludePresetInFilenameSetting(
    includePreset: Boolean,
    onIncludePresetChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_include_preset_in_filename_title),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_include_preset_in_filename_caption),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        icon = { AppIcon(AppIcons.DocumentEdit, null) },
        trailing = {
            AppSwitcher(
                checked = includePreset,
                onCheckedChange = onIncludePresetChange,
            )
        }
    )
}

@Preview
@Composable
fun IncludePresetInFilenameSettingPreview() {
    IncludePresetInFilenameSetting(includePreset = true, onIncludePresetChange = {})
}

@Composable
private fun EmbedThumbnailInMp3Setting(
    embedThumbnailInMp3: Boolean,
    onEmbedThumbnailChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_embed_thumbnail_mp3_title),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_embed_thumbnail_mp3_caption),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
            icon = { AppIcon(AppIcons.MusicNote, null) },
        trailing = {
            AppSwitcher(
                checked = embedThumbnailInMp3,
                onCheckedChange = onEmbedThumbnailChange,
            )
        }
    )
}

@Preview
@Composable
fun EmbedThumbnailInMp3SettingPreview() {
    EmbedThumbnailInMp3Setting(embedThumbnailInMp3 = true, onEmbedThumbnailChange = {})
}


@Composable
private fun ConcurrentFragmentsSetting(
    concurrentFragments: Int,
    onConcurrentFragmentsSelected: (Int) -> Unit,
) {
    val options = (1..5).toList()
    val items = options.map { it.toString() }
    var selected by remember(concurrentFragments) {
        mutableStateOf(options.indexOf(concurrentFragments))
    }

    AppCardExpanderItem(
        heading = { AppText(stringResource(Res.string.settings_concurrent_fragments_title), modifier = Modifier.fillMaxWidth(0.75f)) },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_concurrent_fragments_caption),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        icon = { AppIcon(AppIcons.Flash, null) },
        trailing = {
            AppComboBox(
                modifier = Modifier.width(80.dp),
                header = null,
                placeholder = "",
                selectedIndex = selected,
                items = items,
                onSelectionChange = { index, _ ->
                    selected = index
                    onConcurrentFragmentsSelected(options[index])
                }
            )
        }
    )
}

@Preview
@Composable
fun ConcurrentFragmentsSettingPreview() {
    ConcurrentFragmentsSetting(concurrentFragments = 1, onConcurrentFragmentsSelected = {})
}

@Composable
private fun ParallelDownloadsSetting(
    parallelDownloads: Int,
    onParallelDownloadsSelected: (Int) -> Unit,
) {
    val options = (1..5).toList()
    val items = options.map { it.toString() }
    var selected by remember(parallelDownloads) {
        mutableStateOf(options.indexOf(parallelDownloads))
    }

    AppCardExpanderItem(
        heading = { AppText(stringResource(Res.string.settings_parallel_downloads_title)) },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_parallel_downloads_caption),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        icon = { AppIcon(AppIcons.TopSpeed, null) },
        trailing = {
            AppComboBox(
                modifier = Modifier.width(80.dp),
                header = null,
                placeholder = "",
                selectedIndex = selected,
                items = items,
                onSelectionChange = { index, _ ->
                    selected = index
                    onParallelDownloadsSelected(options[index])
                }
            )
        }
    )
}

@Preview
@Composable
fun ParallelDownloadsSettingPreview() {
    ParallelDownloadsSetting(parallelDownloads = 3, onParallelDownloadsSelected = {})
}

@Composable
private fun ValidateBulkUrlsSetting(
    validateBulkUrls: Boolean,
    onValidateBulkUrlsChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_validate_bulk_urls_title),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_validate_bulk_urls_caption),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        icon = { AppIcon(AppIcons.CheckboxChecked, null) },
        trailing = {
            AppSwitcher(
                checked = validateBulkUrls,
                onCheckedChange = onValidateBulkUrlsChange,
            )
        }
    )
}

@Preview
@Composable
fun ValidateBulkUrlsSettingPreview() {
    ValidateBulkUrlsSetting(validateBulkUrls = false, onValidateBulkUrlsChange = {})
}

@Composable
private fun DownloadDirectorySetting(
    downloadDirPath: String,
    onPickDownloadDir: (String) -> Unit,
) {
    val pickTitle = stringResource(Res.string.settings_download_dir_pick_title)

    AppCardExpanderItem(
        heading = { AppText(stringResource(Res.string.settings_download_dir_title)) },
        caption = {
            Column(Modifier.fillMaxWidth(0.6f)) {
                AppText(stringResource(Res.string.settings_download_dir_caption))
                Spacer(Modifier.height(8.dp))
                AppText(
                    downloadDirPath.ifBlank { stringResource(Res.string.settings_download_dir_not_set) },
                )
            }
        },
        icon = { AppIcon(AppIcons.Power, null) },
        trailing = {
            AppButton(
                onClick = { onPickDownloadDir(pickTitle) },
            ) {
                AppIcon(AppIcons.Folder, stringResource(Res.string.open_directory))
            }
        }
    )
}

@Preview
@Composable
fun DownloadDirectorySettingPreview() {
    DownloadDirectorySetting(downloadDirPath = "/home/user/Downloads", onPickDownloadDir = {})
}

@Composable
private fun ProxySetting(
    proxy: String,
    onProxyChange: (String) -> Unit,
) {
    var currentValue by remember(proxy) { mutableStateOf(proxy) }

    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_proxy_title),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        caption = {
            Column(Modifier.fillMaxWidth(0.4f)) {
                EllipsizedTextWithTooltip(
                    text = stringResource(Res.string.settings_proxy_caption),
                )
                Spacer(Modifier.height(8.dp))
                AppText(
                    proxy.ifBlank { stringResource(Res.string.settings_proxy_not_set) },
                )
            }
        },
        icon = { AppIcon(AppIcons.Globe, null) },
        trailing = {
            AppTextField(
                value = currentValue,
                onValueChange = {
                    currentValue = it
                    onProxyChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.settings_proxy_placeholder),
                singleLine = true,
            )
        }
    )
}

@Preview
@Composable
fun ProxySettingPreview() {
    ProxySetting(proxy = "http://127.0.0.1:8080", onProxyChange = {})
}

@Composable
private fun NoCheckCertificateSetting(
    noCheckCertificate: Boolean,
    onNoCheckCertificateChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_no_check_certificate_title),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_no_check_certificate_caption),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        icon = { AppIcon(AppIcons.LockShield, null) },
        trailing = {
            AppSwitcher(
                checked = noCheckCertificate,
                onCheckedChange = onNoCheckCertificateChange,
            )
        }
    )
}

@Preview
@Composable
fun NoCheckCertificateSettingPreview() {
    NoCheckCertificateSetting(noCheckCertificate = true, onNoCheckCertificateChange = {})
}

@Composable
private fun ClipboardMonitoringSetting(
    clipboardMonitoringEnabled: Boolean,
    onClipboardMonitoringChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_clipboard_monitoring_title),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_clipboard_monitoring_caption),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        icon = { AppIcon(AppIcons.Clipboard, null) },
        trailing = {
            AppSwitcher(
                checked = clipboardMonitoringEnabled,
                onCheckedChange = onClipboardMonitoringChange,
            )
        }
    )
}

@Preview
@Composable
fun ClipboardMonitoringSettingPreview() {
    ClipboardMonitoringSetting(clipboardMonitoringEnabled = true, onClipboardMonitoringChange = {})
}

@Composable
private fun NotifyOnCompleteSetting(
    notifyOnComplete: Boolean,
    onNotifyOnCompleteChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_notify_on_complete_title),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_notify_on_complete_caption),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        icon = { AppIcon(AppIcons.TopSpeed, null) },
        trailing = {
            AppSwitcher(
                checked = notifyOnComplete,
                onCheckedChange = onNotifyOnCompleteChange,
            )
        }
    )
}

@Preview
@Composable
fun NotifyOnCompleteSettingPreview() {
    NotifyOnCompleteSetting(notifyOnComplete = true, onNotifyOnCompleteChange = {})
}

@Composable
private fun AutoLaunchSetting(
    autoLaunchEnabled: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
) {
    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_auto_launch_title),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_auto_launch_caption),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        icon = { AppIcon(AppIcons.Power, null) },
        trailing = {
            AppSwitcher(
                checked = autoLaunchEnabled,
                onCheckedChange = onAutoLaunchChange,
            )
        }
    )
}

@Preview
@Composable
fun AutoLaunchSettingPreview() {
    AutoLaunchSetting(autoLaunchEnabled = true, onAutoLaunchChange = {})
}

@Composable
private fun ResetToDefaultsSetting(
    onResetClick: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    AppCardExpanderItem(
        heading = {
            AppText(
                stringResource(Res.string.settings_reset_title),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_reset_caption),
                modifier = Modifier.fillMaxWidth(0.50f)
            )
        },
        icon = { AppIcon(AppIcons.Warning, null) },
        trailing = {
            AppButton(
                onClick = { showDialog = true },
            ) {
                AppText(stringResource(Res.string.settings_reset_button))
            }
        }
    )

    AppContentDialog(
        title = stringResource(Res.string.settings_reset_dialog_title),
        visible = showDialog,
        primaryButtonText = stringResource(Res.string.settings_reset_dialog_confirm),
        closeButtonText = stringResource(Res.string.settings_reset_dialog_cancel),
        onButtonClick = { button ->
            showDialog = false
            if (button == AppDialogButton.Primary) {
                onResetClick()
            }
        },
        content = {
            AppText(stringResource(Res.string.settings_reset_dialog_message))
        }
    )
}

@Preview
@Composable
fun ResetToDefaultsSettingPreview() {
    ResetToDefaultsSetting(onResetClick = {})
}

@Preview
@Composable
fun SettingsScreenPreviewDefault() {
    SettingsView(state = SettingsState.defaultState, onEvent = {})
}

@Preview
@Composable
fun SettingsScreenPreviewCustomized() {
    SettingsView(state = SettingsState.customizedState, onEvent = {})
}

@Preview
@Composable
fun SettingsScreenPreviewChromeUser() {
    SettingsView(state = SettingsState.chromeUserState, onEvent = {})
}

@Preview
@Composable
fun SettingsScreenPreviewMinimal() {
    SettingsView(state = SettingsState.minimalState, onEvent = {})
}

@Preview
@Composable
fun SettingsScreenPreviewPowerUser() {
    SettingsView(state = SettingsState.powerUserState, onEvent = {})
}
