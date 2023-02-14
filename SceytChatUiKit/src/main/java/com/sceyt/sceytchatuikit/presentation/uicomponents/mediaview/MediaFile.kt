package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

class MediaFile(
        val id: Long,
        val title: String,
        val path: String,
        val type: FileType,
        val dateString: String,
) : java.io.Serializable

enum class FileType {
    Image,
    Video,
}
