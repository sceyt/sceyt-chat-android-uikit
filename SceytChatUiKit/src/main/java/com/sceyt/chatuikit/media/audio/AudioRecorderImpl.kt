package com.sceyt.chatuikit.media.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.sceyt.chatuikit.media.DurationCallback
import java.io.File
import java.util.Arrays
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

class AudioRecorderImpl(private var context: Context,
                        private val file: File) : AudioRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private val recording = AtomicBoolean(false)
    private var timer: Timer? = null
    private var startTime: Long = 0
    private var durationCallback: DurationCallback? = null
    private val amplitudes: ArrayList<Int> = ArrayList()
    private var amplitudeIndex = 0

    override fun startRecording(bitrate: Int, durationCallback: DurationCallback?): Boolean {
        try {
            Log.i("AudioRecorder", "startRecording")
            recording.set(true)
            this.durationCallback = durationCallback
            mediaRecorder = try {
                initMediaRecorder(bitrate)
            } catch (ex: Exception) {
                ex.printStackTrace()
                return false
            }
            return true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun initMediaRecorder(bitrate: Int): MediaRecorder {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.setAudioChannels(1)
        mediaRecorder.setAudioSamplingRate(16000)
        mediaRecorder.setMaxDuration(MAX_TIME)
        mediaRecorder.setAudioEncodingBitRate(bitrate)
        mediaRecorder.prepare()
        mediaRecorder.start()
        startTimer() // move to after recording start
        return mediaRecorder
    }

    override fun stopRecording() {
        Log.i("AudioRecorder", "stopRecording invoked: isRecording -> $recording")
        if (recording.get()) {
            stopTimer()
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            mediaRecorder = null
            recording.set(false)
        }
    }

    override fun getRecordingDuration(): Int {
        val durationSec = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        return max(durationSec, 0)
    }

    override fun getRecordingAmplitudes(): Array<Int> {
        val totalSamples = amplitudes.size
        val scaleFactor = max(1f, totalSamples / MAX_AMP_LEN.toFloat())
        val outputArray = arrayOfNulls<Int>(MAX_AMP_LEN)
        var outputIndex = 0
        if (scaleFactor <= 1) {
            for (i in 0 until totalSamples) {
                if (outputIndex == MAX_AMP_LEN) break
                outputArray[outputIndex++] = amplitudes[i]
            }
        } else {
            for (i in 0 until totalSamples) {
                if (outputIndex == MAX_AMP_LEN) break
                if (i >= outputIndex * scaleFactor) {
                    outputArray[outputIndex++] = amplitudes[i]
                }
            }
        }
        return if (outputIndex == 0) {
            arrayOf(0)
        } else {
            Arrays.copyOf(outputArray, outputIndex)
        }
    }

    private fun startTimer() {
        if (timer != null) {
            timer?.cancel()
        }
        timer = Timer()
        startTime = System.currentTimeMillis()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                amplitudeIndex++
                val amplitude = mediaRecorder?.maxAmplitude ?: return
                if (amplitudeIndex == 1 && amplitude == 0) {
                    return
                }
                val normAmplitude = amplitude * 160 / 32768f
                val db = amplitudeTodB(normAmplitude)
                amplitudes.add(db)
            }
        }, TIMER_PERIOD, TIMER_PERIOD)
    }

    fun amplitudeTodB(amplitude: Float): Int {
        return clampDecibels(20.0f * log10(abs(amplitude).toDouble()))
    }

    fun clampDecibels(value: Double): Int {
        return max(0.0, min(160.0, value)).toInt()
    }

    private fun stopTimer() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    companion object {
        const val AUDIO_FORMAT = "m4a"
        private const val MAX_TIME = 5 * 60 * 1000
        private const val TIMER_PERIOD = 33L
        private const val MAX_AMP_LEN = 50
    }
}