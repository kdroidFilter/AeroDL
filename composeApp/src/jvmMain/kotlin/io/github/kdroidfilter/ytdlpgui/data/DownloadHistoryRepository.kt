package io.github.kdroidfilter.ytdlpgui.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.ytdlpgui.db.Database
import io.github.kdroidfilter.ytdlpgui.db.Download_history
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Simple repository to store and read download history using SQLDelight.
 */
class DownloadHistoryRepository(
    private val db: Database
) {
    data class HistoryItem(
        val id: String,
        val url: String,
        val videoInfo: VideoInfo?,
        val outputPath: String?,
        val isAudio: Boolean,
        val presetHeight: Int?,
        val createdAt: Long
    )

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    // Use a tolerant JSON instance for decoding/encoding
    private val json = Json { ignoreUnknownKeys = true }

    init {
        // Load once on init
        reload()
    }

    fun reload() {
        val rows = db.databaseQueries.selectAllHistory().executeAsList()
        // Keep only a bounded number of newest entries in memory to reduce RAM usage.
        // Older entries remain in the database and can be surfaced later if needed.
        _history.value = rows
            .asSequence()
            .map { it.toModel() }
            .take(MAX_HISTORY_IN_MEMORY)
            .toList()
    }

    fun add(
        id: String,
        url: String,
        videoInfo: VideoInfo?,
        outputPath: String?,
        isAudio: Boolean,
        presetHeight: Int?,
        createdAt: Long = System.currentTimeMillis()
    ) {
        // Avoid serialization failures on contextual types (e.g., Duration)
        val safeInfo = videoInfo?.copy(duration = null)
        val jsonStr = try { safeInfo?.let { json.encodeToString(it) } } catch (_: Throwable) { null }
        try {
            db.databaseQueries.insertHistory(
                id = id,
                url = url,
                video_info_json = jsonStr,
                output_path = outputPath,
                is_audio = if (isAudio) 1 else 0,
                preset_height = presetHeight?.toLong(),
                created_at = createdAt
            )
        } catch (_: Throwable) {
            // ignore insertion errors silently to avoid crashing the app
        }
        // Update in-memory cache
        reload()
    }

    fun delete(id: String) {
        try {
            db.databaseQueries.deleteHistoryById(id)
        } catch (_: Throwable) { /* ignore */ }
        reload()
    }

    fun clear() {
        try {
            db.databaseQueries.clearHistory()
        } catch (_: Throwable) { /* ignore */ }
        reload()
    }

    private fun Download_history.toModel(): HistoryItem {
        val info = try {
            this.video_info_json?.let { json.decodeFromString<VideoInfo>(it) }
        } catch (_: Throwable) { null }
        return HistoryItem(
            id = this.id,
            url = this.url,
            videoInfo = info,
            outputPath = this.output_path,
            isAudio = this.is_audio != 0L,
            presetHeight = this.preset_height?.toInt(),
            createdAt = this.created_at
        )
    }

    companion object {
        // Upper bound for history items kept in memory/UI.
        private const val MAX_HISTORY_IN_MEMORY: Int = 200

        fun createDatabase(dbFile: File): Database {
            dbFile.parentFile?.mkdirs()
            val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
            // Ensure schema exists (idempotent via IF NOT EXISTS)
            Database.Schema.create(driver)
            return Database(driver)
        }

        fun defaultDatabase(): Database {
            val baseDir = File(System.getProperty("user.home"), ".ytdlpgui")
            val dbFile = File(baseDir, "history.db")
            return createDatabase(dbFile)
        }
    }
}
