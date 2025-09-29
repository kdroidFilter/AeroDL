package io.github.kdroidfilter.ytdlp.demos

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.YtDlpWrapper.InitEvent

import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val wrapper = YtDlpWrapper().apply {
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    }
    wrapper.noCheckCertificate = true


    println("🔧 Initializing yt-dlp/ffmpeg…")
    val initOk = wrapper.initialize { ev ->
        when (ev) {
            is InitEvent.CheckingYtDlp -> println("🔍 Checking yt-dlp…")
            is InitEvent.UpdatingYtDlp -> println("🔄 An update is available. Updating yt-dlp...")
            is InitEvent.DownloadingYtDlp -> println("📥 yt-dlp not found. Downloading...")
            is InitEvent.EnsuringFfmpeg -> println("🎬 Checking for FFmpeg...")

            is InitEvent.YtDlpProgress -> {
                val percent = ev.percent?.let { "%.1f%%".format(it) } ?: "..."
                print("\r  -> Downloading yt-dlp: $percent")
            }
            is InitEvent.FfmpegProgress -> {
                val percent = ev.percent?.let { "%.1f%%".format(it) } ?: "..."
                print("\r  -> Downloading FFmpeg: $percent")
            }

            is InitEvent.Completed -> {
                println()
                if (ev.success) {
                    println("✅ Initialization successful!")
                } else {
                    System.err.println("❌ Initialization failed.")
                }
            }
            is InitEvent.Error -> {
                println()
                System.err.println("❌ Error during initialization: ${ev.message}")
                ev.cause?.printStackTrace()
            }
        }
    }

    if (!initOk) {
        println("Stopping because initialization failed.")
        return@runBlocking
    }

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

    println("\n📺 TEST 3: YouTube Channels")
    val channelUrl = "https://www.youtube.com/@BordeauxKt"

    println("\n📺 Channel: $channelUrl")

    wrapper.getVideoInfoList(
        channelUrl,
        maxEntries = 30,
        extractFlat = true,
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