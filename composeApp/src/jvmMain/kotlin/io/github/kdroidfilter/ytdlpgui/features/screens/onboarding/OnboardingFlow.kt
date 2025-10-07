package io.github.kdroidfilter.ytdlpgui.features.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import io.github.composefluent.component.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.OpenFolder
import io.github.composefluent.icons.filled.TopSpeed
import io.github.composefluent.icons.regular.Cookies
import io.github.composefluent.icons.regular.LockShield
import io.github.composefluent.icons.regular.Power
import io.github.kdroidfilter.ytdlpgui.core.ui.icons.BrowserChrome
import io.github.kdroidfilter.ytdlpgui.core.ui.icons.BrowserFirefox
import io.github.kdroidfilter.ytdlpgui.core.ui.icons.Cookie_off
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.settings.SettingsEvents
import io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.settings.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.russhwolf.settings.Settings as KvSettings
import io.github.kdroidfilter.ytdlpgui.core.ui.components.Switcher
import kotlinx.coroutines.launch


@Composable
private fun HeaderRow(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(title)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle)
        }
    }
}

@Composable
private fun NavigationRow(onNext: () -> Unit, onSkip: (() -> Unit)? = null, nextLabel: String = "Suivant", skipLabel: String = "Ignorer") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        if (onSkip != null) {
            Button(onClick = onSkip, content = { Text(skipLabel) })
        }
        Button(onClick = onNext, content = { Text(nextLabel) })
    }
}

@Composable
fun OnboardingWelcomeScreen() {
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenue dans AeroDL")
        Spacer(Modifier.height(12.dp))
        Text("Nous allons configurer quelques réglages pour bien démarrer.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { scope.launch { navigator.navigate(Destination.Onboarding.DownloadDir) } }) { Text("Commencer") }
    }
}

@Composable
fun OnboardingDownloadDirStep() {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.downloadDirPath.collectAsState()
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        HeaderRow("Dossier de téléchargement", "Choisissez où enregistrer vos téléchargements")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.OpenFolder, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.ifBlank { "(Non défini)" })
            Spacer(Modifier.width(12.dp))
            Button(onClick = { vm.onEvents(SettingsEvents.PickDownloadDir("Choisir un dossier")) }) {
                Text("Parcourir…")
            }
        }
        NavigationRow(
            onNext = { scope.launch { navigator.navigate(Destination.Onboarding.Cookies) } },
            onSkip = { scope.launch { navigator.navigate(Destination.Onboarding.Cookies) } }
        )
    }
}

@Composable
fun OnboardingCookiesStep() {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.cookiesFromBrowser.collectAsState()
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        HeaderRow("Cookies depuis un navigateur", "Sélectionnez un navigateur ou désactivez")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.onEvents(SettingsEvents.SetCookiesFromBrowser("chrome")) }) {
                Icon(BrowserChrome, "Chrome", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chrome")
            }
            Button(onClick = { vm.onEvents(SettingsEvents.SetCookiesFromBrowser("firefox")) }) {
                Icon(BrowserFirefox, "Firefox", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Firefox")
            }
            Button(onClick = { vm.onEvents(SettingsEvents.SetCookiesFromBrowser("") ) }) {
                Icon(Cookie_off, "Désactiver", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Désactiver")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Regular.Cookies, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.ifBlank { "Désactivé" })
        }
        NavigationRow(
            onNext = { scope.launch { navigator.navigate(Destination.Onboarding.IncludePreset) } },
            onSkip = { scope.launch { navigator.navigate(Destination.Onboarding.IncludePreset) } }
        )
    }
}

@Composable
fun OnboardingIncludePresetStep() {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.includePresetInFilename.collectAsState()
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        HeaderRow("Inclure le preset dans le nom du fichier")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switcher(checked = state, onCheckStateChange = { vm.onEvents(SettingsEvents.SetIncludePresetInFilename(it)) })
            Spacer(Modifier.width(8.dp))
            Text(if (state) "Activé" else "Désactivé")
        }
        NavigationRow(
            onNext = { scope.launch { navigator.navigate(Destination.Onboarding.Parallel) } },
            onSkip = { scope.launch { navigator.navigate(Destination.Onboarding.Parallel) } }
        )
    }
}

@Composable
fun OnboardingParallelStep() {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.parallelDownloads.collectAsState()
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        HeaderRow("Téléchargements parallèles")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { count ->
                Button(onClick = { vm.onEvents(SettingsEvents.SetParallelDownloads(count)) }) {
                    Text(count.toString())
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TopSpeed, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sélectionné: $state")
        }
        NavigationRow(
            onNext = { scope.launch { navigator.navigate(Destination.Onboarding.NoCheckCert) } },
            onSkip = { scope.launch { navigator.navigate(Destination.Onboarding.NoCheckCert) } }
        )
    }
}

@Composable
fun OnboardingNoCheckStep() {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.noCheckCertificate.collectAsState()
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        HeaderRow("Ignorer la vérification du certificat")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switcher(checked = state, onCheckStateChange = { vm.onEvents(SettingsEvents.SetNoCheckCertificate(it)) })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Regular.LockShield, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (state) "Activé" else "Désactivé")
        }
        NavigationRow(
            onNext = { scope.launch { navigator.navigate(Destination.Onboarding.Clipboard) } },
            onSkip = { scope.launch { navigator.navigate(Destination.Onboarding.Clipboard) } }
        )
    }
}

@Composable
fun OnboardingClipboardStep() {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.clipboardMonitoring.collectAsState()
    val navigator = koinInject<Navigator>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        HeaderRow("Surveillance du presse-papiers")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switcher(checked = state, onCheckStateChange = { vm.onEvents(SettingsEvents.SetClipboardMonitoring(it)) })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Regular.Power, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (state) "Activé" else "Désactivé")
        }
        NavigationRow(
            onNext = { scope.launch { navigator.navigate(Destination.Onboarding.Finish) } },
            onSkip = { scope.launch { navigator.navigate(Destination.Onboarding.Finish) } }
        )
    }
}

@Composable
fun OnboardingFinishStep() {
    val navigator = koinInject<Navigator>()
    val kv = koinInject<KvSettings>()

    // Mark onboarding completed once we enter this step
    LaunchedEffect(Unit) {
        kv.putBoolean("onboarding_completed", true)
        navigator.navigateAndClearBackStack(Destination.MainNavigation.Home)
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Configuration terminée…")
    }
}
