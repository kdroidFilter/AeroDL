package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            fontSize = fontSize,
            textAlign = textAlign ?: TextAlign.Unspecified,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout ?: {},
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            fontSize = fontSize,
            textAlign = textAlign ?: TextAlign.Unspecified,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
        )
    }
}

@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            fontSize = fontSize,
            textAlign = textAlign ?: TextAlign.Unspecified,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout ?: {},
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.Text(
            text = text.toString(),
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            fontSize = fontSize,
            textAlign = textAlign ?: TextAlign.Unspecified,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
        )
    }
}
