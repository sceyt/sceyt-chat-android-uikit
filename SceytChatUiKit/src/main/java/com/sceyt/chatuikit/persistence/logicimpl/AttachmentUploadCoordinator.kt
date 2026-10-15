package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileTransferObserver
import com.sceyt.chatuikit.filetransfer.FileTransferSubscription
import com.sceyt.chatuikit.filetransfer.FileUploadCoordinator
import com.sceyt.chatuikit.filetransfer.FileUploadPreparation
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.persistence.extensions.transcodeVideo
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.file_transfer.isCompleted
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal class AttachmentUploadCoordinator(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val fileUploadCoordinator = FileUploadCoordinator(scope) { SceytChatUIKit.fileTransfer.transport }

    fun uploadFile(attachment: SceytAttachment, task: TransferTask) {
        enqueueUpload(attachment, task, shared = false)
    }

    fun uploadSharedFile(attachment: SceytAttachment, task: TransferTask) {
        task.isSharedUpload = true
        enqueueUpload(attachment, task, shared = true)
    }

    private fun enqueueUpload(attachment: SceytAttachment, task: TransferTask, shared: Boolean) {
        if (completeIfPaused(attachment, task)) return
        if (task.uploadSubscription?.isFinished == false) return
        var progressFilePath = attachment.filePath
        fileUploadCoordinator.upload(
            operationId = attachment.uploadOperationId,
            participantId = attachment.transferKey,
            sharingKey = if (shared) attachment.sourcePath ?: attachment.uploadOperationId else null,
            sequential = !shared,
            prepare = {
                prepareUpload(attachment, task, shared).also { prepared ->
                    if (prepared is FileUploadPreparation.Ready) progressFilePath = prepared.request.sourceFile.path
                }
            },
            observer = object : FileTransferObserver {
                override fun onProgress(progressPercent: Float) {
                    task.state = Uploading
                    task.progressCallback?.onProgress(
                        TransferData(
                            task.messageTid, progressPercent, Uploading,
                            if (shared) task.attachment.filePath else progressFilePath, null,
                        ),
                    )
                }

                override fun onPaused() {
                    publishPaused(attachment, task)
                }

                override fun onResumed(restarted: Boolean) {
                    task.state = WaitingToUpload
                    task.resumePauseCallback?.onResumePause(attachment.toTransferData(WaitingToUpload))
                }

                override fun onResult(result: Result<String>) {
                    task.uploadSubscription = null
                    notifyTaskResult(task, result.fold(
                        onSuccess = { SceytResponse.Success(it) },
                        onFailure = { SceytResponse.Error(it.toSceytException()) },
                    ))
                }
            },
            bind = { bindUpload(attachment, task, it) },
        )
    }

    private fun bindUpload(
        attachment: SceytAttachment,
        task: TransferTask,
        subscription: FileTransferSubscription,
    ) {
        task.uploadSubscription = subscription
        if (fileTransferService.findTransferTask(attachment) == null) subscription.cancel()
        else if (task.state == PauseUpload) subscription.pause()
    }

    private fun completeIfPaused(attachment: SceytAttachment, task: TransferTask): Boolean {
        if (task.uploadSubscription?.isPaused != true && task.state != PauseUpload) return false
        publishPaused(attachment, task)
        // Finish the worker attempt without failing the attachment or clearing its pause.
        task.complete(Result.failure(IllegalStateException("Attachment upload is paused")))
        return true
    }

    private fun publishPaused(attachment: SceytAttachment, task: TransferTask) {
        task.state = PauseUpload
        task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseUpload))
    }

    fun pauseLoad(attachment: SceytAttachment, state: TransferState) {
        if (state !in listOf(PendingUpload, WaitingToUpload, Preparing, FilePathChanged, Uploading)) return
        val task = fileTransferService.findTransferTask(attachment)
        // Ignore stale active-state taps after completion. Pending and queued uploads can still be
        // paused before the worker has created their task.
        if (task == null && state != PendingUpload && state != WaitingToUpload) return
        if (task == null && AttachmentTransferStateStore.getTransferData(attachment)?.state?.isCompleted() == true) return

        val pausedTask = task ?: fileTransferService.findOrCreateTransferTask(attachment)
        val subscription = pausedTask.uploadSubscription
        if (subscription != null && !subscription.isFinished) subscription.pause()
        else publishPaused(attachment, pausedTask)
    }

    fun resumeLoad(attachment: SceytAttachment, state: TransferState) {
        if (state != PendingUpload && state != PauseUpload && state != ErrorUpload) return
        val task = fileTransferService.findOrCreateTransferTask(attachment)
        val subscription = task.uploadSubscription
        if (subscription != null && !subscription.isFinished) {
            subscription.resume()
            return
        }

        task.state = WaitingToUpload
        task.resumePauseCallback?.onResumePause(attachment.toTransferData(WaitingToUpload))
        enqueueUpload(attachment, task, task.isSharedUpload)
    }

    fun cancelUpload(attachment: SceytAttachment) {
        fileTransferService.findTransferTask(attachment)?.let { task ->
            task.uploadSubscription?.cancel()
            task.uploadSubscription = null
        }
    }

    fun cancelAll() = fileUploadCoordinator.cancelAll()

    private suspend fun prepareUpload(
        attachment: SceytAttachment,
        task: TransferTask,
        shared: Boolean,
    ): FileUploadPreparation {
        val checksum = if (SceytChatUIKit.config.preventDuplicateAttachmentUpload)
            attachmentLogic.getFileChecksumData(attachment.sourcePath) else null
        if (checksum?.url.isNotNullOrBlank()) {
            if (!checksum?.resizedFilePath.isNullOrEmpty()) {
                updateFileLocation(task, checkNotNull(checksum.resizedFilePath))
            }
            return FileUploadPreparation.Reused(checkNotNull(checksum?.url))
        }

        val result = prepareAttachment(attachment, checksum, task)
        currentCoroutineContext().ensureActive()
        val prepared = result.fold(
            onSuccess = { path ->
                updateFileLocation(task, path)
                attachment.copy(filePath = path, fileSize = getFileSize(path))
            },
            onFailure = {
                SceytLog.i(TAG, "Couldn't resize file with reason ${it.message}")
                attachment
            },
        )
        val path = prepared.filePath
        require(!path.isNullOrBlank()) { "Attachment source path is missing" }
        val source = File(path)
        require(source.isFile) { "Attachment source file does not exist: $path" }
        return FileUploadPreparation.Ready(
            FileUploadRequest(
                operationId = attachment.uploadOperationId,
                sourceFile = source,
                fileName = prepared.name,
                mimeType = getMimeType(source.path),
                context = AttachmentTransferContext(prepared, shared),
            ),
        )
    }

    private fun updateFileLocation(task: TransferTask, path: String) {
        // Preparation can outlive a shared participant that was deleted.
        if (task.uploadSubscription?.isFinished == false) {
            task.updateFileLocationCallback?.onUpdateFileLocation(path)
        }
    }

    private suspend fun prepareAttachment(
        attachment: SceytAttachment,
        checksumData: FileChecksumData?,
        task: TransferTask,
    ): Result<String> {
        val resizedPath = checksumData?.resizedFilePath
        if (resizedPath != null && File(resizedPath).exists()) return Result.success(resizedPath)

        return when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                val config = SceytChatUIKit.config.imageAttachmentResizeConfig
                resizeImage(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    reqSize = config.dimensionThreshold,
                    quality = config.compressionQuality,
                )
            }
            AttachmentTypeEnum.Video.value -> transcodeAttachment(attachment, task)
            else -> Result.failure(Exception("Unsupported attachment type: ${attachment.type}"))
        }
    }

    private suspend fun transcodeAttachment(
        attachment: SceytAttachment,
        task: TransferTask,
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()
        val destination = transcodeVideo(
            path = attachment.filePath,
            parentDir = context.filesDir,
            config = SceytChatUIKit.config.videoAttachmentResizeConfig,
            progressCallback = { data ->
                val subscription = task.uploadSubscription
                if (continuation.isActive && subscription?.isPaused == false && !subscription.isFinished) {
                    task.preparingCallback?.onPreparing(attachment.toTransferData(Preparing, data.progressPercent))
                }
            },
        ) { result ->
            if (completed.compareAndSet(false, true)) continuation.resume(result)
        }
        continuation.invokeOnCancellation {
            completed.set(true)
            destination?.let(VideoTranscodeHelper::cancel)
        }
    }

    private fun notifyTaskResult(task: TransferTask, response: SceytResponse<String>) {
        fileTransferService.removeTransferTask(task)
        task.state = if (response is SceytResponse.Success) Uploaded else ErrorUpload
        runCatching { task.uploadResultCallback?.onResult(response) }.onFailure {
            SceytLog.e(TAG, "File transfer callback failed", it)
        }
        val result = when (response) {
            is SceytResponse.Success -> Result.success(task.attachment)
            is SceytResponse.Error -> Result.failure(Throwable(response.message.orEmpty()))
        }
        runCatching { task.complete(result) }.onFailure {
            SceytLog.e(TAG, "File transfer completion listener failed", it)
        }
    }

    private fun Throwable.toSceytException(): SceytException =
        this as? SceytException ?: SceytException(0, message)

    private val SceytAttachment.sourcePath: String?
        get() = originalFilePath?.takeIf(String::isNotBlank) ?: filePath?.takeIf(String::isNotBlank)

    private val SceytAttachment.uploadOperationId: String
        get() = "upload:$messageTid"

    private companion object {
        const val TAG = "FileTransferLogic"
    }
}
