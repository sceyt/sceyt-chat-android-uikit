package com.sceyt.chatuikit.media.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class AudioPlayerImpl(
    private val filePath: String,
    private val messageTid: MessageTid,
    private val onStateChanged: (AudioPlayerState) -> Unit
) : AudioPlayer {
    private val player: MediaPlayer = MediaPlayer()
    private var startTime: Long = 0
    private var timer: Timer? = null
    private var stopped = false
    private var playbackSpeed = 1f
    private var status = AudioPlayerStatus.Initializing

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        player.setAudioAttributes(audioAttributes)
    }

    override fun initialize(): Boolean {
        try {
            player.setDataSource(filePath)
            player.setOnSeekCompleteListener { emitState() }
            player.setOnCompletionListener {
                stopTimer()
                stopped = true
                seekToPosition(0)
                status = AudioPlayerStatus.Completed
                emitState(position = 0)
            }
            player.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun play() {
        startTime = System.currentTimeMillis()
        stopped = false
        player.start()
        startTimer()
        status = AudioPlayerStatus.Playing
        emitState()
    }

    override fun pause() {
        player.pause()
        stopTimer()
        status = AudioPlayerStatus.Paused
        emitState()
    }

    override fun stop(savedState: AudioPlaybackState?) {
        stopTimer()
        val duration = player.duration.toLong()
        player.stop()
        stopped = true
        status = AudioPlayerStatus.Stopped
        emitState(
            position = savedState?.position ?: 0,
            duration = duration
        )
    }

    override fun getPlaybackPosition(): Long = player.currentPosition.toLong()

    override fun getAudioDuration(): Long = player.duration.toLong()

    override fun getFilePath(): String = filePath

    override fun getMessageTid(): MessageTid = messageTid

    override fun isPlaying(): Boolean = player.isPlaying

    override fun seekToPosition(position: Long) {
        val wasPlaying = player.isPlaying
        if (wasPlaying) {
            stopTimer()
            player.pause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            player.seekTo(position, MediaPlayer.SEEK_CLOSEST)
        } else {
            player.seekTo(position.toInt())
        }
        if (wasPlaying) {
            startTimer()
            player.start()
        }
    }

    override fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else if (startTime > 0 && !stopped) {
            player.start()
            startTimer()
            status = AudioPlayerStatus.Playing
            emitState()
        } else {
            play()
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (speed !in 0.5f..2f) return
        playbackSpeed = speed
        if (player.audioSessionId > 0) {
            val isPlaying = player.isPlaying
            player.playbackParams = player.playbackParams.setSpeed(speed)
            if (!isPlaying) pause() else emitState()
        }
    }

    override fun getPlaybackSpeed(): Float = playbackSpeed

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        startTime = System.currentTimeMillis()
        timer?.schedule(object : TimerTask() {
            override fun run() = emitState()
        }, TIMER_PERIOD, TIMER_PERIOD)
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun emitState(
        position: Long? = null,
        duration: Long? = null
    ) {
        val resolvedPosition = position ?: try {
            player.currentPosition.toLong()
        } catch (_: IllegalStateException) {
            return
        }
        val resolvedDuration = duration ?: try {
            player.duration.toLong()
        } catch (_: IllegalStateException) {
            return
        }
        onStateChanged(
            AudioPlayerState(
                filePath = filePath,
                messageTid = messageTid,
                status = status,
                position = resolvedPosition,
                duration = resolvedDuration,
                speed = playbackSpeed
            )
        )
    }

    companion object {
        private const val TIMER_PERIOD = 33L
    }
}
