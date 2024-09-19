package com.sceyt.chatuikit.config

@Suppress("Unused")
open class ResizeConfig(
        val dimensionThreshold: Int,
        val compressionQuality: Int
) {
    data object Low : ResizeConfig(720, 80)
    data object Medium : ResizeConfig(1080, 80)
    data object High : ResizeConfig(1600, 90)
}