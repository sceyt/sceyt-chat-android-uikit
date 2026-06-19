package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid

interface AudioPlayer {
    // Constructor accepts created File or file path
    fun initialize(): Boolean
    fun play()
    fun pause()
    fun stop(savedState: AudioPlaybackState? = null)
    fun seekToPosition(position: Long)
    fun togglePlayPause()
    fun setPlaybackSpeed(speed: Float)
    fun getPlaybackSpeed(): Float
    fun getPlaybackPosition(): Long
    fun getAudioDuration(): Long
    fun getFilePath(): String?
    fun getMessageTid(): MessageTid
    fun isPlaying(): Boolean
}
