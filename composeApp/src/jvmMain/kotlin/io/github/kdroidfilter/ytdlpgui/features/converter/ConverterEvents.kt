package io.github.kdroidfilter.ytdlpgui.features.converter

import java.io.File

/**
 * Events for the Converter screen.
 */
sealed class ConverterEvents {
    /**
     * User clicked to open the file picker.
     */
    data object OpenFilePicker : ConverterEvents()

    /**
     * User selected a file via file picker.
     */
    data class FileSelected(val file: File) : ConverterEvents()

    /**
     * User dropped file(s) onto the drop zone.
     */
    data class FilesDropped(val files: List<File>) : ConverterEvents()

    /**
     * User started dragging over the drop zone.
     */
    data object DragEntered : ConverterEvents()

    /**
     * User stopped dragging (left the drop zone or dropped).
     */
    data object DragExited : ConverterEvents()

    /**
     * User changed the output format (Video/Audio).
     */
    data class SetOutputFormat(val format: OutputFormat) : ConverterEvents()

    /**
     * User changed the video quality.
     */
    data class SetVideoQuality(val quality: VideoQuality) : ConverterEvents()

    /**
     * User changed the audio quality.
     */
    data class SetAudioQuality(val quality: AudioQuality) : ConverterEvents()

    /**
     * User started the conversion.
     */
    data object StartConversion : ConverterEvents()

    /**
     * User cancelled the conversion.
     */
    data object CancelConversion : ConverterEvents()

    /**
     * User wants to open the output folder.
     */
    data object OpenOutputFolder : ConverterEvents()

    /**
     * User wants to convert another file (reset).
     */
    data object Reset : ConverterEvents()

    /**
     * Clear error message.
     */
    data object ClearError : ConverterEvents()
}
