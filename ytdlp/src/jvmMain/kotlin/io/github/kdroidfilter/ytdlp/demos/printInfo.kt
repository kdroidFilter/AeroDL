package io.github.kdroidfilter.ytdlp.demos

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
            is InitEvent.UpdatingYtDlp -> println("🔄 An update is available. Updating yt-dlp...")
            is InitEvent.DownloadingYtDlp -> println("📥 yt-dlp not found. Downloading...")
            is InitEvent.EnsuringFfmpeg -> println("🎬 Checking for FFmpeg...")

            // Handle progress with a single, updating line
            is InitEvent.YtDlpProgress -> {
                val percent = ev.percent?.let { "%.1f%%".format(it) } ?: "..."
                print("\r  -> Downloading yt-dlp: $percent")
            }
            is InitEvent.FfmpegProgress -> {
                val percent = ev.percent?.let { "%.1f%%".format(it) } ?: "..."
                print("\r  -> Downloading FFmpeg: $percent")
            }

            // Handle final states
            is InitEvent.Completed -> {
                // Print a newline to move past the progress bar line
                println()
                if (ev.success) {
                    println("✅ Initialization successful!")
                } else {
                    println("❌ Initialization failed.")
                }
            }
            is InitEvent.Error -> {
                println() // Newline after any progress bar
                // Print errors to the standard error stream
                System.err.println("❌ Error during initialization: ${ev.message}")
                ev.cause?.printStackTrace()
            }
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
    val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"

    wrapper.getVideoInfo(videoUrl, timeoutSec = 60)
        .onSuccess { video ->
            println("✅ Video found:")
            println("  📝 Title: ${video.title}")
            println("  👤 Uploader: ${video.uploader}")
            println("  ⏱️ Duration: ${video.duration}")
            println("  👁️ Views: ${video.viewCount}")
            println("  🔗 Direct link: ${video.directUrl ?: "N/A"}")
            println("  📈 Available Resolutions:")
            video.availableResolutions.toSortedMap(compareByDescending { it }).forEach { (height, res) ->
                println("     - ${height}p (Progressive: ${if (res.progressive) "Yes" else "No"})")
            }
        }
        .onFailure {
            println("❌ Failure: ${it.message}")
            it.cause?.printStackTrace()
        }

//    // =========================
//    // TEST 2: YouTube Playlist
//    // =========================
//    println("\n📚 TEST 2: Playlist")
//    val playlistUrl = "https://www.youtube.com/playlist?list=PLqsuMHtPTtp0qTyJ7Zl-ftZslOvPGJLTe"
//    println("\n🎵 Playlist: $playlistUrl")
//
//    wrapper.getPlaylistInfo(
//        playlistUrl,
//        extractFlat = true,  // Faster, just basic metadata
//        timeoutSec = 60
//    ).onSuccess { playlist ->
//        println("✅ Playlist found:")
//        println("  📝 Title: ${playlist.title}")
//        println("  👤 Creator: ${playlist.uploader}")
//        println("  📊 Video count: ${playlist.entryCount}")
//        println("  🎬 First videos:")
//        playlist.entries.take(5).forEachIndexed { index, video ->
//            println("    ${index + 1}. ${video.title}")
//            println("       URL: ${video.url}")
//        }
//    }.onFailure {
//        println("❌ Playlist failure: ${it.message}")
//    }


    // =========================
    // TEST 3: YouTube Channel
    // =========================
    println("\n📺 TEST 3: YouTube Channels")
    val channelUrl = "https://www.youtube.com/@PhilippLackner"

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
            println("     🖼️ Thumbnail: ${video.thumbnail}")
        }
    }.onFailure {
        println("❌ Channel failure: ${it.message}")
    }
}