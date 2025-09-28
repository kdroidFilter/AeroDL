import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.YtDlpWrapper.InitEvent

import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val wrapper = YtDlpWrapper().apply {
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    }
    // Set the parameter globally for all operations
    wrapper.noCheckCertificate = true


    // Initialization
    println("🔧 Initializing yt-dlp/ffmpeg…")
    val initOk = wrapper.initialize { ev ->
        when (ev) {
            is InitEvent.CheckingYtDlp -> println("🔍 Checking yt-dlp…")
            is InitEvent.EnsuringFfmpeg -> println("🎬 Checking FFmpeg…")
            is InitEvent.Completed -> println(if (ev.success) "✅ Init OK" else "❌ Init failed")
            else -> {} // Simplify output
        }
    }

    if (!initOk) {
        println("Stopping because initialization failed.")
        return@runBlocking
    }

    // =========================
    // TEST 1: Simple Video
    // =========================
    println("\n📹 TEST 1: Simple Video")
    val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm" // Me at the zoo

    wrapper.getVideoInfo(videoUrl, timeoutSec = 60)
        .onSuccess { video ->
            println("✅ Video found:")
            println("  📝 Title: ${video.title}")
            println("  👤 Uploader: ${video.uploader}")
            println("  ⏱️ Duration: ${video.duration}")
            println("  👁️ Views: ${video.viewCount}")
            println("   Direct link ${video.directUrl}")
        }
        .onFailure {
            println("❌ Failure: ${it.message}")
            println(it.cause)
        }

    // =========================
    // TEST 2: YouTube Playlist
    // =========================
    println("\n📚 TEST 2: Playlist")
    val playlistUrls = listOf(
        "https://www.youtube.com/playlist?list=PLqsuMHtPTtp0qTyJ7Zl-ftZslOvPGJLTe",
        "https://www.youtube.com/watch?v=2g-5XqAbY9s&list=PLIId9bc1RIsGsLcYycVsX-4DC1Soswxbj",
        "https://www.youtube.com/watch?v=DMDEFfr98gg&list=PLIId9bc1RIsHXSq8xp3kf4IO_gCc14U3r"
    )

    // for (playlistUrl in playlistUrls) { // Test only the first one
    //     println("\n🎵 Playlist: $playlistUrl")
    //
    //     wrapper.getPlaylistInfo(
    //         playlistUrl,
    //         extractFlat = true,  // Faster, just basic metadata
    //         timeoutSec = 60
    //     ).onSuccess { playlist ->
    //         println("✅ Playlist found:")
    //         println("  📝 Title: ${playlist.title}")
    //         println("  👤 Creator: ${playlist.uploader}")
    //         println("  📊 Video count: ${playlist.entryCount}")
    //         println("  🎬 First videos:")
    //         playlist.entries.take(5).forEachIndexed { index, video ->
    //             println("    ${index + 1}. ${video.title}")
    //             println("       URL: ${video.url}")
    //         }
    //     }.onFailure {
    //         println("❌ Playlist failure: ${it.message}")
    //     }
    // }

    // =========================
    // TEST 3: YouTube Channel
    // =========================
    println("\n📺 TEST 3: YouTube Channels")
    val channelUrls = listOf(
        "https://www.youtube.com/@PhilippLackner",
        // "https://www.youtube.com/channel/UCYZ0IYNeA_aCqlM9KIC_8DQ"
    )

    for (channelUrl in channelUrls) { // Test only the first one
        println("\n📺 Channel: $channelUrl")

        // For a channel, we retrieve a list of videos
        wrapper.getVideoInfoList(
            channelUrl,
            maxEntries = 30,
            extractFlat = true,  // Faster
            timeoutSec = 90
        ).onSuccess { videos ->
            println("✅ Channel videos:")
            println("  📊 ${videos.size} videos retrieved")
            videos.forEachIndexed { index, video ->
                println("  ${index + 1}. ${video.title}")
                println("     📅 Date: ${video.uploadDate}")
                println("     👁️ Views: ${video.viewCount}")
                println("      Thumbnail ${video.thumbnail}")
            }
        }.onFailure {
            println("❌ Channel failure: ${it.message}")
        }
    }

    // =========================
    // TEST 4: Detailed Playlist (can be slow)
    // =========================
    // println("\n🎬 TEST 4: Detailed Playlist (can be slow)")
    // val shortPlaylist = "https://www.youtube.com/playlist?list=PLIId9bc1RIsHsrHIda0dpJB-LlVlEklFQ"
    //
    // wrapper.getPlaylistInfo(
    //     shortPlaylist,
    //     extractFlat = false,  // Retrieves ALL info (slow!)
    //     timeoutSec = 120
    // ).onSuccess { playlist ->
    //     println("✅ Full playlist info:")
    //     println("  📝 Title: ${playlist.title}")
    //     playlist.entries.take(3).forEach { video ->
    //         println("\n  🎥 ${video.title}")
    //         println("     ⏱️ Duration: ${video.duration}")
    //         println("     📺 Resolution: ${video.height}p")
    //         println("     👤 Uploader: ${video.uploader}")
    //         println("     🏷️ Tags: ${video.tags.take(5).joinToString(", ")}")
    //         if (video.chapters.isNotEmpty()) {
    //             println("     📑 Chapters: ${video.chapters.size}")
    //         }
    //     }
    // }.onFailure {
    //     println("❌ Failure: ${it.message}")
    // }
    //
    // =========================
    // TEST 5: Special URLs
    // =========================
    // println("\n🔗 TEST 5: Special URLs")
    //
    // // Video in a playlist
    // val videoInPlaylist = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"
    //
    // // Live stream (may fail if not live)
    // val liveUrl = "https://www.youtube.com/@LofiGirl/live"
    //
    // // Shorts
    // val shortsUrl = "https://www.youtube.com/shorts/n0QNaym0jDI"
    //
    // println("Testing video in playlist...")
    // wrapper.getVideoInfo(videoInPlaylist)
    //     .onSuccess { println("  ✅ ${it.title}") }
    //     .onFailure { println("  ❌ ${it.message}") }
}