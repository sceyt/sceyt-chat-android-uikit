package com.sceyt.chatuikit.data.models.messages

abstract class MessageTypeEnum(val value: String) {
    data object Text : MessageTypeEnum("text")
    data object Media : MessageTypeEnum("media")
    data object File : MessageTypeEnum("file")
    data object Link : MessageTypeEnum("link")
    data object System : MessageTypeEnum("system")
    data object Poll : MessageTypeEnum("poll")
}
