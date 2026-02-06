package io.github.kdroidfilter.ytdlp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseManifest(
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("schema_version") val schemaVersion: Int,
    val releases: ReleaseEntries
)

@Serializable
data class ReleaseEntries(
    @SerialName("yt-dlp") val ytDlp: ReleaseInfo,
    @SerialName("yt-dlp-script") val ytDlpScript: ReleaseInfo? = null,  // Pure Python script version
    val python: ReleaseInfo? = null,  // Python standalone for macOS
    val ffmpeg: ReleaseInfo,
    @SerialName("ffmpeg-macos") val ffmpegMacos: ReleaseInfo,
    val deno: ReleaseInfo,
    val aerodl: ReleaseInfo
)

@Serializable
data class ReleaseInfo(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<AssetInfo>
)

@Serializable
data class AssetInfo(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)
