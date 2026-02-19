package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.icons.regular.ArrowSync
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.download_update
import ytdlpgui.composeapp.generated.resources.update_available
import kotlin.math.roundToInt

@OptIn(ExperimentalFluentApi::class)
@Composable
internal fun UpdateInfoBar(
    updateVersion: String,
    updateBody: String?,
    isDownloading: Boolean,
    downloadProgress: Double,
    isReadyToInstall: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoBar(
        title = {
            Text(
                stringResource(Res.string.update_available, updateVersion),
                style = FluentTheme.typography.caption
            )
        },
        message = {
            if (!updateBody.isNullOrBlank()) {
                MarkdownBody(
                    text = updateBody,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = InfoBarDefaults.colors(),
        icon = { InfoBarDefaults.Badge() },
        action = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    isReadyToInstall -> {
                        AccentButton(onClick = onInstall) {
                            Icon(
                                Icons.Default.ArrowSync,
                                contentDescription = null,
                            )
                            Text(
                                "Installer et redémarrer",
                                style = FluentTheme.typography.body,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    isDownloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ProgressBar(
                                progress = (downloadProgress / 100.0).toFloat(),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${downloadProgress.roundToInt()}%",
                                style = FluentTheme.typography.caption,
                            )
                        }
                    }
                    else -> {
                        AccentButton(onClick = onDownload) {
                            Icon(
                                Icons.Default.ArrowDownload,
                                contentDescription = stringResource(Res.string.download_update),
                            )
                            Text(
                                stringResource(Res.string.download_update),
                                style = FluentTheme.typography.body,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        },
        closeAction = { InfoBarDefaults.CloseActionButton(onClick = onDismiss) },
    )
}

@Composable
private fun MarkdownBody(text: String, modifier: Modifier = Modifier, lines: Int = 2) {
    val annotated = remember(text) { buildAnnotatedFromMarkdown(text) }
    val scrollState = rememberScrollState()
    val height = getHeightForLines(lines)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            var textLayoutResult: TextLayoutResult? = null
            Text(
                text = annotated,
                style = FluentTheme.typography.caption,
                fontSize = 10.sp,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.pointerInput(annotated) {
                    detectTapGestures { pos: Offset ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(pos)
                        annotated.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { sa ->
                                openUrlInBrowser(sa.item)
                            }
                    }
                }
            )
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.fillMaxHeight().padding(start = 4.dp)
        )
    }
}

private fun getHeightForLines(lines: Int): Dp = when (lines) {
    1 -> 20.dp
    2 -> 40.dp
    3 -> 60.dp
    else -> (lines * 20).dp
}

private fun buildAnnotatedFromMarkdown(markdown: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val linkRegex = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

    markdown.lines().forEachIndexed { index, rawLine ->
        var line = rawLine
            .replace(Regex("^#{1,6}\\s*"), "") // strip headings
            .replace(Regex("^\\s*[-*]\\s+"), "• ") // bullets

        var lastIndex = 0
        linkRegex.findAll(line).forEach { match ->
            val pre = line.substring(lastIndex, match.range.first)
            builder.append(pre)

            val linkText = match.groupValues[1]
            val url = match.groupValues[2]
            val start = builder.length
            builder.append(linkText)
            val end = builder.length
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline), start, end
            )
            builder.addStringAnnotation("URL", url, start, end)
            lastIndex = match.range.last + 1
        }
        builder.append(line.substring(lastIndex))
        if (index != markdown.lines().lastIndex) builder.append('\n')
    }
    return builder.toAnnotatedString()
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
        isDownloading = false,
        downloadProgress = 0.0,
        isReadyToInstall = false,
        onDownload = {},
        onInstall = {},
        onDismiss = {},
    )
}
