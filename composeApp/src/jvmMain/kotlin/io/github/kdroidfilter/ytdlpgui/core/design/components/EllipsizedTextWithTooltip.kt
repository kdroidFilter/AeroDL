package io.github.kdroidfilter.ytdlpgui.core.design.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Text
import io.github.composefluent.component.TooltipBox

/**
 * Composant qui affiche un texte avec ellipsis après 2 lignes
 * et affiche le texte complet dans un tooltip si le texte est tronqué.
 *
 * @param text Le texte à afficher
 * @param modifier Modifier à appliquer au composant
 * @param maxLines Nombre maximum de lignes avant ellipsis (défaut: 2)
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalFluentApi::class)
@Composable
fun EllipsizedTextWithTooltip(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    var isTextTruncated by remember { mutableStateOf(false) }

    val content = @Composable {
        Text(
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
        TooltipBox(
            tooltip = {
                    Text(
                        text = text,
                        modifier = Modifier.padding(8.dp),
                    )

            },
            content = { content() }
        )
    } else {
        content()
    }
}