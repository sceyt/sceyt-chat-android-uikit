package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileDownloadCoordinator
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferObserver
import com.sceyt.chatuikit.filetransfer.FileTransferSubscription
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.inject
import java.io.File

internal class AttachmentDownloadCoordinator(
    private val context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val fileDownloadCoordinator = FileDownloadCoordinator(
        scope = scope,
        transport = SceytChatUIKit.fileTransfer::transport
    )

    fun downloadFile(attachment: SceytAttachment, task: TransferTask) {
        if (task.downloadSubscription?.isFinished == false) return

        val url = attachment.url
        if (url.isNullOrBlank()) {
            notifyDownloadResult(task, SceytResponse.Error(SceytException(0, "Wrong url")))
            return
        }
        val destination = provideDestination(attachment, task) ?: return
        val existingFile = attachment.isAttachmentExistAndFullyLoaded(destination)
        if (existingFile != null) {
            notifyDownloadResult(task, SceytResponse.Success(existingFile.path))
            return
        }
        startDownload(attachment, task, url, destination)
    }

    private fun startDownload(
        attachment: SceytAttachment,
        task: TransferTask,
        url: String,
        destination: File,
        isResuming: Boolean = false,
    ) {
        val request = FileDownloadRequest(
            operationId = "download:${attachment.transferKey}",
            url = url,
            destinationFile = destination,
            context = AttachmentTransferContext(attachment),
        )
        task.state = Downloading
        fileDownloadCoordinator.download(
            request = request,
            participantId = attachment.transferKey,
            observer = object : FileTransferObserver {
                override fun onStarted() {
                    publishDownloadingProgress(attachment, task, url)
                    if (isResuming) onResumed(restarted = false)
                }

                override fun onProgress(progressPercent: Float) {
                    task.progressCallback?.onProgress(
                        TransferData(task.messageTid, progressPercent, Downloading, null, url),
                    )
                }

                override fun onPaused() {
                    task.state = PauseDownload
                    task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseDownload))
                }

                override fun onResumed(restarted: Boolean) {
                    if (restarted) {
                        val existingFile = attachment.isAttachmentExistAndFullyLoaded(destination)
                        if (existingFile != null) {
                            task.downloadSubscription?.cancel()
                            task.downloadSubscription = null
                            notifyDownloadResult(task, SceytResponse.Success(existingFile.path))
                            return
                        }
                        publishDownloadingProgress(attachment, task, url)
                    }
                    task.state = Downloading
                    task.resumePauseCallback?.onResumePause(attachment.toTransferData(Downloading))
                }

                override fun onResult(result: Result<String>) {
                    task.downloadSubscription = null
                    notifyDownloadResult(
                        task, result.fold(
                            onSuccess = { SceytResponse.Success(it) },
                            onFailure = { SceytResponse.Error(it.toSceytException()) },
                        )
                    )
                }
            },
            bind = { bindDownload(task, it) },
        )
    }

    private fun bindDownload(task: TransferTask, subscription: FileTransferSubscription) {
        task.downloadSubscription = subscription
        if (task.state == PauseDownload) subscription.pause()
    }

    private fun publishDownloadingProgress(
        attachment: SceytAttachment,
        task: TransferTask,
        url: String,
    ) {
        task.progressCallback?.onProgress(
            TransferData(
                task.messageTid,
                attachment.progressPercent ?: 0f,
                Downloading,
                attachment.filePath,
                url
            ),
        )
    }

    fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        if (state != PendingDownload && state != Downloading) return
        val task = fileTransferService.findTransferTask(attachment) ?: return
        val subscription = task.downloadSubscription
        if (subscription != null && !subscription.isFinished) {
            subscription.pause()
        } else {
            task.state = PauseDownload
            task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseDownload))
        }
    }

    fun resumeLoad(attachment: SceytAttachment, state: TransferState) {
        if (state != PendingDownload && state != PauseDownload && state != ErrorDownload) return

        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val subscription = task.downloadSubscription
        if (subscription != null && !subscription.isFinished) {
            if (state == PauseDownload) {
                subscription.resume()
                return
            }
            if (state == PendingDownload) return
            subscription.cancel()
            task.downloadSubscription = null
        }

        val destination = provideDestination(attachment, task) ?: return
        val existingFile = attachment.isAttachmentExistAndFullyLoaded(destination)
        task.state = Downloading
        if (existingFile != null) {
            notifyDownloadResult(task, SceytResponse.Success(existingFile.path))
            return
        }
        val url = attachment.url
        if (url.isNullOrBlank()) {
            notifyDownloadResult(task, SceytResponse.Error(SceytException(0, "Wrong url")))
            return
        }
        startDownload(attachment, task, url, destination, isResuming = true)
    }

    fun cancelAll() = fileDownloadCoordinator.cancelAll()

    private fun Throwable.toSceytException(): SceytException =
        this as? SceytException ?: SceytException(0, message)

    private fun provideDestination(attachment: SceytAttachment, task: TransferTask): File? =
        runCatching {
            SceytChatUIKit.fileTransfer.destinationProvider.provideDestination(context, attachment)
        }.getOrElse { error ->
            notifyDownloadResult(task, SceytResponse.Error(error.toSceytException()))
            null
        }

    private fun notifyDownloadResult(task: TransferTask, response: SceytResponse<String>) {
        fileTransferService.removeTransferTask(task)
        runCatching { task.downloadCallback?.onResult(response) }.onFailure { error ->
            SceytLog.e("AttachmentDownloadCoordinator", "File transfer callback failed", error)
        }
        val result = when (response) {
            is SceytResponse.Success -> Result.success(task.attachment)
            is SceytResponse.Error -> Result.failure(
                response.exception ?: IllegalStateException("File download failed")
            )
        }
        runCatching { task.complete(result) }.onFailure { error ->
            SceytLog.e(
                "AttachmentDownloadCoordinator",
                "File transfer completion listener failed",
                error
            )
        }
    }
}
