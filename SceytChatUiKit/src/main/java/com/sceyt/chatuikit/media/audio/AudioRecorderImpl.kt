package com.sceyt.chatuikit.media.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.VoiceRecorderDuration
import java.io.File
import java.util.Arrays
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

class AudioRecorderImpl(
        private var context: Context,
        private val file: File,
) : AudioRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private val recording = AtomicBoolean(false)
    private var timer: Timer? = null
    private var startTime: Long = 0
    private var finalDuration: Int = NO_FINAL_DURATION
    private var reachedMaxDurationListener: ReachedMaxDurationListener? = null
    private var errorListener: RecorderErrorListener? = null
    private val amplitudes: ArrayList<Int> = ArrayList()
    private var amplitudeIndex = 0
    private var recorderStarted = false

    override fun startRecording(
            reachedMaxDurationListener: ReachedMaxDurationListener?,
            errorListener: RecorderErrorListener?,
    ): Boolean {
        Log.i(TAG, "startRecording")
        this.reachedMaxDurationListener = reachedMaxDurationListener
        this.errorListener = errorListener
        finalDuration = NO_FINAL_DURATION
        return try {
            initMediaRecorder()
            recording.set(true)
            true
        } catch (ex: Exception) {
            ex.printStackTrace()
            stopTimer()
            startTime = 0
            releaseRecorder()
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun initMediaRecorder() {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
        mediaRecorder = recorder

        val voiceRecorderConfig = SceytChatUIKit.config.voiceRecorderConfig
        if (voiceRecorderConfig.maxDuration is VoiceRecorderDuration.MaxDuration) {
            recorder.setMaxDuration(voiceRecorderConfig.maxDuration.durationInMilliseconds.toInt())
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(file.absolutePath)
        recorder.setAudioChannels(1)
        recorder.setAudioEncodingBitRate(voiceRecorderConfig.bitrate)
        recorder.setAudioSamplingRate(voiceRecorderConfig.simplingRate)
        recorder.setOnInfoListener { _, what, extra ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                Log.i(TAG, "Max duration reached: $extra")
                reachedMaxDurationListener?.onReached(getRecordingDuration())
            }
        }
        recorder.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "Recorder error: what -> $what, extra -> $extra")
            val listener = errorListener
            stopRecording()
            listener?.onError(what, extra)
        }
        recorder.prepare()
        recorder.start()
        recorderStarted = true
        startTimer()
    }

    override fun stopRecording() {
        Log.i(TAG, "stopRecording invoked: isRecording -> ${recording.get()}")
        if (recording.compareAndSet(true, false)) {
            finalDuration = elapsedDuration()
            stopTimer()
            releaseRecorder()
        }
    }

    private fun releaseRecorder() {
        val recorder = mediaRecorder ?: return
        mediaRecorder = null
        val wasStarted = recorderStarted
        recorderStarted = false
        try {
            recorder.setOnInfoListener(null)
            recorder.setOnErrorListener(null)
            if (wasStarted) recorder.stop()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            recorder.release()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun getRecordingDuration(): Int {
        if (finalDuration != NO_FINAL_DURATION)
            return finalDuration
        return elapsedDuration()
    }

    private fun elapsedDuration(): Int {
        if (startTime == 0L) return 0
        val durationSec = ((SystemClock.elapsedRealtime() - startTime) / 1000).toInt()
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

    override fun isRecording(): Boolean {
        return recording.get()
    }

    private fun startTimer() {
        if (timer != null) {
            timer?.cancel()
        }
        timer = Timer()
        startTime = SystemClock.elapsedRealtime()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                amplitudeIndex++
                val amplitude = try {
                    mediaRecorder?.maxAmplitude
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                } ?: return

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
        private const val TAG = "AudioRecorder"
        private const val TIMER_PERIOD = 33L
        private const val MAX_AMP_LEN = 50
        private const val NO_FINAL_DURATION = -1
    }
}
