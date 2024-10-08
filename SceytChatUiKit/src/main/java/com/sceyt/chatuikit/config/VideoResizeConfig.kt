package com.sceyt.chatuikit.config

@Suppress("Unused")
open class VideoResizeConfig(
        val dimensionThreshold: Int,
        val compressionQuality: Int,
        val frameRate: Int,
        val bitrate: Int
) {
    data object Low : VideoResizeConfig(800, 80, 30, 500000)
    data object Medium : VideoResizeConfig(1200, 80, 30, 1000000)
    data object High : VideoResizeConfig(1600, 80, 30, 2000000)
}