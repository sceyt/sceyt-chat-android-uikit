package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid

enum class AudioPlayerStatus {
    Idle,
    Initializing,
    Playing,
    Paused,
    Stopped,
    Completed,
    Error
}

data class AudioPlayerState(
    val filePath: String? = null,
    val messageTid: MessageTid? = null,
    val status: AudioPlayerStatus = AudioPlayerStatus.Idle,
    val position: Long = 0,
    val duration: Long = 0,
    val speed: Float = 1f
) {
    val isPlaying: Boolean get() = status == AudioPlayerStatus.Playing

    fun matches(filePath: String?, messageTid: MessageTid): Boolean {
        return filePath != null && this.filePath == filePath && this.messageTid == messageTid
    }
}
