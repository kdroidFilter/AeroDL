package io.github.kdroidfilter.ytdlpgui.features.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Button
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.DropDownButton
import io.github.composefluent.component.FlyoutPlacement
import io.github.composefluent.component.Icon
import io.github.composefluent.component.MenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.DocumentEdit
import io.github.composefluent.icons.filled.OpenFolder
import io.github.composefluent.icons.filled.TopSpeed
import io.github.composefluent.icons.regular.Cookies
import io.github.composefluent.icons.regular.LockShield
import io.github.composefluent.icons.regular.Power
import io.github.kdroidfilter.ytdlpgui.core.presentation.components.Switcher
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.BrowserChrome
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.BrowserFirefox
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.BrowserSafari
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.Cookie_off
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.settings_auto_launch_title
import ytdlpgui.composeapp.generated.resources.settings_browser_chrome
import ytdlpgui.composeapp.generated.resources.settings_browser_disable
import ytdlpgui.composeapp.generated.resources.settings_browser_firefox
import ytdlpgui.composeapp.generated.resources.settings_browser_safari
import ytdlpgui.composeapp.generated.resources.settings_browser_select
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_caption
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_title
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_label
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_title
import ytdlpgui.composeapp.generated.resources.settings_download_dir_caption
import ytdlpgui.composeapp.generated.resources.settings_download_dir_not_set
import ytdlpgui.composeapp.generated.resources.settings_download_dir_pick_title
import ytdlpgui.composeapp.generated.resources.settings_download_dir_title
import ytdlpgui.composeapp.generated.resources.settings_include_preset_in_filename_caption
import ytdlpgui.composeapp.generated.resources.settings_include_preset_in_filename_title
import ytdlpgui.composeapp.generated.resources.settings_no_check_certificate_caption
import ytdlpgui.composeapp.generated.resources.settings_no_check_certificate_title
import ytdlpgui.composeapp.generated.resources.settings_parallel_downloads_caption
import ytdlpgui.composeapp.generated.resources.settings_parallel_downloads_title
import ytdlpgui.composeapp.generated.resources.settings_select
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_title
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_caption
import ytdlpgui.composeapp.generated.resources.settings_auto_launch_title
import ytdlpgui.composeapp.generated.resources.settings_auto_launch_caption
import ytdlpgui.composeapp.generated.resources.open_directory

@Composable
fun SettingsScreen() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state = collectSettingsState(viewModel)
    SettingsView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun SettingsView(
    state: SettingsState,
    onEvent: (SettingsEvents) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            // Cookies-from-browser selection using CardExpanderItem with a flyout
            CardExpanderItem(
                heading = { Text(stringResource(Res.string.settings_cookies_from_browser_title)) },
                caption = {
                    Text(
                        stringResource(Res.string.settings_cookies_from_browser_label),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Regular.Cookies,
                        contentDescription = stringResource(Res.string.settings_cookies_from_browser_title)
                    )
                },
                trailing = {
                    // Define options to reduce duplication and drive label/icon selection
                    val options = listOf(
                        Triple("chrome", BrowserChrome, Res.string.settings_browser_chrome),
                        Triple("firefox", BrowserFirefox, Res.string.settings_browser_firefox),
//                    Triple("safari", BrowserSafari, Res.string.settings_browser_safari),
                        Triple("", Cookie_off, Res.string.settings_browser_disable),
                    )
                    val selectedOption =
                        options.firstOrNull { (id, _, _) -> id.equals(state.cookiesFromBrowser, ignoreCase = true) }
                    val selectionLabel = selectedOption?.third?.let { stringResource(it) }
                        ?: state.cookiesFromBrowser.ifBlank { stringResource(Res.string.settings_browser_disable) }
                    val selectionIcon =
                        selectedOption?.second ?: if (state.cookiesFromBrowser.isBlank()) Cookie_off else Cookie_off

                    MenuFlyoutContainer(
                        flyout = {
                            options.forEach { (id, iconVec, labelRes) ->
                                MenuFlyoutItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = {
                                        onEvent(SettingsEvents.SetCookiesFromBrowser(id))
                                        isFlyoutVisible = false
                                    },
                                    icon = { Icon(iconVec, stringResource(labelRes)) }
                                )
                            }
                        },
                        content = {
                            DropDownButton(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                                onClick = { isFlyoutVisible = !isFlyoutVisible },
                                content = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(1f),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Icon(
                                            selectionIcon,
                                            selectionLabel,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(selectionLabel.ifBlank { stringResource(Res.string.settings_browser_select) })
                                    }
                                },
                            )
                        },
                        adaptivePlacement = true,
                        placement = FlyoutPlacement.Bottom
                    )
                }
            )
        }
        item {
            // Toggle for including preset in file name
            CardExpanderItem(
                heading = {
                    Text(
                        stringResource(Res.string.settings_include_preset_in_filename_title),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                },
                caption = {
                    Text(
                        stringResource(Res.string.settings_include_preset_in_filename_caption),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                },
                icon = { Icon(Icons.Filled.DocumentEdit, null) },
                trailing = {
                    Switcher(
                        checked = state.includePresetInFilename,
                        onCheckStateChange = { onEvent(SettingsEvents.SetIncludePresetInFilename(it)) },
                    )
                }
            )
        }
        item {
            // Selector for parallel downloads
            CardExpanderItem(
                heading = { Text(stringResource(Res.string.settings_parallel_downloads_title)) },
                caption = {
                    Text(
                        stringResource(Res.string.settings_parallel_downloads_caption),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                },
                icon = { Icon(Icons.Filled.TopSpeed, null) },
                trailing = {
                    val options = (1..5).toList()
                    val selectionLabel = state.parallelDownloads.toString()
                    MenuFlyoutContainer(
                        flyout = {
                            options.forEach { count ->
                                MenuFlyoutItem(
                                    modifier = Modifier.width(56.dp),
                                    text = { Text(count.toString()) },
                                    onClick = {
                                        onEvent(SettingsEvents.SetParallelDownloads(count))
                                        isFlyoutVisible = false
                                    }
                                )
                            }
                        },
                        content = {
                            DropDownButton(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                                onClick = { isFlyoutVisible = !isFlyoutVisible },
                                content = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Text(selectionLabel)
                                    }
                                },
                            )
                        },
                        placement = FlyoutPlacement.Bottom
                    )
                }
            )
        }
        item {
            // Download directory picker
            val pickTitle = stringResource(Res.string.settings_download_dir_pick_title)
            CardExpanderItem(
                heading = { Text(stringResource(Res.string.settings_download_dir_title)) },
                caption = {
                    Column(Modifier.fillMaxWidth(0.6f)) {
                        Text(stringResource(Res.string.settings_download_dir_caption))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (state.downloadDirPath.ifBlank { stringResource(Res.string.settings_download_dir_not_set) }),
                        )
                    }
                },
                icon = { Icon(Icons.Regular.Power, null) },
                trailing = {
                    Button(
                        iconOnly = true,
                        onClick = { onEvent(SettingsEvents.PickDownloadDir(pickTitle)) },
                        content = {
                            Icon(Icons.Filled.OpenFolder, stringResource(Res.string.open_directory))
                        },
                    )
                }
            )
        }
        item {
            // Toggle for no-check-certificate
            CardExpanderItem(
                heading = { Text(stringResource(Res.string.settings_no_check_certificate_title)) },
                caption = {
                    Text(
                        stringResource(Res.string.settings_no_check_certificate_caption),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                },
                icon = { Icon(Icons.Default.LockShield, null) },
                trailing = {
                    Switcher(
                        checked = state.noCheckCertificate,
                        onCheckStateChange = { onEvent(SettingsEvents.SetNoCheckCertificate(it)) },
                    )
                }
            )
        }
        item {
            // Toggle for clipboard monitoring
            CardExpanderItem(
                heading = { Text(stringResource(Res.string.settings_clipboard_monitoring_title)) },
                caption = {
                    Text(
                        stringResource(Res.string.settings_clipboard_monitoring_caption),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                },
                icon = { Icon(Icons.Regular.Power, null) },
                trailing = {
                    Switcher(
                        checked = state.clipboardMonitoringEnabled,
                        onCheckStateChange = { onEvent(SettingsEvents.SetClipboardMonitoring(it)) },
                    )
                }
            )
        }
        item {
            // Toggle for auto-launch at system startup
            CardExpanderItem(
                heading = { Text(stringResource(Res.string.settings_auto_launch_title)) },
                caption = {
                    Text(
                        stringResource(Res.string.settings_auto_launch_caption),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                },
                icon = { Icon(Icons.Regular.Power, null) },
                trailing = {
                    Switcher(
                        checked = state.autoLaunchEnabled,
                        onCheckStateChange = { onEvent(SettingsEvents.SetAutoLaunchEnabled(it)) },
                    )
                }
            )
        }
    }
}