package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayApp
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.component.Button
import io.github.composefluent.component.Text
import io.github.composefluent.darkColors
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.PictureInPictureExit
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.icons.regular.Window
import io.github.composefluent.lightColors
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.util.allowYtDlpLogging
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.greeting
import ytdlpgui.composeapp.generated.resources.quit
import java.io.File

@OptIn(ExperimentalTrayAppApi::class)
fun main() = application {

    val wrapper = YtDlpWrapper().apply {
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    }
    wrapper.noCheckCertificate = true
    allowYtDlpLogging = true




    var videoReady by rememberSaveable { mutableStateOf(false)}
    val videoState = rememberVideoPlayerState()
    TrayApp(
        icon = Icons.Default.ArrowDownload,
        tooltip = "YTDLP GUI",
        windowSize = DpSize(300.dp, 500.dp),
        menu = {
            CheckableItem("Ouvrir au démarrage", checked = true, onCheckedChange = {})
            Item(
                "Quitter",
                onClick = { exitApplication() },
                icon = Icons.Filled.PictureInPictureExit
            )
        }
    ) {
        FluentTheme(colors = if (isSystemInDarkMode()) darkColors() else lightColors()) {
            Mica(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {


                    LaunchedEffect(Unit){
                        val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"

                        wrapper.initialize { event ->
                            // Display initialization events to inform the user
                            when (event) {
                                is YtDlpWrapper.InitEvent.DownloadingYtDlp -> println("    -> Downloading yt-dlp...")
                                is YtDlpWrapper.InitEvent.EnsuringFfmpeg -> println("    -> Checking FFmpeg...")
                                is YtDlpWrapper.InitEvent.Completed -> if (event.success) println("✅ Initialization completed successfully!")
                                is YtDlpWrapper.InitEvent.Error -> System.err.println("❌ Initialization error: ${event.message}")
                                else -> {} // Ignore other events like progress to keep the output concise
                            }
                        }

                        wrapper.getVideoInfo(videoUrl, timeoutSec = 60)
                            .onSuccess { video ->
                                println("✅ Video found:")
                                println("  📝 Title: ${video.title}")
                                println("  👤 Uploader: ${video.uploader}")
                                println("  ⏱️ Duration: ${video.duration}")
                                println("  👁️ Views: ${video.viewCount}")
                                println("  🔗 Direct link: ${video.directUrl ?: "N/A"}")
                                videoReady = true
                                video.directUrl?.let { videoState.openUri(it) }
                                println("  📈 Available Resolutions:")
                                video.availableResolutions.toSortedMap(compareByDescending { it }).forEach { (height, res) ->
                                    println("     - ${height}p (Progressive: ${if (res.progressive) "Yes" else "No"})")
                                }
                            }
                            .onFailure {
                                println("❌ Failure: ${it.message}")
                                it.cause?.printStackTrace()
                            }


                    }
                    if (videoReady) {
                        Box(Modifier.fillMaxSize()) {
                            VideoPlayerSurface(videoState)
                        }
                    }
                }
            }
        }
    }
}