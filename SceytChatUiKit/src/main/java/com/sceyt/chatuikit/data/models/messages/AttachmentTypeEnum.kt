package com.sceyt.chatuikit.data.models.messages

enum class AttachmentTypeEnum(val value: String) {
    Image("image"),
    Video("video"),
    Voice("voice"),
    Link("link"),
    File ("file"),
    /** Preview image of the message's video, sent as its own attachment and not displayed as a separate item. */
    Preview("preview");
}
