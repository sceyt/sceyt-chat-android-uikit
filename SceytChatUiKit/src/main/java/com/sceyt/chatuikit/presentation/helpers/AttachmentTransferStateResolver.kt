package com.sceyt.chatuikit.presentation.helpers

import android.util.Size
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider

internal fun <T : AttachmentDataProvider> T.applyLatestTransferState(
    thumbFor: ThumbFor? = null,
    thumbSize: Size? = null,
): T {
    AttachmentTransferStateStore.getTransferData(attachment)?.let { transferData ->
        updateAttachment(AttachmentTransferStateStore.getUpdatedAttachment(attachment, transferData))
        updateTransferData(transferData)
    }

    thumbFor?.let {
        AttachmentTransferStateStore.getThumbPath(attachment, it, thumbSize)?.let(::updateThumbPath)
    }

    return this
}
