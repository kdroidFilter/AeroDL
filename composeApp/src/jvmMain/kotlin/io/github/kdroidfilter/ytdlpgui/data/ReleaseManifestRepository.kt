package io.github.kdroidfilter.ytdlpgui.data

import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.platformtools.releasefetcher.github.model.Release
import io.github.kdroidfilter.ytdlp.model.AssetInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseEntries
import io.github.kdroidfilter.ytdlp.model.ReleaseInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant

/**
 * Fetches release information directly from the GitHub API via GitHubReleaseFetcher.
 * Network fetch happens only during onboarding or periodic checks (not during AOT training).
 */
class ReleaseManifestRepository {

    @Volatile
    private var cached: ReleaseManifest? = null

    fun getCachedManifest(): ReleaseManifest? = cached

    /**
     * Fetches latest releases from the GitHub API in parallel and builds the manifest.
     */
    suspend fun fetchManifest(): ReleaseManifest? {
        val httpClient = KtorConfig.createHttpClient()
        return runCatching {
            coroutineScope {
                val ytDlp = async { fetchRelease("yt-dlp", "yt-dlp", httpClient) }
                val ffmpeg = async { fetchRelease("yt-dlp", "FFmpeg-Builds", httpClient) }
                val ffmpegMacos = async { fetchRelease("kdroidFilter", "FFmpeg-Builds", httpClient) }
                val deno = async { fetchRelease("denoland", "deno", httpClient) }
                val aerodl = async { fetchRelease("kdroidFilter", "AeroDL", httpClient) }
                val python = async { fetchRelease("indygreg", "python-build-standalone", httpClient) }

                ReleaseManifest(
                    generatedAt = Instant.now().toString(),
                    schemaVersion = 1,
                    releases = ReleaseEntries(
                        ytDlp = ytDlp.await(),
                        ytDlpScript = ytDlp.await(),
                        python = python.await(),
                        ffmpeg = ffmpeg.await(),
                        ffmpegMacos = ffmpegMacos.await(),
                        deno = deno.await(),
                        aerodl = aerodl.await()
                    )
                )
            }
        }.onFailure { error ->
            if (LoggerConfig.enabled) {
                System.err.println("[ReleaseManifestRepository] Failed to fetch manifest: ${error.message}")
                error.printStackTrace()
            }
        }.also {
            httpClient.close()
        }.getOrNull()?.also { cached = it }
    }

    private suspend fun fetchRelease(
        owner: String,
        repo: String,
        httpClient: io.ktor.client.HttpClient
    ): ReleaseInfo {
        val release = GitHubReleaseFetcher(owner, repo, httpClient).getLatestRelease()
            ?: error("Failed to fetch $owner/$repo release")
        return release.toReleaseInfo()
    }
}

private fun Release.toReleaseInfo() = ReleaseInfo(
    tagName = tag_name,
    body = body,
    assets = assets.map { AssetInfo(it.name, it.browser_download_url) }
)
