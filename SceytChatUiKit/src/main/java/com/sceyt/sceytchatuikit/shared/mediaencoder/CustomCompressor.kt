package com.sceyt.sceytchatuikit.shared.mediaencoder

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.util.Log
import com.abedelazizshe.lightcompressorlibrary.CompressionProgressListener
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.findTrack
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.generateWidthAndHeight
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.getBitrate
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.hasQTI
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.prepareVideoHeight
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.prepareVideoWidth
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.printException
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.setOutputFileParameters
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.setUpMP4Movie
import com.sceyt.sceytchatuikit.shared.mediaencoder.CompressorUtils.validateInputs
import com.abedelazizshe.lightcompressorlibrary.utils.StreamableVideo
import com.abedelazizshe.lightcompressorlibrary.video.*
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Created by AbedElaziz Shehadeh on 27 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */
object CustomCompressor {

    // 2Mbps
    private const val MIN_BITRATE = 2000000

    // H.264 Advanced Video Coding
    private const val MIME_TYPE = "video/avc"
    private const val MEDIACODEC_TIMEOUT_DEFAULT = 100L

    // MediaExtractor extracts encoded media data from the source
    private lateinit var extractor: MediaExtractor
    private lateinit var compressionProgressListener: CompressionProgressListener
    private var duration: Long = 0
    private var rotation: Int = 0

    private const val INVALID_BITRATE =
            "The provided bitrate is smaller than what is needed for compression " +
                    "try to set isMinBitRateEnabled to false"

    var isRunning = true

    fun compressVideo(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
            destination: String,
            streamableFile: String?,
            configuration: CustomConfiguration,
            listener: CompressionProgressListener,
    ): Result {

        extractor = MediaExtractor()
        compressionProgressListener = listener
        // Retrieve the source's metadata to be used as input to generate new values for compression
        val mediaMetadataRetriever = MediaMetadataRetriever()

        validateInputs(context, srcUri, srcPath)?.let {
            return Result(
                success = false,
                failureMessage = it
            )
        }

        if (context != null && srcUri != null && srcPath == null) {

            try {
                mediaMetadataRetriever.setDataSource(context, srcUri)
            } catch (exception: IllegalArgumentException) {
                printException(exception)
                return Result(
                    success = false,
                    failureMessage = "${exception.message}"
                )
            }

            extractor.setDataSource(context, srcUri, null)
        } else {
            try {
                mediaMetadataRetriever.setDataSource(srcPath)
            } catch (exception: IllegalArgumentException) {
                printException(exception)
                return Result(
                    success = false,
                    failureMessage = "${exception.message}"
                )
            }

            val file = File(srcPath!!)
            if (!file.canRead()) return Result(
                success = false,
                failureMessage = "The source file cannot be accessed!"
            )

            extractor.setDataSource(file.toString())
        }

        val height: Double = prepareVideoHeight(mediaMetadataRetriever)

        val width: Double = prepareVideoWidth(mediaMetadataRetriever)

        val rotationData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

        val bitrateData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

        val durationData =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)


        if (rotationData.isNullOrEmpty() || bitrateData.isNullOrEmpty() || durationData.isNullOrEmpty()) {
            // Exit execution
            return Result(
                success = false,
                failureMessage = "Failed to extract video meta-data, please try again"
            )
        }

        rotation = rotationData.toInt()
        val bitrate = bitrateData.toInt()
        duration = durationData.toLong() * 1000

        // Check for a min video bitrate before compression
        // Note: this is an experimental value
        if (configuration.isMinBitrateCheckEnabled && bitrate <= MIN_BITRATE)
            return Result(success = false, failureMessage = INVALID_BITRATE)

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
            newWidth,
            newHeight,
            destination,
            newBitrate,
            streamableFile,
            configuration.frameRate,
            configuration.disableAudio
        )
    }

    @Suppress("DEPRECATION")
    private fun start(
            newWidth: Int,
            newHeight: Int,
            destination: String,
            newBitrate: Int,
            streamableFile: String?,
            frameRate: Int?,
            disableAudio: Boolean
    ): Result {

        if (newWidth != 0 && newHeight != 0) {

            val cacheFile = File(destination)

            try {
                // MediaCodec accesses encoder and decoder components and processes the new video
                //input to generate a compressed/smaller size video
                val bufferInfo = MediaCodec.BufferInfo()

                // Setup mp4 movie
                val movie = setUpMP4Movie(rotation, cacheFile)

                // MediaMuxer outputs MP4 in this app
                val mediaMuxer = MP4Builder().createMovie(movie)

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

                val decoder: MediaCodec

                val hasQTI = hasQTI()

                val encoder = prepareEncoder(outputFormat, hasQTI)

                val inputSurface: InputSurface
                val outputSurface: OutputSurface

                try {
                    var inputDone = false
                    var outputDone = false

                    var videoTrackIndex = -5

                    inputSurface = InputSurface(encoder.createInputSurface())
                    inputSurface.makeCurrent()
                    //Move to executing state
                    encoder.start()

                    outputSurface = OutputSurface()

                    decoder = prepareDecoder(inputFormat, outputSurface)

                    //Move to executing state
                    decoder.start()

                    while (!outputDone) {
                        if (!inputDone) {

                            val index = extractor.sampleTrackIndex

                            if (index == videoIndex) {
                                val inputBufferIndex =
                                        decoder.dequeueInputBuffer(MEDIACODEC_TIMEOUT_DEFAULT)
                                if (inputBufferIndex >= 0) {
                                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                                    val chunkSize = extractor.readSampleData(inputBuffer!!, 0)
                                    when {
                                        chunkSize < 0 -> {

                                            decoder.queueInputBuffer(
                                                inputBufferIndex,
                                                0,
                                                0,
                                                0L,
                                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                            )
                                            inputDone = true
                                        }

                                        else -> {

                                            decoder.queueInputBuffer(
                                                inputBufferIndex,
                                                0,
                                                chunkSize,
                                                extractor.sampleTime,
                                                0
                                            )
                                            extractor.advance()

                                        }
                                    }
                                }

                            } else if (index == -1) { //end of file
                                val inputBufferIndex =
                                        decoder.dequeueInputBuffer(MEDIACODEC_TIMEOUT_DEFAULT)
                                if (inputBufferIndex >= 0) {
                                    decoder.queueInputBuffer(
                                        inputBufferIndex,
                                        0,
                                        0,
                                        0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputDone = true
                                }
                            }
                        }

                        var decoderOutputAvailable = true
                        var encoderOutputAvailable = true

                        loop@ while (decoderOutputAvailable || encoderOutputAvailable) {

                            if (!isRunning) {
                                compressionProgressListener.onProgressCancelled()
                                return Result(
                                    success = false,
                                    failureMessage = "The compression has stopped!"
                                )
                            }

                            //Encoder
                            val encoderStatus =
                                    encoder.dequeueOutputBuffer(bufferInfo, MEDIACODEC_TIMEOUT_DEFAULT)

                            when {
                                encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable =
                                        false

                                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    val newFormat = encoder.outputFormat
                                    if (videoTrackIndex == -5)
                                        videoTrackIndex = mediaMuxer.addTrack(newFormat, false)
                                }

                                encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                                    // ignore this status
                                }

                                encoderStatus < 0 -> throw RuntimeException("unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                                else -> {
                                    val encodedData = encoder.getOutputBuffer(encoderStatus)
                                            ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                                    if (bufferInfo.size > 1) {
                                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                            mediaMuxer.writeSampleData(
                                                videoTrackIndex,
                                                encodedData, bufferInfo, false
                                            )
                                        }

                                    }

                                    outputDone =
                                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                                    encoder.releaseOutputBuffer(encoderStatus, false)
                                }
                            }
                            if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) continue@loop

                            //Decoder
                            val decoderStatus =
                                    decoder.dequeueOutputBuffer(bufferInfo, MEDIACODEC_TIMEOUT_DEFAULT)
                            when {
                                decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable =
                                        false

                                decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                                    // ignore this status
                                }

                                decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    // ignore this status
                                }

                                decoderStatus < 0 -> throw RuntimeException("unexpected result from decoder.dequeueOutputBuffer: $decoderStatus")
                                else -> {
                                    val doRender = bufferInfo.size != 0

                                    decoder.releaseOutputBuffer(decoderStatus, doRender)
                                    if (doRender) {
                                        var errorWait = false
                                        try {
                                            outputSurface.awaitNewImage()
                                        } catch (e: Exception) {
                                            errorWait = true
                                            Log.e(
                                                "Compressor",
                                                e.message ?: "Compression failed at swapping buffer"
                                            )
                                        }

                                        if (!errorWait) {
                                            outputSurface.drawImage()

                                            inputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)

                                            compressionProgressListener.onProgressChanged(bufferInfo.presentationTimeUs.toFloat() / duration.toFloat() * 100)

                                            inputSurface.swapBuffers()
                                        }
                                    }
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        decoderOutputAvailable = false
                                        encoder.signalEndOfInputStream()
                                    }
                                }
                            }
                        }
                    }

                } catch (exception: Exception) {
                    printException(exception)
                    return Result(success = false, failureMessage = exception.message)
                }

                dispose(
                    videoIndex,
                    decoder,
                    encoder,
                    inputSurface,
                    outputSurface,
                )

                processAudio(
                    mediaMuxer = mediaMuxer,
                    bufferInfo = bufferInfo,
                    disableAudio = disableAudio,
                )

                extractor.release()
                try {
                    mediaMuxer.finishMovie()
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
            return Result(success = true, failureMessage = null)
        }

        return Result(success = false, failureMessage = "Something went wrong, please try again")
    }

    private fun processAudio(
            mediaMuxer: MP4Builder,
            bufferInfo: MediaCodec.BufferInfo,
            disableAudio: Boolean,
    ) {
        val audioIndex = findTrack(extractor, isVideo = false)
        if (audioIndex >= 0 && !disableAudio) {
            extractor.selectTrack(audioIndex)
            val audioFormat = extractor.getTrackFormat(audioIndex)
            val muxerTrackIndex = mediaMuxer.addTrack(audioFormat, true)
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
                        mediaMuxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo, true)
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

    private fun prepareEncoder(outputFormat: MediaFormat, hasQTI: Boolean): MediaCodec {

        // This seems to cause an issue with certain phones
        // val encoderName = MediaCodecList(REGULAR_CODECS).findEncoderForFormat(outputFormat)
        // val encoder: MediaCodec = MediaCodec.createByCodecName(encoderName)
        // Log.i("encoderName", encoder.name)
        // c2.qti.avc.encoder results in a corrupted .mp4 video that does not play in
        // Mac and iphones
        val encoder = if (hasQTI) {
            MediaCodec.createByCodecName("c2.android.avc.encoder")
        } else {
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
