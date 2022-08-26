package com.sceyt.sceytchatuikit.data.models.messages

internal enum class MessageTypeEnum {
    Loading,
    Text,
    Deleted,
    SingleVideoOrImage,
    Attachments;

    var incoming = false
}
