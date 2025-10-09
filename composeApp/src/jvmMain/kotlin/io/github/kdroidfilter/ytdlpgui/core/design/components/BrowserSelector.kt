package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.composefluent.component.ComboBox
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.settings_browser_chrome
import ytdlpgui.composeapp.generated.resources.settings_browser_disable
import ytdlpgui.composeapp.generated.resources.settings_browser_firefox
import ytdlpgui.composeapp.generated.resources.settings_browser_safari
import ytdlpgui.composeapp.generated.resources.settings_browser_select
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
    val availableBrowsers = getPlatformBrowserOptions()
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

    ComboBox(
        header = header,
        placeholder = actualPlaceholder,
        selected = selected,
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
 * Get browser options based on the current operating system.
 * - Linux: Firefox, Chrome, Disable
 * - Windows: Firefox, Disable
 * - macOS: Firefox, Safari, Disable
 */
@Composable
private fun getPlatformBrowserOptions(): List<BrowserOption> {
    return when (getOperatingSystem()) {
        OperatingSystem.WINDOWS -> listOf(
            BrowserOption("firefox", Res.string.settings_browser_firefox),
            BrowserOption("", Res.string.settings_browser_disable)
        )
        OperatingSystem.MACOS -> listOf(
            BrowserOption("firefox", Res.string.settings_browser_firefox),
            BrowserOption("safari", Res.string.settings_browser_safari),
            BrowserOption("", Res.string.settings_browser_disable)
        )
        else -> listOf(
            BrowserOption("firefox", Res.string.settings_browser_firefox),
            BrowserOption("chrome", Res.string.settings_browser_chrome),
            BrowserOption("", Res.string.settings_browser_disable)
        )
    }
}