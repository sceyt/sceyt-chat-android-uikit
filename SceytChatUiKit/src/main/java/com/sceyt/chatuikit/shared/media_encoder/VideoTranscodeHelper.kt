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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume

object VideoTranscodeHelper : SceytKoinComponent {
    private val application by inject<Application>()
    private var pendingTranscodeQue = ConcurrentLinkedQueue<PendingTranscodeData>()

    @Volatile
    private var currentTranscodePath: String? = null

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

    private fun checkAndTranscode(
            destination: File,
            filePath: String,
            config: VideoResizeConfig = VideoResizeConfig.Medium,
            callback: (VideoTranscodeData) -> Unit
    ) {

        if (currentTranscodePath == null) {
            currentTranscodePath = filePath
            CustomVideoCompressor.start(
                context = application,
                srcUri = filePath.toUri(),
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
                        callback(VideoTranscodeData(Cancelled))
                        uploadNext()
                    }

                    override fun onFailure(failureMessage: String) {
                        callback(VideoTranscodeData(Failure, failureMessage))
                        uploadNext()
                    }

                    override fun onProgress(percent: Float) {
                        callback(VideoTranscodeData(Progress, progressPercent = 0f))
                    }

                    override fun onStart() {
                        callback(VideoTranscodeData(Start))
                    }

                    override fun onSuccess() {
                        callback(VideoTranscodeData(Success))
                        uploadNext()
                    }
                },
            )
        } else {
            val alreadyExist = currentTranscodePath == filePath || pendingTranscodeQue.any { it.filePath == filePath }

            if (!alreadyExist)
                pendingTranscodeQue.add(PendingTranscodeData(destination, filePath, config, callback))
        }
    }

    private fun uploadNext() {
        currentTranscodePath = null
        if (pendingTranscodeQue.isEmpty()) return
        pendingTranscodeQue.poll()?.let {
            checkAndTranscode(it.destination, it.filePath, it.config, it.callback)
        }
    }

    fun cancel(filePath: String?) {
        filePath ?: return
        if (currentTranscodePath == filePath) {
            CustomVideoCompressor.cancel()
        } else {
            pendingTranscodeQue.find { it.filePath == filePath }?.let {
                pendingTranscodeQue.remove(it)
            }
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