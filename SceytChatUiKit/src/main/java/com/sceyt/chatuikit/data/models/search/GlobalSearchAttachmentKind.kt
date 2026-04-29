package com.sceyt.chatuikit.data.models.search

import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum

enum class GlobalSearchAttachmentKind {
    Media, File, Voice, Link
}

internal fun GlobalSearchAttachmentKind.toAttachmentTypes(): List<String> = when (this) {
    GlobalSearchAttachmentKind.Media -> listOf(
        AttachmentTypeEnum.Image.value,
        AttachmentTypeEnum.Video.value
    )
    GlobalSearchAttachmentKind.File -> listOf(AttachmentTypeEnum.File.value)
    GlobalSearchAttachmentKind.Voice -> listOf(AttachmentTypeEnum.Voice.value)
    GlobalSearchAttachmentKind.Link -> listOf(AttachmentTypeEnum.Link.value)
}
