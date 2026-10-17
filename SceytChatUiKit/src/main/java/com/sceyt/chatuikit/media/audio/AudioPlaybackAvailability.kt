package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.isCompleted
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider

internal fun AttachmentDataProvider.isAudioPlaybackAvailable(): Boolean {
    val transferState = transferData?.state ?: attachment.transferState
    return isAudioPlaybackAvailable(filePath, transferState)
}

internal fun isAudioPlaybackAvailable(
    filePath: String?,
    transferState: TransferState?
): Boolean {
    return !filePath.isNullOrBlank() && transferState?.isCompleted() == true
}
