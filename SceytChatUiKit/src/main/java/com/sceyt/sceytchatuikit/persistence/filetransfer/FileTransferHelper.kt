package com.sceyt.sceytchatuikit.persistence.filetransfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.runOnMainThread
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.mappers.getDimensions
import com.sceyt.sceytchatuikit.persistence.mappers.upsertSizeMetadata
import org.koin.core.component.inject
import java.io.File

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messagesLogic by inject<PersistenceAttachmentLogic>()

    private val onTransferUpdatedLiveData_ = MutableLiveData<TransferData>()
    val onTransferUpdatedLiveData: LiveData<TransferData> = onTransferUpdatedLiveData_

    fun createTransferTask(attachment: SceytAttachment, isFromUpload: Boolean): TransferTask {
        return TransferTask(
            attachment = attachment,
            messageTid = attachment.messageTid,
            state = attachment.transferState,
            progressCallback = getProgressUpdateCallback(attachment),
            resumePauseCallback = getResumePauseCallback(attachment),
            resultCallback = if (isFromUpload) getUploadResultCallback(attachment)
            else getDownloadResultCallback(attachment),
            updateFileLocationCallback = getUpdateFileLocationCallback(attachment),
            thumbCallback = getThumbCallback(attachment))
    }

    fun getProgressUpdateCallback(attachment: SceytAttachment) = ProgressUpdateCallback {
        attachment.transferState = it.state
        attachment.progressPercent = it.progressPercent
        initPrettySizes(it, attachment.fileSize)
        emitAttachmentTransferUpdate(it)
    }

    fun getResumePauseCallback(attachment: SceytAttachment) = ResumePauseCallback {
        attachment.transferState = it.state
        initPrettySizes(it, attachment.fileSize)
        emitAttachmentTransferUpdate(it)
        messagesLogic.updateTransferDataByMsgTid(it)
    }

    fun getDownloadResultCallback(attachment: SceytAttachment) = TransferResultCallback {
        when (it) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid,
                    100f, TransferState.Downloaded, it.data, attachment.url)
                initPrettySizes(transferData, attachment.fileSize)
                attachment.updateWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }

            is SceytResponse.Error -> {
                val transferData = TransferData(
                    attachment.messageTid, attachment.progressPercent ?: 0f,
                    TransferState.ErrorDownload, null, attachment.url)
                initPrettySizes(transferData, attachment.fileSize)
                attachment.updateWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
                SceytLog.e(this.TAG, "Couldn't download file url:${attachment.url} error:${it.message}")
            }
        }
    }

    fun getUploadResultCallback(attachment: SceytAttachment) = TransferResultCallback { result ->
        when (result) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid,
                    100f, TransferState.Uploaded, attachment.filePath, result.data.toString())
                initPrettySizes(transferData, attachment.fileSize)
                attachment.updateWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }

            is SceytResponse.Error -> {
                val transferData = TransferData(attachment.messageTid,
                    attachment.progressPercent ?: 0f,
                    TransferState.ErrorUpload, attachment.filePath, null)
                initPrettySizes(transferData, attachment.fileSize)
                emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
                SceytLog.e(this.TAG, "Couldn't upload file " + result.message.toString())
            }
        }
        fileTransferService.findTransferTask(attachment)?.onCompletionListeners?.values?.forEach {
            it.invoke((result is SceytResponse.Success), result.data)
        }
    }

    fun getUpdateFileLocationCallback(attachment: SceytAttachment) = UpdateFileLocationCallback { newPath ->
        val transferData = TransferData(attachment.messageTid, 0f, TransferState.FilePathChanged, newPath, attachment.url)

        val newFile = File(newPath)
        if (newFile.exists()) {
            val fileSize = getFileSize(newPath)
            val dimensions = getDimensions(attachment.type, newPath)
            attachment.filePath = newPath
            attachment.fileSize = fileSize
            attachment.upsertSizeMetadata(dimensions)
            initPrettySizes(transferData, attachment.fileSize)
            emitAttachmentTransferUpdate(transferData)
            messagesLogic.updateAttachmentFilePathAndMetadata(attachment.messageTid, newPath, fileSize, attachment.metadata)
        }
    }

    fun getThumbCallback(attachment: SceytAttachment) = ThumbCallback { newPath, thumbData ->
        val transferData = TransferData(attachment.messageTid, attachment.progressPercent ?: 0f,
            TransferState.ThumbLoaded, newPath, attachment.url, thumbData)

        initPrettySizes(transferData, attachment.fileSize)
        emitAttachmentTransferUpdate(transferData)
    }


    fun emitAttachmentTransferUpdate(data: TransferData) {
        runOnMainThread {
            onTransferUpdatedLiveData_.value = data
        }
    }

    private fun initPrettySizes(data: TransferData, fileSize: Long) {
        val progressPercent = data.progressPercent
        val format = if (progressPercent > 99f) "%.2f" else "%.1f"
        data.fileTotalSize = fileSize.toPrettySize()
        data.fileLoadedSize = (fileSize * progressPercent / 100).toPrettySize(format)
    }
}