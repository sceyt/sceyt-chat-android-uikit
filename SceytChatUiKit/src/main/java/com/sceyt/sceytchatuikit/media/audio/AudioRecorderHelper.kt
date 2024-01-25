package com.sceyt.sceytchatuikit.media.audio

import android.content.Context
import com.google.gson.GsonBuilder
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AudioRecorderHelper(private val context: Context) {
    private val recorderExecutor: Executor = Executors.newSingleThreadScheduledExecutor()
    private var audioFile: File? = null
    private var currentRecorder: AudioRecorder? = null
    private val serializer = GsonBuilder().create()
    private val audioFocusHelper: AudioFocusHelper by lazy { AudioFocusHelper(context) }

    fun startRecording(directoryToSaveFile: String, onRecorderStart: OnRecorderStart? = null) {
        recorderExecutor.execute {
            audioFocusHelper.requestAudioFocusCompat()
            val audioFile = FileManager.createFile(AudioRecorderImpl.AUDIO_FORMAT, directoryToSaveFile).also {
                this.audioFile = it
            }
            val recorder = AudioRecorderImpl(context,audioFile).also { currentRecorder = it }
            val started = recorder.startRecording(32000, null)
            onRecorderStart?.onStart(started)
        }
    }

    fun stopRecording(onRecorderStop: OnRecorderStop? = null) {
        recorderExecutor.execute {
            currentRecorder?.stopRecording()
            val duration = currentDuration
            var isTooShort = false
            if (duration < 1) {
                audioFile?.delete()
                isTooShort = true
            }
            audioFocusHelper.abandonCallAudioFocusCompat()
            onRecorderStop?.onStop(isTooShort, audioFile, duration, currentAmplitudes)
        }
    }

    fun cancelRecording(onRecorderCancel: OnRecorderCancel? = null) {
        recorderExecutor.execute {
            currentRecorder?.stopRecording()
            audioFile?.delete()
            onRecorderCancel?.onCancel()
            audioFocusHelper.abandonCallAudioFocusCompat()
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