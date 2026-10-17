package com.sceyt.chatuikit.extensions

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.media.audio.AudioPlayerImpl
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class MediaDataSourceAndroidTest {

    private lateinit var context: Context
    private lateinit var dir: File
    private lateinit var colonFile: File
    private lateinit var plainFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dir = File(context.filesDir, "media_data_source_test").apply { mkdirs() }
        colonFile = File(dir, "2026-10-03 13:46:40_8396c32cdcf95549_0.wav").apply { writeWav() }
        plainFile = File(dir, "plain.wav").apply { writeWav() }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun platformRetrieverStringSourceFailsForPathWithColon() {
        val retriever = MediaMetadataRetriever()

        val result = runCatching { retriever.setDataSource(colonFile.absolutePath) }
        retriever.release()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun retrieverReadsDurationForPathWithColon() {
        val retriever = MediaMetadataRetriever()

        retriever.setDataSourceFromPath(colonFile.absolutePath)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()

        assertThat(duration?.toLongOrNull()).isGreaterThan(0L)
    }

    @Test
    fun retrieverReadsDurationForPlainPath() {
        val retriever = MediaMetadataRetriever()

        retriever.setDataSourceFromPath(plainFile.absolutePath)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()

        assertThat(duration?.toLongOrNull()).isGreaterThan(0L)
    }

    @Test
    fun getVideoDurationReadsPathWithColon() {
        val duration = FileResizeUtil.getVideoDuration(context, colonFile.absolutePath)

        assertThat(duration).isNotNull()
        assertThat(duration).isGreaterThan(0L)
    }

    @Test
    fun getVideoDurationReadsContentUri() {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", plainFile)

        val duration = FileResizeUtil.getVideoDuration(context, uri.toString())

        assertThat(duration).isNotNull()
        assertThat(duration).isGreaterThan(0L)
    }

    @Test
    fun getVideoSizeOpensPathWithColon() {
        assertThat(FileResizeUtil.getVideoSize(colonFile.absolutePath)).isNotNull()
        assertThat(FileResizeUtil.getVideoSizeOriented(colonFile.absolutePath)).isNotNull()
    }

    @Test
    fun platformPlayerStringSourceFailsForPathWithColon() {
        val player = MediaPlayer()

        val result = runCatching {
            player.setDataSource(colonFile.absolutePath)
            player.prepare()
        }
        player.release()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun playerPreparesPathWithColon() {
        val player = MediaPlayer()

        player.setDataSourceFromPath(colonFile.absolutePath)
        player.prepare()
        val duration = player.duration
        player.release()

        assertThat(duration).isGreaterThan(0)
    }

    @Test
    fun audioPlayerInitializesPathWithColon() {
        val audioPlayer = AudioPlayerImpl(colonFile.absolutePath, 1L) {}

        val initialized = audioPlayer.initialize()
        val duration = audioPlayer.getAudioDuration()
        audioPlayer.stop()

        assertThat(initialized).isTrue()
        assertThat(duration).isGreaterThan(0L)
    }

    private fun File.writeWav() {
        val sampleRate = 8000
        val dataSize = sampleRate * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2)
            .putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(dataSize)
        writeBytes(buffer.array())
    }
}
