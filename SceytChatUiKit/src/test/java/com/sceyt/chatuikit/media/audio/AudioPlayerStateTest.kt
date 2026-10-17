package com.sceyt.chatuikit.media.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioPlayerStateTest {

    @Test
    fun matches_requiresSameFilePathAndMessageTid() {
        val state = AudioPlayerState(
            filePath = "/voice/message.aac",
            messageTid = 42L
        )

        assertThat(state.matches("/voice/message.aac", 42L)).isTrue()
        assertThat(state.matches("/voice/other.aac", 42L)).isFalse()
        assertThat(state.matches("/voice/message.aac", 43L)).isFalse()
        assertThat(state.matches(null, 42L)).isFalse()
    }

    @Test
    fun isPlaying_isTrueOnlyForPlayingStatus() {
        assertThat(AudioPlayerState(status = AudioPlayerStatus.Playing).isPlaying).isTrue()
        assertThat(AudioPlayerState(status = AudioPlayerStatus.Paused).isPlaying).isFalse()
        assertThat(AudioPlayerState(status = AudioPlayerStatus.Completed).isPlaying).isFalse()
    }
}
