package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque

internal class AttachmentUploadCoordinator(
    context: Context,
    attachmentLogic: PersistenceAttachmentLogic,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val preparer = UploadPreparer(context, attachmentLogic)
    private val commands = Channel<() -> Unit>(Channel.UNLIMITED)
    private val uploads = linkedSetOf<Upload>()
    private val pending = ArrayDeque<Upload>()
    private var current: Upload? = null

    init {
        // One coroutine owns upload state. Reentrant callbacks enqueue their next change.
        scope.launch {
            for (command in commands) notify(command)
        }
    }

    fun uploadFile(
        attachment: SceytAttachment,
        task: TransferTask,
        onResult: (TransferTask, SceytResponse<String>) -> Unit,
        onPaused: (TransferTask) -> Unit,
    ) = enqueue(attachment, task, shared = false, onResult, onPaused)

    fun uploadSharedFile(
        attachment: SceytAttachment,
        task: TransferTask,
        onResult: (TransferTask, SceytResponse<String>) -> Unit,
        onPaused: (TransferTask) -> Unit,
    ) = enqueue(attachment, task, shared = true, onResult, onPaused)

    private fun enqueue(
        attachment: SceytAttachment,
        task: TransferTask,
        shared: Boolean,
        onResult: (TransferTask, SceytResponse<String>) -> Unit,
        onPaused: (TransferTask) -> Unit,
    ) = dispatch {
        findMember(task)?.let { member ->
            if (task.state == PauseUpload) {
                pauseMember(member)
                notify { onPaused(task) }
            } else if (member.paused) resume(member)
            return@dispatch
        }
        val sharingKey = if (shared) attachment.originalFilePath?.takeIf(String::isNotBlank)
            ?: attachment.filePath?.takeIf(String::isNotBlank)
            ?: attachment.uploadOperationId else null
        // Completed groups retain only paused callers; a new share starts a new operation.
        val upload = uploads.firstOrNull {
            shared && it.sharingKey == sharingKey && it.result?.isSuccess != true
        } ?: Upload(sharingKey).also(uploads::add)
        val member = Member(upload, attachment, task, onResult)
        upload.members[task] = member
        upload.result = null
        // A pause command can run after the worker calls upload but before this admission.
        if (task.state == PauseUpload) {
            member.paused = true
            notify { onPaused(task) }
        } else admit(upload)
    }

    fun pause(task: TransferTask, onPaused: () -> Unit) = dispatch {
        findMember(task)?.let(::pauseMember)
        notify(onPaused)
    }

    private fun pauseMember(member: Member) {
        if (member.paused) return
        member.paused = true
        if (!member.upload.hasActiveMembers) {
            pauseOperation(member.upload)
            pending.remove(member.upload)
            releaseTurn(member.upload)
        }
    }

    fun cancel(task: TransferTask) = dispatch {
        val member = findMember(task) ?: return@dispatch
        removeMember(member)
        val upload = member.upload
        if (!upload.hasActiveMembers) {
            if (upload.members.isEmpty()) stopAttempt(upload) else pauseOperation(upload)
            pending.remove(upload)
            releaseTurn(upload)
        }
    }

    fun cancelAll() = dispatch {
        val active = uploads.toList()
        uploads.clear()
        pending.clear()
        current = null
        active.forEach { upload ->
            upload.members.values.forEach { it.finished = true }
            upload.members.clear()
            stopAttempt(upload)
        }
    }

    private fun admit(upload: Upload) {
        if (!upload.hasActiveMembers || upload !in uploads) return
        if (!upload.shared && current !== upload) {
            if (current != null) {
                if (upload !in pending) pending.addLast(upload)
                return
            }
            current = upload
        }
        upload.attempt?.let { attempt ->
            if (!attempt.paused) return
            if (attempt.completed || attempt.job.isActive && resumeTransport(attempt)) {
                attempt.paused = false
                return
            }
            stopAttempt(upload)
        }
        val owner = upload.members.values.firstOrNull { !it.paused } ?: return
        val attempt = Attempt(owner.attachment.uploadOperationId)
        attempt.job = scope.launch(start = CoroutineStart.LAZY) { performUpload(owner, attempt) }
        upload.attempt = attempt
        attempt.job.start()
    }

    private suspend fun performUpload(owner: Member, attempt: Attempt) {
        val result = try {
            val prepared = preparer.prepare(
                attachment = owner.attachment,
                onFilePrepared = { path ->
                    // Preparation belongs to the physical upload, even if its first caller is deleted.
                    if (!owner.finished) owner.task.updateFileLocationCallback?.onUpdateFileLocation(
                        path
                    )
                },
                onProgress = { progress ->
                    dispatch {
                        if (owner.upload.attempt === attempt && !owner.paused && !owner.finished) {
                            owner.task.preparingCallback?.onPreparing(
                                owner.attachment.toTransferData(
                                    Preparing,
                                    progress
                                )
                            )
                        }
                    }
                },
            )
            val response = prepared.reusedUrl?.let { FileTransferResult.Success(it) } ?: run {
                currentCoroutineContext().ensureActive()
                val source = File(checkNotNull(prepared.attachment.filePath))
                attempt.filePath = source.path
                val backend = SceytChatUIKit.fileTransfer.transport
                attempt.transport = backend
                backend.upload(
                    FileUploadRequest(
                        operationId = attempt.operationId,
                        sourceFile = source,
                        fileName = prepared.attachment.name,
                        mimeType = getMimeType(source.path),
                        context = AttachmentTransferContext(
                            attachment = prepared.attachment,
                            isSharedUpload = owner.upload.shared
                        ),
                    ),
                ) { progress -> dispatch { handleProgress(owner.upload, attempt, progress) } }
            }
            currentCoroutineContext().ensureActive()
            when (response) {
                is FileTransferResult.Success -> {
                    check(response.result.isNotBlank()) { "File upload returned an empty remote reference" }
                    Result.success(response.result)
                }

                is FileTransferResult.Failure -> Result.failure(response.error)
                is FileTransferResult.Interrupted -> Result.failure(response.error)
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            Result.failure(error)
        }
        attempt.completed = true
        dispatch { finish(owner.upload, attempt, result) }
    }

    private fun handleProgress(upload: Upload, attempt: Attempt, progressPercent: Float) {
        if (upload.attempt !== attempt || !upload.hasActiveMembers) return
        upload.members.values.filter { !it.paused }.forEach { member ->
            notify {
                member.task.state = Uploading
                member.task.progressCallback?.onProgress(
                    TransferData(
                        messageTid = member.task.messageTid,
                        progressPercent = progressPercent,
                        state = Uploading,
                        filePath = if (upload.shared)
                            member.task.attachment.filePath else attempt.filePath,
                        url = null,
                    ),
                )
            }
        }
    }

    private fun finish(upload: Upload, attempt: Attempt, result: Result<String>) {
        if (upload.attempt !== attempt) return
        upload.attempt = null
        upload.result = result
        val finished = upload.members.values.filter {
            !it.paused || !upload.shared && result.isFailure
        }
        finished.forEach(::removeMember)
        pending.remove(upload)
        releaseTurn(upload)
        finished.forEach { complete(it, result) }
    }

    private fun resume(member: Member) {
        val upload = member.upload
        upload.result?.getOrNull()?.let { url ->
            removeMember(member)
            complete(member, Result.success(url))
            return
        }
        upload.result = null
        member.paused = false
        notify {
            member.task.state = WaitingToUpload
            member.task.resumePauseCallback?.onResumePause(
                member.task.attachment.toTransferData(
                    WaitingToUpload
                )
            )
        }
        dispatch { admit(upload) }
    }

    private fun pauseOperation(upload: Upload) {
        val attempt = upload.attempt ?: return
        if (attempt.completed || attempt.paused) return
        val paused = attempt.job.isActive && runCatching {
            attempt.transport?.pause(attempt.operationId) == true
        }.getOrDefault(false)
        if (paused) attempt.paused = true else stopAttempt(upload)
    }

    private fun resumeTransport(attempt: Attempt): Boolean = runCatching {
        attempt.transport?.resume(attempt.operationId) == true
    }.getOrDefault(false)

    private fun stopAttempt(upload: Upload) {
        val attempt = upload.attempt
        upload.attempt = null
        attempt?.job?.cancel()
    }

    private fun releaseTurn(upload: Upload) {
        if (current !== upload) return
        current = pending.pollFirst()
        current?.let { next -> dispatch { admit(next) } }
    }

    private fun findMember(task: TransferTask): Member? =
        uploads.firstNotNullOfOrNull { it.members[task] }

    private fun removeMember(member: Member) {
        member.finished = true
        member.upload.members.remove(member.task)
        if (member.upload.members.isEmpty()) uploads.remove(member.upload)
    }

    private fun complete(member: Member, result: Result<String>) = notify {
        member.onResult(
            member.task, result.fold(
                onSuccess = { SceytResponse.Success(it) },
                onFailure = {
                    SceytResponse.Error(it as? SceytException ?: SceytException(0, it.message))
                },
            )
        )
    }

    private fun dispatch(command: () -> Unit) {
        commands.trySend(command)
    }

    private fun notify(callback: () -> Unit) {
        runCatching(callback).onFailure { SceytLog.e(TAG, "File transfer callback failed", it) }
    }

    private class Upload(val sharingKey: String?) {
        val shared get() = sharingKey != null
        val members = linkedMapOf<TransferTask, Member>()
        val hasActiveMembers get() = members.values.any { !it.paused }
        var attempt: Attempt? = null
        var result: Result<String>? = null
    }

    private class Attempt(val operationId: String) {
        lateinit var job: Job
        var paused = false
        var filePath: String? = null

        @Volatile
        var transport: FileTransferTransport? = null

        @Volatile
        var completed = false
    }

    private class Member(
        val upload: Upload,
        val attachment: SceytAttachment,
        val task: TransferTask,
        val onResult: (TransferTask, SceytResponse<String>) -> Unit,
    ) {
        var paused = false

        @Volatile
        var finished = false
    }

    private val SceytAttachment.uploadOperationId get() = "upload:$messageTid"

    private companion object {
        const val TAG = "AttachmentUploadCoordinator"
    }
}
