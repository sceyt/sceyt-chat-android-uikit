package com.sceyt.chatuikit.presentation.components.channel.messages

import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.isCompleted
import com.sceyt.chatuikit.persistence.file_transfer.isError
import com.sceyt.chatuikit.persistence.file_transfer.isPaused

internal object MessageListTransferUpdatePolicy {

    fun shouldApplyToMessageList(transfer: TransferData): Boolean {
        return transfer.state.isCompleted() ||
                transfer.state.isError() ||
                transfer.state.isPaused() ||
                transfer.state == FilePathChanged ||
                isMessageListThumbLoaded(transfer)
    }

    fun shouldUpdateAdapterItem(transfer: TransferData): Boolean {
        return when (transfer.state) {
            Downloading, Uploading, Preparing, WaitingToUpload, PendingUpload, PendingDownload -> false
            else -> true
        }
    }

    fun isMessageListThumbLoaded(transfer: TransferData): Boolean {
        return transfer.state == ThumbLoaded && transfer.thumbData?.key == ThumbFor.MessagesLisView.value
    }
}
