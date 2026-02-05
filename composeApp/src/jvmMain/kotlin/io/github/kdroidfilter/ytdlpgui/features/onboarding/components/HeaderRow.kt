package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography

@Composable
internal fun HeaderRow(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        AppText(title, style = AppTypography.subtitle)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            ExpandableDescription(description = subtitle)
        }
    }
}

@Preview
@Composable
private fun HeaderRowPreview() {
    Column(Modifier.padding(16.dp)) {
        HeaderRow(
            title = "Welcome to yt-dlp GUI",
            subtitle = "Let's set up your preferences"
        )
    }
}

@Preview
@Composable
private fun HeaderRowWithoutSubtitlePreview() {
    Column(Modifier.padding(16.dp)) {
        HeaderRow(title = "Download Directory")
    }
}
