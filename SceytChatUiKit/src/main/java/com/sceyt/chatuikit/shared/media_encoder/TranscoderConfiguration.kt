package com.sceyt.chatuikit.shared.media_encoder


data class TranscoderConfiguration(
        var quality: VideoQuality = VideoQuality.MEDIUM,
        var frameRate: Int? = null,
        var isMinBitrateCheckEnabled: Boolean = true,
        var videoBitrate: Int? = null,
        var videoBitrateCoefficient: Float? = null,
        var disableAudio: Boolean = false
)

enum class VideoQuality {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}