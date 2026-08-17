package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

internal class FileTransferLogicImpl(
    context: Context,
    attachmentLogic: PersistenceAttachmentLogic,
    thumbPathResolver: ThumbPathResolver,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : FileTransferLogic {
    private val uploadCoordinator = AttachmentUploadCoordinator(context, attachmentLogic, scope)
    private val downloadCoordinator = AttachmentDownloadCoordinator(context, scope)
    private val thumbCoordinator = AttachmentThumbCoordinator(context, thumbPathResolver)

    override fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        uploadCoordinator.uploadFile(attachment, task)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask) {
        uploadCoordinator.uploadSharedFile(attachment, task)
    }

    override fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        downloadCoordinator.downloadFile(attachment, task)
    }

    override fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingUpload, Uploading, Preparing, FilePathChanged, WaitingToUpload ->
                uploadCoordinator.pauseLoad(attachment, state)

            PendingDownload, Downloading ->
                downloadCoordinator.pauseLoad(attachment, state)

            else -> Unit
        }
    }

    override fun resumeLoad(attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingUpload, PauseUpload, ErrorUpload ->
                uploadCoordinator.resumeLoad(attachment, state)

            PendingDownload, PauseDownload, ErrorDownload ->
                downloadCoordinator.resumeLoad(attachment, state)

            else -> Unit
        }
    }

    override fun getAttachmentThumb(
        messageTid: Long,
        attachment: SceytAttachment,
        data: ThumbData,
    ) {
        thumbCoordinator.getAttachmentThumb(messageTid, attachment, data)
    }

    override fun clearPreparingThumbPaths() {
        thumbCoordinator.clearPreparingThumbPaths()
    }

    override fun cancelAll() {
        uploadCoordinator.cancelAll()
        downloadCoordinator.cancelAll()
        thumbCoordinator.clear()
        scope.coroutineContext.cancelChildren()
    }
}
