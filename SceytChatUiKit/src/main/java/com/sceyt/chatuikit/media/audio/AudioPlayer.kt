package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer

interface AudioPlayer {
    // Constructor accepts created File or file path
    fun initialize(): Boolean
    fun play()
    fun pause()
    fun stop()
    fun seekToPosition(position: Long)
    fun togglePlayPause()
    fun addEventListener(event: OnAudioPlayer, tag: String, filePath: String)
    fun setPlaybackSpeed(speed: Float)
    fun getPlaybackSpeed(): Float
    fun getPlaybackPosition(): Long
    fun getAudioDuration(): Long
    fun getFilePath(): String?
    fun isPlaying(): Boolean
}