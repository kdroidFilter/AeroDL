package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import com.kdroid.composetray.tray.api.rememberTrayAppState
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.darkColors
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.PictureInPictureExit
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.lightColors
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.App
import io.github.kdroidfilter.ytdlpgui.di.appModule
import org.koin.compose.KoinApplication

@OptIn(ExperimentalTrayAppApi::class, ExperimentalFluentApi::class)
fun main() = application {

    KoinApplication(application = {
        modules(appModule)
    }) {

        val trayAppState = rememberTrayAppState(
            initialWindowSize = DpSize(350.dp, 500.dp),
            initiallyVisible = true
        )
        val trayVisible by trayAppState.isVisible.collectAsState()


        TrayApp(
            state = trayAppState,
            icon = Icons.Default.ArrowDownload,
            tooltip = "YTDLP GUI",
            menu = {
                CheckableItem("Ouvrir au démarrage", checked = true, onCheckedChange = {})
                if (!trayVisible) Item("Afficher la fenetre", onClick = { trayAppState.show() })
                else Item("Cacher la fenetre", onClick = { trayAppState.hide() })
                Item("Quitter", onClick = { exitApplication() }, icon = Icons.Filled.PictureInPictureExit)
            }
        ) {
            val navController = rememberNavController()

            FluentTheme(colors = if (isSystemInDarkMode()) darkColors() else lightColors()) {
                Mica(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isSystemInDarkMode()) Color.DarkGray else Color.LightGray,
                            RoundedCornerShape(12.dp)
                        )

                ) {

                    App(navController)


//                Column(
//                    Modifier.fillMaxSize().padding(4.dp),
//                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
//                ) {
//
//                    var selectedIndex by remember { mutableStateOf(0) }
//                    var expanded by remember { mutableStateOf(false) }
//                    TopNav(
//                        expanded = expanded,
//                        onExpandedChanged = { expanded = it },
//                    ) {
//                        items(3) { index ->
//                            TopNavItem(
//                                selected = index == selectedIndex,
//                                onClick = {
//                                    selectedIndex = index
//                                },
//                                text = {
//                                    Text(text = "Item ${index + 1}")
//                                },
//                                icon = {
//                                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
//                                }
//                            )
//                        }
//                    }
//
//
//                    var value by remember { mutableStateOf(TextFieldValue()) }
//                    TextField(value, onValueChange = { value = it }, modifier = Modifier.fillMaxWidth())
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    ProgressBar(modifier = Modifier.fillMaxWidth())
//                    LaunchedEffect(Unit){
//                        val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"
//
//                        wrapper.initialize { event ->
//                            // Display initialization events to inform the user
//                            when (event) {
//                                is YtDlpWrapper.InitEvent.DownloadingYtDlp -> println("    -> Downloading yt-dlp...")
//                                is YtDlpWrapper.InitEvent.EnsuringFfmpeg -> println("    -> Checking FFmpeg...")
//                                is YtDlpWrapper.InitEvent.Completed -> if (event.success) println("✅ Initialization completed successfully!")
//                                is YtDlpWrapper.InitEvent.Error -> System.err.println("❌ Initialization error: ${event.message}")
//                                else -> {} // Ignore other events like progress to keep the output concise
//                            }
//                        }
//
//                        wrapper.getVideoInfo(videoUrl, timeoutSec = 60)
//                            .onSuccess { video ->
//                                println("✅ Video found:")
//                                println("  📝 Title: ${video.title}")
//                                println("  👤 Uploader: ${video.uploader}")
//                                println("  ⏱️ Duration: ${video.duration}")
//                                println("  👁️ Views: ${video.viewCount}")
//                                println("  🔗 Direct link: ${video.directUrl ?: "N/A"}")
//                                videoReady = true
//                                video.directUrl?.let { videoState.openUri(it) }
//                                println("  📈 Available Resolutions:")
//                                video.availableResolutions.toSortedMap(compareByDescending { it }).forEach { (height, res) ->
//                                    println("     - ${height}p (Progressive: ${if (res.progressive) "Yes" else "No"})")
//                                }
//                            }
//                            .onFailure {
//                                println("❌ Failure: ${it.message}")
//                                it.cause?.printStackTrace()
//                            }
//
//
//                    }
//                    if (videoReady) {
//                        Box(Modifier.fillMaxSize()) {
//                            VideoPlayerSurface(videoState) {
//
//                                Button(onClick = {
//                                    videoState.toggleFullscreen()
//                                }) {
//                                    Icon(Icons.Default.ArrowDown, "")
//                                }
//                            }
//                        }
//                    }
//                }
                }
            }
        }
    }
}