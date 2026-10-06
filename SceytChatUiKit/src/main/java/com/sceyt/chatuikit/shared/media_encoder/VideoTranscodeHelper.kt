package com.sceyt.chatuikit.shared.media_encoder

import android.app.Application
import androidx.core.net.toUri
import com.sceyt.chatuikit.config.VideoResizeConfig
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.shared.media_encoder.TranscodeResultEnum.Cancelled
import com.sceyt.chatuikit.shared.media_encoder.TranscodeResultEnum.Failure
import com.sceyt.chatuikit.shared.media_encoder.TranscodeResultEnum.Progress
import com.sceyt.chatuikit.shared.media_encoder.TranscodeResultEnum.Start
import com.sceyt.chatuikit.shared.media_encoder.TranscodeResultEnum.Success
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume

object VideoTranscodeHelper : SceytKoinComponent {
    private val application by inject<Application>()
    private var pendingTranscodeQue = ConcurrentLinkedQueue<PendingTranscodeData>()

    @Volatile
    private var currentDestination: File? = null

    @Volatile
    private var currentRun: Any? = null

    suspend fun transcodeAsResult(
            destination: File,
            path: String,
            config: VideoResizeConfig = VideoResizeConfig.Medium
    ): VideoTranscodeData {
        return suspendCancellableCoroutine {
            checkAndTranscode(destination, path, config) { data ->
                when (data.resultType) {
                    Cancelled -> it.resume(VideoTranscodeData(Cancelled))
                    Failure -> {
                        SceytLog.i("transcodeVideoFailure", data.errorMessage)
                        it.resume(VideoTranscodeData(Failure, data.errorMessage))
                    }

                    Success -> it.resume(VideoTranscodeData(Success))
                    Progress, Start -> Unit
                }
            }
        }
    }

    fun transcodeAsResultWithCallback(
            destination: File,
            path: String,
            config: VideoResizeConfig = VideoResizeConfig.Medium,
            callback: (VideoTranscodeData) -> Unit
    ) {
        checkAndTranscode(destination, path, config, callback)
    }

    @Synchronized
    private fun checkAndTranscode(
            destination: File,
            filePath: String,
            config: VideoResizeConfig = VideoResizeConfig.Medium,
            callback: (VideoTranscodeData) -> Unit
    ) {

        if (currentDestination == null) {
            currentDestination = destination
            val run = Any()
            currentRun = run
            val isLocalPath = filePath.startsWith("/")
            val srcUri = if (isLocalPath) null else filePath.toUri()
            val srcPath = if (isLocalPath) filePath else null
            val job = CustomVideoCompressor.start(
                context = application,
                srcUri = srcUri,
                srcPath = srcPath,
                destPath = destination.absolutePath,
                configureWith = TranscoderConfiguration(
                    quality = config.quality,
                    frameRate = config.frameRate,
                    isMinBitrateCheckEnabled = true,
                    disableAudio = false,
                    videoBitrate = config.bitrate,
                    videoBitrateCoefficient = config.bitrateCoefficient,
                    shortSideThreshold = config.shortSideThreshold,
                ),
                listener = object : CompressionListener {
                    override fun onCancelled() {
                        finishRun(run, VideoTranscodeData(Cancelled), callback)
                    }

                    override fun onFailure(failureMessage: String) {
                        finishRun(run, VideoTranscodeData(Failure, failureMessage), callback)
                    }

                    override fun onProgress(percent: Float) {
                        callback(VideoTranscodeData(Progress, progressPercent = 0f))
                    }

                    override fun onStart() {
                        callback(VideoTranscodeData(Start))
                    }

                    override fun onSuccess() {
                        finishRun(run, VideoTranscodeData(Success), callback)
                    }
                },
            )
            job.invokeOnCompletion { cause ->
                val data = if (cause == null || cause is CancellationException) {
                    VideoTranscodeData(Cancelled)
                } else {
                    VideoTranscodeData(Failure, cause.message)
                }
                finishRun(run, data, callback)
            }
        } else {
            pendingTranscodeQue.add(PendingTranscodeData(destination, filePath, config, callback))
        }
    }

    private fun finishRun(
            run: Any,
            data: VideoTranscodeData,
            callback: (VideoTranscodeData) -> Unit,
    ) {
        if (!releaseRun(run)) return
        callback(data)
        uploadNext()
    }

    @Synchronized
    private fun releaseRun(run: Any): Boolean {
        if (currentRun !== run) return false
        currentRun = null
        return true
    }

    @Synchronized
    private fun uploadNext() {
        currentDestination = null
        if (pendingTranscodeQue.isEmpty()) return
        pendingTranscodeQue.poll()?.let {
            checkAndTranscode(it.destination, it.filePath, it.config, it.callback)
        }
    }

    /** Cancels only the request writing to [destination], not others using the same source. */
    @Synchronized
    fun cancel(destination: File) {
        pendingTranscodeQue.removeAll { it.destination == destination }
        if (currentDestination == destination) {
            CustomVideoCompressor.cancel()
        }
    }
}

private data class PendingTranscodeData(
        val destination: File,
        val filePath: String,
        val config: VideoResizeConfig,
        val callback: (VideoTranscodeData) -> Unit
)

data class VideoTranscodeData(
        val resultType: TranscodeResultEnum,
        val errorMessage: String? = null,
        val progressPercent: Float = 0f
)

enum class TranscodeResultEnum {
    Cancelled, Failure, Progress, Start, Success
}
