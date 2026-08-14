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
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.persistence.extensions.transcodeVideo
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
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import java.io.File
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal class AttachmentUploadCoordinator(
    private val context: Context,
    private val attachmentLogic: PersistenceAttachmentLogic,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()

    private val pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private val uploadQueueLock = Any()

    private var currentUploadingAttachment: SceytAttachment? = null

    private val pausedTaskIds = ConcurrentHashMap.newKeySet<Long>()
    private val uploadJobs = ConcurrentHashMap<Long, Job>()
    private val resizingAttachmentIds = ConcurrentHashMap.newKeySet<Long>()
    private val sharingFilesPath = ConcurrentHashMap.newKeySet<ShareFileData>()

    fun uploadFile(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        checkAndUpload(attachment, task)
    }

    fun uploadSharedFile(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        fileTransferService.addTransferTask(task)

        val shareFileData = ShareFileData(
            originalPath = attachment.originalFilePath.toString(),
            messageTid = attachment.messageTid,
        )
        val sharedUploadInProgress = sharingFilesPath
            .filter { it.originalPath == attachment.originalFilePath }
            .any { uploadJobs[it.messageTid]?.isActive == true }

        sharingFilesPath.add(shareFileData)
        if (sharedUploadInProgress) return

        launchUploadJob(attachment, task) {
            if (pausedTaskIds.contains(attachment.messageTid)) return@launchUploadJob

            val checksum = getAttachmentChecksum(attachment.originalFilePath)
            val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, task)

            if (uploaded && url != null) {
                getAppropriateTasks(task).forEach { transferTask ->
                    transferTask.uploadResultCallback?.onResult(SceytResponse.Success(url))
                }

                removeFromSharingPath(attachment.originalFilePath)
                return@launchUploadJob
            }

            val result = prepareAttachment(
                attachment = attachment,
                checksumData = checksum,
                task = task,
            )
            currentCoroutineContext().ensureActive()

            val uploadAttachment = result.fold(
                onSuccess = { path ->
                    task.updateFileLocationCallback?.onUpdateFileLocation(path)
                    attachment.copy(filePath = path, fileSize = getFileSize(path))
                },
                onFailure = {
                    SceytLog.i(TAG, "Couldn't resize sharing file with reason ${it.message}")
                    attachment
                },
            )

            uploadSharedAttachment(uploadAttachment, task)
        }
    }

    fun pauseLoad(
        attachment: SceytAttachment,
        state: TransferState,
    ) {
        when (state) {
            PendingUpload,
            Uploading,
            Preparing,
            FilePathChanged,
            WaitingToUpload -> {
                uploadJobs.remove(attachment.messageTid)?.cancel()
                pausedTaskIds.add(attachment.messageTid)

                if (attachment.type == AttachmentTypeEnum.Video.value) {
                    VideoTranscodeHelper.cancel(attachment.filePath)
                }

                fileTransferService.findTransferTask(attachment)?.let { task ->
                    task.state = PauseUpload
                    task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseUpload))
                }

                uploadNext(attachment.messageTid)
            }

            else -> return
        }
    }

    fun resumeLoad(
        attachment: SceytAttachment,
        state: TransferState,
    ) {
        when (state) {
            PendingUpload,
            PauseUpload,
            ErrorUpload -> {
                pausedTaskIds.remove(attachment.messageTid)

                var wasSharing = false

                if (
                    sharingFilesPath.any {
                        it.originalPath == attachment.originalFilePath
                    }
                ) {
                    wasSharing = true

                    fileTransferService.getTasks().values
                        .find { task ->
                            task.attachment.filePath == attachment.filePath &&
                                    task.attachment.messageTid !=
                                    attachment.messageTid &&
                                    task.attachment.transferState == Uploaded
                        }?.let { task ->
                            if (task.attachment.url != null) {
                                fileTransferService
                                    .findTransferTask(attachment)
                                    ?.uploadResultCallback
                                    ?.onResult(SceytResponse.Success(task.attachment.url))
                                return
                            }
                        }
                }

                fileTransferService.findTransferTask(attachment)
                    ?.resumePauseCallback
                    ?.onResumePause(attachment.toTransferData(WaitingToUpload))

                if (resizingAttachmentIds.contains(attachment.messageTid)) return

                if (wasSharing) {
                    uploadSharedFile(
                        attachment = attachment,
                        task = fileTransferService.findOrCreateTransferTask(attachment),
                    )
                } else {
                    uploadFile(
                        attachment = attachment,
                        task = fileTransferService.findOrCreateTransferTask(attachment),
                    )
                }
            }

            else -> return
        }
    }

    private fun checkAndUpload(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        val shouldUpload = synchronized(uploadQueueLock) {
            if (currentUploadingAttachment == null) {
                currentUploadingAttachment = attachment
                true
            } else {
                val alreadyExists =
                    currentUploadingAttachment?.messageTid == attachment.messageTid ||
                            pendingUploadQueue.any {
                                it.first.messageTid == attachment.messageTid
                            }

                if (!alreadyExists) {
                    pendingUploadQueue.add(attachment to task)
                }

                false
            }
        }

        if (shouldUpload) {
            uploadAttachment(attachment, task)
        }
    }

    private fun uploadNext(messageTid: Long) {
        val nextUpload = synchronized(uploadQueueLock) {
            if (currentUploadingAttachment?.messageTid != messageTid) {
                return
            }

            currentUploadingAttachment = null

            pendingUploadQueue.poll()?.also { (attachment) ->
                currentUploadingAttachment = attachment
            }
        } ?: return

        uploadAttachment(nextUpload.first, nextUpload.second)
    }

    private fun uploadAttachment(
        attachment: SceytAttachment,
        task: TransferTask,
    ) = launchUploadJob(attachment, task) {
        if (pausedTaskIds.contains(attachment.messageTid)) {
            uploadNext(attachment.messageTid)
            return@launchUploadJob
        }

        val checksum = getAttachmentChecksum(attachment.originalFilePath)
        val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, task)

        if (uploaded && url != null) {
            task.uploadResultCallback?.onResult(SceytResponse.Success(url))
            uploadNext(attachment.messageTid)
            return@launchUploadJob
        }

        val result = prepareAttachment(
            attachment = attachment,
            checksumData = checksum,
            task = task,
        )
        currentCoroutineContext().ensureActive()

        val uploadAttachment = result.fold(
            onSuccess = { path ->
                task.updateFileLocationCallback?.onUpdateFileLocation(path)
                attachment.copy(filePath = path, fileSize = getFileSize(path))
            },
            onFailure = {
                SceytLog.i(TAG, "Couldn't resize file with reason ${it.message}")
                attachment
            },
        )

        uploadAttachmentWithTransport(
            attachment = uploadAttachment,
            task = task,
            onComplete = { uploadNext(attachment.messageTid) },
        )
    }

    private suspend fun uploadSharedAttachment(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        uploadAttachmentWithTransport(
            attachment = attachment,
            task = task,
            isSharedUpload = true,
            onProgress = { progressPercent ->
                if (pausedTaskIds.contains(attachment.messageTid)) {
                    return@uploadAttachmentWithTransport
                }

                getAppropriateTasks(task).forEach { transferTask ->
                    transferTask.state = Uploading

                    transferTask.progressCallback?.onProgress(
                        TransferData(
                            messageTid = transferTask.messageTid,
                            progressPercent = progressPercent,
                            state = Uploading,
                            filePath = transferTask.attachment.filePath,
                            url = null,
                        ),
                    )
                }
            },
            onResult = { response ->
                getAppropriateTasks(task).forEach { transferTask ->
                    transferTask.uploadResultCallback?.onResult(response)
                }

                removeFromSharingPath(attachment.originalFilePath)
            },
        )
    }

    private suspend fun uploadAttachmentWithTransport(
        attachment: SceytAttachment,
        task: TransferTask,
        isSharedUpload: Boolean = false,
        onProgress: ((Float) -> Unit)? = null,
        onResult: ((SceytResponse<String>) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        val sourceFile = File(attachment.filePath.orEmpty())
        val request = FileUploadRequest(
            operationId = attachment.uploadOperationId,
            sourceFile = sourceFile,
            fileName = attachment.name,
            mimeType = getMimeType(sourceFile.path),
            attachment = attachment,
            isSharedUpload = isSharedUpload,
        )

        performUpload(request, attachment, task, onProgress, onResult, onComplete)
    }

    private fun launchUploadJob(
        attachment: SceytAttachment,
        task: TransferTask,
        block: suspend () -> Unit,
    ) {
        val messageTid = attachment.messageTid
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                SceytLog.e(TAG, "Upload preparation failed", error)
                runCatching {
                    task.uploadResultCallback?.onResult(
                        SceytResponse.Error(error.toSceytException()),
                    )
                }
                uploadNext(messageTid)
            } finally {
                uploadJobs.remove(messageTid, currentCoroutineContext().job)
            }
        }

        uploadJobs.put(messageTid, job)?.cancel()
        job.start()
    }

    private suspend fun performUpload(
        request: FileUploadRequest,
        attachment: SceytAttachment,
        task: TransferTask,
        onProgress: ((Float) -> Unit)?,
        onResult: ((SceytResponse<String>) -> Unit)?,
        onComplete: (() -> Unit)?,
    ) {
        val job = currentCoroutineContext().job

        try {
            val result = SceytChatUIKit.fileTransfer.transport.upload(
                request = request,
                callback = { event ->
                    if (job.isActive) {
                        handleUploadEvent(event, attachment, task, onProgress)
                    }
                },
            )
            currentCoroutineContext().ensureActive()
            notifyUploadResult(SceytResponse.Success(result), task, onResult)
            onComplete?.invoke()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            notifyUploadResult(
                response = SceytResponse.Error(error.toSceytException()),
                task = task,
                onResult = onResult,
            )
            onComplete?.invoke()
        }
    }

    private fun handleUploadEvent(
        event: FileTransferEvent,
        attachment: SceytAttachment,
        task: TransferTask,
        onProgress: ((Float) -> Unit)?,
    ) {
        if (event !is FileTransferEvent.Progress) return

        if (onProgress != null) {
            onProgress(event.progressPercent)
        } else {
            task.progressCallback?.onProgress(
                TransferData(
                    messageTid = task.messageTid,
                    progressPercent = event.progressPercent,
                    state = Uploading,
                    filePath = attachment.filePath,
                    url = null,
                ),
            )
        }
    }

    private fun notifyUploadResult(
        response: SceytResponse<String>,
        task: TransferTask,
        onResult: ((SceytResponse<String>) -> Unit)?,
    ) {
        if (onResult != null) {
            onResult(response)
        } else {
            task.uploadResultCallback?.onResult(response)
        }
    }

    private suspend fun prepareAttachment(
        attachment: SceytAttachment,
        checksumData: FileChecksumData?,
        task: TransferTask,
    ): Result<String> {
        val resizedPath = checksumData?.resizedFilePath

        if (resizedPath != null && File(resizedPath).exists()) {
            return Result.success(resizedPath)
        }

        return when (attachment.type) {
            AttachmentTypeEnum.Image.value -> {
                resizingAttachmentIds.add(attachment.messageTid)
                try {
                    val resizeConfig = SceytChatUIKit.config.imageAttachmentResizeConfig
                    resizeImage(
                        path = attachment.filePath,
                        parentDir = context.filesDir,
                        reqSize = resizeConfig.dimensionThreshold,
                        quality = resizeConfig.compressionQuality,
                    )
                } finally {
                    resizingAttachmentIds.remove(attachment.messageTid)
                }
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
        resizingAttachmentIds.add(attachment.messageTid)

        continuation.invokeOnCancellation {
            completed.set(true)
            resizingAttachmentIds.remove(attachment.messageTid)
            VideoTranscodeHelper.cancel(attachment.filePath)
        }

        transcodeVideo(
            path = attachment.filePath,
            parentDir = context.filesDir,
            quality = SceytChatUIKit.config.attachmentTransferConfig.videoTranscodeQuality,
            progressCallback = { data ->
                if (continuation.isActive && !pausedTaskIds.contains(attachment.messageTid)) {
                    task.preparingCallback?.onPreparing(
                        attachment.toTransferData(Preparing, data.progressPercent),
                    )
                }
            },
        ) { result ->
            if (completed.compareAndSet(false, true)) {
                resizingAttachmentIds.remove(attachment.messageTid)
                continuation.resume(result)
            }
        }
    }

    private fun removeFromSharingPath(filePath: String?) {
        val current = sharingFilesPath.firstOrNull {
            it.originalPath == filePath
        } ?: return

        sharingFilesPath.removeAll {
            it.originalPath == current.originalPath
        }
        sharingFilesPath.remove(current)
    }

    @Synchronized
    private fun getAppropriateTasks(
        task: TransferTask,
    ): List<TransferTask> {
        val currentTaskOriginalPath =
            sharingFilesPath.firstOrNull {
                it.originalPath == task.attachment.originalFilePath
            }?.originalPath

        val sharedTasks = sharingFilesPath.filter {
            it.originalPath == currentTaskOriginalPath
        }

        return fileTransferService.getTasks().values.filter { transferTask ->
            sharedTasks.any { data ->
                data.messageTid == transferTask.attachment.messageTid
            } && transferTask.state != PauseUpload
        }
    }

    private suspend fun getAttachmentChecksum(
        filePath: String?,
    ): FileChecksumData? {
        if (!SceytChatUIKit.config.preventDuplicateAttachmentUpload) {
            return null
        }

        return attachmentLogic.getFileChecksumData(filePath)
    }

    private fun checkMaybeAlreadyUploadedWithAnotherMessage(
        checksumData: FileChecksumData?,
        task: TransferTask,
    ): Pair<Boolean, String?> {
        checksumData ?: return false to ""

        if (checksumData.url.isNotNullOrBlank()) {
            if (!checksumData.resizedFilePath.isNullOrEmpty()) {
                task.updateFileLocationCallback?.onUpdateFileLocation(checksumData.resizedFilePath)
            }

            return true to checksumData.url
        }

        return false to ""
    }

    private fun Throwable?.toSceytException(): SceytException? {
        return when (this) {
            null -> null
            is SceytException -> this
            else -> SceytException(0, message)
        }
    }

    private val SceytAttachment.uploadOperationId: String
        get() = "upload:$messageTid"

    private data class ShareFileData(
        val originalPath: String,
        val messageTid: Long,
    )

    private companion object {
        const val TAG = "FileTransferLogic"
    }
}
