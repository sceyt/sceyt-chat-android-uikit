package com.sceyt.sceytchatuikit.shared.mediaencoder

import android.app.Application
import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Cancelled
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Failure
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Progress
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Start
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Success
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume

object VideoTranscodeHelper : SceytKoinComponent {
    private val application by inject<Application>()
    private var pendingTranscodeQue: ConcurrentLinkedQueue<PendingTranscodeData> = ConcurrentLinkedQueue()

    @Volatile
    private var currentTranscodePath: String? = null

    suspend fun transcodeAsResult(destination: File, path: String, quality: VideoQuality = VideoQuality.MEDIUM): VideoTranscodeData {
        return suspendCancellableCoroutine {
            checkAndTranscode(destination, path, quality) { data ->
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

    fun transcodeAsResultWithCallback(destination: File, path: String, quality: VideoQuality = VideoQuality.MEDIUM,
                                      callback: (VideoTranscodeData) -> Unit) {
        checkAndTranscode(destination, path, quality, callback)
    }

    private fun checkAndTranscode(destination: File, filePath: String, quality: VideoQuality = VideoQuality.MEDIUM,
                                  callback: (VideoTranscodeData) -> Unit) {

        if (currentTranscodePath == null) {
            currentTranscodePath = filePath
            CustomVideoCompressor.start(
                context = application,
                srcUri = Uri.parse(filePath),
                destPath = destination.absolutePath,
                configureWith = CustomConfiguration(
                    quality = quality,
                    isMinBitrateCheckEnabled = true,
                    disableAudio = false,
                    videoBitrateCoefficient = 0.09f,
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
                pendingTranscodeQue.add(PendingTranscodeData(destination, filePath, quality, callback))
        }
    }

    private fun uploadNext() {
        currentTranscodePath = null
        if (pendingTranscodeQue.isEmpty()) return
        pendingTranscodeQue.poll()?.let {
            checkAndTranscode(it.destination, it.filePath, it.quality, it.callback)
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
        val quality: VideoQuality,
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