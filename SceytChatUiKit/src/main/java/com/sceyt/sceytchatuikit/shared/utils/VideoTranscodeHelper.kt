package com.sceyt.sceytchatuikit.shared.utils

import android.content.Context
import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.sceyt.sceytchatuikit.logger.SceytLog
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object VideoTranscodeHelper {

    suspend fun transcodeAsResult(context: Context, destination: File, uri: String, quality: VideoQuality = VideoQuality.MEDIUM): VideoTranscodeData {
        return suspendCancellableCoroutine {
            try {
                VideoCompressor.start(
                    context = context,
                    srcUri = Uri.parse(uri),
                    destPath = destination.absolutePath,
                    configureWith = Configuration(
                        quality = quality,
                        isMinBitrateCheckEnabled = false,
                        disableAudio = false,
                    ),
                    listener = object : CompressionListener {
                        override fun onCancelled() {
                            it.resume(VideoTranscodeData(TranscodeResultEnum.Cancelled))
                        }

                        override fun onFailure(failureMessage: String) {
                            SceytLog.i("transcodeVideoFailure", failureMessage)
                            it.resume(VideoTranscodeData(TranscodeResultEnum.Failure, failureMessage))
                        }

                        override fun onProgress(percent: Float) {
                        }

                        override fun onStart() {
                        }

                        override fun onSuccess() {
                            it.resume(VideoTranscodeData(TranscodeResultEnum.Success))
                        }
                    },
                )
            } catch (ex: Exception) {
                SceytLog.e("transcodeVideoException", ex.message.toString())
                it.resume(VideoTranscodeData(TranscodeResultEnum.Failure, ex.message.toString()))
            }
        }
    }

    fun transcodeAsResultWithCallback(context: Context, destination: File, uri: String, quality: VideoQuality = VideoQuality.MEDIUM, callback: (VideoTranscodeData) -> Unit) {
        try {
            VideoCompressor.start(
                context = context,
                srcUri = Uri.parse(uri),
                destPath = destination.absolutePath,
                configureWith = Configuration(
                    quality = quality,
                    isMinBitrateCheckEnabled = false,
                    disableAudio = false,
                ),
                listener = object : CompressionListener {
                    override fun onCancelled() {
                        callback(VideoTranscodeData(TranscodeResultEnum.Cancelled))
                    }

                    override fun onFailure(failureMessage: String) {
                        callback(VideoTranscodeData(TranscodeResultEnum.Failure, failureMessage))
                    }

                    override fun onProgress(percent: Float) {
                        callback(VideoTranscodeData(TranscodeResultEnum.Progress, progressPercent = percent))
                    }

                    override fun onStart() {
                        callback(VideoTranscodeData(TranscodeResultEnum.Start))
                    }

                    override fun onSuccess() {
                        callback(VideoTranscodeData(TranscodeResultEnum.Success))
                    }
                },
            )
        } catch (ex: Exception) {
            SceytLog.e("transcodeVideoException", ex.message.toString())
            callback(VideoTranscodeData(TranscodeResultEnum.Failure, ex.message.toString()))
        }
    }
}


data class VideoTranscodeData(
        val resultType: TranscodeResultEnum,
        val errorMessage: String? = null,
        val progressPercent: Float = 0f
)

enum class TranscodeResultEnum {
    Cancelled, Failure, Progress, Start, Success
}