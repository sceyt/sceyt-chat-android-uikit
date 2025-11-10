package com.sceyt.chatuikit.data.models.messages

enum class MessageTypeEnum(val value: String) {
    Text("text"),
    Media("media"),
    File("file"),
    Link("link"),
    System("system"),
    Poll("poll");

    companion object {
        fun fromValue(value: String): MessageTypeEnum? {
            return when (value) {
                "text" -> Text
                "media" -> Media
                "file" -> File
                "link" -> Link
                "system" -> System
                "poll" -> Poll
                else -> null
            }
        }
    }
}
