package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppAccentButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppInfoBar
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppInfoBarSeverity
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.download_update
import ytdlpgui.composeapp.generated.resources.update_available

@Composable
internal fun UpdateInfoBar(
    updateVersion: String,
    updateBody: String?,
    updateUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppInfoBar(
        title = stringResource(Res.string.update_available, updateVersion),
        message = updateBody?.takeIf { it.isNotBlank() } ?: "",
        severity = AppInfoBarSeverity.Info,
        modifier = modifier.fillMaxWidth(),
        onDismiss = onDismiss,
        action = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                AppAccentButton(onClick = {
                    openUrlInBrowser(updateUrl)
                    onDismiss()
                }) {
                    AppIcon(
                        AppIcons.Download, contentDescription = stringResource(Res.string.download_update),
                    )
                    AppText(
                        stringResource(Res.string.download_update),
                        style = AppTypography.body,
                        fontSize = 12.sp,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun UpdateInfoBarPreview() {
    UpdateInfoBar(
        updateVersion = "1.2.3",
        updateBody = """
            ## Nouveautés
            - Améliorations de performance
            - Corrections de bugs
            Consultez le changelog complet [ici](https://example.com/changelog).
        """.trimIndent(),
        updateUrl = "https://example.com/download",
        onDismiss = {},
    )
}
