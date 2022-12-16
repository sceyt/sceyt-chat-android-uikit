package com.sceyt.sceytchatuikit.data.models.messages

enum class AttachmentTypeEnum {
    Image,
    Video,
    File;

    fun value(): String {
        return when (this) {
            Image -> "image"
            Video -> "video"
            File -> "file"
        }
    }
}
