package io.github.kdroidfilter.ytdlp.demos

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    println("🚀 Starting download demo...")

    // Use runBlocking to execute async code in a blocking context (ideal for a console app)
    runBlocking {
        // 1. Create a wrapper instance
        val ytDlpWrapper = YtDlpWrapper()
        // Set the parameter globally for all operations of this wrapper instance
        ytDlpWrapper.noCheckCertificate = true


        // (Optional) Define a custom download directory
        val downloadsDir = File(System.getProperty("user.home"), "YtDlpWrapper_Downloads")
        downloadsDir.mkdirs()
        ytDlpWrapper.downloadDir = downloadsDir
        println("📂 Files will be saved in: ${downloadsDir.absolutePath}")

        // 2. Initialize the wrapper. This is crucial and must be done first.
        println("🔄 Initializing yt-dlp and FFmpeg (may take a moment on the first run)...")
        val initSuccess = ytDlpWrapper.initialize { event ->
            // Display initialization events to inform the user
            when (event) {
                is YtDlpWrapper.InitEvent.DownloadingYtDlp -> println("    -> Downloading yt-dlp...")
                is YtDlpWrapper.InitEvent.EnsuringFfmpeg -> println("    -> Checking FFmpeg...")
                is YtDlpWrapper.InitEvent.Completed -> if (event.success) println("✅ Initialization completed successfully!")
                is YtDlpWrapper.InitEvent.Error -> System.err.println("❌ Initialization error: ${event.message}")
                else -> {} // Ignore other events like progress to keep the output concise
            }
        }

        if (!initSuccess) {
            println("🛑 Initialization failed. The program will now exit.")
            return@runBlocking
        }

        // 3. Get Video Info first to check available resolutions
        val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"
        println("\n🎬 Fetching video info for: $videoUrl")

        val videoInfo = ytDlpWrapper.getVideoInfo(videoUrl).getOrElse {
            println("🛑 Could not retrieve video info: ${it.message}")
            return@runBlocking
        }
        println("    ✅ Video Found: ${videoInfo.title}")
        println("    📈 Available Resolutions (Progressive / Downloadable):")
        videoInfo.availableResolutions.toSortedMap().forEach { (height, res) ->
            println("        - ${height}p (Progressive: ${res.progressive}, Downloadable: ${res.downloadable})")
        }


        // 4. Check if the desired quality is available and start the download
        val desiredPreset = YtDlpWrapper.Preset.P1080
        val isPresetAvailable = videoInfo.availableResolutions[desiredPreset.height]?.downloadable ?: false

        if (!isPresetAvailable) {
            println("\n⚠️ Desired quality ${desiredPreset.height}p is not available. Aborting download.")
            return@runBlocking
        }
        println("\n🎬 Starting download for ${desiredPreset.height}p version...")

        // Use a Channel to receive the final result of the download
        val resultChannel = Channel<Boolean>()

        val downloadHandle = ytDlpWrapper.downloadMp4At(
            url = videoUrl,
            preset = desiredPreset,
            onEvent = { event ->
                when (event) {
                    is Event.Started -> println("    -> Download process started.")
                    is Event.Progress -> {
                        // Display progress on a single line for a clean console
                        val progressPercent = event.percent ?: 0.0
                        print("\r    -> Progress: ${"%.1f".format(progressPercent)}%")
                    }
                    is Event.Completed -> {
                        println("\n    -> Download finished.")
                        if (event.success) {
                            println("🎉 Success!")
                            launch { resultChannel.send(true) }
                        } else {
                            System.err.println("    -> The download finished but failed (exit code: ${event.exitCode}).")
                            launch { resultChannel.send(false) }
                        }
                    }
                    is Event.Error -> {
                        System.err.println("\n❌ Download error: ${event.message}")
                        launch { resultChannel.send(false) }
                    }
                    is Event.Cancelled -> {
                        println("\n C Cancelled.")
                        launch { resultChannel.send(false) }
                    }
                    is Event.NetworkProblem -> {
                        System.err.println("\n🌐 Network problem: ${event.detail}")
                        launch { resultChannel.send(false) }
                    }
                    else -> {} // Ignore log events for this demo
                }
            }
        )

        // 5. Wait for the result from the channel
        val success = resultChannel.receive()

        if (success) {
            println("\n👍 The file was downloaded successfully.")
        } else {
            println("\n👎 An error occurred during the download.")
        }
    }
}