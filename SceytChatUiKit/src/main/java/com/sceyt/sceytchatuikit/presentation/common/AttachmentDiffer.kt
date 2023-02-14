package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentPayLoadDiff

fun SceytAttachment.diff(other: SceytAttachment): AttachmentPayLoadDiff {
    return AttachmentPayLoadDiff(
        filePathChanged = filePath != other.filePath,
        urlChanged = url != other.url,
        progressPercentChanged = progressPercent != other.progressPercent,
    )
}

fun SceytAttachment.diffBetweenServerData(other: SceytAttachment): AttachmentPayLoadDiff {
    return AttachmentPayLoadDiff(
        filePathChanged = false,
        urlChanged = url != other.url,
        progressPercentChanged = false
    )
}