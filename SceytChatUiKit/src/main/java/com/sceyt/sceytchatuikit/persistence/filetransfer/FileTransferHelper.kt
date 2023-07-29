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
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData.Companion.withPrettySizes
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.persistence.mappers.getDimensions
import com.sceyt.sceytchatuikit.persistence.mappers.upsertSizeMetadata
import org.koin.core.component.inject
import java.io.File

object FileTransferHelper : SceytKoinComponent {
    private val fileTransferService by inject<FileTransferService>()
    private val messagesLogic by inject<PersistenceAttachmentLogic>()
    private val messagesCache by inject<MessagesCache>()

    private val onTransferUpdatedLiveData_ = MutableLiveData<TransferData>()
    val onTransferUpdatedLiveData: LiveData<TransferData> = onTransferUpdatedLiveData_

    fun createTransferTask(attachment: SceytAttachment, isFromUpload: Boolean): TransferTask {
        return TransferTask(
            attachment = attachment,
            messageTid = attachment.messageTid,
            state = attachment.transferState,
            progressCallback = getProgressUpdateCallback(attachment),
            preparingCallback = getPreparingCallback(attachment),
            resumePauseCallback = getResumePauseCallback(attachment),
            resultCallback = if (isFromUpload) getUploadResultCallback(attachment)
            else getDownloadResultCallback(attachment),
            updateFileLocationCallback = getUpdateFileLocationCallback(attachment),
            thumbCallback = getThumbCallback(attachment))
    }

    fun getProgressUpdateCallback(attachment: SceytAttachment) = ProgressUpdateCallback {
        attachment.transferState = it.state
        attachment.progressPercent = it.progressPercent
        it.withPrettySizes(attachment.fileSize)
        messagesCache.updateAttachmentTransferData(it)
        emitAttachmentTransferUpdate(it)
    }

    fun getPreparingCallback(attachment: SceytAttachment) = PreparingCallback {
        attachment.transferState = it.state
        it.withPrettySizes(attachment.fileSize)
        messagesCache.updateAttachmentTransferData(it)
        emitAttachmentTransferUpdate(it)
    }

    fun getResumePauseCallback(attachment: SceytAttachment) = ResumePauseCallback {
        attachment.transferState = it.state
        it.withPrettySizes(attachment.fileSize)
        emitAttachmentTransferUpdate(it)
        messagesLogic.updateTransferDataByMsgTid(it)
    }

    fun getDownloadResultCallback(attachment: SceytAttachment) = TransferResultCallback {
        when (it) {
            is SceytResponse.Success -> {
                val transferData = TransferData(attachment.messageTid, 100f,
                    TransferState.Downloaded, it.data, attachment.url).withPrettySizes(attachment.fileSize)

                attachment.updateWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }

            is SceytResponse.Error -> {
                val transferData = TransferData(
                    attachment.messageTid, attachment.progressPercent ?: 0f,
                    ErrorDownload, null, attachment.url).withPrettySizes(attachment.fileSize)

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
                val transferData = TransferData(attachment.messageTid, 100f,
                    Uploaded, attachment.filePath, result.data.toString()).withPrettySizes(attachment.fileSize)

                attachment.updateWithTransferData(transferData)
                emitAttachmentTransferUpdate(transferData)
                messagesLogic.updateAttachmentWithTransferData(transferData)
            }

            is SceytResponse.Error -> {
                val transferData = TransferData(attachment.messageTid, attachment.progressPercent
                        ?: 0f,
                    ErrorUpload, attachment.filePath, null).withPrettySizes(attachment.fileSize)

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
        val transferData = TransferData(attachment.messageTid, 0f,
            TransferState.FilePathChanged, newPath, attachment.url)

        val newFile = File(newPath)
        if (newFile.exists()) {
            val fileSize = getFileSize(newPath)
            val dimensions = getDimensions(attachment.type, newPath)
            attachment.filePath = newPath
            attachment.fileSize = fileSize
            attachment.upsertSizeMetadata(dimensions)

            emitAttachmentTransferUpdate(transferData.withPrettySizes(fileSize))
            messagesLogic.updateAttachmentFilePathAndMetadata(attachment.messageTid, newPath, fileSize, attachment.metadata)
        }
    }

    fun getThumbCallback(attachment: SceytAttachment) = ThumbCallback { newPath, thumbData ->
        val transferData = TransferData(attachment.messageTid, attachment.progressPercent ?: 0f,
            TransferState.ThumbLoaded, newPath, attachment.url, thumbData).withPrettySizes(attachment.fileSize)

        emitAttachmentTransferUpdate(transferData)
    }

    fun emitAttachmentTransferUpdate(data: TransferData) {
        runOnMainThread {
            onTransferUpdatedLiveData_.value = data
        }
    }

    @JvmStatic
    fun getFilePrettySizes(fileSize: Long, progressPercent: Float): Pair<String, String> {
        val format = if (fileSize > 99f) "%.2f" else "%.1f"
        val fileTotalSize = fileSize.toPrettySize()
        val fileLoadedSize = (fileSize * progressPercent / 100).toPrettySize(format)
        return Pair(fileLoadedSize, fileTotalSize)
    }
}