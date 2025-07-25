package com.sceyt.chatuikit.shared.media_encoder

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.math.roundToInt

object CompressorUtils {

    private const val MIN_HEIGHT = 640.0
    private const val MIN_WIDTH = 368.0

    // 10 seconds between I-frames
    private const val I_FRAME_INTERVAL = 10

    fun validateInputs(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
    ): String? {
        if (srcPath != null && srcUri != null) {
            Log.w("Compressor", "ARE YOU SURE YOU WANT TO PASS BOTH srcPath AND srcUri?")
        }

        if (context == null && srcPath == null && srcUri == null) {
            return "You need to provide either a srcUri or a srcPath"
        }

        if (context == null && srcPath == null) {
            return "You need to provide the application context"
        }

        return null
    }

    fun prepareVideoWidth(
            mediaMetadataRetriever: MediaMetadataRetriever,
    ): Double? {
        val widthData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        return if (widthData.isNullOrEmpty()) {
            null
        } else {
            widthData.toDouble()
        }
    }

    fun prepareVideoHeight(
            mediaMetadataRetriever: MediaMetadataRetriever,
    ): Double? {
        val heightData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        return if (heightData.isNullOrEmpty()) {
            null
        } else {
            heightData.toDouble()
        }
    }

    /**
     * Set output parameters like bitrate and frame rate
     */
    fun setOutputFileParameters(
            inputFormat: MediaFormat,
            outputFormat: MediaFormat,
            newBitrate: Int,
            frameRate: Int?,
    ) {
        var newFrameRate = getFrameRate(inputFormat, frameRate)
        val iFrameInterval = getIFrameIntervalRate(inputFormat)
        outputFormat.apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            if (newFrameRate > 60) {
                // Some encoders behave unexpectedly when frame rate is above 60
                newFrameRate = 60
            }

            setInteger(MediaFormat.KEY_FRAME_RATE, newFrameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            setInteger(MediaFormat.KEY_BIT_RATE, newBitrate)

            if (Build.VERSION.SDK_INT > 23) {

                getColorStandard(inputFormat)?.let {
                    setInteger(MediaFormat.KEY_COLOR_STANDARD, it)
                }

                getColorTransfer(inputFormat)?.let {
                    setInteger(MediaFormat.KEY_COLOR_TRANSFER, it)
                }

                getColorRange(inputFormat)?.let {
                    setInteger(MediaFormat.KEY_COLOR_RANGE, it)
                }
            }

            Log.i(
                "Output file parameters",
                "videoFormat: $this"
            )
        }
    }

    private fun getFrameRate(format: MediaFormat, frameRate: Int?): Int {
        if (frameRate != null) return frameRate
        return if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) format.getInteger(MediaFormat.KEY_FRAME_RATE)
        else 30
    }

    private fun getIFrameIntervalRate(format: MediaFormat): Int {
        return if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) format.getInteger(
            MediaFormat.KEY_I_FRAME_INTERVAL
        )
        else I_FRAME_INTERVAL
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getColorStandard(format: MediaFormat): Int? {
        return if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) format.getInteger(
            MediaFormat.KEY_COLOR_STANDARD
        )
        else null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getColorTransfer(format: MediaFormat): Int? {
        return if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) format.getInteger(
            MediaFormat.KEY_COLOR_TRANSFER
        )
        else null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getColorRange(format: MediaFormat): Int? {
        return if (format.containsKey(MediaFormat.KEY_COLOR_RANGE)) format.getInteger(
            MediaFormat.KEY_COLOR_RANGE
        )
        else null
    }

    /**
     * Counts the number of tracks (video, audio) found in the file source provided
     * @param extractor what is used to extract the encoded data
     * @param isVideo to determine whether we are processing video or audio at time of call
     * @return index of the requested track
     */
    fun findTrack(
            extractor: MediaExtractor,
            isVideo: Boolean,
    ): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (isVideo) {
                if (mime?.startsWith("video/")!!) return i
            } else {
                if (mime?.startsWith("audio/")!!) return i
            }
        }
        return -5
    }

    fun printException(exception: Exception) {
        var message = "An error has occurred!"
        exception.localizedMessage?.let {
            message = it
        }
        Log.e("Compressor", message, exception)
    }

    /**
     * Get fixed bitrate value based on the file's current bitrate
     * @param bitrate file's current bitrate
     * @return new smaller bitrate value
     */
    fun getBitrate(
            bitrate: Long,
            quality: VideoQuality,
    ): Int {
        return when (quality) {
            VideoQuality.VERY_LOW -> (bitrate * 0.1).roundToInt()
            VideoQuality.LOW -> (bitrate * 0.2).roundToInt()
            VideoQuality.MEDIUM -> (bitrate * 0.3).roundToInt()
            VideoQuality.HIGH -> (bitrate * 0.4).roundToInt()
            VideoQuality.VERY_HIGH -> (bitrate * 0.6).roundToInt()
        }
    }

    /**
     * Generate new width and height for source file
     * @param width file's original width
     * @param height file's original height
     * @return new width and height pair
     */
    fun generateWidthAndHeight(
            width: Double,
            height: Double
    ): Pair<Int, Int> {

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newHeight = 480
            newWidth = (480 * width / height / 16).roundToInt() * 16
        } else {
            newWidth = 480
            newHeight = (480 * height / width / 16).roundToInt() * 16
        }

        /* when {
             width >= 1920 || height >= 1920 -> {
                 newWidth = generateWidthHeightValue(width, 0.3)
                 newHeight = generateWidthHeightValue(height, 0.3)
             }
             width >= 1280 || height >= 1280 -> {
                 newWidth = generateWidthHeightValue(width, 0.75)
                 newHeight = generateWidthHeightValue(height, 0.75)
             }
             width >= 960 || height >= 960 -> {
                 newWidth = generateWidthHeightValue(width, 0.95)
                 newHeight = generateWidthHeightValue(height, 0.95)
             }
             else -> {
                 newWidth = generateWidthHeightValue(width, 0.9)
                 newHeight = generateWidthHeightValue(height, 0.9)
             }
         }*/

        Log.i("CompressorUtil", "New width: $newWidth, new height: $newHeight")
        return Pair(newWidth, newHeight)
    }

    fun hasQTI(): Boolean {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        for (codec in list) {
            Log.i("CODECS: ", codec.name)
            if (codec.name.contains("qti.avc")) {
                return true
            }
        }
        return false
    }

    fun hasOMX(): Boolean {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        for (codec in list) {
            Log.i("CODECS: ", codec.name)
            if (codec.name.contains("OMX.qcom.video.encoder.avc")) {
                return true
            }
        }
        return false
    }
}