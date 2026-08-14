package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

internal class AttachmentDownloadCoordinator(
    private val context: Context,
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()

    private val downloadingUrlKeys = ConcurrentHashMap.newKeySet<String>()
    private val pausedTaskIds = ConcurrentHashMap.newKeySet<Long>()

    fun downloadFile(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        val url = attachment.url

        if (url.isNullOrBlank()) {
            task.downloadCallback?.onResult(SceytResponse.Error(SceytException(0, "Wrong url")))
            return
        }

        val destinationFile =
            SceytChatUIKit.fileTransfer.destinationProvider.provideDestination(context, attachment)

        val existingFile = attachment.isAttachmentExistAndFullyLoaded(destinationFile)

        if (existingFile != null) {
            task.downloadCallback?.onResult(SceytResponse.Success(existingFile.path))
            return
        }

        val downloadMapKey = attachment.downloadMapKey

        if (!downloadingUrlKeys.add(downloadMapKey)) {
            return
        }

        pausedTaskIds.remove(attachment.messageTid)

        task.progressCallback?.onProgress(
            TransferData(
                messageTid = task.messageTid,
                progressPercent = attachment.progressPercent ?: 0f,
                state = Downloading,
                filePath = attachment.filePath,
                url = url,
            ),
        )

        SceytChatUIKit.fileTransfer.transport.download(
            request = FileDownloadRequest(
                operationId = attachment.downloadOperationId,
                url = url,
                destinationFile = destinationFile,
                attachment = attachment,
            ),
            callback = object : FileTransferCallback<String> {
                override fun onProgress(progressPercent: Float) {
                    if (pausedTaskIds.contains(attachment.messageTid)) {
                        return
                    }

                    task.progressCallback?.onProgress(
                        TransferData(
                            messageTid = task.messageTid,
                            progressPercent = progressPercent,
                            state = Downloading,
                            filePath = null,
                            url = url,
                        ),
                    )
                }

                override fun onSuccess(result: String?) {
                    task.downloadCallback?.onResult(SceytResponse.Success(result))
                    downloadingUrlKeys.remove(downloadMapKey)
                }

                override fun onFailure(error: Throwable?) {
                    destinationFile.delete()

                    task.downloadCallback?.onResult(SceytResponse.Error(error.toSceytException()))
                    downloadingUrlKeys.remove(downloadMapKey)
                }
            },
        )
    }

    fun pauseLoad(
        attachment: SceytAttachment,
        state: TransferState,
    ) {
        if (state != PendingDownload && state != Downloading) {
            return
        }

        pausedTaskIds.add(attachment.messageTid)

        fileTransferService.findTransferTask(attachment)?.let { task ->
            task.state = PauseUpload
            task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseDownload))
        }

        SceytChatUIKit.fileTransfer.transport.pause(attachment.downloadOperationId)
    }

    fun resumeLoad(
        attachment: SceytAttachment,
        state: TransferState,
    ) {
        if (
            state != PendingDownload &&
            state != PauseDownload &&
            state != ErrorDownload
        ) {
            return
        }

        pausedTaskIds.remove(attachment.messageTid)

        val destinationFile =
            SceytChatUIKit.fileTransfer.destinationProvider
                .provideDestination(context, attachment)

        val existingFile =
            attachment.isAttachmentExistAndFullyLoaded(destinationFile)

        val task = fileTransferService.findTransferTask(attachment)

        if (existingFile != null) {
            task?.downloadCallback?.onResult(SceytResponse.Success(existingFile.path))
            return
        }

        val resumed = SceytChatUIKit.fileTransfer.transport.resume(attachment.downloadOperationId)

        if (!resumed) {
            downloadingUrlKeys.remove(attachment.downloadMapKey)

            downloadFile(
                attachment = attachment,
                task = fileTransferService.findOrCreateTransferTask(attachment),
            )
        }

        task?.resumePauseCallback?.onResumePause(attachment.toTransferData(Downloading))
    }

    private fun Throwable?.toSceytException(): SceytException? {
        return when (this) {
            null -> null
            is SceytException -> this
            else -> SceytException(0, message)
        }
    }

    private val SceytAttachment.downloadMapKey: String
        get() = url + messageTid

    private val SceytAttachment.downloadOperationId: String
        get() = "download:$messageTid"
}
