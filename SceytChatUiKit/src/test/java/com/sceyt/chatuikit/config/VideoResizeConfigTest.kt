package com.sceyt.chatuikit.config

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.shared.media_encoder.CompressorUtils
import com.sceyt.chatuikit.shared.media_encoder.TranscoderConfiguration
import com.sceyt.chatuikit.shared.media_encoder.VideoQuality
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.roundToInt

/**
 * Locks the values the transcoder used before video resizing became configurable, so the
 * defaults keep producing byte-identical output.
 */
@RunWith(RobolectricTestRunner::class)
class VideoResizeConfigTest {

    @Test
    fun `default config keeps the previously hardcoded transcode settings`() {
        val config = VideoResizeConfig()

        assertThat(config.shortSideThreshold).isEqualTo(LEGACY_SHORT_SIDE)
        assertThat(config.bitrateCoefficient).isEqualTo(LEGACY_BITRATE_COEFFICIENT)
        assertThat(config.bitrate).isNull()
        assertThat(config.frameRate).isNull()
    }

    @Test
    fun `medium preset is the default config`() {
        val medium = VideoResizeConfig.Medium

        assertThat(medium.shortSideThreshold).isEqualTo(LEGACY_SHORT_SIDE)
        assertThat(medium.bitrateCoefficient).isEqualTo(LEGACY_BITRATE_COEFFICIENT)
        assertThat(medium.bitrate).isNull()
        assertThat(medium.frameRate).isNull()
        assertThat(medium.quality).isEqualTo(VideoQuality.MEDIUM)
    }

    @Test
    fun `config used by the kit defaults to the medium preset`() {
        assertThat(SceytChatUIKitConfig().videoAttachmentResizeConfig)
            .isEqualTo(VideoResizeConfig.Medium)
    }

    @Test
    fun `transcoder configuration defaults to the legacy short side`() {
        assertThat(TranscoderConfiguration().shortSideThreshold).isEqualTo(LEGACY_SHORT_SIDE)
    }

    @Test
    fun `default threshold reproduces the previously hardcoded output size`() {
        listOf(
            1920.0 to 1080.0,
            1080.0 to 1920.0,
            1280.0 to 720.0,
            640.0 to 640.0,
        ).forEach { (width, height) ->
            assertThat(CompressorUtils.generateWidthAndHeight(width, height))
                .isEqualTo(legacyWidthAndHeight(width, height))
        }
    }

    @Test
    fun `threshold scales the shorter side`() {
        val (width, height) = CompressorUtils.generateWidthAndHeight(
            width = 1920.0,
            height = 1080.0,
            shortSideThreshold = 720,
        )

        assertThat(height).isEqualTo(720)
        assertThat(width).isEqualTo(1280)
    }

    @Test
    fun `low threshold allows resizing videos below the legacy threshold`() {
        assertThat(CompressorUtils.shouldResizeVideo(720.0, 400.0, 360)).isTrue()
    }

    @Test
    fun `high threshold does not upscale smaller videos`() {
        assertThat(CompressorUtils.shouldResizeVideo(1280.0, 600.0, 720)).isFalse()
    }

    private fun legacyWidthAndHeight(width: Double, height: Double): Pair<Int, Int> {
        return if (width > height) {
            Pair((LEGACY_SHORT_SIDE * width / height / 16).roundToInt() * 16, LEGACY_SHORT_SIDE)
        } else {
            Pair(LEGACY_SHORT_SIDE, (LEGACY_SHORT_SIDE * height / width / 16).roundToInt() * 16)
        }
    }

    private companion object {
        const val LEGACY_SHORT_SIDE = 480
        const val LEGACY_BITRATE_COEFFICIENT = 0.09f
    }
}
