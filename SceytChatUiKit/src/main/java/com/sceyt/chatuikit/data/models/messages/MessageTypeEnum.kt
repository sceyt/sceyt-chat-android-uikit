package com.sceyt.chatuikit.data.models.messages

enum class MessageTypeEnum(val value: String) {
    Text("text"),
    Media("media"),
    File("file"),
    Link("link"),
    System("system"),
    Poll("poll");
}
