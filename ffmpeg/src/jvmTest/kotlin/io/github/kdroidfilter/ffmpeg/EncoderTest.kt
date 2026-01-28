package io.github.kdroidfilter.ffmpeg

import io.github.kdroidfilter.ffmpeg.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EncoderTest {

    @Test
    fun `video encoder ffmpeg names are correct`() {
        assertEquals("libx264", VideoEncoder.H264.ffmpegName)
        assertEquals("libx265", VideoEncoder.H265.ffmpegName)
        assertEquals("libsvtav1", VideoEncoder.AV1.ffmpegName)
        assertEquals("libvpx-vp9", VideoEncoder.VP9.ffmpegName)
    }

    @Test
    fun `video encoder CRF defaults are sensible`() {
        assertEquals(23, VideoEncoder.H264.defaultCrf)
        assertEquals(28, VideoEncoder.H265.defaultCrf)
        assertEquals(35, VideoEncoder.AV1.defaultCrf)
    }

    @Test
    fun `video encoder profiles for pixel format`() {
        assertEquals("high", VideoEncoder.H264.getProfile(PixelFormat.BIT_8))
        assertEquals("high10", VideoEncoder.H264.getProfile(PixelFormat.BIT_10))
        assertEquals("main", VideoEncoder.H265.getProfile(PixelFormat.BIT_8))
        assertEquals("main10", VideoEncoder.H265.getProfile(PixelFormat.BIT_10))
        assertEquals("0", VideoEncoder.AV1.getProfile(null))
        assertNull(VideoEncoder.VP9.getProfile(null))
    }

    @Test
    fun `audio encoder ffmpeg names are correct`() {
        assertEquals("aac", AudioEncoder.AAC.ffmpegName)
        assertEquals("libfdk_aac", AudioEncoder.AAC_FDK.ffmpegName)
        assertEquals("libmp3lame", AudioEncoder.MP3.ffmpegName)
        assertEquals("libopus", AudioEncoder.OPUS.ffmpegName)
        assertEquals("flac", AudioEncoder.FLAC.ffmpegName)
    }

    @Test
    fun `encoder preset values`() {
        assertEquals("ultrafast", EncoderPreset.ULTRAFAST.ffmpegValue)
        assertEquals("medium", EncoderPreset.MEDIUM.ffmpegValue)
        assertEquals("veryslow", EncoderPreset.VERYSLOW.ffmpegValue)
        assertEquals("6", EncoderPreset.PRESET_6.ffmpegValue)
    }

    @Test
    fun `svt-av1 preset from int`() {
        assertEquals(EncoderPreset.PRESET_0, EncoderPreset.fromSvtAv1Preset(0))
        assertEquals(EncoderPreset.PRESET_6, EncoderPreset.fromSvtAv1Preset(6))
        assertEquals(EncoderPreset.PRESET_13, EncoderPreset.fromSvtAv1Preset(13))
        assertEquals(EncoderPreset.PRESET_13, EncoderPreset.fromSvtAv1Preset(100)) // capped
        assertEquals(EncoderPreset.PRESET_0, EncoderPreset.fromSvtAv1Preset(-5)) // capped
    }

    @Test
    fun `video bitrate values`() {
        assertEquals(5_000_000L, VideoBitrate.M5.bitsPerSecond)
        assertEquals("5M", VideoBitrate.M5.ffmpegValue)
        assertEquals(20_000_000L, VideoBitrate.M20.bitsPerSecond)
        assertEquals("20M", VideoBitrate.M20.ffmpegValue)
    }

    @Test
    fun `audio bitrate values`() {
        assertEquals(192_000L, AudioBitrate.K192.bitsPerSecond)
        assertEquals("192k", AudioBitrate.K192.ffmpegValue)
        assertEquals(320_000L, AudioBitrate.K320.bitsPerSecond)
        assertEquals("320k", AudioBitrate.K320.ffmpegValue)
    }

    @Test
    fun `resolution values`() {
        assertEquals(1920, Resolution.P1080.width)
        assertEquals(1080, Resolution.P1080.height)
        assertEquals("1080p (Full HD)", Resolution.P1080.displayName)
        assertEquals("scale=-2:1080", Resolution.P1080.toScaleFilter())
    }

    @Test
    fun `pixel format values`() {
        assertEquals("yuv420p", PixelFormat.BIT_8.ffmpegValue)
        assertEquals(8, PixelFormat.BIT_8.bitDepth)
        assertEquals("yuv420p10le", PixelFormat.BIT_10.ffmpegValue)
        assertEquals(10, PixelFormat.BIT_10.bitDepth)
    }

    @Test
    fun `pixel format from bit depth`() {
        assertEquals(PixelFormat.BIT_8, PixelFormat.fromBitDepth(8))
        assertEquals(PixelFormat.BIT_10, PixelFormat.fromBitDepth(10))
        assertEquals(PixelFormat.BIT_10, PixelFormat.fromBitDepth(12))
    }

    @Test
    fun `sample rate values`() {
        assertEquals(44100, SampleRate.HZ_44100.hz)
        assertEquals("44100", SampleRate.HZ_44100.ffmpegValue)
        assertEquals(48000, SampleRate.HZ_48000.hz)
    }

    @Test
    fun `channels values`() {
        assertEquals(2, Channels.STEREO.count)
        assertEquals("2", Channels.STEREO.ffmpegValue)
        assertEquals(6, Channels.SURROUND_5_1.count)
    }

    @Test
    fun `container format from extension`() {
        assertEquals(ContainerFormat.MP4, ContainerFormat.fromExtension("mp4"))
        assertEquals(ContainerFormat.MP4, ContainerFormat.fromExtension(".mp4"))
        assertEquals(ContainerFormat.MKV, ContainerFormat.fromExtension("mkv"))
        assertEquals(ContainerFormat.MP3, ContainerFormat.fromExtension("mp3"))
        assertNull(ContainerFormat.fromExtension("xyz"))
    }
}
