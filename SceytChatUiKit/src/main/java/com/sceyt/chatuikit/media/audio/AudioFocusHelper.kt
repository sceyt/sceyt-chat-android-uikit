package com.sceyt.chatuikit.media.audio

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.audio.AudioFocusRequestCompat
import androidx.media3.common.audio.AudioManagerCompat
import androidx.media3.common.util.UnstableApi

@UnstableApi
class AudioFocusHelper(private val context: Context) {
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var listener: ((Boolean) -> Unit)? = null

    var hasFocus = false
        private set

    private val audioFocusRequestCompat = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
        .setOnAudioFocusChangeListener { focusChange ->
            hasFocus = focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            listener?.invoke(hasFocus)
        }
        .build()


    fun requestAudioFocusCompat(): Boolean {
        if (hasFocus) return true
        val result = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequestCompat)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    fun abandonCallAudioFocusCompat() {
        val result = AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequestCompat)
        hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED).not()
    }

    fun setListeners(onAudioFocusChanged: (Boolean) -> Unit) {
        listener = onAudioFocusChanged
    }
}