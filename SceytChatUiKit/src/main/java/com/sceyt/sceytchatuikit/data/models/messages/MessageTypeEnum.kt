package com.sceyt.sceytchatuikit.data.models.messages

enum class MessageTypeEnum {
    Text,
    Media,
    File,
    Link,
    System;

    fun value(): String {
        return when (this) {
            Text -> "text"
            Media -> "media"
            File -> "file"
            Link -> "link"
            System -> "system"
        }
    }
}
