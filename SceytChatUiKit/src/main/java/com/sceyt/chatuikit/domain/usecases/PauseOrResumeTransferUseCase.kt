package com.sceyt.chatuikit.domain.usecases

import android.content.Context
import androidx.work.ExistingWorkPolicy
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PauseOrResumeTransferUseCase(
    private val context: Context,
    private val fileTransferService: FileTransferService,
    private val attachmentInteractor: AttachmentInteractor,
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

            TransferState.PauseDownload -> {
                val task = fileTransferService.findTransferTask(attachment)
                if (task != null)
                    fileTransferService.resume(attachment.messageTid, attachment, state)
                else
                    fileTransferService.download(attachment)
            }

            TransferState.PauseUpload -> {
                val task = fileTransferService.findTransferTask(attachment)
                if (task != null)
                    fileTransferService.resume(attachment.messageTid, attachment, state)
                else {
                    // Update transfer state to Uploading, otherwise SendAttachmentWorkManager will
                    // not start uploading.
                    attachmentInteractor.updateTransferDataByMsgTid(
                        TransferData(
                            messageTid = attachment.messageTid,
                            progressPercent = attachment.progressPercent ?: 0f,
                            state = TransferState.Uploading,
                            filePath = attachment.filePath,
                            url = attachment.url
                        )
                    )
                    UploadAndSendAttachmentWorkManager.schedule(
                        context = context,
                        messageTid = attachment.messageTid,
                        channelId = channelId,
                        workPolicy = ExistingWorkPolicy.REPLACE
                    )
                }
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
