package com.sceyt.chatuikit.presentation.components.picker.adapter

sealed class MediaItem(val media: MediaData) {
    data class Image(private val data: MediaData) : MediaItem(data)
    data class Video(private val data: MediaData,
                     val duration: Double) : MediaItem(data)

    override fun equals(other: Any?): Boolean {
        return other is MediaItem && other.media == media
    }

    override fun hashCode(): Int {
        return media.hashCode()
    }
}