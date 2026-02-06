package io.github.kdroidfilter.manifest

import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.platformtools.releasefetcher.github.model.Release
import io.github.kdroidfilter.ytdlp.model.AssetInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseEntries
import io.github.kdroidfilter.ytdlp.model.ReleaseInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun main() = runBlocking {
    println("Generating release manifest...")

    val httpClient = KtorConfig.createHttpClient()

    // Fetch all releases via GitHubReleaseFetcher
    val ytdlp = GitHubReleaseFetcher("yt-dlp", "yt-dlp", httpClient).getLatestRelease()
        ?: error("Failed to fetch yt-dlp release")
    println("  yt-dlp: ${ytdlp.tag_name}")

    val ffmpeg = GitHubReleaseFetcher("yt-dlp", "FFmpeg-Builds", httpClient).getLatestRelease()
        ?: error("Failed to fetch FFmpeg release")
    println("  ffmpeg: ${ffmpeg.tag_name}")

    val ffmpegMacos = GitHubReleaseFetcher("kdroidFilter", "FFmpeg-Builds", httpClient).getLatestRelease()
        ?: error("Failed to fetch FFmpeg macOS release")
    println("  ffmpeg-macos: ${ffmpegMacos.tag_name}")

    val deno = GitHubReleaseFetcher("denoland", "deno", httpClient).getLatestRelease()
        ?: error("Failed to fetch Deno release")
    println("  deno: ${deno.tag_name}")

    val aerodl = GitHubReleaseFetcher("kdroidFilter", "AeroDL", httpClient).getLatestRelease()
        ?: error("Failed to fetch AeroDL release")
    println("  aerodl: ${aerodl.tag_name}")

    val python = GitHubReleaseFetcher("indygreg", "python-build-standalone", httpClient).getLatestRelease()
        ?: error("Failed to fetch Python standalone release")
    println("  python: ${python.tag_name}")

    // Build the manifest
    val manifest = ReleaseManifest(
        generatedAt = Instant.now().toString(),
        schemaVersion = 1,
        releases = ReleaseEntries(
            ytDlp = ytdlp.toReleaseInfo(),
            ytDlpScript = ytdlp.toReleaseInfo(), // Same release, but used for pure Python script
            python = python.toReleaseInfo(),
            ffmpeg = ffmpeg.toReleaseInfo(),
            ffmpegMacos = ffmpegMacos.toReleaseInfo(),
            deno = deno.toReleaseInfo(),
            aerodl = aerodl.toReleaseInfo()
        )
    )

    // Write to docs/api/releases.json at the project root.
    // The output path can be overridden via the MANIFEST_OUTPUT env var.
    val outputPath = System.getenv("MANIFEST_OUTPUT") ?: "docs/api/releases.json"
    val outputFile = File(outputPath)
    outputFile.parentFile.mkdirs()
    outputFile.writeText(json.encodeToString(ReleaseManifest.serializer(), manifest))
    println("Manifest written to ${outputFile.absolutePath}")

    httpClient.close()
}

private fun Release.toReleaseInfo() = ReleaseInfo(
    tagName = tag_name,
    body = body,
    assets = assets.map { AssetInfo(it.name, it.browser_download_url) }
)
