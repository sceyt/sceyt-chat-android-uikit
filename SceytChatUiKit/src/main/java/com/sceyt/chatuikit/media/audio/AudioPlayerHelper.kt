package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object AudioPlayerHelper {
    private val playerExecutor: Executor = Executors.newSingleThreadScheduledExecutor()
    private var currentPlayer: AudioPlayer? = null
    private val playerToggleListeners = ConcurrentHashMap<String, OnToggleCallback>()

    fun init(filePath: String, messageTid: MessageTid, events: OnAudioPlayer, tag: String) {
        playerExecutor.execute {
            currentPlayer?.let {
                if (it.getFilePath() == filePath && it.getMessageTid() == messageTid) {
                    events.onInitialized(
                        alreadyInitialized = true,
                        player = it,
                        filePath = filePath,
                        messageTid = messageTid
                    )
                    currentPlayer?.addEventListener(event = events, tag = tag)
                    return@execute
                }
                it.stop()
            }
            val player = AudioPlayerImpl(filePath, messageTid)
            player.addEventListener(event = events, tag = tag)
            player.initialize()
            currentPlayer = player
            events.onInitialized(
                alreadyInitialized = false,
                player = player,
                filePath = filePath,
                messageTid = messageTid
            )
        }
    }

    fun addEventListener(
        events: OnAudioPlayer,
        tag: String,
    ) {
        playerExecutor.execute {
            currentPlayer?.addEventListener(events, tag)
        }
    }

    fun seek(filePath: String?, messageTid: MessageTid, position: Long) {
        playerExecutor.execute {
            if (isCurrentPlayer(filePath, messageTid))
                currentPlayer?.seekToPosition(position)
        }
    }

    fun play() {
        playerExecutor.execute {
            currentPlayer?.play()
        }
    }

    fun stop(filePath: String, messageTid: MessageTid) {
        playerExecutor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
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

    fun pause(filePath: String, messageTid: MessageTid) {
        playerExecutor.execute {
            if (isCurrentPlayer(filePath, messageTid))
                currentPlayer?.pause()
        }
    }

    fun pauseAll() {
        playerExecutor.execute {
            currentPlayer?.pause()
        }
    }

    fun toggle(filePath: String, messageTid: MessageTid) {
        playerExecutor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.togglePlayPause()
                for (callback in playerToggleListeners.values) {
                    callback.onToggle()
                }
            }
        }
    }

    fun setPlaybackSpeed(filePath: String?, messageTid: MessageTid, speed: Float) {
        playerExecutor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.setPlaybackSpeed(speed)
            }
        }
    }

    fun addToggleCallback(tag: String, callback: OnToggleCallback) {
        playerToggleListeners[tag] = callback
    }

    fun getCurrentPlayer(): AudioPlayer? {
        return currentPlayer
    }

    fun alreadyInitialized(path: String, messageTid: MessageTid): Boolean {
        return isCurrentPlayer(path, messageTid)
    }

    fun isPlaying(path: String?, messageTid: MessageTid): Boolean {
        return isCurrentPlayer(path, messageTid) && currentPlayer?.isPlaying() == true
    }

    fun isCurrentPlayer(path: String?, messageTid: MessageTid): Boolean {
        currentPlayer ?: return false
        path ?: return false
        return currentPlayer?.getFilePath() == path
                && currentPlayer?.getMessageTid() == messageTid
    }

    interface OnAudioPlayer {
        fun onInitialized(
            alreadyInitialized: Boolean,
            player: AudioPlayer,
            filePath: String,
            messageTid: MessageTid
        )

        fun onProgress(position: Long, duration: Long, filePath: String, messageTid: MessageTid)
        fun onSeek(position: Long, filePath: String, messageTid: MessageTid) {}
        fun onToggle(playing: Boolean, filePath: String, messageTid: MessageTid)
        fun onStop(filePath: String, messageTid: MessageTid)
        fun onPaused(filePath: String, messageTid: MessageTid)
        fun onSpeedChanged(speed: Float, filePath: String, messageTid: MessageTid) {}
        fun onError(filePath: String, messageTid: MessageTid) {}
    }

    fun interface OnToggleCallback {
        fun onToggle()
    }
}