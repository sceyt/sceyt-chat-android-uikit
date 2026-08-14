package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferListeners
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

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

    override fun cancelAllTransfers() {
        tasks.clear()
    }

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
    class UploadCall(
        val request: FileUploadRequest,
        private val callback: FileTransferCallback,
    ) {
        val result = CompletableDeferred<String?>()

        @Volatile
        var cancelled = false

        fun progress(progressPercent: Float) {
            callback.onEvent(FileTransferEvent.Progress(progressPercent))
        }

        fun waitingForNetwork() {
            callback.onEvent(FileTransferEvent.WaitingForNetwork)
        }

        fun succeed(remoteReference: String?) {
            result.complete(remoteReference)
        }

        fun fail(error: Throwable) {
            result.completeExceptionally(error)
        }
    }

    class DownloadCall(
        val request: FileDownloadRequest,
        private val callback: FileTransferCallback,
    ) {
        val result = CompletableDeferred<String?>()

        @Volatile
        var cancelled = false

        fun progress(progressPercent: Float) {
            callback.onEvent(FileTransferEvent.Progress(progressPercent))
        }

        fun waitingForNetwork() {
            callback.onEvent(FileTransferEvent.WaitingForNetwork)
        }

        fun succeed(localPath: String?) {
            result.complete(localPath)
        }

        fun fail(error: Throwable) {
            result.completeExceptionally(error)
        }
    }

    val uploadCalls = CopyOnWriteArrayList<UploadCall>()
    val downloadCalls = CopyOnWriteArrayList<DownloadCall>()
    var downloadCancellationGate: CompletableDeferred<Unit>? = null

    override suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): String? {
        val call = UploadCall(request, callback)
        uploadCalls += call
        return try {
            call.result.await()
        } catch (error: CancellationException) {
            call.cancelled = true
            throw error
        }
    }

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): String? {
        val call = DownloadCall(request, callback)
        downloadCalls += call
        return try {
            call.result.await()
        } catch (error: CancellationException) {
            call.cancelled = true
            downloadCancellationGate?.let { gate ->
                withContext(NonCancellable) {
                    gate.await()
                }
            }
            throw error
        }
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
