package com.sceyt.chatuikit.media.audio

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.VoiceRecorderConfig
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSystemClock
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class AudioRecorderImplDurationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        SceytChatUIKit.config.voiceRecorderConfig = VoiceRecorderConfig()
        file = temporaryFolder.newFile("record.m4a")
    }

    @Test
    fun `duration grows with elapsed realtime while recording`() {
        val recorder = startedRecorder()

        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))

        assertThat(recorder.getRecordingDuration()).isEqualTo(5)
    }

    @Test
    fun `duration is frozen at stop and does not grow afterwards`() {
        val recorder = startedRecorder()

        ShadowSystemClock.advanceBy(Duration.ofSeconds(4))
        recorder.stopRecording()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(26))

        assertThat(recorder.getRecordingDuration()).isEqualTo(4)
    }

    @Test
    fun `repeated stop keeps the first frozen duration`() {
        val recorder = startedRecorder()

        ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
        recorder.stopRecording()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(10))
        recorder.stopRecording()

        assertThat(recorder.getRecordingDuration()).isEqualTo(3)
    }

    @Test
    fun `recording flag is cleared after stop`() {
        val recorder = startedRecorder()
        assertThat(recorder.isRecording()).isTrue()

        recorder.stopRecording()

        assertThat(recorder.isRecording()).isFalse()
    }

    @Test
    fun `duration is zero when recording never started`() {
        val recorder = AudioRecorderImpl(RuntimeEnvironment.getApplication(), file)

        ShadowSystemClock.advanceBy(Duration.ofSeconds(9))

        assertThat(recorder.getRecordingDuration()).isEqualTo(0)
        assertThat(recorder.isRecording()).isFalse()
    }

    private fun startedRecorder(): AudioRecorderImpl {
        val recorder = AudioRecorderImpl(RuntimeEnvironment.getApplication(), file)
        val started = recorder.startRecording(
            reachedMaxDurationListener = null,
            errorListener = null
        )
        assertThat(started).isTrue()
        return recorder
    }
}