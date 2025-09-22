package com.sceyt.chatuikit.config

import kotlin.time.Duration.Companion.minutes

sealed class VoiceRecorderDuration {
    data object Unlimited : VoiceRecorderDuration()
    data class MaxDuration(val durationInMilliseconds: Long) : VoiceRecorderDuration()
}

data class VoiceRecorderConfig(
        val maxDuration: VoiceRecorderDuration = VoiceRecorderDuration.MaxDuration(5.minutes.inWholeMilliseconds),
        val bitrate: Int = 32000,
        val simplingRate: Int = 16000,
)