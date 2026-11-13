package com.sceyt.chatuikit.presentation.helpers

import android.util.Size
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class ExoPlayerHelperAspectRatioTest {
    private val context = RuntimeEnvironment.getApplication()
    private lateinit var playerView: PlayerView
    private lateinit var helper: ExoPlayerHelper

    @Before
    fun setUp() {
        playerView = PlayerView(context)
        helper = ExoPlayerHelper(context, playerView)
    }

    @After
    fun tearDown() {
        helper.releasePlayer()
    }

    @Test
    fun `content frame fills player view before video track is known`() {
        assertThat(measureContentFrame()).isEqualTo(Size(VIEW_WIDTH, VIEW_HEIGHT))
    }

    @Test
    fun `selected landscape video track fits content frame to video ratio`() {
        helper.onTracksChanged(tracks(videoFormat(width = 4096, height = 2304)))

        assertThat(measureContentFrame()).isEqualTo(Size(1080, 607))
    }

    @Test
    fun `rotated video track fits content frame to display ratio`() {
        helper.onTracksChanged(tracks(videoFormat(width = 1920, height = 1080, rotationDegrees = 90)))

        assertThat(measureContentFrame()).isEqualTo(Size(1080, 1920))
    }

    @Test
    fun `non square pixels are applied to video ratio`() {
        helper.onTracksChanged(tracks(videoFormat(width = 1440, height = 1080, pixelWidthHeightRatio = 4f / 3f)))

        assertThat(measureContentFrame()).isEqualTo(Size(1080, 607))
    }

    @Test
    fun `unselected video track keeps content frame size`() {
        helper.onTracksChanged(tracks(videoFormat(width = 4096, height = 2304), selected = false))

        assertThat(measureContentFrame()).isEqualTo(Size(VIEW_WIDTH, VIEW_HEIGHT))
    }

    @Test
    fun `several selected video formats keep content frame size`() {
        val group = Tracks.Group(
            TrackGroup(videoFormat(width = 1920, height = 1080), videoFormat(width = 1080, height = 1920)),
            true,
            intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
            booleanArrayOf(true, true)
        )

        helper.onTracksChanged(Tracks(listOf(group)))

        assertThat(measureContentFrame()).isEqualTo(Size(VIEW_WIDTH, VIEW_HEIGHT))
    }

    @Test
    fun `single selected format in multi format group fits content frame`() {
        val group = Tracks.Group(
            TrackGroup(videoFormat(width = 1080, height = 1920), videoFormat(width = 1920, height = 1080)),
            true,
            intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
            booleanArrayOf(false, true)
        )

        helper.onTracksChanged(Tracks(listOf(group)))

        assertThat(measureContentFrame()).isEqualTo(Size(1080, 607))
    }

    @Test
    fun `audio only tracks keep content frame size`() {
        val audioFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_AAC)
            .build()

        helper.onTracksChanged(tracks(audioFormat))

        assertThat(measureContentFrame()).isEqualTo(Size(VIEW_WIDTH, VIEW_HEIGHT))
    }

    @Test
    fun `video track without size keeps content frame size`() {
        helper.onTracksChanged(tracks(videoFormat(width = Format.NO_VALUE, height = Format.NO_VALUE)))

        assertThat(measureContentFrame()).isEqualTo(Size(VIEW_WIDTH, VIEW_HEIGHT))
    }

    private fun measureContentFrame(): Size {
        playerView.measure(
            View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT, View.MeasureSpec.EXACTLY)
        )
        playerView.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT)
        val contentFrame = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_content_frame)
        return Size(contentFrame.measuredWidth, contentFrame.measuredHeight)
    }

    private fun videoFormat(
        width: Int,
        height: Int,
        rotationDegrees: Int = 0,
        pixelWidthHeightRatio: Float = 1f,
    ): Format {
        return Format.Builder()
            .setSampleMimeType(MimeTypes.VIDEO_H264)
            .setWidth(width)
            .setHeight(height)
            .setRotationDegrees(rotationDegrees)
            .setPixelWidthHeightRatio(pixelWidthHeightRatio)
            .build()
    }

    private fun tracks(format: Format, selected: Boolean = true): Tracks {
        val group = Tracks.Group(
            TrackGroup(format),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(selected)
        )
        return Tracks(listOf(group))
    }

    private companion object {
        const val VIEW_WIDTH = 1080
        const val VIEW_HEIGHT = 2340
    }
}
