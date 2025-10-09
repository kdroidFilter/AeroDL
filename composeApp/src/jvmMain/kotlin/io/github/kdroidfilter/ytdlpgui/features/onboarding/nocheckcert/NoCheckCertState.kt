package io.github.kdroidfilter.ytdlpgui.features.onboarding.nocheckcert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class NoCheckCertState(
    val noCheckCertificate: Boolean = false
) {
    companion object {
        val enabledState = NoCheckCertState(noCheckCertificate = true)
        val disabledState = NoCheckCertState(noCheckCertificate = false)
    }
}

@Composable
fun collectNoCheckCertState(viewModel: OnboardingViewModel): NoCheckCertState = NoCheckCertState(
    noCheckCertificate = viewModel.noCheckCertificate.collectAsState().value
)
