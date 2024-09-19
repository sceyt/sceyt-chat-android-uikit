package com.sceyt.chatuikit.config

@Suppress("Unused")
enum class ResizeConfig(
        val dimensionThreshold: Int,
        val compressionQuality: Int
) {
    Low(720, 80),
    Medium(1080, 80),
    High(1600, 90)
}