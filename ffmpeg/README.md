# FFmpeg Wrapper Module

A Kotlin Multiplatform (JVM) wrapper for FFmpeg, providing an async, event-driven API for media conversion and analysis.

## Features

- **Automatic Binary Management**: Downloads and installs FFmpeg automatically
- **Async Event-Driven API**: All operations use Kotlin Coroutines with event callbacks
- **Media Analysis**: Extract detailed information from media files via FFprobe
- **Video Encoding**: Support for H.264, H.265/HEVC, AV1, VP9
- **Audio Encoding**: Support for AAC, MP3, Opus, FLAC, and more
- **Progress Tracking**: Real-time progress updates during conversion
- **Cancellation Support**: Cancel running conversions at any time

## Quick Start

```kotlin
val ffmpeg = FfmpegWrapper()

// Initialize (downloads FFmpeg if needed)
ffmpeg.initialize { event ->
    when (event) {
        is InitEvent.DownloadingFfmpeg -> println("Downloading FFmpeg...")
        is InitEvent.FfmpegProgress -> println("Progress: ${event.percent}%")
        is InitEvent.Completed -> println("Ready!")
        is InitEvent.Error -> println("Error: ${event.message}")
        else -> {}
    }
}

// Analyze a media file
val info = ffmpeg.analyze(File("video.mkv")).getOrNull()
println("Duration: ${info?.duration}")
println("Video: ${info?.primaryVideo?.resolution}")
println("Audio: ${info?.primaryAudio?.codec}")

// Convert video
val handle = ffmpeg.convert(
    inputFile = File("input.mkv"),
    outputFile = File("output.mp4"),
    options = ConversionOptions.h264(crf = 23)
) { event ->
    when (event) {
        is ConversionEvent.Started -> println("Started")
        is ConversionEvent.Progress -> {
            println("Time: ${event.timeProcessed}, Speed: ${event.speed}x")
        }
        is ConversionEvent.Completed -> println("Done: ${event.outputFile}")
        is ConversionEvent.Error -> println("Error: ${event.message}")
        is ConversionEvent.Cancelled -> println("Cancelled")
        else -> {}
    }
}

// Cancel if needed
handle.cancel()
```

## Conversion Options

### Video Encoding

```kotlin
// H.264 with CRF quality (recommended)
ConversionOptions.h264(crf = 23, preset = EncoderPreset.MEDIUM)

// H.265/HEVC with CRF quality
ConversionOptions.h265(crf = 28, preset = EncoderPreset.SLOW)

// AV1 encoding
ConversionOptions.av1(crf = 35, preset = 6)

// Stream copy (no re-encoding)
ConversionOptions.copy()

// Custom options
ConversionOptions(
    video = VideoOptions(
        encoder = VideoEncoder.H264,
        compressionType = CompressionType.CRF,
        crf = 20,
        preset = EncoderPreset.SLOWER,
        resolution = Resolution.P1080,
        pixelFormat = PixelFormat.BIT_10
    ),
    audio = AudioOptions(
        encoder = AudioEncoder.AAC,
        bitrate = AudioBitrate.K256
    )
)
```

### Audio Extraction

```kotlin
// Extract to MP3
ConversionOptions.audioMp3(bitrate = AudioBitrate.K320)

// Extract to AAC
ConversionOptions.audioAac(bitrate = AudioBitrate.K256)

// Quick methods
ffmpeg.extractAudioMp3(inputFile, outputFile, AudioBitrate.K192) { event -> }
ffmpeg.extractAudioAac(inputFile, outputFile, AudioBitrate.K256) { event -> }
```

### Trimming

```kotlin
ffmpeg.trim(
    inputFile = inputFile,
    outputFile = outputFile,
    startTime = Duration.ofSeconds(30),
    duration = Duration.ofMinutes(5),
    copyStreams = true  // Fast, no re-encoding
) { event -> }
```

## Video Encoders

| Encoder | FFmpeg Name | CRF Range | Default CRF |
|---------|-------------|-----------|-------------|
| H.264   | libx264     | 0-51      | 23          |
| H.265   | libx265     | 0-51      | 28          |
| AV1     | libsvtav1   | 0-63      | 35          |
| VP9     | libvpx-vp9  | 0-63      | 31          |

## Audio Encoders

| Encoder | FFmpeg Name   | Supports CBR | Supports VBR |
|---------|---------------|--------------|--------------|
| AAC     | aac           | Yes          | No           |
| AAC FDK | libfdk_aac    | Yes          | Yes          |
| MP3     | libmp3lame    | Yes          | Yes          |
| Opus    | libopus       | Yes          | No           |
| FLAC    | flac          | N/A (lossless) | N/A       |

## Encoder Presets

### H.264/H.265 Presets
`ultrafast`, `superfast`, `veryfast`, `faster`, `fast`, `medium`, `slow`, `slower`, `veryslow`, `placebo`

### SVT-AV1 Presets
`0` (slowest/best) to `13` (fastest)

## Media Analysis

```kotlin
val result = ffmpeg.analyze(File("video.mkv"))
result.onSuccess { info ->
    println("Format: ${info.format.formatName}")
    println("Duration: ${info.duration}")
    println("File size: ${info.fileSize}")

    info.primaryVideo?.let { video ->
        println("Video: ${video.codec} ${video.resolution} @ ${video.frameRate} fps")
        println("Bit depth: ${video.bitDepth}")
        println("HDR: ${video.isHdr}")
    }

    info.primaryAudio?.let { audio ->
        println("Audio: ${audio.codec} ${audio.channels}ch @ ${audio.sampleRate} Hz")
    }

    info.subtitleStreams.forEach { sub ->
        println("Subtitle: ${sub.language} (${sub.codec})")
    }
}
```

## Dependencies

The module uses:
- `kotlinx-coroutines` for async operations
- `kotlinx-serialization` for JSON parsing (FFprobe output)
- `platformtools` for GitHub release fetching
- `filekit` for cross-platform app data directories

## Platform Support

- Windows (x64, x86, ARM64)
- macOS (Intel, Apple Silicon)
- Linux (x64, ARM64)

FFmpeg binaries are downloaded from:
- Windows/Linux: [yt-dlp/FFmpeg-Builds](https://github.com/yt-dlp/FFmpeg-Builds)
- macOS: [eugeneware/ffmpeg-static](https://github.com/eugeneware/ffmpeg-static)
