package io.github.kdroidfilter.ytdlp.demos

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CompletableFuture

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

        // 3. Start the download
        // URL of a copyright-free test video (Big Buck Bunny)
        val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"
        println("\n🎬 Starting download for: $videoUrl")

        // noCheckCertificate is no longer needed here, as it's set globally
        val table = ytDlpWrapper.probeAvailability(videoUrl)
        println(table)

        // A CompletableFuture is used to wait for the asynchronous download to finish
        val downloadFuture = CompletableFuture<Boolean>()

        ytDlpWrapper.downloadMp4At(
            url = videoUrl,
            preset = YtDlpWrapper.Preset.P1080, // Specify 1080p quality
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
                        if(event.success) {
                            println("🎉 Success!")
                            downloadFuture.complete(true)
                        } else {
                            System.err.println("    -> The download finished but failed (exit code: ${event.exitCode}).")
                            downloadFuture.complete(false)
                        }
                    }
                    is Event.Error -> {
                        System.err.println("\n❌ Download error: ${event.message}")
                        downloadFuture.complete(false)
                    }
                    is Event.Cancelled -> {
                        println("\n C Cancelled.")
                        downloadFuture.complete(false)
                    }
                    is Event.NetworkProblem -> {
                        System.err.println("\n🌐 Network problem: ${event.detail}")
                        downloadFuture.complete(false)
                    }
                    else -> {} // Ignore log events for this demo
                }
            }
        )

        // 4. Wait for the result
        val success = downloadFuture.get() // Blocks the main thread until the future is completed

        if (success) {
            println("\n👍 The file was downloaded successfully.")
        } else {
            println("\n👎 An error occurred during the download.")
        }
    }
}