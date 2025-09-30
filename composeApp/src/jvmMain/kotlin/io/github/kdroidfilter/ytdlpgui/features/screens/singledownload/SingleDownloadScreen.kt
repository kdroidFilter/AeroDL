package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Pause
import io.github.composefluent.icons.regular.Play
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration

@Composable
fun SingleDownloadScreen() {
    val viewModel = koinViewModel<SingleDownloadViewModel>()
    val state = collectSingleDownloadState(viewModel)
    SingleDownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun SingleDownloadView(
    state: SingleDownloadState,
    onEvent: (SingleDownloadEvents) -> Unit,
) {
    val videoPlayerState = rememberVideoPlayerState()
    if (state.isLoading) Loader() else
        VideoInfoSection(
            videoPlayerState = videoPlayerState,
            videoInfo = state.videoInfo
        )
}

@Composable
private fun Loader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ProgressRing()
    }
}

/**
 * Main video section: preview on the right (as in the screenshot), title +
 * description on the left.
 */
@Composable
private fun VideoInfoSection(videoPlayerState: VideoPlayerState, videoInfo: VideoInfo?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Textual info (title + description)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 16.dp)
        ) {
            Text(
                text = videoInfo?.title.orEmpty(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            ThumbnailWithDuration(
                thumbnailUrl = videoInfo?.thumbnail,
                duration = videoInfo?.duration,
                videoPlayerState = videoPlayerState,
                videoInfo = videoInfo
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = videoInfo?.description.orEmpty(),
                fontSize = 14.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }


    }
}

/** Small pill for the duration text. */
@Composable
private fun DurationChip(text: String, modifier: Modifier = Modifier) {
    if (text.isEmpty()) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp, color = Color.White)
    }
}

/** Format java.time.Duration? into H:MM:SS (or MM:SS). */
private fun formatDuration(d: Duration?): String {
    if (d == null) return ""
    val totalSec = d.seconds.coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ThumbnailWithDuration(
    thumbnailUrl: String?,
    duration: Duration?,
    videoPlayerState: VideoPlayerState,
    videoInfo: VideoInfo?
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .onPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Exit) { isHovered = false }
    ) {

        Box(contentAlignment = Alignment.Center) {
            if (!videoPlayerState.isPlaying) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    imageVector = Icons.Default.Play,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clickable(
                        onClick = {
                            if (videoPlayerState.hasMedia) videoPlayerState.play()
                            else videoInfo?.directUrl?.let { videoPlayerState.openUri(it) }
                        }
                    ))

                // Subtle bottom gradient to improve contrast with the duration badge
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x66000000)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                // Duration badge (bottom-right), similar to YouTube style
                DurationChip(
                    text = formatDuration(duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            } else {
                VideoPlayerSurface(videoPlayerState, modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (videoPlayerState.isLoading) {
                            ProgressRing(modifier = Modifier.size(48.dp))
                        }
                        if (isHovered) {
                            IconButton({videoPlayerState.pause()}) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}
