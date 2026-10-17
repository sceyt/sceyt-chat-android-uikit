package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

internal class AudioPlayerCoordinator(
    private val executor: Executor,
    private val playerFactory: (
        filePath: String,
        messageTid: MessageTid,
        onStateChanged: (AudioPlayerState) -> Unit
    ) -> AudioPlayer
) {
    private val playbackStates = ConcurrentHashMap<String, AudioPlaybackState>()
    private val _state = MutableStateFlow(AudioPlayerState())

    @Volatile
    private var currentPlayer: AudioPlayer? = null

    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    fun init(filePath: String, messageTid: MessageTid) = executor.execute {
        currentPlayer?.let { player ->
            if (player.getFilePath() == filePath && player.getMessageTid() == messageTid) {
                return@execute
            }
            val savedState = savePlaybackState(player)
            player.stop(savedState)
        }

        val player = playerFactory(filePath, messageTid, ::onPlayerStateChanged)
        currentPlayer = player
        _state.value = AudioPlayerState(
            filePath = filePath,
            messageTid = messageTid,
            status = AudioPlayerStatus.Initializing
        )

        if (!player.initialize()) {
            _state.value = _state.value.copy(status = AudioPlayerStatus.Error)
            currentPlayer = null
            return@execute
        }

        val savedState = playbackStates[getStateKey(filePath, messageTid)]
        if (savedState != null && savedState.speed != 1f) {
            player.setPlaybackSpeed(savedState.speed)
        }

        player.play()
        if (savedState != null && savedState.position > 0) {
            player.seekToPosition(savedState.position)
        }
    }

    fun seek(filePath: String?, messageTid: MessageTid, position: Long) {
        executor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.seekToPosition(position)
            }
        }
    }

    fun play() = executor.execute { currentPlayer?.play() }

    fun stop(filePath: String, messageTid: MessageTid) {
        executor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.let { player ->
                    val savedState = savePlaybackState(player)
                    player.stop(savedState)
                }
                currentPlayer = null
            }
        }
    }

    fun stopAll() {
        executor.execute {
            currentPlayer?.let { player ->
                val savedState = savePlaybackState(player)
                player.stop(savedState)
            }
            currentPlayer = null
        }
    }

    fun pause(filePath: String, messageTid: MessageTid) {
        executor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.pause()
            }
        }
    }

    fun pauseAll() = executor.execute { currentPlayer?.pause() }

    fun toggle(filePath: String, messageTid: MessageTid) {
        executor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.togglePlayPause()
            }
        }
    }

    fun setPlaybackSpeed(filePath: String?, messageTid: MessageTid, speed: Float) {
        executor.execute {
            if (isCurrentPlayer(filePath, messageTid)) {
                currentPlayer?.setPlaybackSpeed(speed)
                if (filePath != null) {
                    val currentPosition = currentPlayer?.getPlaybackPosition() ?: 0
                    playbackStates[getStateKey(filePath, messageTid)] =
                        AudioPlaybackState(currentPosition, speed)
                }
            }
        }
    }

    fun getCurrentPlayer(): AudioPlayer? = currentPlayer

    fun isPlaying(path: String?, messageTid: MessageTid): Boolean {
        return state.value.matches(path, messageTid) && state.value.isPlaying
    }

    fun isCurrentPlayer(path: String?, messageTid: MessageTid): Boolean {
        path ?: return false
        return currentPlayer?.getFilePath() == path && currentPlayer?.getMessageTid() == messageTid
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

    private fun onPlayerStateChanged(state: AudioPlayerState) {
        val player = currentPlayer ?: return
        if (player.getFilePath() != state.filePath || player.getMessageTid() != state.messageTid) {
            return
        }
        if (state.status == AudioPlayerStatus.Completed) {
            onPlaybackCompleted(state)
        }
        _state.value = state
    }

    private fun onPlaybackCompleted(state: AudioPlayerState) {
        val filePath = state.filePath ?: return
        val messageTid = state.messageTid ?: return
        val stateKey = getStateKey(filePath, messageTid)
        if (state.speed != 1f) {
            playbackStates[stateKey] = AudioPlaybackState(position = 0, speed = state.speed)
        } else {
            playbackStates.remove(stateKey)
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
}
