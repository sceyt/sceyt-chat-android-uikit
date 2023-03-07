package com.sceyt.sceytchatuikit.data.models.messages

enum class AttachmentTypeEnum {
    Image,
    Video,
    Voice,
    Link,
    File;

    fun value(): String {
        return when (this) {
            Image -> "image"
            Video -> "video"
            Voice -> "voice"
            Link -> "link"
            File -> "file"
        }
    }
}
