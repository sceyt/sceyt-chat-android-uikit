package com.sceyt.chatuikit.media.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import com.sceyt.chatuikit.SceytChatUIKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.min

data class AudioRecordData(
    val file: File,
    val duration: Int,
    val amplitudes: List<Int>,
)

class AudioRecorderHelper(
    private val scope: CoroutineScope,
    private val context: Context,
) {
    private val recorderDispatcher =
        Executors.newSingleThreadScheduledExecutor().asCoroutineDispatcher()
    private var audioFile: File? = null
    private var currentRecorder: AudioRecorder? = null
    private val audioFocusHelper: AudioFocusHelper by lazy { AudioFocusHelper(context) }

    fun startRecording(
        directoryToSaveFile: String,
        onRecorderStart: OnRecorderStart? = null,
        onRecorderError: OnRecorderError? = null,
        onRecordReachedMaxDurationListener: ReachedMaxDurationListener? = null,
    ) {
        scope.launch(recorderDispatcher) {
            audioFocusHelper.requestAudioFocusCompat()
            val audioFile = FileManager.createFile(
                extension = AudioRecorderImpl.AUDIO_FORMAT,
                directory = directoryToSaveFile
            ).also {
                this@AudioRecorderHelper.audioFile = it
            }
            val recorder = AudioRecorderImpl(context, audioFile).also { currentRecorder = it }
            val started = recorder.startRecording(
                reachedMaxDurationListener = onRecordReachedMaxDurationListener,
                errorListener = { what, extra ->
                    scope.launch(Dispatchers.Main) {
                        onRecorderError?.onError(what, extra)
                    }
                }
            )

            onRecorderStart?.let { listener ->
                withContext(Dispatchers.Main) {
                    listener.onStart(started)
                }
            }
        }
    }

    fun stopRecording(onRecorderStop: OnRecorderStop? = null) {
        scope.launch(recorderDispatcher) {
            val recorder = currentRecorder
            val amplitudes = recorder?.getRecordingAmplitudes() ?: arrayOf(0)
            val file = audioFile
            recorder?.stopRecording()
            currentRecorder = null
            audioFile = null
            val duration = resolveDuration(file, recorder?.getRecordingDuration() ?: 0)
            val isTooShort = duration < 1
            if (isTooShort)
                file?.delete()
            audioFocusHelper.abandonCallAudioFocusCompat()
            withContext(Dispatchers.Main) {
                onRecorderStop?.onStop(isTooShort, file, duration, amplitudes)
            }
        }
    }

    fun cancelRecording(onRecorderCancel: OnRecorderCancel? = null) {
        scope.launch(recorderDispatcher) {
            currentRecorder?.stopRecording()
            currentRecorder = null
            audioFile?.delete()
            audioFile = null
            audioFocusHelper.abandonCallAudioFocusCompat()
            withContext(Dispatchers.Main) {
                onRecorderCancel?.onCancel()
            }
        }
    }

    fun isRecording(): Boolean {
        return currentRecorder?.isRecording() == true
    }

    private val currentAmplitudes: Array<Int>
        get() = currentRecorder?.getRecordingAmplitudes() ?: arrayOf(0)

    private val currentDuration: Int
        get() = currentRecorder?.getRecordingDuration() ?: 0

    internal fun resolveDuration(file: File?, recordedDuration: Int): Int {
        val path = file?.takeIf { it.exists() }?.absolutePath ?: return recordedDuration
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            durationMs?.let { (it / 1000).toInt() } ?: durationFromFileSize(file, recordedDuration)
        } catch (ex: Exception) {
            ex.printStackTrace()
            durationFromFileSize(file, recordedDuration)
        } finally {
            try {
                retriever.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun durationFromFileSize(file: File, recordedDuration: Int): Int {
        val bitrate = SceytChatUIKit.config.voiceRecorderConfig.bitrate
        if (bitrate <= 0) return recordedDuration
        val estimated = (file.length() * 8 / bitrate).toInt()
        return min(recordedDuration, estimated)
    }

    fun getAudioRecordData(): AudioRecordData? {
        val file = audioFile ?: return null
        val duration = currentDuration
        if (duration < 1) {
            return null
        }
        return AudioRecordData(file, duration, currentAmplitudes.toList())
    }

    fun interface OnRecorderStart {
        fun onStart(started: Boolean)
    }

    fun interface OnRecorderError {
        fun onError(what: Int, extra: Int)
    }

    fun interface OnRecorderStop {
        fun onStop(tooShort: Boolean, recordedFile: File?, duration: Int, amplitudes: Array<Int>)
    }

    fun interface OnRecorderCancel {
        fun onCancel()
    }
}