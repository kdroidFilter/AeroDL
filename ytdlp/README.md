Of course\! Here is a comprehensive and user-friendly README for the `YtDlpWrapper` library, based on the code you provided.

-----

# YtDlpWrapper 🛡️

A robust, user-friendly Kotlin library for interacting with the powerful `yt-dlp` command-line tool. This wrapper simplifies downloading videos, extracting audio, and fetching metadata by providing a clean, asynchronous, and type-safe Kotlin API.

It automatically manages `yt-dlp` and `FFmpeg` binaries, so you don't have to.

\#\# ✨ Features

* **⚙️ Automatic Dependency Management**: Automatically downloads and updates the correct versions of `yt-dlp` and `FFmpeg` for the user's operating system (Windows, macOS, Linux).
* **🎬 Simple High-Level API**: Easy-to-use functions for common tasks like `downloadMp4At(preset)`, `downloadAudioMp3()`, and `getVideoInfo()`.
* **📡 Asynchronous & Event-Driven**: Built with Kotlin Coroutines, providing a non-blocking API that emits events for `Started`, `Progress`, `Completed`, `Error`, etc.
* **📦 Type-Safe Data Models**: Fetches and parses video/playlist metadata into clean Kotlin data classes (`VideoInfo`, `PlaylistInfo`).
* **🔗 Direct URL Extraction**: Get direct, playable/downloadable URLs for specific video or audio formats.
* **- Cancel Support**: All download operations return a `Handle` that allows for easy cancellation.
* **🔧 Highly Configurable**: Easily customize download directories, output templates, and pass custom arguments to `yt-dlp`.
* **🌐 Network Pre-checks**: Intelligently checks for network connectivity before starting a download to fail fast and provide better error messages.

## 🚀 Getting Started

### 1\. Add the Dependency

Add the library to your `build.gradle.kts` file.

```kotlin
dependencies {
    // Replace with the actual coordinates once published
    implementation("io.github.kdroidfilter:ytdlp-wrapper:1.0.0")
}
```

### 2\. Crucial First Step: Initialization

Before you can do anything else, you **must** initialize the wrapper. This process checks for, downloads, or updates `yt-dlp` and `FFmpeg`. It's an asynchronous operation that provides events to update your UI.

```kotlin
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val wrapper = YtDlpWrapper()

    println("🔧 Initializing yt-dlp/ffmpeg…")
    val isInitialized = wrapper.initialize { event ->
        // Use these events to show progress in your UI
        when (event) {
            is YtDlpWrapper.InitEvent.DownloadingYtDlp -> println("📥 Downloading yt-dlp...")
            is YtDlpWrapper.InitEvent.EnsuringFfmpeg -> println("🎬 Checking for FFmpeg...")
            is YtDlpWrapper.InitEvent.YtDlpProgress -> {
                val percent = event.percent?.let { "%.1f%%".format(it) } ?: "..."
                print("\r  -> yt-dlp download: $percent")
            }
            is YtDlpWrapper.InitEvent.Completed -> {
                println() // New line after progress bar
                if (event.success) println("✅ Initialization successful!")
                else System.err.println("❌ Initialization failed.")
            }
            is YtDlpWrapper.InitEvent.Error -> System.err.println("❌ Error: ${event.message}")
            else -> {} // Ignore other events for this simple example
        }
    }

    if (!isInitialized) {
        println("Stopping because initialization failed.")
        return@runBlocking
    }

    // Now you are ready to use the wrapper!
}
```

## 📚 Usage Examples

All examples assume you have an initialized `YtDlpWrapper` instance called `wrapper`.

### Example 1: Download a Video at a Specific Quality

This is the most common use case. The library makes it simple to fetch metadata, check for available resolutions, and then download the best quality.

```kotlin
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

// --- Inside a coroutine scope ---

val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"

// 1. (Optional but recommended) Get video info first
println("\n🎬 Fetching video info for: $videoUrl")
val videoInfo = wrapper.getVideoInfo(videoUrl).getOrElse {
    println("🛑 Could not retrieve video info: ${it.message}")
    return@runBlocking
}
println("    ✅ Video Found: ${videoInfo.title}")
println("    📈 Available Downloadable Resolutions: ${videoInfo.availableResolutions.keys.sorted()}p")

// 2. Choose a quality and start the download
val desiredPreset = YtDlpWrapper.Preset.P1080
println("\n🎬 Starting download for ${desiredPreset.height}p version...")

// Use a Channel to wait for the final result in a console app
val resultChannel = Channel<Boolean>()

val downloadHandle = wrapper.downloadMp4At(
    url = videoUrl,
    preset = desiredPreset,
    onEvent = { event ->
        when (event) {
            is Event.Started -> println("    -> Download process started.")
            is Event.Progress -> {
                // This prints progress on a single, updating line
                print("\r    -> Progress: ${"%.1f".format(event.percent)}%")
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
            is Event.Cancelled -> println("\n C Cancelled.")
            else -> {} // Ignore other events
        }
    }
)

// Later, if you need to cancel:
// downloadHandle.cancel()

// 3. Wait for the download to complete
val success = resultChannel.receive()
if (success) {
    println("\n👍 The file was downloaded successfully to ${wrapper.downloadDir?.absolutePath}.")
} else {
    println("\n👎 An error occurred during the download.")
}
```

### Example 2: Download Audio-Only as MP3

Extracting audio is just as simple. You can specify quality presets.

```kotlin
val audioUrl = "..." // URL of a video or song

wrapper.downloadAudioMp3WithPreset(
    url = audioUrl,
    preset = YtDlpWrapper.AudioQualityPreset.HIGH, // Or .LOW, .MEDIUM, .MAXIMUM
    onEvent = { event -> /* ... handle events as above ... */ }
)
```

### Example 3: Get Video Metadata

Quickly fetch all available information about a video without downloading it.

```kotlin
wrapper.getVideoInfo(videoUrl)
    .onSuccess { video ->
        println("✅ Video found:")
        println("  📝 Title: ${video.title}")
        println("  👤 Uploader: ${video.uploader}")
        println("  ⏱️ Duration: ${video.duration}")
        println("  🔗 Best Progressive URL: ${video.directUrl ?: "N/A"}")
        video.availableResolutions.toSortedMap(compareByDescending { it }).forEach { (height, res) ->
            println("     - ${height}p (Progressive: ${res.progressive}, Downloadable: ${res.downloadable})")
        }
    }
    .onFailure { error ->
        println("❌ Failure: ${error.message}")
    }
```

### Example 4: List Videos in a Playlist or Channel

You can efficiently "scrape" the list of videos from a channel or playlist page.

```kotlin
val channelUrl = "https://www.youtube.com/@PhilippLackner"

wrapper.getVideoInfoList(channelUrl, maxEntries = 5)
    .onSuccess { videos ->
        println("✅ Found ${videos.size} videos:")
        videos.forEachIndexed { index, video ->
            println("  ${index + 1}. ${video.title} (${video.uploadDate})")
        }
    }
    .onFailure { error ->
        println("❌ Channel failure: ${error.message}")
    }
```

### Example 5: Get a Direct Stream URL

If you just need a URL to feed into a media player, this is the most efficient way.

```kotlin
// Get a progressive (video+audio) stream URL for 720p
val progressiveUrlResult = wrapper.getProgressiveUrl(
    url = videoUrl,
    preset = YtDlpWrapper.Preset.P720
)
progressiveUrlResult.onSuccess { url ->
    println("▶️ Playable 720p URL: $url")
}

// Get an audio-only stream URL, preferring opus codec
val audioUrlResult = wrapper.getAudioStreamUrl(
    url = videoUrl,
    preferredCodecs = listOf("opus", "m4a")
)
audioUrlResult.onSuccess { url ->
    println("🎵 Playable audio URL: $url")
}
```

## ⚙️ Configuration

You can configure the wrapper instance before calling any methods.

```kotlin
val wrapper = YtDlpWrapper().apply {
    // Set a custom directory for all downloads
    downloadDir = File("/path/to/my/videos")

    // Disable SSL certificate checks globally (useful for some networks)
    // This can also be set per-call in most functions.
    noCheckCertificate = true

    // Manually specify paths if you don't want automatic downloads
    ytDlpPath = "/usr/local/bin/yt-dlp"
    ffmpegPath = "/usr/local/bin/ffmpeg"
}
```

## ⚠️ Error Handling

The library uses two main mechanisms for errors:

1.  **`Result<T>`**: For one-shot operations like `getVideoInfo`, the result is wrapped in a `Result` class. Use `.onSuccess` and `.onFailure` to handle the outcome.
2.  **`Event.Error`**: For long-running processes like downloads, errors are emitted through the `onEvent` callback. This provides detailed error messages and often a root `cause`.

-----