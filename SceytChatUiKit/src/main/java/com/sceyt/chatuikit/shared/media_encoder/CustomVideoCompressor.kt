package com.sceyt.chatuikit.shared.media_encoder

import android.content.Context
import android.net.Uri
import com.sceyt.chatuikit.shared.media_encoder.CustomCompressor.compressVideo
import com.sceyt.chatuikit.shared.media_encoder.CustomCompressor.isRunning
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object CustomVideoCompressor : CoroutineScope {

    private var job: Job? = null

    /**
     * This function compresses a given [srcPath] or [srcUri] video file and writes the compressed
     * video file at [destPath]
     *
     * The source video can be provided as a string path or a content uri. If both [srcPath] and
     * [srcUri] are provided, [srcUri] will be ignored.
     *
     * Passing [srcUri] requires [context].
     *
     * @param [context] the application context.
     * @param [srcUri] the content Uri of the video file.
     * @param [srcPath] the path of the provided video file to be compressed
     * @param [destPath] the path where the output compressed video file should be saved
     * @param [listener] a compression listener that listens to compression [CompressionListener.onStart],
     * [CompressionListener.onProgress], [CompressionListener.onFailure], [CompressionListener.onSuccess]
     * and if the compression was [CompressionListener.onCancelled]
     * @param [configureWith] to allow add video compression configuration that could be:
     * [TranscoderConfiguration.quality] to allow choosing a video quality that can be [VideoQuality.LOW],
     * [VideoQuality.MEDIUM], [VideoQuality.HIGH], and [VideoQuality.VERY_HIGH].
     * This defaults to [VideoQuality.MEDIUM]
     * [TranscoderConfiguration.isMinBitrateCheckEnabled] to determine if the checking for a minimum bitrate threshold
     * before compression is enabled or not. This default to `true`
     * [TranscoderConfiguration.videoBitrate] which is a custom bitrate for the video. You might consider setting
     * [TranscoderConfiguration.isMinBitrateCheckEnabled] to `false` if your bitrate is less than 2000000.
     */
    @JvmStatic
    @JvmOverloads
    fun start(
            context: Context? = null,
            srcUri: Uri? = null,
            srcPath: String? = null,
            destPath: String,
            streamableFile: String? = null,
            listener: CompressionListener,
            configureWith: TranscoderConfiguration,
    ) {
        job = doVideoCompression(
            context,
            srcUri,
            srcPath,
            destPath,
            streamableFile,
            configureWith,
            listener,
        )
    }

    /**
     * Call this function to cancel video compression process which will call [CompressionListener.onCancelled]
     */
    @JvmStatic
    fun cancel() {
        job?.cancel()
        isRunning = false
        CustomCompressor.cancel()
    }

    private fun doVideoCompression(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
            destPath: String,
            streamableFile: String? = null,
            configuration: TranscoderConfiguration,
            listener: CompressionListener,
    ) = launch {
        isRunning = true
        val result = startCompression(
            context,
            srcUri,
            srcPath,
            destPath,
            streamableFile,
            configuration,
            listener,
        )

        // Runs in Main(UI) Thread
        if (result.isSuccess) {
            listener.onSuccess()
        } else {
            listener.onFailure(result.exceptionOrNull()?.message ?: "An error has occurred!")
        }
    }

    private suspend fun startCompression(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
            destPath: String,
            streamableFile: String? = null,
            configuration: TranscoderConfiguration,
            listener: CompressionListener,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext compressVideo(
            context,
            srcUri,
            srcPath,
            destPath,
            streamableFile,
            configuration,
            object : CompressionProgressListener {
                override fun onProgressChanged(percent: Float) {
                    listener.onProgress(percent)
                }

                override fun onProgressCancelled() {
                    listener.onCancelled()
                }
            },
            startCompressingListener = {
                listener.onStart()
            })
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}
