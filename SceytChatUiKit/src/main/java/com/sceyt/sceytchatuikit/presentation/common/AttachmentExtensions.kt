package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentPayLoadDiff


internal fun SceytAttachment.diff(other: SceytAttachment): AttachmentPayLoadDiff {
    return AttachmentPayLoadDiff(
        filePathChanged = filePath != other.filePath,
        urlChanged = url != other.url,
        progressPercentChanged = progressPercent != other.progressPercent,
    )
}