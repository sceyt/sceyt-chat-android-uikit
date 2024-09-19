package com.sceyt.chatuikit.config

@Suppress("Unused")
enum class VideoResizeConfig(
        val dimensionThreshold: Int,
        val compressionQuality: Int,
        val frameRate: Int,
        val bitrate: Int

) {
    Low(800, 80, 30, 500000),
    Medium(1200, 80, 30, 1000000),
    High(1600, 80, 30, 2000000)
}