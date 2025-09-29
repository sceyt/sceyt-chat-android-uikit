package com.sceyt.chatuikit.media.audio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

data class AudioRecordData(
        val file: File,
        val duration: Int,
        val amplitudes: List<Int>,
)

class AudioRecorderHelper(
        private val scope: CoroutineScope,
        private val context: Context,
) {
    private val recorderDispatcher = Executors.newSingleThreadScheduledExecutor().asCoroutineDispatcher()
    private var audioFile: File? = null
    private var currentRecorder: AudioRecorder? = null
    private val audioFocusHelper: AudioFocusHelper by lazy { AudioFocusHelper(context) }

    fun startRecording(
            directoryToSaveFile: String,
            onRecorderStart: OnRecorderStart? = null,
            onRecordReachedMaxDurationListener: ReachedMaxDurationListener? = null,
    ) {
        scope.launch(recorderDispatcher) {
            audioFocusHelper.requestAudioFocusCompat()
            val audioFile = FileManager.createFile(AudioRecorderImpl.AUDIO_FORMAT, directoryToSaveFile).also {
                this@AudioRecorderHelper.audioFile = it
            }
            val recorder = AudioRecorderImpl(context, audioFile).also { currentRecorder = it }
            val started = recorder.startRecording(onRecordReachedMaxDurationListener)

            onRecorderStart?.let { listener ->
                withContext(Dispatchers.Main) {
                    listener.onStart(started)
                }
            }
        }
    }

    fun stopRecording(onRecorderStop: OnRecorderStop? = null) {
        scope.launch(recorderDispatcher) {
            val duration = currentDuration
            val amplitudes = currentAmplitudes
            val file = audioFile
            currentRecorder?.stopRecording()
            var isTooShort = false
            if (duration < 1) {
                audioFile?.delete()
                audioFile = null
                isTooShort = true
            } else {
                audioFile = null
            }
            audioFocusHelper.abandonCallAudioFocusCompat()
            withContext(Dispatchers.Main) {
                onRecorderStop?.onStop(isTooShort, file, duration, amplitudes)
            }
        }
    }

    fun cancelRecording(onRecorderCancel: OnRecorderCancel? = null) {
        scope.launch(recorderDispatcher) {
            currentRecorder?.stopRecording()
            audioFile?.delete()
            audioFile = null
            audioFocusHelper.abandonCallAudioFocusCompat()
            withContext(Dispatchers.Main) {
                onRecorderCancel?.onCancel()
            }
        }
    }

    private val currentAmplitudes: Array<Int>
        get() = currentRecorder?.getRecordingAmplitudes() ?: arrayOf(0)

    private val currentDuration: Int
        get() = currentRecorder?.getRecordingDuration() ?: 0

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

    fun interface OnRecorderStop {
        fun onStop(tooShort: Boolean, recordedFile: File?, duration: Int, amplitudes: Array<Int>)
    }

    fun interface OnRecorderCancel {
        fun onCancel()
    }
}