package com.sceyt.chatuikit.shared.media_encoder

import com.abedelazizshe.lightcompressorlibrary.VideoQuality

data class CustomConfiguration(
        var quality: VideoQuality = VideoQuality.MEDIUM,
        var frameRate: Int? = null,
        var isMinBitrateCheckEnabled: Boolean = true,
        var videoBitrate: Int? = null,
        var videoBitrateCoefficient: Float? = null,
        var disableAudio: Boolean = false
)
