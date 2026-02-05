package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTooltip

/**
 * Composant qui affiche un texte avec ellipsis après 2 lignes
 * et affiche le texte complet dans un tooltip si le texte est tronqué.
 *
 * @param text Le texte à afficher
 * @param modifier Modifier à appliquer au composant
 * @param maxLines Nombre maximum de lignes avant ellipsis (défaut: 2)
 */
@Composable
fun EllipsizedTextWithTooltip(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    var isTextTruncated by remember { mutableStateOf(false) }

    val content = @Composable {
        AppText(
            text = text,
            modifier = modifier,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                isTextTruncated = textLayoutResult.hasVisualOverflow
            }
        )
    }

    if (isTextTruncated) {
        AppTooltip(
            tooltip = text,
            content = { content() }
        )
    } else {
        content()
    }
}
