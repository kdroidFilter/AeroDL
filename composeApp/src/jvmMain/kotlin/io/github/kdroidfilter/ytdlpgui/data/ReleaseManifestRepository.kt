package io.github.kdroidfilter.ytdlpgui.data

import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.warnln
import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.ytdlp.model.AssetInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseEntries
import io.github.kdroidfilter.ytdlp.model.ReleaseInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Builds the release manifest with hardcoded URLs for stable dependencies (FFmpeg, Deno, Python)
 * and a live GitHub API fetch for yt-dlp (the only frequently-updated component).
 */
class ReleaseManifestRepository {

    enum class ManifestSource(val label: String) {
        NETWORK("network"),
        CACHE("cache"),
        FALLBACK("fallback"),
    }

    @Volatile
    private var cached: ReleaseManifest? = null

    @Volatile
    private var lastManifestSource: ManifestSource? = null

    fun getCachedManifest(): ReleaseManifest? = cached

    fun getLastManifestSource(): ManifestSource? = lastManifestSource

    /**
     * Fetches the latest yt-dlp release from GitHub API and combines it
     * with hardcoded release info for FFmpeg, Deno, and Python.
     */
    suspend fun fetchManifest(): ReleaseManifest? {
        val httpClient = KtorConfig.createHttpClient()
        val fetchedManifest = runCatching {
            val responseText = httpClient
                .get("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
                .bodyAsText()
            val release = JSON.decodeFromString<GitHubRelease>(responseText)
            val ytDlpInfo = ReleaseInfo(
                tagName = release.tagName,
                body = release.body,
                assets = release.assets.map { AssetInfo(it.name, it.browserDownloadUrl) },
            )
            buildManifest(ytDlpInfo)
        }.onFailure { error ->
            warnln { "[ReleaseManifestRepository] Failed to fetch manifest from GitHub API: ${error.message}" }
        }.getOrNull()

        httpClient.close()

        val (resolved, source) = when {
            fetchedManifest != null -> fetchedManifest to ManifestSource.NETWORK
            cached != null -> cached!! to ManifestSource.CACHE
            else -> buildManifest(YTDLP_FALLBACK_RELEASE).also {
                warnln {
                    "[ReleaseManifestRepository] Using hardcoded yt-dlp fallback release: $YTDLP_FALLBACK_TAG"
                }
            } to ManifestSource.FALLBACK
        }

        infoln {
            "[ReleaseManifestRepository] Manifest source=${source.label}, yt-dlp tag=${resolved.releases.ytDlp.tagName}"
        }

        cached = resolved
        lastManifestSource = source
        return resolved
    }

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        private fun buildManifest(ytDlpInfo: ReleaseInfo): ReleaseManifest =
            ReleaseManifest(
                generatedAt = Instant.now().toString(),
                schemaVersion = 1,
                releases = ReleaseEntries(
                    ytDlp = ytDlpInfo,
                    ytDlpScript = ytDlpInfo,
                    python = PYTHON_RELEASE,
                    ffmpeg = FFMPEG_RELEASE,
                    ffmpegMacos = FFMPEG_MACOS_RELEASE,
                    deno = DENO_RELEASE,
                    aerodl = AERODL_PLACEHOLDER,
                ),
            )

        // ---- yt-dlp fallback (used only when GitHub API is unavailable and no cache exists) ----
        private const val YTDLP_FALLBACK_TAG = "2025.12.08"
        private const val YTDLP_FALLBACK_BASE =
            "https://github.com/yt-dlp/yt-dlp/releases/download/$YTDLP_FALLBACK_TAG"
        private val YTDLP_FALLBACK_RELEASE = ReleaseInfo(
            tagName = YTDLP_FALLBACK_TAG,
            assets = listOf(
                "yt-dlp",
                "yt-dlp.exe",
                "yt-dlp_macos",
                "yt-dlp_linux",
                "yt-dlp_linux_aarch64",
                "yt-dlp_linux_armv7l",
                "yt-dlp_musllinux",
                "yt-dlp_musllinux_aarch64",
                "yt-dlp_win_arm64.exe",
                "yt-dlp_x86.exe",
            ).map { AssetInfo(it, "$YTDLP_FALLBACK_BASE/$it") },
        )

        // ---- FFmpeg for Windows/Linux (yt-dlp/FFmpeg-Builds, tag=latest) ----
        private const val FFMPEG_BASE = "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest"
        private val FFMPEG_RELEASE = ReleaseInfo(
            tagName = "latest",
            assets = listOf(
                "ffmpeg-master-latest-win64-gpl.zip",
                "ffmpeg-master-latest-winarm64-gpl.zip",
                "ffmpeg-master-latest-win32-gpl.zip",
                "ffmpeg-master-latest-linux64-gpl.tar.xz",
                "ffmpeg-master-latest-linuxarm64-gpl.tar.xz",
            ).map { AssetInfo(it, "$FFMPEG_BASE/$it") },
        )

        // ---- FFmpeg for macOS (eugeneware/ffmpeg-static, tag=b6.1.1) ----
        private const val FFMPEG_MACOS_BASE = "https://github.com/eugeneware/ffmpeg-static/releases/download/b6.1.1"
        private val FFMPEG_MACOS_RELEASE = ReleaseInfo(
            tagName = "b6.1.1",
            assets = listOf(
                "ffmpeg-darwin-x64",
                "ffmpeg-darwin-arm64",
                "ffprobe-darwin-x64",
                "ffprobe-darwin-arm64",
            ).map { AssetInfo(it, "$FFMPEG_MACOS_BASE/$it") },
        )

        // ---- Deno (denoland/deno) ----
        private const val DENO_VERSION = "v2.6.8"
        private const val DENO_BASE = "https://github.com/denoland/deno/releases/download/$DENO_VERSION"
        private val DENO_RELEASE = ReleaseInfo(
            tagName = DENO_VERSION,
            assets = listOf(
                "deno-x86_64-pc-windows-msvc.zip",
                "deno-aarch64-pc-windows-msvc.zip",
                "deno-x86_64-unknown-linux-gnu.zip",
                "deno-aarch64-unknown-linux-gnu.zip",
                "deno-x86_64-apple-darwin.zip",
                "deno-aarch64-apple-darwin.zip",
            ).map { AssetInfo(it, "$DENO_BASE/$it") },
        )

        // ---- Python standalone for macOS (indygreg/python-build-standalone) ----
        private const val PYTHON_TAG = "20260203"
        private const val PYTHON_BASE =
            "https://github.com/indygreg/python-build-standalone/releases/download/$PYTHON_TAG"
        private val PYTHON_RELEASE = ReleaseInfo(
            tagName = PYTHON_TAG,
            assets = listOf(
                "cpython-3.12.12+$PYTHON_TAG-aarch64-apple-darwin-install_only.tar.gz",
                "cpython-3.12.12+$PYTHON_TAG-x86_64-apple-darwin-install_only.tar.gz",
            ).map { AssetInfo(it, "$PYTHON_BASE/$it") },
        )

        // ---- AeroDL placeholder (not used for update checks) ----
        private val AERODL_PLACEHOLDER = ReleaseInfo(
            tagName = "v0.0.0",
            assets = emptyList(),
        )
    }
}

/** Minimal model for the GitHub releases API - only the fields we actually need. */
@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)
