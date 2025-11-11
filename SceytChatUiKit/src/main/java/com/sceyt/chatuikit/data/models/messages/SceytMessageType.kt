package com.sceyt.chatuikit.data.models.messages

abstract class SceytMessageType(
    val value: String
) {
    data object Text : SceytMessageType("text")
    data object Media : SceytMessageType("media")
    data object File : SceytMessageType("file")
    data object Link : SceytMessageType("link")
    data object System : SceytMessageType("system")
    data object Poll : SceytMessageType("poll")
    data class Unsupported(val type: String) : SceytMessageType(type)

    open fun getFromString(type: String): SceytMessageType {
        return when (type) {
            Text.value -> Text
            Media.value -> Media
            File.value -> File
            Link.value -> Link
            System.value -> System
            Poll.value -> Poll
            else -> Unsupported(type)
        }
    }
}