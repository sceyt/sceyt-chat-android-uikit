package com.sceyt.chatuikit.shared.mediaencoder

import android.content.Context
import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.TranscodeQuality
import com.sceyt.chatuikit.shared.mediaencoder.TranscodeResultEnum.Cancelled
import com.sceyt.chatuikit.shared.mediaencoder.TranscodeResultEnum.Failure
import com.sceyt.chatuikit.shared.mediaencoder.TranscodeResultEnum.Progress
import com.sceyt.chatuikit.shared.mediaencoder.TranscodeResultEnum.Start
import com.sceyt.chatuikit.shared.mediaencoder.TranscodeResultEnum.Success
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume

object VideoTranscodeHelper {
    private var pendingTranscodeQue: ConcurrentLinkedQueue<PendingTranscodeData> = ConcurrentLinkedQueue()

    @Volatile
    private var currentTranscodePath: String? = null

    suspend fun transcodeAsResult(context: Context, destination: File, path: String,
                                  quality: TranscodeQuality = TranscodeQuality.Medium): VideoTranscodeData {
        return suspendCancellableCoroutine {
            checkAndTranscode(context, destination, path, quality.toVideoQuality()) { data ->
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

    fun transcodeAsResultWithCallback(context: Context, destination: File, path: String,
                                      quality: TranscodeQuality = TranscodeQuality.Medium,
                                      callback: (VideoTranscodeData) -> Unit) {
        checkAndTranscode(context, destination, path, quality.toVideoQuality(), callback)
    }

    private fun checkAndTranscode(context: Context, destination: File, filePath: String,
                                  quality: VideoQuality = VideoQuality.MEDIUM,
                                  callback: (VideoTranscodeData) -> Unit) {

        if (currentTranscodePath == null) {
            currentTranscodePath = filePath
            CustomVideoCompressor.start(
                context = context,
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
                        uploadNext(context)
                    }

                    override fun onFailure(failureMessage: String) {
                        callback(VideoTranscodeData(Failure, failureMessage))
                        uploadNext(context)
                    }

                    override fun onProgress(percent: Float) {
                        callback(VideoTranscodeData(Progress, progressPercent = 0f))
                    }

                    override fun onStart() {
                        callback(VideoTranscodeData(Start))
                    }

                    override fun onSuccess() {
                        callback(VideoTranscodeData(Success))
                        uploadNext(context)
                    }
                },
            )
        } else {
            val alreadyExist = currentTranscodePath == filePath || pendingTranscodeQue.any { it.filePath == filePath }

            if (!alreadyExist)
                pendingTranscodeQue.add(PendingTranscodeData(destination, filePath, quality, callback))
        }
    }

    private fun uploadNext(context: Context) {
        currentTranscodePath = null
        if (pendingTranscodeQue.isEmpty()) return
        pendingTranscodeQue.poll()?.let {
            checkAndTranscode(context, it.destination, it.filePath, it.quality, it.callback)
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

private fun TranscodeQuality.toVideoQuality(): VideoQuality {
    return when (this) {
        TranscodeQuality.VeryHigh -> VideoQuality.VERY_HIGH
        TranscodeQuality.High -> VideoQuality.HIGH
        TranscodeQuality.Medium -> VideoQuality.MEDIUM
        TranscodeQuality.Low -> VideoQuality.LOW
        TranscodeQuality.VeryLow -> VideoQuality.VERY_LOW
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