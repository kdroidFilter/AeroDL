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
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.*
import io.github.composefluent.icons.regular.*
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.design.components.BrowserSelector
import io.github.kdroidfilter.ytdlpgui.core.design.components.EllipsizedTextWithTooltip
import io.github.kdroidfilter.ytdlpgui.core.design.components.Switcher
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
private fun CookiesFromBrowserSetting(
    currentBrowser: String,
    onBrowserSelected: (String) -> Unit,
) {
    CardExpanderItem(
        heading = {
            Text(
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
            Icon(
                imageVector = Icons.Regular.Cookies,
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
    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Filled.DocumentEdit, null) },
        trailing = {
            Switcher(
                checked = includePreset,
                onCheckStateChange = onIncludePresetChange,
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
    CardExpanderItem(
        heading = {
            Text(
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
            icon = { Icon(Icons.Filled.MusicNote1, null) },
        trailing = {
            Switcher(
                checked = embedThumbnailInMp3,
                onCheckStateChange = onEmbedThumbnailChange,
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

    CardExpanderItem(
        heading = { Text(stringResource(Res.string.settings_concurrent_fragments_title), modifier = Modifier.fillMaxWidth(0.75f)) },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_concurrent_fragments_caption),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        icon = { Icon(Icons.Filled.Flash, null) },
        trailing = {
            ComboBox(
                modifier = Modifier.width(80.dp),
                header = null,
                placeholder = "",
                selected = selected,
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

    CardExpanderItem(
        heading = { Text(stringResource(Res.string.settings_parallel_downloads_title)) },
        caption = {
            EllipsizedTextWithTooltip(
                text = stringResource(Res.string.settings_parallel_downloads_caption),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        },
        icon = { Icon(Icons.Filled.TopSpeed, null) },
        trailing = {
            ComboBox(
                modifier = Modifier.width(80.dp),
                header = null,
                placeholder = "",
                selected = selected,
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
    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Regular.CheckboxChecked, null) },
        trailing = {
            Switcher(
                checked = validateBulkUrls,
                onCheckStateChange = onValidateBulkUrlsChange,
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

    CardExpanderItem(
        heading = { Text(stringResource(Res.string.settings_download_dir_title)) },
        caption = {
            Column(Modifier.fillMaxWidth(0.6f)) {
                Text(stringResource(Res.string.settings_download_dir_caption))
                Spacer(Modifier.height(8.dp))
                Text(
                    downloadDirPath.ifBlank { stringResource(Res.string.settings_download_dir_not_set) },
                )
            }
        },
        icon = { Icon(Icons.Regular.Power, null) },
        trailing = {
            Button(
                iconOnly = true,
                onClick = { onPickDownloadDir(pickTitle) },
                content = {
                    Icon(Icons.Filled.OpenFolder, stringResource(Res.string.open_directory))
                },
            )
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

    CardExpanderItem(
        heading = {
            Text(
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
                Text(
                    proxy.ifBlank { stringResource(Res.string.settings_proxy_not_set) },
                )
            }
        },
        icon = { Icon(Icons.Regular.Globe, null) },
        trailing = {
            TextField(
                value = currentValue,
                onValueChange = {
                    currentValue = it
                    onProxyChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.settings_proxy_placeholder), maxLines = 1) },
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
    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Default.LockShield, null) },
        trailing = {
            Switcher(
                checked = noCheckCertificate,
                onCheckStateChange = onNoCheckCertificateChange,
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
    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Regular.Clipboard, null) },
        trailing = {
            Switcher(
                checked = clipboardMonitoringEnabled,
                onCheckStateChange = onClipboardMonitoringChange,
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
    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Filled.TopSpeed, null) },
        trailing = {
            Switcher(
                checked = notifyOnComplete,
                onCheckStateChange = onNotifyOnCompleteChange,
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
    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Regular.Power, null) },
        trailing = {
            Switcher(
                checked = autoLaunchEnabled,
                onCheckStateChange = onAutoLaunchChange,
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

    CardExpanderItem(
        heading = {
            Text(
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
        icon = { Icon(Icons.Regular.Warning, null) },
        trailing = {
            Button(
                onClick = { showDialog = true },
                content = {
                    Text(stringResource(Res.string.settings_reset_button))
                },
            )
        }
    )

    ContentDialog(
        title = stringResource(Res.string.settings_reset_dialog_title),
        visible = showDialog,
        primaryButtonText = stringResource(Res.string.settings_reset_dialog_confirm),
        closeButtonText = stringResource(Res.string.settings_reset_dialog_cancel),
        onButtonClick = {
            showDialog = false
            onResetClick()
        },
        content = {
            Text(stringResource(Res.string.settings_reset_dialog_message))
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
