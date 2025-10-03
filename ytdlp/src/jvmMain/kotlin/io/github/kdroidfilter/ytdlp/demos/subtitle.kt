package io.github.kdroidfilter.ytdlp.demos

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val wrapper = YtDlpWrapper().apply {
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp-with-subtitles")
        noCheckCertificate = true
    }

    // Initialize the wrapper
    println("🔧 Initializing yt-dlp/ffmpeg...")
    val initOk = wrapper.initialize { event ->
        when (event) {
            is YtDlpWrapper.InitEvent.Completed -> {
                if (event.success) println("✅ Initialization successful!")
                else System.err.println("❌ Initialization failed.")
            }
            is YtDlpWrapper.InitEvent.Error -> {
                System.err.println("❌ Error: ${event.message}")
            }
            else -> {} // Silence other events for brevity
        }
    }

    if (!initOk) {
        println("Stopping because initialization failed.")
        return@runBlocking
    }

    val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm" // Example YouTube video

    // ===== EXAMPLE 1: Get video info with all subtitles =====
    println("\n📺 EXAMPLE 1: Fetching video info with all available subtitles...")

    wrapper.getVideoInfoWithAllSubtitles(
        url = videoUrl,
        includeAutoSubtitles = true
    ).onSuccess { video ->
        println("✅ Video: ${video.title}")
        println("📝 Available Subtitles:")

        // Show manual subtitles
        val manualSubs = video.availableSubtitles.filter { !it.value.isAutomatic }
        if (manualSubs.isNotEmpty()) {
            println("  Manual subtitles:")
            manualSubs.forEach { (lang, info) ->
                println("    - $lang: ${info.languageName ?: "Unknown"}")
                info.formats.forEach { format ->
                    println("      • ${format.ext} format available")
                }
            }
        }

        // Show automatic captions
        val autoSubs = video.automaticCaptions
        if (autoSubs.isNotEmpty()) {
            println("  Auto-generated subtitles:")
            autoSubs.forEach { (lang, info) ->
                println("    - $lang: ${info.languageName ?: "Unknown"} (auto)")
            }
        }

        if (video.availableSubtitles.isEmpty() && video.automaticCaptions.isEmpty()) {
            println("  ⚠️ No subtitles available for this video")
        }
    }.onFailure {
        println("❌ Failed to get video info: ${it.message}")
    }

    // ===== EXAMPLE 2: Download video with specific subtitles =====
    println("\n📺 EXAMPLE 2: Download video with English and French subtitles...")

    val downloadHandle1 = wrapper.downloadWithSpecificSubtitles(
        url = videoUrl,
        languages = listOf("en", "fr"), // English and French
        preset = YtDlpWrapper.Preset.P720,
        includeAutoSubtitles = true,    // Include auto-generated if manual not available
        keepSubtitleFiles = true,       // Also save .srt files separately
        subtitleFormat = "srt",
        onEvent = { event ->
            when (event) {
                is Event.Started -> println("  → Download started")
                is Event.Progress -> print("\r  → Progress: ${"%.1f".format(event.percent ?: 0.0)}%")
                is Event.Completed -> {
                    println("\n  → ${if (event.success) "✅ Success!" else "❌ Failed"}")
                }
                is Event.Error -> println("\n  ❌ Error: ${event.message}")
                else -> {}
            }
        }
    )

    // Wait for completion (in a real app, you wouldn't block like this)
    downloadHandle1.job.join()

    // ===== EXAMPLE 3: Download video with ALL available subtitles =====
    println("\n📺 EXAMPLE 3: Download video with ALL available subtitles...")

    val downloadHandle2 = wrapper.downloadWithSubtitles(
        url = videoUrl,
        preset = YtDlpWrapper.Preset.P480,
        subtitleLanguages = null,       // null = download all available
        includeAutoSubtitles = true,
        onEvent = { event ->
            when (event) {
                is Event.Started -> println("  → Download started")
                is Event.Progress -> print("\r  → Progress: ${"%.1f".format(event.percent ?: 0.0)}%")
                is Event.Completed -> {
                    println("\n  → ${if (event.success) "✅ Success!" else "❌ Failed"}")
                }
                is Event.Error -> println("\n  ❌ Error: ${event.message}")
                else -> {}
            }
        }
    )

    downloadHandle2.job.join()

    // ===== EXAMPLE 4: Download ONLY subtitles (no video) =====
    println("\n📺 EXAMPLE 4: Download ONLY subtitles without video...")

    val subtitleDir = File(wrapper.downloadDir, "subtitles-only")
    subtitleDir.mkdirs()

    val downloadHandle3 = wrapper.downloadSubtitlesOnly(
        url = videoUrl,
        languages = listOf("en", "es", "fr", "de"), // Multiple languages
        includeAutoSubtitles = true,
        subtitleFormat = "srt",
        outputDir = subtitleDir,
        onEvent = { event ->
            when (event) {
                is Event.Started -> println("  → Subtitle download started")
                is Event.Completed -> {
                    if (event.success) {
                        println("  ✅ Subtitles downloaded to: ${subtitleDir.absolutePath}")
                        // List downloaded subtitle files
                        subtitleDir.listFiles()?.filter { it.extension == "srt" }?.forEach {
                            println("    📄 ${it.name}")
                        }
                    } else {
                        println("  ❌ Failed to download subtitles")
                    }
                }
                is Event.Error -> println("  ❌ Error: ${event.message}")
                else -> {}
            }
        }
    )

    downloadHandle3.job.join()

    // ===== EXAMPLE 5: Advanced - Custom subtitle configuration =====
    println("\n📺 EXAMPLE 5: Advanced custom subtitle configuration...")

    val customSubtitleOptions = io.github.kdroidfilter.ytdlp.core.SubtitleOptions(
        languages = listOf("en", "ja"),  // English and Japanese
        writeSubtitles = true,            // Write subtitle files
        embedSubtitles = true,            // Embed in video
        writeAutoSubtitles = true,        // Include auto-generated
        subFormat = "ass",                // Advanced SubStation Alpha format
        convertSubtitles = "srt"          // Also convert to SRT
    )

    val customOptions = io.github.kdroidfilter.ytdlp.core.Options(
        format = "bestvideo[height<=1080]+bestaudio/best",
        outputTemplate = "%(title)s_with_subs.%(ext)s",
        noCheckCertificate = wrapper.noCheckCertificate,
        cookiesFromBrowser = wrapper.cookiesFromBrowser,
        targetContainer = "mkv",          // MKV container supports more subtitle formats
        subtitles = customSubtitleOptions
    )

    val downloadHandle4 = wrapper.download(
        url = videoUrl,
        options = customOptions,
        onEvent = { event ->
            when (event) {
                is Event.Started -> println("  → Custom download started")
                is Event.Progress -> print("\r  → Progress: ${"%.1f".format(event.percent ?: 0.0)}%")
                is Event.Completed -> {
                    println("\n  → ${if (event.success) "✅ Success!" else "❌ Failed"}")
                }
                is Event.Error -> println("\n  ❌ Error: ${event.message}")
                else -> {}
            }
        }
    )

    downloadHandle4.job.join()

    println("\n🎉 All examples completed!")
    println("📁 Check the download directory: ${wrapper.downloadDir?.absolutePath}")
}

/**
 * Utility function to display subtitle availability in a user-friendly way
 */
fun printSubtitleInfo(video: io.github.kdroidfilter.ytdlp.model.VideoInfo) {
    println("📝 Subtitle Information for: ${video.title}")

    val allLanguages = video.getAllSubtitleLanguages()
    if (allLanguages.isEmpty()) {
        println("  ⚠️ No subtitles available")
        return
    }

    println("  Total languages available: ${allLanguages.size}")

    // Group by manual vs automatic
    val manualLanguages = video.getManualSubtitleLanguages()
    val autoLanguages = allLanguages - manualLanguages

    if (manualLanguages.isNotEmpty()) {
        println("  📖 Manual subtitles (${manualLanguages.size}):")
        manualLanguages.forEach { lang ->
            val info = video.availableSubtitles[lang]!!
            println("    • $lang - ${info.languageName ?: "Unknown"}")
        }
    }

    if (autoLanguages.isNotEmpty()) {
        println("  🤖 Auto-generated subtitles (${autoLanguages.size}):")
        autoLanguages.forEach { lang ->
            val info = video.availableSubtitles[lang]!!
            println("    • $lang - ${info.languageName ?: "Unknown"}")
        }
    }
}