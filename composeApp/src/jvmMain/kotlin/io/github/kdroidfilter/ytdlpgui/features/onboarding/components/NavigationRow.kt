package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppAccentButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.next
import ytdlpgui.composeapp.generated.resources.onboarding_previous
import ytdlpgui.composeapp.generated.resources.onboarding_skip

@Composable
internal fun NavigationRow(
    onNext: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    nextLabel: String? = null,
    previousLabel: String? = null,
    nextEnabled: Boolean = true
) {
    val resolvedNext = nextLabel ?: stringResource(Res.string.next)
    val resolvedPrevious = previousLabel ?: stringResource(Res.string.onboarding_previous)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            if (onPrevious != null) {
                AppButton(
                    onClick = onPrevious,
                    content = { AppText(resolvedPrevious) }
                )
            }
        }
        Row {
            AppAccentButton(
                onClick = onNext,
                enabled = nextEnabled,
                content = { AppText(resolvedNext) }
            )
        }
    }
}

@Preview
@Composable
private fun NavigationRowFullPreview() {
    Column(Modifier.padding(16.dp)) {
        NavigationRow(
            onNext = {},
            onPrevious = {},
            nextEnabled = true
        )
    }
}

@Preview
@Composable
private fun NavigationRowOnlyNextPreview() {
    Column(Modifier.padding(16.dp)) {
        NavigationRow(
            onNext = {},
            nextEnabled = true
        )
    }
}

@Preview
@Composable
private fun NavigationRowNextDisabledPreview() {
    Column(Modifier.padding(16.dp)) {
        NavigationRow(
            onNext = {},
            onPrevious = {},
            nextEnabled = false
        )
    }
}
