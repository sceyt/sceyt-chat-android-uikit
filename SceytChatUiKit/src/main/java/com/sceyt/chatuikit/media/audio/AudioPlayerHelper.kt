package com.sceyt.chatuikit.media.audio

import android.util.Log
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class AudioPlaybackState(
    val position: Long,
    val speed: Float
)

object AudioPlayerHelper {
    private const val TAG = "AudioPlayerHelper"
    private val playerExecutor: Executor = Executors.newSingleThreadScheduledExecutor()
    private var currentPlayer: AudioPlayer? = null
    private val playerToggleListeners = ConcurrentHashMap<String, OnToggleCallback>()
    private val playbackStates = ConcurrentHashMap<String, AudioPlaybackState>()

    /** Pending listeners that will be added to the player when it's initialized */
    private val pendingListeners = ConcurrentHashMap<String, OnAudioPlayer>()

    fun init(filePath: String, messageTid: MessageTid, events: OnAudioPlayer, tag: String) {
        playerExecutor.execute {
            currentPlayer?.let { player ->
                if (player.getFilePath() == filePath && player.getMessageTid() == messageTid) {
                    events.onInitialized(
                        alreadyInitialized = true,
                        player = player,
                        filePath = filePath,
                        messageTid = messageTid
                    )
                    currentPlayer?.addEventListener(event = events, tag = tag)
                    return@execute
                }
                // Save current player's state before stopping
                val savedState = savePlaybackState(player)
                (player as? AudioPlayerImpl)?.stop(savedState) ?: player.stop()
            }

            val player = AudioPlayerImpl(filePath, messageTid)
            player.addEventListener(event = events, tag = tag)
            // Add all pending listeners to the new player
            for ((listenerTag, listener) in pendingListeners) {
                player.addEventListener(event = listener, tag = listenerTag)
            }
            player.initialize()
            currentPlayer = player

            // Apply saved state and start playing
            val stateKey = getStateKey(filePath, messageTid)
            val savedState = playbackStates[stateKey]

            if (savedState != null) {
                // Apply saved speed (individual per message)
                if (savedState.speed != 1f) {
                    player.setPlaybackSpeed(savedState.speed)
                }

                player.play()

                if (savedState.position > 0) {
                    player.seekToPosition(savedState.position)
                }
            } else {
                player.play()
            }

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
            // Store as pending listener so it's added when player is initialized
            pendingListeners[tag] = events
            // Also add to current player if exists
            currentPlayer?.addEventListener(events, tag)
        }
    }

    fun removeEventListener(tag: String) {
        playerExecutor.execute {
            pendingListeners.remove(tag)
            currentPlayer?.removeEventListener(tag)
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
                currentPlayer?.let { player -> savePlaybackState(player) }
                currentPlayer?.stop()
                currentPlayer = null
            }
        }
    }

    fun stopAll() {
        playerExecutor.execute {
            currentPlayer?.let { player ->
                savePlaybackState(player)
                player.stop()
            }
            currentPlayer = null
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
                // Save speed so it persists when playback completes
                if (filePath != null) {
                    val stateKey = getStateKey(filePath, messageTid)
                    val currentPosition = currentPlayer?.getPlaybackPosition() ?: 0
                    playbackStates[stateKey] = AudioPlaybackState(currentPosition, speed)
                }
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

    fun getPlaybackState(filePath: String, messageTid: MessageTid): AudioPlaybackState? {
        return playbackStates[getStateKey(filePath, messageTid)]
    }

    fun clearPlaybackState(filePath: String, messageTid: MessageTid) {
        playbackStates.remove(getStateKey(filePath, messageTid))
    }

    fun clearAllPlaybackStates() {
        playbackStates.clear()
    }

    /**
     * Called when playback completes naturally.
     * Resets position to 0 but keeps the speed in saved state, and clears currentPlayer.
     */
    fun onPlaybackCompleted(filePath: String, messageTid: MessageTid) {
        playerExecutor.execute {
            // Clear current player
            currentPlayer = null

            // Reset position to 0 but keep speed
            val stateKey = getStateKey(filePath, messageTid)
            val existingState = playbackStates[stateKey]
            if (existingState != null && existingState.speed != 1f) {
                playbackStates[stateKey] =
                    AudioPlaybackState(position = 0, speed = existingState.speed)
                Log.d(
                    TAG,
                    "Reset position to 0 but kept speed ${existingState.speed} for $stateKey"
                )
            } else {
                // No saved state or speed is 1x, no need to keep it
                playbackStates.remove(stateKey)
                Log.d(
                    TAG,
                    "Removed playback state for $stateKey as speed is default, size: ${playbackStates.size}"
                )
            }
        }
    }

    private fun getStateKey(filePath: String, messageTid: MessageTid): String {
        return "$filePath-$messageTid"
    }

    private fun savePlaybackState(player: AudioPlayer): AudioPlaybackState? {
        val filePath = player.getFilePath() ?: return null
        val state = AudioPlaybackState(
            position = player.getPlaybackPosition(),
            speed = player.getPlaybackSpeed()
        )
        playbackStates[getStateKey(filePath, player.getMessageTid())] = state
        return state
    }

    interface OnAudioPlayer {
        fun onInitialized(
            alreadyInitialized: Boolean,
            player: AudioPlayer,
            filePath: String,
            messageTid: MessageTid
        ){}

        fun onProgress(position: Long, duration: Long, filePath: String, messageTid: MessageTid)
        fun onSeek(position: Long, filePath: String, messageTid: MessageTid) {}
        fun onToggle(playing: Boolean, filePath: String, messageTid: MessageTid)

        /**
         * Called when playback stops.
         * @param savedState non-null if stopped due to switching to another voice message (contains position/speed to restore UI),
         *                   null if playback completed naturally (UI should reset to beginning)
         */
        fun onStop(filePath: String, messageTid: MessageTid, savedState: AudioPlaybackState?)
        fun onPaused(filePath: String, messageTid: MessageTid)
        fun onSpeedChanged(speed: Float, filePath: String, messageTid: MessageTid) {}
        fun onError(filePath: String, messageTid: MessageTid) {}
    }

    fun interface OnToggleCallback {
        fun onToggle()
    }
}