package com.sceyt.sceytchatuikit.data.models.messages

enum class AttachmentTypeEnum {
    Image,
    Video,
    Voice,
    File;

    fun value(): String {
        return when (this) {
            Image -> "image"
            Video -> "video"
            Voice -> "voice"
            File -> "file"
        }
    }
}
