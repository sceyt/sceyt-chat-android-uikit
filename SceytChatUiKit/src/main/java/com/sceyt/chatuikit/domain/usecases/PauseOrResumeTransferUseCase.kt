package com.sceyt.chatuikit.domain.usecases

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PauseOrResumeTransferUseCase(
    private val context: Context,
    private val fileTransferService: FileTransferService,
) {

    suspend operator fun invoke(
        attachment: SceytAttachment,
        channelId: Long? = null
    ) = withContext(Dispatchers.IO) {
        when (val state = attachment.transferState ?: return@withContext ) {
            TransferState.PendingUpload, TransferState.ErrorUpload -> {
                UploadAndSendAttachmentWorkManager.schedule(
                    context = context,
                    messageTid = attachment.messageTid,
                    channelId = channelId
                )
            }

            TransferState.PendingDownload, TransferState.ErrorDownload -> {
                fileTransferService.download(attachment)
            }

            TransferState.PauseDownload,
            TransferState.PauseUpload -> {
                fileTransferService.resume(attachment.messageTid, attachment, state, channelId)
            }

            TransferState.Uploading,
            TransferState.Downloading,
            TransferState.Preparing,
            TransferState.FilePathChanged,
            TransferState.WaitingToUpload -> {
                fileTransferService.pause(attachment.messageTid, attachment, state)
            }

            TransferState.Uploaded,
            TransferState.Downloaded,
            TransferState.ThumbLoaded -> {
                FileTransferHelper.emitAttachmentTransferUpdate(
                    TransferData(
                        messageTid = attachment.messageTid,
                        progressPercent = attachment.progressPercent ?: 0f,
                        state = attachment.transferState,
                        filePath = attachment.filePath,
                        url = attachment.url
                    )
                )
            }
        }
    }
}
