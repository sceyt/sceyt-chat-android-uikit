package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferListeners
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import java.util.concurrent.ConcurrentHashMap

internal class TestFileTransferService : FileTransferService {
    private val tasks = ConcurrentHashMap<String, TransferTask>()
    private var customListeners: FileTransferListeners.Listeners? = null

    override fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners) {
        customListeners = fileTransferListeners
    }

    override fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask {
        return tasks.getOrPut(attachment.messageTid.toString()) {
            TransferTask(attachment, attachment.messageTid, attachment.transferState)
        }
    }

    override fun findTransferTask(attachment: SceytAttachment): TransferTask? {
        return tasks[attachment.messageTid.toString()]
    }

    override fun addTransferTask(task: TransferTask) {
        tasks[task.messageTid.toString()] = task
    }

    override fun removeTransferTask(messageTid: Long) {
        tasks.remove(messageTid.toString())
    }

    override fun getTasks(): Map<String, TransferTask> = tasks

    override fun clearPreparingThumbPaths() = Unit

    override fun upload(attachment: SceytAttachment, transferTask: TransferTask) {
        customListeners?.upload(attachment, transferTask)
    }

    override fun uploadSharedFile(attachment: SceytAttachment, transferTask: TransferTask) {
        customListeners?.uploadSharedFile(attachment, transferTask)
    }

    override fun download(attachment: SceytAttachment, transferTask: TransferTask) {
        customListeners?.download(attachment, transferTask)
    }

    override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        customListeners?.pause(messageTid, attachment, state)
    }

    override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        customListeners?.resume(messageTid, attachment, state)
    }

    override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
        customListeners?.getThumb(messageTid, attachment, thumbData)
    }
}

internal class RecordingFileTransferTransport : FileTransferTransport {
    data class UploadCall(
        val request: FileUploadRequest,
        val callback: FileTransferCallback<String>,
    )

    data class DownloadCall(
        val request: FileDownloadRequest,
        val callback: FileTransferCallback<String>,
    )

    val uploadCalls = mutableListOf<UploadCall>()
    val downloadCalls = mutableListOf<DownloadCall>()
    val pausedOperationIds = mutableListOf<String>()
    val resumedOperationIds = mutableListOf<String>()
    var pauseResult = true
    var resumeResult = false

    override fun upload(request: FileUploadRequest, callback: FileTransferCallback<String>) {
        uploadCalls += UploadCall(request, callback)
    }

    override fun download(request: FileDownloadRequest, callback: FileTransferCallback<String>) {
        downloadCalls += DownloadCall(request, callback)
    }

    override fun pause(operationId: String): Boolean {
        pausedOperationIds += operationId
        return pauseResult
    }

    override fun resume(operationId: String): Boolean {
        resumedOperationIds += operationId
        return resumeResult
    }
}

internal fun attachment(
    messageTid: Long = 10L,
    name: String = "attachment.txt",
    type: String = AttachmentTypeEnum.File.value,
    fileSize: Long = 4L,
    url: String? = "https://cdn.test/attachment.txt",
    filePath: String? = "/tmp/attachment.txt",
    originalFilePath: String? = filePath,
    state: TransferState? = TransferState.PendingUpload,
): SceytAttachment {
    return SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = name,
        type = type,
        metadata = null,
        fileSize = fileSize,
        createdAt = 1_000L,
        url = url,
        filePath = filePath,
        transferState = state,
        progressPercent = 0f,
        originalFilePath = originalFilePath,
        linkPreviewDetails = null,
    )
}

internal fun transferTask(attachment: SceytAttachment): TransferTask {
    return TransferTask(attachment, attachment.messageTid, attachment.transferState)
}
