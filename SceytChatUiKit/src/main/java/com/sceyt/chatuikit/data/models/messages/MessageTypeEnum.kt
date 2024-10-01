package com.sceyt.chatuikit.data.models.messages

enum class MessageTypeEnum(val value: String) {
    Text("text"),
    Media("media"),
    File("file"),
    Link("link"),
    System("system");

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
