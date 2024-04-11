package com.sceyt.chatuikit.media.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.presentation.common.ConcurrentHashSet
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

class AudioPlayerImpl(private val filePath: String) : AudioPlayer {
    private val player: MediaPlayer = MediaPlayer()
    private var startTime: Long = 0
    private var timer: Timer? = null
    private val events = ConcurrentHashMap<String, ConcurrentHashSet<Pair<String, OnAudioPlayer>>>()
    private var stopped = false
    private var playbackSpeed = 1f

    init {
        //addToEvents(filePath, tag, events)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        player.setAudioAttributes(audioAttributes)
    }

    override fun initialize(): Boolean {
        try {
            player.setDataSource(filePath)
            player.setOnPreparedListener {
                for (event in getEvents(filePath))
                    event.second.onProgress(player.currentPosition.toLong(), player.duration.toLong(), filePath)
            }

            player.setOnSeekCompleteListener {
                for (event in getEvents(filePath))
                    event.second.onSeek(player.currentPosition.toLong(), filePath)
            }

            player.setOnCompletionListener {
                stopTimer()
                stopped = true
                seekToPosition(0)
                for (event in getEvents(filePath))
                    event.second.onStop(filePath)
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
        player.start()
        startTimer()
        for (event in getEvents(filePath))
            event.second.onToggle(player.isPlaying, filePath)
    }

    override fun pause() {
        player.pause()
        stopTimer()
        for (event in getEvents(filePath))
            event.second.onPaused(filePath)
    }

    override fun stop() {
        stopTimer()
        player.stop()
        stopped = true
        for (event in getEvents(filePath))
            event.second.onStop(filePath)
    }

    override fun getPlaybackPosition(): Long {
        return player.currentPosition.toLong()
    }

    override fun getAudioDuration(): Long {
        return player.duration.toLong()
    }

    override fun getFilePath(): String {
        return filePath
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

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
        if (player.isPlaying)
            pause()
        else if (startTime > 0 && !stopped) {
            player.start()
            startTimer()
        } else play()

        for (event in getEvents(filePath))
            event.second.onToggle(player.isPlaying, filePath)
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (speed < 0.5f || speed > 2f) return
        playbackSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (player.audioSessionId > 0) {
                val isPlaying = player.isPlaying
                player.playbackParams = player.playbackParams.setSpeed(speed)
                if (!isPlaying) pause()
                for (event in getEvents(filePath))
                    event.second.onSpeedChanged(speed, filePath)
            }
        }
    }

    override fun getPlaybackSpeed(): Float {
        return playbackSpeed
    }

    override fun addEventListener(event: OnAudioPlayer, tag: String, filePath: String) {
        addToEvents(filePath, tag, event)
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        startTime = System.currentTimeMillis()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val currentPosition = player.currentPosition
                for (event in getEvents(filePath))
                    event.second.onProgress(currentPosition.toLong(), player.duration.toLong(), filePath)
            }
        }, TIMER_PERIOD, TIMER_PERIOD)
    }

    private fun stopTimer() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    private fun addToEvents(filePath: String, tag: String, event: OnAudioPlayer) {
        var events = events[filePath]
        if (events == null) {
            events = ConcurrentHashSet()
        }
        events.add(Pair(tag, event))
        this.events[filePath] = events
    }

    private fun getEvents(filePath: String): ConcurrentHashSet<Pair<String, OnAudioPlayer>> {
        val events = events[filePath]
        return events ?: ConcurrentHashSet()
    }

    companion object {
        private const val TIMER_PERIOD = 33L
    }
}