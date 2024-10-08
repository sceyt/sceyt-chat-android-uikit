package com.sceyt.chatuikit.persistence.differs

import com.sceyt.chatuikit.presentation.components.picker.adapter.MediaData

data class GalleryMediaItemDiff(
        var filePathChanged: Boolean,
        val checkStateChanged: Boolean
) {

    fun hasDifference(): Boolean {
        return filePathChanged || checkStateChanged
    }

    companion object {
        val DEFAULT = GalleryMediaItemDiff(
            filePathChanged = true,
            checkStateChanged = true
        )
    }
}

fun MediaData.diff(other: MediaData): GalleryMediaItemDiff {
    return GalleryMediaItemDiff(
        filePathChanged = realPath != other.realPath,
        checkStateChanged = selected != other.selected
    )
}