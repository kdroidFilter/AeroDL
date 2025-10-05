package io.github.kdroidfilter.ytdlpgui.data

import io.github.kdroidfilter.ytdlpgui.db.Database
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Repository to fetch and cache supported sites from yt-dlp GitHub extractor directory.
 * On first initialization, it pulls the file listing via GitHub API and stores entries in DB.
 */
class SupportedSitesRepository(
    private val db: Database
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Basic blacklist to avoid overly generic or internal extractors
    private val blacklistNames = setOf(
        "__init__", "common", "generic", "_helper", "utils"
    )

    suspend fun initializeFromGitHubIfEmpty() {
        val count = runCatching<Long> { db.databaseQueries.countSupportedSites().executeAsOne() }.getOrElse { 0L }
        if (count > 0) return
        val items = runCatching<List<GithubContentItem>> { fetchExtractorDirectory() }.getOrElse { emptyList() }
        val now = System.currentTimeMillis()
        items
            .asSequence()
            .filter { it.type == "file" }
            .filter { it.name.endsWith(".py") }
            .filter { !it.name.startsWith("_") }
            .filter { it.name !in setOf("__init__.py") }
            .forEach { item ->
                val base = item.name.removeSuffix(".py")
                if (base.lowercase() in blacklistNames) return@forEach
                runCatching<Unit> {
                    db.databaseQueries.insertSupportedSite(
                        id = base,
                        filename = item.name,
                        download_url = item.downloadUrl,
                        sha = item.sha,
                        size = item.size?.toLong(),
                        created_at = now
                    )
                }
            }
    }

    fun clear() {
        runCatching<Unit> { db.databaseQueries.clearSupportedSites() }
    }

    /**
     * Returns simple keyword tokens (based on extractor base names) to match hostnames.
     */
    fun siteKeywords(): List<String> {
        return runCatching { db.databaseQueries.selectAllSupportedSiteIds().executeAsList() }
            .getOrElse { emptyList() }
            .asSequence()
            .map { it.lowercase() }
            .filter { it.isNotBlank() }
            .filter { it !in blacklistNames }
            .toList()
    }

    /**
     * Check if the given URL appears to be supported by any of the known extractor keywords.
     * Uses a simple hostname contains check for minimal false negatives.
     */
    fun matchesKnownSite(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase()
        } catch (_: Throwable) { null }
        val text = (host ?: url).lowercase()
        val keys = siteKeywords()
        // Keep it lightweight; bail out fast on small lists
        for (k in keys) {
            if (k.length < 3) continue // ignore too short tokens
            if (text.contains(k)) return true
        }
        return false
    }

    private fun fetchExtractorDirectory(): List<GithubContentItem> {
        val apiUrl = "https://api.github.com/repos/yt-dlp/yt-dlp/contents/yt_dlp/extractor?ref=master"
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "ytdlpgui/1.0")
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        return conn.inputStream.use { input ->
            val body = input.bufferedReader().readText()
            json.decodeFromString(body)
        }
    }
}

@Serializable
private data class GithubContentItem(
    val name: String,
    val path: String? = null,
    val sha: String? = null,
    val size: Int? = null,
    val type: String,
    @SerialName("download_url") val downloadUrl: String? = null,
)
