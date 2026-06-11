package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class AudioPlaybackState(
    val position: Long,
    val speed: Float
)

object AudioPlayerHelper {
    private val playerExecutor: Executor = Executors.newSingleThreadScheduledExecutor()
    private val coordinator = AudioPlayerCoordinator(playerExecutor, ::AudioPlayerImpl)

    val state: StateFlow<AudioPlayerState> = coordinator.state

    fun init(filePath: String, messageTid: MessageTid) = coordinator.init(filePath, messageTid)

    fun seek(filePath: String?, messageTid: MessageTid, position: Long) =
        coordinator.seek(filePath, messageTid, position)

    fun play() = coordinator.play()

    fun stop(filePath: String, messageTid: MessageTid) = coordinator.stop(filePath, messageTid)

    fun stopAll() = coordinator.stopAll()

    fun pause(filePath: String, messageTid: MessageTid) = coordinator.pause(filePath, messageTid)

    fun pauseAll() = coordinator.pauseAll()

    fun toggle(filePath: String, messageTid: MessageTid) = coordinator.toggle(filePath, messageTid)

    fun setPlaybackSpeed(filePath: String?, messageTid: MessageTid, speed: Float) =
        coordinator.setPlaybackSpeed(filePath, messageTid, speed)

    fun getCurrentPlayer(): AudioPlayer? = coordinator.getCurrentPlayer()

    fun alreadyInitialized(path: String, messageTid: MessageTid): Boolean {
        return isCurrentPlayer(path, messageTid)
    }

    fun isPlaying(path: String?, messageTid: MessageTid): Boolean {
        return coordinator.isPlaying(path, messageTid)
    }

    fun isCurrentPlayer(path: String?, messageTid: MessageTid): Boolean {
        return coordinator.isCurrentPlayer(path, messageTid)
    }

    fun getPlaybackState(filePath: String, messageTid: MessageTid): AudioPlaybackState? {
        return coordinator.getPlaybackState(filePath, messageTid)
    }

    fun clearPlaybackState(filePath: String, messageTid: MessageTid) {
        coordinator.clearPlaybackState(filePath, messageTid)
    }

    fun clearAllPlaybackStates() {
        coordinator.clearAllPlaybackStates()
    }
}
