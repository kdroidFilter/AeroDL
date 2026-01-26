package io.github.kdroidfilter.ffmpeg

import io.github.kdroidfilter.ffmpeg.core.*
import io.github.kdroidfilter.ffmpeg.model.*
import io.github.kdroidfilter.ffmpeg.util.CommandBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandBuilderTest {

    private val ffmpegPath = "/usr/bin/ffmpeg"
    private val inputFile = File("/input/video.mkv")
    private val outputFile = File("/output/video.mp4")

    @Test
    fun `build basic h264 command`() {
        val options = ConversionOptions.h264(crf = 23, preset = EncoderPreset.MEDIUM)
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-c:v"))
        assertTrue(cmd.contains("libx264"))
        assertTrue(cmd.contains("-crf"))
        assertTrue(cmd.contains("23"))
        assertTrue(cmd.contains("-preset"))
        assertTrue(cmd.contains("medium"))
        assertTrue(cmd.contains("-y")) // overwrite
    }

    @Test
    fun `build h265 command`() {
        val options = ConversionOptions.h265(crf = 28, preset = EncoderPreset.SLOW)
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("libx265"))
        assertTrue(cmd.contains("28"))
        assertTrue(cmd.contains("slow"))
    }

    @Test
    fun `build audio only command`() {
        val options = ConversionOptions.audioMp3(bitrate = AudioBitrate.K320)
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-vn")) // no video
        assertTrue(cmd.contains("-c:a"))
        assertTrue(cmd.contains("libmp3lame"))
        assertTrue(cmd.contains("-b:a"))
        assertTrue(cmd.contains("320k"))
    }

    @Test
    fun `build copy command`() {
        val options = ConversionOptions.copy()
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        val videoEncoderIndex = cmd.indexOf("-c:v")
        assertTrue(videoEncoderIndex >= 0)
        assertEquals("copy", cmd[videoEncoderIndex + 1])

        val audioEncoderIndex = cmd.indexOf("-c:a")
        assertTrue(audioEncoderIndex >= 0)
        assertEquals("copy", cmd[audioEncoderIndex + 1])
    }

    @Test
    fun `build command with trim options`() {
        val options = ConversionOptions(
            startTime = java.time.Duration.ofSeconds(30),
            duration = java.time.Duration.ofMinutes(5)
        )
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-ss"))
        assertTrue(cmd.contains("00:00:30.000"))
        assertTrue(cmd.contains("-t"))
        assertTrue(cmd.contains("00:05:00.000"))
    }

    @Test
    fun `build command with resolution scaling`() {
        val options = ConversionOptions(
            video = VideoOptions(
                encoder = VideoEncoder.H264,
                resolution = Resolution.P1080
            )
        )
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-vf"))
        assertTrue(cmd.any { it.contains("scale") && it.contains("1080") })
    }

    @Test
    fun `build command with audio options`() {
        val options = ConversionOptions(
            video = null,
            audio = AudioOptions(
                encoder = AudioEncoder.AAC,
                bitrate = AudioBitrate.K256,
                sampleRate = SampleRate.HZ_48000,
                channels = Channels.STEREO
            )
        )
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-c:a"))
        assertTrue(cmd.contains("aac"))
        assertTrue(cmd.contains("-b:a"))
        assertTrue(cmd.contains("256k"))
        assertTrue(cmd.contains("-ar"))
        assertTrue(cmd.contains("48000"))
        assertTrue(cmd.contains("-ac"))
        assertTrue(cmd.contains("2"))
    }

    @Test
    fun `build command with stream selection`() {
        val options = ConversionOptions(
            streamSelection = StreamSelection(
                videoStream = 0,
                audioStream = 1,
                subtitleStream = -1
            )
        )
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-map"))
        assertTrue(cmd.contains("0:v:0"))
        assertTrue(cmd.contains("0:a:1"))
        assertTrue(cmd.contains("-sn")) // subtitle excluded
    }

    @Test
    fun `build command with metadata`() {
        val options = ConversionOptions(
            metadata = mapOf(
                "title" to "My Video",
                "artist" to "Test Artist"
            )
        )
        val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)

        assertTrue(cmd.contains("-metadata"))
        assertTrue(cmd.contains("title=My Video"))
        assertTrue(cmd.contains("artist=Test Artist"))
    }

    @Test
    fun `build ffprobe command`() {
        val cmd = CommandBuilder.buildProbeCommand("/usr/bin/ffprobe", inputFile)

        assertEquals("/usr/bin/ffprobe", cmd[0])
        assertTrue(cmd.contains("-print_format"))
        assertTrue(cmd.contains("json"))
        assertTrue(cmd.contains("-show_format"))
        assertTrue(cmd.contains("-show_streams"))
        assertTrue(cmd.contains(inputFile.absolutePath))
    }
}
