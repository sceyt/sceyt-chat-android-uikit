package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.util.Log
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.mappers.getDimensions
import com.sceyt.sceytchatuikit.persistence.mappers.upsertSizeMetadata
import org.koin.core.component.inject
import java.io.File

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messagesLogic by inject<PersistenceAttachmentLogic>()

    fun createTransferTask(attachment: SceytAttachment, isFromUpload: Boolean): TransferTask {
        return TransferTask(
            attachment = attachment,
            messageTid = attachment.messageTid,
            state = attachment.transferState,
            progressCallback = getProgressUpdateCallback(attachment),
            resultCallback = if (isFromUpload) getUploadResultCallback(attachment)
            else getDownloadResultCallback(attachment),
            updateFileLocationCallback = getUpdateFileLocationCallback(attachment),
            thumbCallback = getThumbCallback(attachment))
    }

    fun getProgressUpdateCallback(attachment: SceytAttachment) = ProgressUpdateCallback {
        attachment.transferState = it.state
        attachment.progressPercent = it.progressPercent
        MessageEventsObserver.emitAttachmentTransferUpdate(it)
        messagesLogic.updateTransferDataByMsgTid(it)
    }

    fun getDownloadResultCallback(attachment: SceytAttachment) = TransferResultCallback {
        when (it) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid, attachment.tid,
                    100f, TransferState.Downloaded, it.data, attachment.url)
                attachment.updateWithTransferData(transferData)
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }
            is SceytResponse.Error -> {
                val transferData = TransferData(
                    attachment.messageTid, attachment.tid, attachment.progressPercent ?: 0f,
                    TransferState.ErrorDownload, null, attachment.url)

                attachment.updateWithTransferData(transferData)
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
                Log.e(this.TAG, "Couldn't download file url:${attachment.url} error:${it.message}")
            }
        }
    }

    fun getUploadResultCallback(attachment: SceytAttachment) = TransferResultCallback { result ->
        when (result) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid,
                    attachment.tid, 100f, TransferState.Uploaded, attachment.filePath, result.data.toString())
                attachment.updateWithTransferData(transferData)
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }
            is SceytResponse.Error -> {
                val transferData = TransferData(attachment.messageTid,
                    attachment.tid, attachment.progressPercent
                            ?: 0f, TransferState.ErrorUpload, attachment.filePath, null)

                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
                Log.e(this.TAG, "Couldn't upload file " + result.message.toString())
            }
        }
        fileTransferService.findTransferTask(attachment)?.onCompletionListeners?.values?.forEach {
            it.invoke((result is SceytResponse.Success), result.data)
        }
    }

    fun getUpdateFileLocationCallback(attachment: SceytAttachment) = UpdateFileLocationCallback { newPath ->
        val transferData = TransferData(attachment.messageTid,
            attachment.tid, 0f, TransferState.FilePathChanged, newPath, attachment.url)

        val newFile = File(newPath)
        if (newFile.exists()) {
            val fileSize = getFileSize(newPath)
            val dimensions = getDimensions(attachment.type, newPath)
            attachment.filePath = newPath
            attachment.fileSize = fileSize
            attachment.upsertSizeMetadata(dimensions)
            MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
            messagesLogic.updateAttachmentFilePathAndMetadata(attachment.messageTid, newPath, fileSize, attachment.metadata)
        }
    }

    fun getThumbCallback(attachment: SceytAttachment) = ThumbCallback { newPath ->
        val transferData = TransferData(attachment.messageTid,
            attachment.tid, attachment.progressPercent
                    ?: 0f, TransferState.ThumbLoaded, newPath, attachment.url)

        MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
    }
}