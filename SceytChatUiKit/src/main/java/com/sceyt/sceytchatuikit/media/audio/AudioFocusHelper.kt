package com.sceyt.sceytchatuikit.media.audio

import android.content.Context
import android.media.AudioManager
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat

class AudioFocusHelper(private val context: Context) {
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var hasFocus = false
        private set

    private val audioFocusRequestCompat = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT)
        .setOnAudioFocusChangeListener { focusChange ->
            hasFocus = focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
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
}