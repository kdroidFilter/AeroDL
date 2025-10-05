package io.github.kdroidfilter.ytdlpgui.core.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import org.jetbrains.skia.Image as SkiaImage

object NotificationThumbUtils {
    /**
     * Resolve a thumbnail URL to use in notifications. Prefers a provided direct thumbnail,
     * otherwise tries to derive a YouTube thumbnail from the URL if possible.
     */
    fun resolveThumbnailUrl(directThumb: String?, url: String): String? {
        if (!directThumb.isNullOrBlank()) return directThumb
        val id = YouTubeThumbnailHelper.extractVideoId(url) ?: return null
        return YouTubeThumbnailHelper.getThumbnailUrl(id, YouTubeThumbnailHelper.ThumbnailQuality.MEDIUM)
    }

    /**
     * Build a composable lambda that loads and renders the image at [thumbUrl] as a large icon.
     * Returns null if [thumbUrl] is null.
     */
    fun buildLargeIcon(thumbUrl: String?): (@Composable () -> Unit)? {
        if (thumbUrl.isNullOrBlank()) return null
        return {
            var imageBitmap by remember(thumbUrl) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(thumbUrl) {
                runCatching {
                    val bytes = java.net.URL(thumbUrl).readBytes()
                    val skiaImage = SkiaImage.makeFromEncoded(bytes)
                    imageBitmap = skiaImage.toComposeImageBitmap()
                }
            }
            imageBitmap?.let { img ->
                Image(
                    bitmap = img,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
