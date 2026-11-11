package com.sceyt.chatuikit.shared.media_encoder


data class TranscoderConfiguration(
        var quality: VideoQuality = VideoQuality.MEDIUM,
        var frameRate: Int? = null,
        var isMinBitrateCheckEnabled: Boolean = true,
        var videoBitrate: Int? = null,
        var videoBitrateCoefficient: Float? = null,
        var disableAudio: Boolean = false,
        var shortSideThreshold: Int = DEFAULT_SHORT_SIDE_THRESHOLD
) {
    companion object {
        const val DEFAULT_SHORT_SIDE_THRESHOLD = 480
    }
}

enum class VideoQuality {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}