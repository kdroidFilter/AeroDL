package io.github.kdroidfilter.ytdlpgui.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Button
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.Text
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_progress_label
import ytdlpgui.composeapp.generated.resources.next
import ytdlpgui.composeapp.generated.resources.onboarding_skip

@Composable
internal fun HeaderRow(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(title)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle)
        }
    }
}

@Composable
internal fun OnboardingProgress(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val steps = remember { OnboardingStep.values().toList() }
    val currentIndex = steps.indexOf(step).takeIf { it >= 0 } ?: 0
    val totalSteps = steps.size
    val progress = ((currentIndex + 1).toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = stringResource(Res.string.onboarding_progress_label, currentIndex + 1, totalSteps),
            style = FluentTheme.typography.caption
        )
        Spacer(Modifier.height(6.dp))
        ProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun NavigationRow(
    onNext: () -> Unit,
    onSkip: (() -> Unit)? = null,
    nextLabel: String? = null,
    skipLabel: String? = null
) {
    val resolvedNext = nextLabel ?: stringResource(Res.string.next)
    val resolvedSkip = skipLabel ?: stringResource(Res.string.onboarding_skip)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        if (onSkip != null) {
            Button(
                modifier = Modifier.padding(end = 8.dp),
                onClick = onSkip,
                content = { Text(resolvedSkip) }
            )
        }
        Button(onClick = onNext, content = { Text(resolvedNext) })
    }
}
