package com.sceyt.chatuikit.media.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

class AudioPlayerImpl(
    private val filePath: String,
    private val messageTid: MessageTid
) : AudioPlayer {
    private val player: MediaPlayer = MediaPlayer()
    private var startTime: Long = 0
    private var timer: Timer? = null
    private val events = ConcurrentHashMap<String, ConcurrentHashMap<String, OnAudioPlayer>>()
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

            player.setOnSeekCompleteListener {
                for (event in getEvents())
                    event.value.onSeek(
                        position = player.currentPosition.toLong(),
                        filePath = filePath,
                        messageTid = messageTid
                    )
            }

            player.setOnCompletionListener {
                stopTimer()
                stopped = true
                seekToPosition(0)
                // Clear currentPlayer and update saved state (keep speed if not default)
                AudioPlayerHelper.onPlaybackCompleted(filePath, messageTid)
                for (event in getEvents())
                    event.value.onStop(
                        filePath = filePath,
                        messageTid = messageTid,
                        savedState = null
                    )
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
        for (event in getEvents())
            event.value.onToggle(player.isPlaying, filePath, messageTid)
    }

    override fun pause() {
        player.pause()
        stopTimer()
        for (event in getEvents())
            event.value.onPaused(filePath, messageTid)
    }

    override fun stop(savedState: AudioPlaybackState?) {
        stopTimer()
        player.stop()
        stopped = true
        for (event in getEvents())
            event.value.onStop(filePath, messageTid, savedState)
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

    override fun getMessageTid(): MessageTid {
        return messageTid
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

        for (event in getEvents())
            event.value.onToggle(
                playing = player.isPlaying,
                filePath = filePath,
                messageTid = messageTid
            )
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (speed !in 0.5f..2f) return
        playbackSpeed = speed
        if (player.audioSessionId > 0) {
            val isPlaying = player.isPlaying
            player.playbackParams = player.playbackParams.setSpeed(speed)
            if (!isPlaying) pause()
            for (event in getEvents())
                event.value.onSpeedChanged(
                    speed = speed,
                    filePath = filePath,
                    messageTid = messageTid
                )
        }
    }

    override fun getPlaybackSpeed(): Float {
        return playbackSpeed
    }

    override fun addEventListener(event: OnAudioPlayer, tag: String) {
        addToEvents(tag, event)
    }

    override fun removeEventListener(tag: String) {
        events[filePath]?.remove(tag)
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        startTime = System.currentTimeMillis()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val currentPosition = player.currentPosition
                for (event in getEvents())
                    event.value.onProgress(
                        position = currentPosition.toLong(),
                        duration = player.duration.toLong(),
                        filePath = filePath,
                        messageTid = messageTid
                    )
            }
        }, TIMER_PERIOD, TIMER_PERIOD)
    }

    private fun stopTimer() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    private fun addToEvents(tag: String, event: OnAudioPlayer) {
        val events = getEvents()
        events[tag] = event
        this.events[filePath] = events
    }

    private fun getEvents(): ConcurrentHashMap<String, OnAudioPlayer> {
        val events = events[filePath]
        return events ?: ConcurrentHashMap()
    }

    companion object {
        private const val TIMER_PERIOD = 33L
    }
}