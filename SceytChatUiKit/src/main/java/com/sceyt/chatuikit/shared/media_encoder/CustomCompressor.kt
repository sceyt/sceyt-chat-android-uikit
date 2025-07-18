package com.sceyt.chatuikit.shared.media_encoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.findTrack
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.generateWidthAndHeight
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.getBitrate
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.prepareVideoHeight
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.prepareVideoWidth
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.printException
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.setOutputFileParameters
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils.validateInputs
import com.sceyt.chatuikit.shared.media_encoder.transcoder.CallbackBasedTranscoder
import com.sceyt.chatuikit.shared.media_encoder.transcoder.InputSurface
import com.sceyt.chatuikit.shared.media_encoder.transcoder.OutputSurface
import com.sceyt.chatuikit.shared.media_encoder.transcoder.StreamableVideo
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object CustomCompressor {

    // 1.5Mbps
    private const val MIN_BITRATE = 1500000

    // H.264 Advanced Video Coding
    private const val MIME_TYPE = "video/avc"

    // MediaExtractor extracts encoded media data from the source
    private lateinit var extractor: MediaExtractor
    private lateinit var compressionProgressListener: CompressionProgressListener
    private var duration: Long = 0
    private var rotation: Int = 0

    private const val INVALID_BITRATE =
            "The provided bitrate is smaller than what is needed for compression " +
                    "try to set isMinBitRateEnabled to false"

    var isRunning = true
    var isCancelled = false

    private var callbackBasedTranscoder: CallbackBasedTranscoder? = null


    fun cancel() {
        callbackBasedTranscoder?.cancel()
        isCancelled = true
    }

    fun compressVideo(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
            destination: String,
            streamableFile: String?,
            configuration: TranscoderConfiguration,
            listener: CompressionProgressListener,
            startCompressingListener: () -> Unit,
    ): Result<Boolean> {

        extractor = MediaExtractor()
        compressionProgressListener = listener
        // Retrieve the source's metadata to be used as input to generate new values for compression
        val mediaMetadataRetriever = MediaMetadataRetriever()

        validateInputs(context, srcUri, srcPath)?.let {
            return Result.failure(Exception(it))
        }

        if (context != null && srcUri != null && srcPath == null) {

            try {
                mediaMetadataRetriever.setDataSource(context, srcUri)
            } catch (exception: Exception) {
                printException(exception)
                return Result.failure(exception)
            }

            extractor.setDataSource(context, srcUri, null)
        } else {
            try {
                mediaMetadataRetriever.setDataSource(srcPath)
            } catch (exception: Exception) {
                printException(exception)
                return Result.failure(exception)
            }

            val file = File(srcPath!!)
            if (!file.canRead())
                return Result.failure(Exception("The source file cannot be accessed!"))
            try {
                extractor.setDataSource(file.toString())
            } catch (ex: Exception) {
                printException(ex)
                return Result.failure(ex)
            }
        }

        val height = prepareVideoHeight(mediaMetadataRetriever)
                ?: return Result.failure(Exception("Failed to get video height"))

        val width: Double = prepareVideoWidth(mediaMetadataRetriever)
                ?: return Result.failure(Exception("Failed to get video width"))

        val rotationData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

        val bitrateData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

        val durationData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)


        if (rotationData.isNullOrEmpty() || bitrateData.isNullOrEmpty() || durationData.isNullOrEmpty()) {
            // Exit execution
            return Result.failure(Exception("Failed to extract video meta-data, please try again"))
        }

        rotation = rotationData.toInt()
        val bitrate = bitrateData.toLong()
        duration = durationData.toLong() * 1000

        // Check for a min video bitrate before compression
        // Note: this is an experimental value
        if (configuration.isMinBitrateCheckEnabled && bitrate <= MIN_BITRATE) {
            Log.i("CompressorUtil", "Ignore compressing: INVALID_BITRATE = $bitrate")
            return Result.failure(Exception(INVALID_BITRATE))
        }

        if (width <= 480 || height <= 480) {
            Log.i(
                "CompressorUtil",
                "Ignore compressing: Video ratio is too small to resize width = $width, height = $height"
            )
            return Result.failure(
                Exception("Video ratio is too small to resize width = $width, height = $height"))
        }

        // Video min bitrate, and resolution is acceptable, start compressing
        startCompressingListener.invoke()

        //Handle new bitrate value
        val newBitrate: Int =
                when {
                    configuration.videoBitrate != null -> configuration.videoBitrate!!
                    configuration.videoBitrateCoefficient != null -> {
                        (bitrate * configuration.videoBitrateCoefficient!!).roundToInt()
                    }

                    else -> getBitrate(bitrate, configuration.quality)
                }

        //Handle new width and height values
        var (newWidth, newHeight) = generateWidthAndHeight(
            width,
            height
        )

        //Handle rotation values and swapping height and width if needed
        rotation = when (rotation) {
            90, 270 -> {
                val tempHeight = newHeight
                newHeight = newWidth
                newWidth = tempHeight
                0
            }

            180 -> 0
            else -> rotation
        }

        return start(
            context,
            newWidth,
            newHeight,
            destination,
            newBitrate,
            streamableFile,
            configuration.frameRate,
            configuration.disableAudio
        )
    }

    private fun start(
            context: Context?,
            newWidth: Int,
            newHeight: Int,
            destination: String,
            newBitrate: Int,
            streamableFile: String?,
            frameRate: Int?,
            disableAudio: Boolean
    ): Result<Boolean> {

        if (newWidth != 0 && newHeight != 0) {

            val cacheFile = File(destination)

            try {
                // MediaCodec accesses encoder and decoder components and processes the new video
                // input to generate a compressed/smaller size video
                val bufferInfo = MediaCodec.BufferInfo()

                // Setup mp4 movie
//                val movie = setUpMP4Movie(rotation, cacheFile)

                // MediaMuxer outputs MP4 in this app
//                val mediaMuxer = MP4Builder().createMovie(movie)
                val mediaMuxer = MediaMuxer(cacheFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                mediaMuxer.setOrientationHint(rotation)

                var muxerTrackIndex = -5

                val audioIndex = findTrack(extractor, isVideo = false)
                if (audioIndex >= 0) {
                    extractor.selectTrack(audioIndex)
                    val audioFormat = extractor.getTrackFormat(audioIndex)
                    muxerTrackIndex = mediaMuxer.addTrack(audioFormat)
                    extractor.unselectTrack(audioIndex)
                }

                // Start with video track
                val videoIndex = findTrack(extractor, isVideo = true)

                extractor.selectTrack(videoIndex)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                val inputFormat = extractor.getTrackFormat(videoIndex)

                val outputFormat: MediaFormat =
                        MediaFormat.createVideoFormat(MIME_TYPE, newWidth, newHeight)
                //set output format
                setOutputFileParameters(
                    inputFormat,
                    outputFormat,
                    newBitrate,
                    frameRate
                )

                /* val decoder: MediaCodec

                                val hasQTI = hasQTI()
                                val hasOMX = hasOMX()

                                val encoder = prepareEncoder(outputFormat, hasQTI, hasOMX)

                val inputSurface: InputSurface
                val outputSurface: OutputSurface

                var inputBufferIndex: Int
                var inputBuffer: ByteBuffer?
                var chunkSize: Int
                var frameIndex: Long = 0*/

                try {

                    // Using this only for video transcode
                    callbackBasedTranscoder = CallbackBasedTranscoder(context)
                    callbackBasedTranscoder?.setSize(newWidth, newHeight)
                    callbackBasedTranscoder?.setMediaMuxer(mediaMuxer)
                    callbackBasedTranscoder?.setMediaExtractor(extractor)
                    callbackBasedTranscoder?.setCopyVideo()
                    callbackBasedTranscoder?.setOutputFilePath(destination)
                    callbackBasedTranscoder?.setOutputVideoFormat(outputFormat)
                    callbackBasedTranscoder?.printAllLogs(false)

                    callbackBasedTranscoder?.runTranscode()

                    callbackBasedTranscoder = null
                    isRunning = false

                    if (isCancelled) {
                        isCancelled = false

                        compressionProgressListener.onProgressCancelled()

                        return Result.failure(Exception("The compression has stopped!"))
                    }
                } catch (exception: Exception) {
                    printException(exception)
                    return Result.failure(exception)
                }

                extractor.unselectTrack(videoIndex)

                processAudio(
                    mediaMuxer = mediaMuxer,
                    bufferInfo = bufferInfo,
                    disableAudio = disableAudio,
                    muxerTrackIndex = muxerTrackIndex,
                )

                extractor.release()
                try {
//                    mediaMuxer.finishMovie()
                    mediaMuxer.stop()
                    mediaMuxer.release()
                } catch (e: Exception) {
                    printException(e)
                }

            } catch (exception: Exception) {
                printException(exception)
            }

            streamableFile?.let {
                try {
                    val result = StreamableVideo.start(`in` = cacheFile, out = File(it))
                    if (result && cacheFile.exists()) {
                        cacheFile.delete()
                    }

                } catch (e: Exception) {
                    printException(e)
                }
            }
            return Result.success(true)
        }

        return Result.failure(Exception("Something went wrong, please try again"))
    }

    private fun processAudio(
            mediaMuxer: MediaMuxer,
            bufferInfo: MediaCodec.BufferInfo,
            disableAudio: Boolean,
            muxerTrackIndex: Int,
    ) {
        val audioIndex = findTrack(extractor, isVideo = false)
        if (audioIndex >= 0 && !disableAudio) {
            extractor.selectTrack(audioIndex)
            val audioFormat = extractor.getTrackFormat(audioIndex)

            //Added before muxer.start()
            //val muxerTrackIndex = mediaMuxer.addTrack(audioFormat, true)

            var maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)

            if (maxBufferSize <= 0) {
                maxBufferSize = 64 * 1024
            }

            var buffer: ByteBuffer = ByteBuffer.allocateDirect(maxBufferSize)
            if (Build.VERSION.SDK_INT >= 28) {
                val size = extractor.sampleSize
                if (size > maxBufferSize) {
                    maxBufferSize = (size + 1024).toInt()
                    buffer = ByteBuffer.allocateDirect(maxBufferSize)
                }
            }
            var inputDone = false
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (!inputDone) {
                val index = extractor.sampleTrackIndex
                if (index == audioIndex) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size >= 0) {
                        bufferInfo.apply {
                            presentationTimeUs = extractor.sampleTime
                            offset = 0
                            flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }
                        mediaMuxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                        extractor.advance()

                    } else {
                        bufferInfo.size = 0
                        inputDone = true
                    }
                } else if (index == -1) {
                    inputDone = true
                }
            }
            extractor.unselectTrack(audioIndex)
        }
    }

    private fun prepareEncoder(
            outputFormat: MediaFormat,
            hasQTI: Boolean,
            hasOMX: Boolean
    ): MediaCodec {

        // This seems to cause an issue with certain phones
        // val encoderName = MediaCodecList(REGULAR_CODECS).findEncoderForFormat(outputFormat)
        // val encoder: MediaCodec = MediaCodec.createByCodecName(encoderName)
        // Log.i("encoderName", encoder.name)
        // c2.qti.avc.encoder results in a corrupted .mp4 video that does not play in
        // Mac and iphones
        val encoder = if (hasOMX) {
            SceytLog.i("Compressor", "Using OMX.qcom.video.encoder.avc")
            MediaCodec.createByCodecName("OMX.qcom.video.encoder.avc")
        } else if (hasQTI) {
            SceytLog.i("Compressor", "Using c2.android.avc.encoder")
            MediaCodec.createByCodecName("c2.android.avc.encoder")
        } else {
            SceytLog.i("Compressor", "Using default encoder")
            MediaCodec.createEncoderByType(MIME_TYPE)
        }
        encoder.configure(
            outputFormat, null, null,
            MediaCodec.CONFIGURE_FLAG_ENCODE
        )

        return encoder
    }

    private fun prepareDecoder(
            inputFormat: MediaFormat,
            outputSurface: OutputSurface,
    ): MediaCodec {
        // This seems to cause an issue with certain phones
        // val decoderName =
        //    MediaCodecList(REGULAR_CODECS).findDecoderForFormat(inputFormat)
        // val decoder = MediaCodec.createByCodecName(decoderName)
        // Log.i("decoderName", decoder.name)

        // val decoder = if (hasQTI) {
        // MediaCodec.createByCodecName("c2.android.avc.decoder")
        //} else {

        val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
        //}

        decoder.configure(inputFormat, outputSurface.getSurface(), null, 0)

        return decoder
    }

    private fun dispose(
            videoIndex: Int,
            decoder: MediaCodec,
            encoder: MediaCodec,
            inputSurface: InputSurface,
            outputSurface: OutputSurface,
    ) {
        extractor.unselectTrack(videoIndex)

        decoder.stop()
        decoder.release()

        encoder.stop()
        encoder.release()

        inputSurface.release()
        outputSurface.release()
    }
}
