package com.sceyt.chatuikit.media.audio

import android.media.MediaMetadataRetriever
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.VoiceRecorderConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AudioRecorderHelperDurationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var helper: AudioRecorderHelper

    @Before
    fun setUp() {
        SceytChatUIKit.config.voiceRecorderConfig = VoiceRecorderConfig(bitrate = BITRATE)
        helper = AudioRecorderHelper(
            scope = CoroutineScope(Dispatchers.Unconfined),
            context = RuntimeEnvironment.getApplication()
        )
    }

    @Test
    fun `duration comes from the recorded file, not from the timer`() {
        val file = createRecordFile(sizeInBytes = 16_000)
        setFileDuration(file, durationMs = 4_000)

        val duration = helper.resolveDuration(file, recordedDuration = 30)

        assertThat(duration).isEqualTo(4)
    }

    @Test
    fun `unreadable file falls back to the size estimate when the timer is inflated`() {
        val file = createRecordFile(sizeInBytes = 16_000)

        val duration = helper.resolveDuration(file, recordedDuration = 30)

        assertThat(duration).isEqualTo(4)
    }

    @Test
    fun `unreadable file keeps the timer value when it is shorter than the size estimate`() {
        val file = createRecordFile(sizeInBytes = 120_000)

        val duration = helper.resolveDuration(file, recordedDuration = 12)

        assertThat(duration).isEqualTo(12)
    }

    @Test
    fun `missing file keeps the timer value`() {
        val duration = helper.resolveDuration(File("no/such/record.m4a"), recordedDuration = 7)

        assertThat(duration).isEqualTo(7)
    }

    @Test
    fun `null file keeps the timer value`() {
        val duration = helper.resolveDuration(null, recordedDuration = 5)

        assertThat(duration).isEqualTo(5)
    }

    @Test
    fun `sub second recording resolves to zero so it is treated as too short`() {
        val file = createRecordFile(sizeInBytes = 2_000)
        setFileDuration(file, durationMs = 500)

        val duration = helper.resolveDuration(file, recordedDuration = 1)

        assertThat(duration).isEqualTo(0)
    }

    private fun createRecordFile(sizeInBytes: Int): File {
        return temporaryFolder.newFile("record.m4a").apply {
            writeBytes(ByteArray(sizeInBytes))
        }
    }

    private fun setFileDuration(file: File, durationMs: Long) {
        ShadowMediaMetadataRetriever.addMetadata(
            file.absolutePath,
            MediaMetadataRetriever.METADATA_KEY_DURATION,
            durationMs.toString()
        )
    }

    private companion object {
        const val BITRATE = 32_000
    }
}