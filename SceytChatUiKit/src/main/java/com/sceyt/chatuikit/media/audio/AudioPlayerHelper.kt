package com.sceyt.chatuikit.media.audio

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object AudioPlayerHelper {
    private val playerExecutor: Executor = Executors.newSingleThreadScheduledExecutor()
    private var currentPlayer: AudioPlayer? = null
    private val playerToggleListeners = ConcurrentHashMap<String, OnToggleCallback>()

    fun init(filePath: String, events: OnAudioPlayer, tag: String) {
        playerExecutor.execute {
            currentPlayer?.let {
                if (it.getFilePath() == filePath) {
                    events.onInitialized(true, it, filePath)
                    currentPlayer?.addEventListener(events, tag, filePath)
                    return@execute
                }
                it.stop()
            }
            val player = AudioPlayerImpl(filePath)
            player.addEventListener(events, tag, filePath)
            player.initialize()
            currentPlayer = player
            events.onInitialized(false, player, filePath)
        }
    }

    fun addEventListener(events: OnAudioPlayer, tag: String, filePath: String) {
        playerExecutor.execute {
            currentPlayer?.addEventListener(events, tag, filePath)
        }
    }

    fun seek(filePath: String?, position: Long) {
        playerExecutor.execute {
            if (currentPlayer?.getFilePath() == filePath)
                currentPlayer?.seekToPosition(position)
        }
    }

    fun play() {
        playerExecutor.execute {
            currentPlayer?.play()
        }
    }

    fun stop(filePath: String) {
        playerExecutor.execute {
            if (currentPlayer?.getFilePath() == filePath) {
                currentPlayer?.stop()
                currentPlayer = null
            }
        }
    }

    fun stopAll() {
        playerExecutor.execute {
            if (currentPlayer != null) {
                currentPlayer?.stop()
                currentPlayer = null
            }
        }
    }

    fun pause(filePath: String) {
        playerExecutor.execute {
            if (currentPlayer?.getFilePath() == filePath)
                currentPlayer?.pause()
        }
    }

    fun pauseAll() {
        playerExecutor.execute {
            currentPlayer?.pause()
        }
    }

    fun toggle(filePath: String) {
        playerExecutor.execute {
            if (currentPlayer?.getFilePath() == filePath) {
                currentPlayer?.togglePlayPause()
                for (callback in playerToggleListeners.values) {
                    callback.onToggle()
                }
            }
        }
    }

    fun setPlaybackSpeed(filePath: String?, speed: Float) {
        playerExecutor.execute {
            if (currentPlayer?.getFilePath().equals(filePath)) {
                currentPlayer?.setPlaybackSpeed(speed)
            }
        }
    }

    fun addToggleCallback(tag: String, callback: OnToggleCallback) {
        playerToggleListeners[tag] = callback
    }

    fun getCurrentPlayingAudioPath(): String? {
        return currentPlayer?.getFilePath()
    }

    fun getCurrentPlayer(): AudioPlayer? {
        return currentPlayer
    }

    fun alreadyInitialized(path: String): Boolean {
        currentPlayer ?: return false
        return currentPlayer?.getFilePath().equals(path)
    }

    fun isPlaying(path: String?): Boolean {
        currentPlayer ?: return false
        path ?: return false
        return currentPlayer?.getFilePath().equals(path) && currentPlayer?.isPlaying() == true
    }

    interface OnAudioPlayer {
        fun onInitialized(alreadyInitialized: Boolean, player: AudioPlayer, filePath: String)
        fun onProgress(position: Long, duration: Long, filePath: String)
        fun onSeek(position: Long, filePath: String) {}
        fun onToggle(playing: Boolean, filePath: String)
        fun onStop(filePath: String)
        fun onPaused(filePath: String)
        fun onSpeedChanged(speed: Float, filePath: String)
        fun onError(filePath: String) {}
    }

    fun interface OnToggleCallback {
        fun onToggle()
    }
}