package io.github.kdroidfilter.manifest

import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.ytdlp.model.AssetInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseEntries
import io.github.kdroidfilter.ytdlp.model.ReleaseInfo
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File
import java.time.Instant

private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
)

fun main() = runBlocking {
    println("Generating release manifest...")

    val httpClient = KtorConfig.createHttpClient()

    val ytdlp = httpClient.fetchLatestRelease("yt-dlp", "yt-dlp")
    println("  yt-dlp: ${ytdlp.tag_name}")

    val ffmpeg = httpClient.fetchLatestRelease("yt-dlp", "FFmpeg-Builds")
    println("  ffmpeg: ${ffmpeg.tag_name}")

    val ffmpegMacos = httpClient.fetchLatestRelease("kdroidFilter", "FFmpeg-Builds")
    println("  ffmpeg-macos: ${ffmpegMacos.tag_name}")

    val deno = httpClient.fetchLatestRelease("denoland", "deno")
    println("  deno: ${deno.tag_name}")

    val aerodl = httpClient.fetchLatestRelease("kdroidFilter", "AeroDL")
    println("  aerodl: ${aerodl.tag_name}")

    val python = httpClient.fetchLatestRelease("indygreg", "python-build-standalone")
    println("  python: ${python.tag_name}")

    val manifest = ReleaseManifest(
        generatedAt = Instant.now().toString(),
        schemaVersion = 1,
        releases = ReleaseEntries(
            ytDlp = ytdlp.toReleaseInfo(),
            ytDlpScript = ytdlp.toReleaseInfo(),
            python = python.toReleaseInfo(),
            ffmpeg = ffmpeg.toReleaseInfo(),
            ffmpegMacos = ffmpegMacos.toReleaseInfo(),
            deno = deno.toReleaseInfo(),
            aerodl = aerodl.toReleaseInfo()
        )
    )

    val outputDir = File(System.getenv("MANIFEST_OUTPUT") ?: "docs/api")
    outputDir.mkdirs()

    val jsonFile = File(outputDir, "releases.json")
    jsonFile.writeText(json.encodeToString(ReleaseManifest.serializer(), manifest))
    println("JSON manifest written to ${jsonFile.absolutePath}")

    @OptIn(ExperimentalSerializationApi::class)
    val pbFile = File(outputDir, "releases.pb")
    pbFile.writeBytes(ProtoBuf.encodeToByteArray(ReleaseManifest.serializer(), manifest))
    println("Protobuf manifest written to ${pbFile.absolutePath} (${pbFile.length()} bytes)")

    httpClient.close()
}

private suspend fun HttpClient.fetchLatestRelease(owner: String, repo: String): GitHubRelease {
    val response = get("https://api.github.com/repos/$owner/$repo/releases/latest") {
        header(HttpHeaders.UserAgent, "AeroDL-manifest-generator")
        header(HttpHeaders.Accept, "application/vnd.github+json")
    }
    if (!response.status.isSuccess()) {
        error("Failed to fetch $owner/$repo: ${response.status}")
    }
    return response.body()
}

private fun GitHubRelease.toReleaseInfo() = ReleaseInfo(
    tagName = tag_name,
    body = body,
    assets = assets.map { AssetInfo(it.name, it.browser_download_url) }
)
