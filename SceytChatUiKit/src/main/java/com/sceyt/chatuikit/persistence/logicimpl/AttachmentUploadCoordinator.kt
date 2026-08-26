package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.TransferRole
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
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import com.sceyt.chatuikit.persistence.mappers.needsVideoThumbUpload
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.shared.media_encoder.VideoTranscodeHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val videoThumbUploader: VideoThumbUploader =
        VideoThumbUploader(context, attachmentLogic),
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()

    private val pendingUploadQueue: Queue<Pair<SceytAttachment, TransferTask>> = LinkedList()
    private val uploadQueueLock = Any()

    private var currentUploadingAttachment: SceytAttachment? = null

    private val pausedTaskIds = ConcurrentHashMap.newKeySet<Long>()
    private val uploadJobs = ConcurrentHashMap<String, Job>()
    private val sharingFilesPath = ConcurrentHashMap.newKeySet<ShareFileData>()
    private val sharingFilesLock = Any()

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
        val shareFileData = ShareFileData(
            sourceKey = attachment.sharedSourceKey,
            messageTid = attachment.messageTid,
        )
        val (sharedUploadInProgress, completedThumbUrl) = synchronized(sharingFilesLock) {
            val existing = sharingFilesPath.firstOrNull {
                it.sourceKey == shareFileData.sourceKey
            }
            shareFileData.completedThumbUrl = existing?.completedThumbUrl
            sharingFilesPath.add(shareFileData)
            (existing != null) to existing?.completedThumbUrl
        }
        if (sharedUploadInProgress) {
            completedThumbUrl?.let { url ->
                scope.launch {
                    videoThumbUploader.applyThumbUrl(attachment, listOf(task), url)
                }
            }
            return
        }

        startSharedUpload(attachment, task)
    }

    private fun startSharedUpload(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        launchUploadJob(
            attachment = attachment,
            onError = { response ->
                takeAppropriateTasks(task).forEach { transferTask ->
                    notifyTaskResult(transferTask, response)
                }
            },
        ) {
            val checksum = getAttachmentChecksum(attachment.sourcePath)
            val response: SceytResponse<String>? = withVideoThumbUpload(
                attachment = attachment,
                tasks = { getAppropriateTasksIncludingPaused(task) },
                checksumData = checksum,
            ) {
                val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, task)

                if (uploaded && url != null) {
                    return@withVideoThumbUpload SceytResponse.Success(url)
                }

                val result = prepareAttachment(
                    attachment = attachment,
                    checksumData = checksum,
                    task = task,
                )
                currentCoroutineContext().ensureActive()

                if (isSharedTransferPaused(attachment)) {
                    return@withVideoThumbUpload null
                }

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

            response?.onSuccessNotNull {
                saveCompletedSharedUpload(attachment, it)
            }
            response?.let {
                takeAppropriateTasks(task).forEach { transferTask ->
                    notifyTaskResult(transferTask, it)
                }
            }
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
                val messageTid = attachment.messageTid
                pausedTaskIds.add(messageTid)

                fileTransferService.findTransferTask(attachment)?.let { task ->
                    task.state = PauseUpload
                    task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseUpload))
                }

                if (pauseSharedUpload(attachment, state)) return

                val operationId = attachment.uploadOperationId
                val currentJob = uploadJobs[operationId]
                val pausedByTransport = state == Uploading &&
                        currentJob?.isActive == true &&
                        pauseUploadOperations(messageTid)

                if (!pausedByTransport && currentJob != null) {
                    cancelUploadJob(operationId, currentJob)
                }

                uploadNext(messageTid)
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
                val wasSharedTransferPaused = isSharedTransferPaused(attachment)
                val wasSharing = isSharedUpload(attachment)
                pausedTaskIds.remove(attachment.messageTid)

                if (wasSharing) {
                    findCompletedUpload(attachment)?.let { url ->
                        fileTransferService.findTransferTask(attachment)?.let { task ->
                            notifyTaskResult(task, SceytResponse.Success(url))
                        }
                        removeSharedMember(attachment.messageTid)
                        return
                    }
                }

                fileTransferService.findTransferTask(attachment)
                    ?.resumePauseCallback
                    ?.onResumePause(attachment.toTransferData(WaitingToUpload))

                if (wasSharing) {
                    val task = fileTransferService.findOrCreateTransferTask(attachment)
                    if (wasSharedTransferPaused) {
                        resumeSharedUpload(attachment, task)
                    } else if (findActiveSharedUpload(getSharedMessageIds(attachment)) == null) {
                        startSharedUpload(attachment, task)
                    }
                    return
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

    fun cancelAll() {
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()

        synchronized(uploadQueueLock) {
            pendingUploadQueue.clear()
            currentUploadingAttachment = null
        }

        pausedTaskIds.clear()
        synchronized(sharingFilesLock) {
            sharingFilesPath.clear()
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
            startOrResumeUpload(attachment, task)
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

        startOrResumeUpload(nextUpload.first, nextUpload.second)
    }

    private fun startOrResumeUpload(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        val messageTid = attachment.messageTid
        if (pausedTaskIds.contains(messageTid)) {
            uploadNext(messageTid)
            return
        }

        val operationId = attachment.uploadOperationId
        val currentJob = uploadJobs[operationId]
        if (currentJob?.isActive == true) {
            if (resumeUploadOperations(messageTid)) return
            cancelUploadJob(operationId, currentJob)
        }

        uploadAttachment(attachment, task)
    }

    private fun pauseSharedUpload(
        attachment: SceytAttachment,
        state: TransferState,
    ): Boolean {
        val sharedMessageIds = getSharedMessageIds(attachment)
        if (sharedMessageIds.isEmpty()) return false
        if (sharedMessageIds.any { !pausedTaskIds.contains(it) }) return true

        findActiveSharedUpload(sharedMessageIds)?.let { (messageTid, job) ->
            if (state != Uploading || !pauseUploadOperations(messageTid)) {
                cancelUploadJob(uploadOperationId(messageTid), job)
            }
        }
        return true
    }

    private fun resumeSharedUpload(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        val sharedMessageIds = getSharedMessageIds(attachment)
        findActiveSharedUpload(sharedMessageIds)?.let { (messageTid, job) ->
            if (resumeUploadOperations(messageTid)) return
            cancelUploadJob(uploadOperationId(messageTid), job)
        }

        startSharedUpload(attachment, task)
    }

    private fun isSharedTransferPaused(attachment: SceytAttachment): Boolean {
        val sharedMessageIds = getSharedMessageIds(attachment)
        return sharedMessageIds.isNotEmpty() && sharedMessageIds.all(pausedTaskIds::contains)
    }

    private fun isSharedUpload(attachment: SceytAttachment): Boolean =
        synchronized(sharingFilesLock) {
            sharingFilesPath.any { it.messageTid == attachment.messageTid }
        }

    private fun getSharedMessageIds(attachment: SceytAttachment): List<Long> =
        synchronized(sharingFilesLock) {
            val sourceKey = sharingFilesPath.firstOrNull {
                it.messageTid == attachment.messageTid
            }?.sourceKey ?: return@synchronized emptyList()

            sharingFilesPath
                .filter { it.sourceKey == sourceKey }
                .map(ShareFileData::messageTid)
        }

    private fun findCompletedUpload(attachment: SceytAttachment): String? {
        return synchronized(sharingFilesLock) {
            sharingFilesPath.firstOrNull {
                it.messageTid == attachment.messageTid
            }?.completedUrl
        }
    }

    private fun saveCompletedSharedUpload(attachment: SceytAttachment, url: String) {
        updateSharedMembers(attachment) { it.completedUrl = url }
    }

    private fun saveCompletedSharedThumb(attachment: SceytAttachment, url: String) {
        updateSharedMembers(attachment) { it.completedThumbUrl = url }
    }

    private fun updateSharedMembers(
        attachment: SceytAttachment,
        update: (ShareFileData) -> Unit,
    ) {
        synchronized(sharingFilesLock) {
            val sourceKey = sharingFilesPath.firstOrNull {
                it.messageTid == attachment.messageTid
            }?.sourceKey ?: return

            sharingFilesPath.forEach { member ->
                if (member.sourceKey == sourceKey) {
                    update(member)
                }
            }
        }
    }

    private fun findActiveSharedUpload(messageIds: List<Long>): Pair<Long, Job>? {
        messageIds.forEach { messageTid ->
            val operationId = uploadOperationId(messageTid)
            uploadJobs[operationId]?.takeIf(Job::isActive)?.let { job ->
                return messageTid to job
            }
        }
        return null
    }

    private fun pauseUploadOperations(messageTid: Long): Boolean {
        if (!pauseTransport(uploadOperationId(messageTid))) return false

        val thumbOperationId = uploadOperationId(messageTid, TransferRole.Thumbnail)
        uploadJobs[thumbOperationId]?.takeIf(Job::isActive)?.let { job ->
            if (!pauseTransport(thumbOperationId)) {
                cancelUploadJob(thumbOperationId, job)
            }
        }
        return true
    }

    private fun resumeUploadOperations(messageTid: Long): Boolean {
        if (!resumeTransport(uploadOperationId(messageTid))) return false

        val thumbOperationId = uploadOperationId(messageTid, TransferRole.Thumbnail)
        uploadJobs[thumbOperationId]?.takeIf(Job::isActive)?.let { job ->
            if (!resumeTransport(thumbOperationId)) {
                cancelUploadJob(thumbOperationId, job)
            }
        }
        return true
    }

    private fun pauseTransport(operationId: String): Boolean {
        return runCatching {
            SceytChatUIKit.fileTransfer.transport.pause(operationId)
        }.getOrDefault(false)
    }

    private fun resumeTransport(operationId: String): Boolean {
        return runCatching {
            SceytChatUIKit.fileTransfer.transport.resume(operationId)
        }.getOrDefault(false)
    }

    private fun cancelUploadJob(operationId: String, job: Job) {
        if (uploadJobs.remove(operationId, job)) {
            job.cancel()
        }
    }

    private suspend fun <T> withVideoThumbUpload(
        attachment: SceytAttachment,
        tasks: () -> List<TransferTask>,
        checksumData: FileChecksumData?,
        block: suspend () -> T,
    ): T = coroutineScope {
        if (attachment.type != AttachmentTypeEnum.Video.value) {
            return@coroutineScope block()
        }

        val knownThumbUrl = attachment.getVideoThumbUrl()?.takeIf(String::isNotBlank)
            ?: getVideoThumbUrl(checksumData?.metadata)?.takeIf(String::isNotBlank)
        if (knownThumbUrl != null) {
            saveCompletedSharedThumb(attachment, knownThumbUrl)
            videoThumbUploader.applyThumbUrl(attachment, tasks(), knownThumbUrl)
            return@coroutineScope block()
        }

        if (!attachment.needsVideoThumbUpload()) {
            return@coroutineScope block()
        }

        val thumbJob = launch {
            try {
                videoThumbUploader.uploadThumb(attachment)?.let { url ->
                    saveCompletedSharedThumb(attachment, url)
                    val targetTasks = tasks()
                    scope.launch {
                        videoThumbUploader.applyThumbUrl(attachment, targetTasks, url)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                SceytLog.e(TAG, "Video thumb upload failed", error)
            }
        }
        val thumbOperationId = uploadOperationId(attachment.messageTid, TransferRole.Thumbnail)
        uploadJobs.put(thumbOperationId, thumbJob)?.cancel()

        try {
            block()
        } finally {
            uploadJobs.remove(thumbOperationId, thumbJob)
            thumbJob.cancel()
        }
    }

    private fun uploadAttachment(
        attachment: SceytAttachment,
        task: TransferTask,
    ) = launchUploadJob(
        attachment = attachment,
        onError = { response -> notifyTaskResult(task, response) },
    ) {
        if (pausedTaskIds.contains(attachment.messageTid)) {
            uploadNext(attachment.messageTid)
            return@launchUploadJob
        }

        val checksum = getAttachmentChecksum(attachment.sourcePath)
        val response: SceytResponse<String>? = withVideoThumbUpload(
            attachment = attachment,
            tasks = { listOf(task) },
            checksumData = checksum,
        ) {
            val (uploaded, url) = checkMaybeAlreadyUploadedWithAnotherMessage(checksum, task)

            if (uploaded && url != null) {
                return@withVideoThumbUpload SceytResponse.Success(url)
            }

            val result = prepareAttachment(
                attachment = attachment,
                checksumData = checksum,
                task = task,
            )
            currentCoroutineContext().ensureActive()

            if (pausedTaskIds.contains(attachment.messageTid)) {
                return@withVideoThumbUpload null
            }

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

            uploadAttachmentWithTransport(uploadAttachment, task)
        }

        response?.let { notifyTaskResult(task, it) }
        uploadNext(attachment.messageTid)
    }

    private suspend fun uploadSharedAttachment(
        attachment: SceytAttachment,
        task: TransferTask,
    ): SceytResponse<String> {
        return uploadAttachmentWithTransport(
            attachment = attachment,
            task = task,
            isSharedUpload = true,
            onProgress = { progressPercent ->
                getAppropriateTasks(task).forEach { transferTask ->
                    transferTask.state = Uploading

                    runCatching {
                        transferTask.progressCallback?.onProgress(
                            TransferData(
                                messageTid = transferTask.messageTid,
                                progressPercent = progressPercent,
                                state = Uploading,
                                filePath = transferTask.attachment.filePath,
                                url = null,
                            ),
                        )
                    }.onFailure(::logCallbackFailure)
                }
            },
        )
    }

    private suspend fun uploadAttachmentWithTransport(
        attachment: SceytAttachment,
        task: TransferTask,
        isSharedUpload: Boolean = false,
        onProgress: ((Float) -> Unit)? = null,
    ): SceytResponse<String> {
        val sourcePath = attachment.filePath
        require(!sourcePath.isNullOrBlank()) { "Attachment source path is missing" }

        val sourceFile = File(sourcePath)
        require(sourceFile.isFile) { "Attachment source file does not exist: $sourcePath" }

        val request = FileUploadRequest(
            operationId = attachment.uploadOperationId,
            sourceFile = sourceFile,
            fileName = attachment.name,
            mimeType = getMimeType(sourceFile.path),
            attachment = attachment,
            isSharedUpload = isSharedUpload,
        )

        return performUpload(request, attachment, task, onProgress)
    }

    private fun launchUploadJob(
        attachment: SceytAttachment,
        onError: (SceytResponse.Error<String>) -> Unit,
        block: suspend () -> Unit,
    ) {
        val messageTid = attachment.messageTid
        val operationId = attachment.uploadOperationId
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                SceytLog.e(TAG, "Upload preparation failed", error)
                runCatching { onError(SceytResponse.Error(error.toSceytException())) }
                uploadNext(messageTid)
            } finally {
                uploadJobs.remove(operationId, currentCoroutineContext().job)
            }
        }

        uploadJobs.put(operationId, job)?.cancel()
        job.start()
    }

    private suspend fun performUpload(
        request: FileUploadRequest,
        attachment: SceytAttachment,
        task: TransferTask,
        onProgress: ((Float) -> Unit)?,
    ): SceytResponse<String> = coroutineScope {
        val networkWaitTriggered = AtomicBoolean()
        lateinit var transferJob: Deferred<String?>

        val response = try {
            transferJob = async(start = CoroutineStart.LAZY) {
                SceytChatUIKit.fileTransfer.transport.upload(
                    request = request,
                    callback = { event ->
                        if (transferJob.isActive &&
                            (request.isSharedUpload || attachment.messageTid !in pausedTaskIds)
                        ) {
                            when (event) {
                                is FileTransferEvent.WaitingForNetwork -> {
                                    if (networkWaitTriggered.compareAndSet(false, true)) {
                                        transferJob.cancel()
                                    }
                                }

                                else -> handleUploadEvent(event, attachment, task, onProgress)
                            }
                        }
                    },
                )
            }
            transferJob.start()
            val result = transferJob.await().takeUnless { it.isNullOrBlank() }
                ?: throw IllegalStateException("File upload returned an empty remote reference")
            currentCoroutineContext().ensureActive()
            SceytResponse.Success(result)
        } catch (error: CancellationException) {
            if (!networkWaitTriggered.get()) throw error
            SceytResponse.Error(SceytException(0, "Waiting for network"))
        } catch (error: Throwable) {
            SceytResponse.Error(error.toSceytException())
        }

        response
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
                val resizeConfig = SceytChatUIKit.config.imageAttachmentResizeConfig
                resizeImage(
                    path = attachment.filePath,
                    parentDir = context.filesDir,
                    reqSize = resizeConfig.dimensionThreshold,
                    quality = resizeConfig.compressionQuality,
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

        continuation.invokeOnCancellation {
            completed.set(true)
            VideoTranscodeHelper.cancel(attachment.filePath)
        }

        transcodeVideo(
            path = attachment.filePath,
            parentDir = context.filesDir,
            config = SceytChatUIKit.config.videoAttachmentResizeConfig,
            progressCallback = { data ->
                if (continuation.isActive && !pausedTaskIds.contains(attachment.messageTid)) {
                    task.preparingCallback?.onPreparing(
                        attachment.toTransferData(Preparing, data.progressPercent),
                    )
                }
            },
        ) { result ->
            if (completed.compareAndSet(false, true)) {
                continuation.resume(result)
            }
        }
    }

    private fun getAppropriateTasks(
        task: TransferTask,
    ): List<TransferTask> = synchronized(sharingFilesLock) {
        getAppropriateTasksLocked(task, includePaused = false)
    }

    private fun getAppropriateTasksIncludingPaused(
        task: TransferTask,
    ): List<TransferTask> = synchronized(sharingFilesLock) {
        getAppropriateTasksLocked(task, includePaused = true)
    }

    private fun takeAppropriateTasks(
        task: TransferTask,
    ): List<TransferTask> = synchronized(sharingFilesLock) {
        getAppropriateTasksLocked(task, includePaused = false).also {
            val completedMessageIds = it.mapTo(HashSet(), TransferTask::messageTid)
            sharingFilesPath.removeAll { member ->
                completedMessageIds.contains(member.messageTid)
            }
        }
    }

    private fun getAppropriateTasksLocked(
        task: TransferTask,
        includePaused: Boolean,
    ): List<TransferTask> {
        val sourceKey = sharingFilesPath.firstOrNull {
            it.messageTid == task.messageTid
        }?.sourceKey ?: return emptyList()

        val sharedTasks = sharingFilesPath.filter {
            it.sourceKey == sourceKey
        }

        return fileTransferService.getTasks().values.filter { transferTask ->
            sharedTasks.any { data ->
                data.messageTid == transferTask.attachment.messageTid
            } && (includePaused || !pausedTaskIds.contains(transferTask.messageTid))
        }
    }

    private fun removeSharedMember(messageTid: Long) {
        synchronized(sharingFilesLock) {
            sharingFilesPath.removeAll { it.messageTid == messageTid }
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

    private fun notifyTaskResult(
        task: TransferTask,
        response: SceytResponse<String>,
    ) {
        runCatching {
            task.uploadResultCallback?.onResult(response)
        }.onFailure(::logCallbackFailure)
    }

    private fun logCallbackFailure(error: Throwable) {
        SceytLog.e(TAG, "File transfer callback failed", error)
    }

    private val SceytAttachment.sourcePath: String?
        get() = originalFilePath?.takeIf(String::isNotBlank)
            ?: filePath?.takeIf(String::isNotBlank)

    private val SceytAttachment.sharedSourceKey: String
        get() = sourcePath ?: uploadOperationId

    private val SceytAttachment.uploadOperationId: String
        get() = uploadOperationId(messageTid)

    private data class ShareFileData(
        val sourceKey: String,
        val messageTid: Long,
    ) {
        var completedUrl: String? = null
        var completedThumbUrl: String? = null
    }

    private companion object {
        const val TAG = "FileTransferLogic"
    }
}
