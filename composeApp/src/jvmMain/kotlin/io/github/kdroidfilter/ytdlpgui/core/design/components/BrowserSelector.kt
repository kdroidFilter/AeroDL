package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppComboBox
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.BrowserDetector
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.SupportedBrowser
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.settings_browser_brave
import ytdlpgui.composeapp.generated.resources.settings_browser_chrome
import ytdlpgui.composeapp.generated.resources.settings_browser_chromium
import ytdlpgui.composeapp.generated.resources.settings_browser_disable
import ytdlpgui.composeapp.generated.resources.settings_browser_edge
import ytdlpgui.composeapp.generated.resources.settings_browser_firefox
import ytdlpgui.composeapp.generated.resources.settings_browser_opera
import ytdlpgui.composeapp.generated.resources.settings_browser_safari
import ytdlpgui.composeapp.generated.resources.settings_browser_select
import ytdlpgui.composeapp.generated.resources.settings_browser_vivaldi
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_title

/**
 * Shared browser selector component using ComboBox.
 * Automatically detects the operating system and provides appropriate browser options.
 *
 * @param currentBrowser The currently selected browser value (e.g., "firefox", "chrome", "")
 * @param onBrowserSelected Callback invoked when a browser is selected
 * @param header Header text for the ComboBox (null = no header, not provided = default header)
 * @param placeholder Optional placeholder text for the ComboBox
 * @param useDefaultPlaceholder If true, uses the default "Select browser" placeholder
 */
@Composable
fun BrowserSelector(
    currentBrowser: String,
    onBrowserSelected: (String) -> Unit,
    header: String? = stringResource(Res.string.settings_cookies_from_browser_title),
    placeholder: String? = null,
    useDefaultPlaceholder: Boolean = false,
) {
    val availableBrowsers = remember { getInstalledBrowserOptions() }
    val browserLabels = availableBrowsers.map { stringResource(it.labelRes) }
    val browserValues = availableBrowsers.map { it.value }

    var selected by remember(currentBrowser) {
        mutableStateOf(
            browserValues.indexOf(currentBrowser).takeIf { it >= 0 }
        )
    }

    val actualPlaceholder = when {
        placeholder != null -> placeholder
        useDefaultPlaceholder -> stringResource(Res.string.settings_browser_select)
        else -> stringResource(Res.string.settings_browser_disable)
    }

    AppComboBox(
        header = header,
        placeholder = actualPlaceholder,
        selectedIndex = selected,
        items = browserLabels,
        onSelectionChange = { index, _ ->
            selected = index
            onBrowserSelected(browserValues[index])
        }
    )
}

/**
 * Browser option data class.
 */
data class BrowserOption(
    val value: String,
    val labelRes: StringResource
)

/**
 * Get browser options based on installed browsers on the system.
 * Only returns browsers that are actually installed, plus a disable option.
 */
private fun getInstalledBrowserOptions(): List<BrowserOption> {
    val installedBrowsers = BrowserDetector.getInstalledBrowsers()

    val browserOptions = installedBrowsers.mapNotNull { browser ->
        val labelRes = when (browser) {
            SupportedBrowser.FIREFOX -> Res.string.settings_browser_firefox
            SupportedBrowser.CHROME -> Res.string.settings_browser_chrome
            SupportedBrowser.SAFARI -> Res.string.settings_browser_safari
            SupportedBrowser.EDGE -> Res.string.settings_browser_edge
            SupportedBrowser.BRAVE -> Res.string.settings_browser_brave
            SupportedBrowser.OPERA -> Res.string.settings_browser_opera
            SupportedBrowser.VIVALDI -> Res.string.settings_browser_vivaldi
            SupportedBrowser.CHROMIUM -> Res.string.settings_browser_chromium
        }
        BrowserOption(browser.value, labelRes)
    }

    // Always add the disable option at the end
    return browserOptions + BrowserOption("", Res.string.settings_browser_disable)
}
