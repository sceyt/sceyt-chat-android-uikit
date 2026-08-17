package com.sceyt.chatuikit.config

import com.sceyt.chatuikit.shared.media_encoder.VideoQuality

/**
 * Controls how a video attachment is transcoded before upload.
 *
 * The output bitrate is resolved in order: [bitrate], then [bitrateCoefficient], then [quality].
 * Only the first non-null one applies, so leave the earlier ones null to use a later one.
 *
 * @property shortSideThreshold target size of the shorter side, in pixels. The longer side keeps
 * the source aspect ratio, rounded to a multiple of 16.
 * @property quality preset share of the source bitrate. Applies only when both [bitrate] and
 * [bitrateCoefficient] are null.
 * @property frameRate output frame rate, or null to keep the source one.
 * @property bitrate absolute output bitrate in bits per second, or null to derive it.
 * @property bitrateCoefficient share of the source bitrate to keep, or null to derive it
 * from [quality].
 */
@Suppress("Unused")
open class VideoResizeConfig(
    val shortSideThreshold: Int = DEFAULT_SHORT_SIDE_THRESHOLD,
    val quality: VideoQuality = VideoQuality.MEDIUM,
    val frameRate: Int? = null,
    val bitrate: Int? = null,
    val bitrateCoefficient: Float? = DEFAULT_BITRATE_COEFFICIENT,
) {
    data object Low : VideoResizeConfig(
        shortSideThreshold = 360,
        quality = VideoQuality.LOW,
        bitrateCoefficient = 0.05f,
    )

    data object Medium : VideoResizeConfig()

    data object High : VideoResizeConfig(
        shortSideThreshold = 720,
        quality = VideoQuality.HIGH,
        bitrateCoefficient = 0.2f,
    )

    companion object {
        const val DEFAULT_SHORT_SIDE_THRESHOLD = 480
        const val DEFAULT_BITRATE_COEFFICIENT = 0.09f
    }
}