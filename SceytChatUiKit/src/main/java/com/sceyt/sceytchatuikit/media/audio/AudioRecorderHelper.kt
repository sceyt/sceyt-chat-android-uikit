package com.sceyt.sceytchatuikit.media.audio

import android.content.Context
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

class AudioRecorderHelper(
        private val scope: CoroutineScope,
        private val context: Context
) {
    private val recorderDispatcher = Executors.newSingleThreadScheduledExecutor().asCoroutineDispatcher()
    private var audioFile: File? = null
    private var currentRecorder: AudioRecorder? = null
    private val serializer = GsonBuilder().create()
    private val audioFocusHelper: AudioFocusHelper by lazy { AudioFocusHelper(context) }

    fun startRecording(directoryToSaveFile: String, onRecorderStart: OnRecorderStart? = null) {
        scope.launch(recorderDispatcher) {
            audioFocusHelper.requestAudioFocusCompat()
            val audioFile = FileManager.createFile(AudioRecorderImpl.AUDIO_FORMAT, directoryToSaveFile).also {
                this@AudioRecorderHelper.audioFile = it
            }
            val recorder = AudioRecorderImpl(context, audioFile).also { currentRecorder = it }
            val started = recorder.startRecording(32000, null)
            withContext(Dispatchers.Main) {
                onRecorderStart?.onStart(started)
            }
        }
    }

    fun stopRecording(onRecorderStop: OnRecorderStop? = null) {
        scope.launch(recorderDispatcher) {
            currentRecorder?.stopRecording()
            val duration = currentDuration
            var isTooShort = false
            if (duration < 1) {
                audioFile?.delete()
                isTooShort = true
            }
            audioFocusHelper.abandonCallAudioFocusCompat()
            withContext(Dispatchers.Main) {
                onRecorderStop?.onStop(isTooShort, audioFile, duration, currentAmplitudes)
            }
        }
    }

    fun cancelRecording(onRecorderCancel: OnRecorderCancel? = null) {
        scope.launch(recorderDispatcher) {
            currentRecorder?.stopRecording()
            audioFile?.delete()
            audioFocusHelper.abandonCallAudioFocusCompat()
            withContext(Dispatchers.Main) {
                onRecorderCancel?.onCancel()
            }
        }
    }

    val currentAmplitudes: Array<Int>
        get() = currentRecorder?.getRecordingAmplitudes() ?: arrayOf(0)

    val currentDuration: Int
        get() = currentRecorder?.getRecordingDuration() ?: 0

    val jsonAmplitudes: String
        get() {
            val amps = currentAmplitudes
            return serializer.toJson(amps, Array<Int>::class.java)
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