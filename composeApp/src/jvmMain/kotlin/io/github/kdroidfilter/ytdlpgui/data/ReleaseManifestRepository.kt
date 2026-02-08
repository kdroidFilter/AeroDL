package io.github.kdroidfilter.ytdlpgui.data

import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.network.HttpsConnectionFactory
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Fetches the static release manifest in protobuf format.
 * No HTTP call at startup to avoid AOT cache memory bloat.
 * Network fetch happens only during onboarding or periodic checks.
 */
@OptIn(ExperimentalSerializationApi::class)
class ReleaseManifestRepository {

    @Volatile
    private var cached: ReleaseManifest? = null

    /**
     * Returns the in-memory cached manifest, or null if not yet fetched.
     */
    fun getCachedManifest(): ReleaseManifest? = cached

    /**
     * Fetches manifest from network as protobuf and caches in memory.
     */
    suspend fun fetchManifest(): ReleaseManifest? {
        return runCatching {
            val conn = HttpsConnectionFactory.openConnection(MANIFEST_URL)
            val bytes = try {
                conn.inputStream.readBytes()
            } finally {
                conn.disconnect()
            }
            ProtoBuf.decodeFromByteArray(ReleaseManifest.serializer(), bytes)
        }.onFailure { error ->
            if (LoggerConfig.enabled) {
                System.err.println("[ReleaseManifestRepository] Failed to fetch manifest: ${error.message}")
                error.printStackTrace()
            }
        }.getOrNull()?.also { cached = it }
    }

    companion object {
        const val MANIFEST_URL = "https://kdroidfilter.github.io/AeroDL/api/releases.pb"
    }
}
