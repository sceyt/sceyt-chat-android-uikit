package com.sceyt.sceytchatuikit.imagepicker.adapter

import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker.MediaModel

sealed class MediaItem(val media: MediaModel) {
    data class Image(private val data: MediaModel) : MediaItem(data)
    data class Video(private val data: MediaModel,
                     val duration: Double) : MediaItem(data)

    override fun equals(other: Any?): Boolean {
        return other is MediaItem && other.media == media
    }

    override fun hashCode(): Int {
        return media.hashCode()
    }
}