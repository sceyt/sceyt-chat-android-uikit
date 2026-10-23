package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque

internal sealed interface FileUploadPreparation {
    data class Ready(val request: FileUploadRequest) : FileUploadPreparation
    data class Reused(val url: String) : FileUploadPreparation
}

internal class FileUploadCoordinator(
    private val scope: CoroutineScope,
    private val transport: () -> FileTransferTransport,
) {
    private val control = FileTransferControl()
    private val operations = mutableMapOf<String, Operation>()
    private val pending = ArrayDeque<Operation>()
    private var current: Operation? = null

    fun upload(
        operationId: String,
        participantId: String,
        sharingKey: String? = null,
        sequential: Boolean = true,
        prepare: suspend () -> FileUploadPreparation,
        observer: FileTransferObserver,
        bind: (FileTransferSubscription) -> Unit,
    ) = control.execute {
        val key = if (sharingKey == null) "single:$operationId" else "shared:$sharingKey"
        val operation = operations.getOrPut(key) { Operation(key, sequential, sharingKey != null) }
        val existing = operation.members[participantId]
        if (existing != null) {
            bind(existing)
            return@execute
        }

        val member = Member(operation, participantId, operationId, prepare, observer)
        operation.members[participantId] = member
        bind(member)
        operation.result = null
        control.execute { admit(operation) }
    }

    private fun admit(operation: Operation) {
        if (!operation.hasActiveMembers || operations[operation.key] !== operation) return
        if (operation.sequential && current !== operation) {
            if (current != null) {
                if (!pending.contains(operation)) pending.addLast(operation)
                return
            }
            current = operation
        }

        operation.attempt?.let { attempt ->
            if (!attempt.paused) return
            if (attempt.completed || attempt.job.isActive && resumeTransport(attempt)) {
                attempt.paused = false
                return
            }
            stopAttempt(operation)
        }
        start(operation)
    }

    private fun start(operation: Operation) {
        val owner = operation.members.values.firstOrNull { !it.isPaused } ?: return
        val attempt = Attempt(owner.operationId)
        attempt.job = scope.launch(start = CoroutineStart.LAZY) {
            val result = try {
                when (val prepared = owner.prepare()) {
                    is FileUploadPreparation.Reused -> Result.success(prepared.url)
                    is FileUploadPreparation.Ready -> {
                        currentCoroutineContext().ensureActive()
                        val backend = transport()
                        attempt.transport = backend
                        backend.upload(prepared.request) { event ->
                            control.execute { handleEvent(operation, attempt, event) }
                        }
                    }
                }.onSuccess { url ->
                    check(url.isNotBlank()) { "File upload returned an empty remote reference" }
                    currentCoroutineContext().ensureActive()
                }.onFailure {
                    if (!attempt.waitingForNetwork) currentCoroutineContext().ensureActive()
                }
            } catch (error: Throwable) {
                if (!attempt.waitingForNetwork) currentCoroutineContext().ensureActive()
                Result.failure(
                    if (attempt.waitingForNetwork && error is CancellationException)
                        IllegalStateException("Waiting for network") else error,
                )
            }
            attempt.completed = true
            control.execute { finish(operation, attempt, result) }
        }
        operation.attempt = attempt
        operation.members.values.filter { !it.isPaused }.forEach { notify { it.observer.onStarted() } }
        control.execute {
            if (operation.attempt === attempt && operation.hasActiveMembers) control.start(attempt.job)
        }
    }

    private fun handleEvent(operation: Operation, attempt: Attempt, event: FileTransferEvent) {
        if (operation.attempt !== attempt || !operation.hasActiveMembers) return
        when (event) {
            is FileTransferEvent.Progress -> operation.members.values.filter { !it.isPaused }.forEach {
                notify { it.observer.onProgress(event.progressPercent) }
            }

            FileTransferEvent.WaitingForNetwork -> {
                if (!attempt.waitingForNetwork) {
                    attempt.waitingForNetwork = true
                    attempt.job.cancel()
                }
            }
        }
    }

    private fun finish(operation: Operation, attempt: Attempt, result: Result<String>) {
        if (operation.attempt !== attempt) return
        operation.attempt = null
        operation.result = result
        val finished = operation.members.values.filter { !operation.shared || !it.isPaused }
        finished.forEach {
            operation.members.remove(it.id)
            it.finished = true
        }
        // Failed groups keep only their paused participants, so a later share can join and restart.
        // Successful paused participants retain their result through their subscription alone.
        if (result.isSuccess || operation.members.isEmpty()) operations.remove(operation.key, operation)
        pending.remove(operation)
        releaseTurn(operation)
        finished.forEach { notify { it.observer.onResult(result) } }
    }

    private fun pause(member: Member) = control.execute {
        if (member.finished || member.paused) return@execute
        member.paused = true
        val operation = member.operation
        if (!operation.hasActiveMembers) {
            pauseOperation(operation)
            pending.remove(operation)
            releaseTurn(operation)
        }
        notify { member.observer.onPaused() }
    }

    private fun pauseOperation(operation: Operation) {
        val attempt = operation.attempt ?: return
        if (attempt.completed) return
        val paused = attempt.job.isActive && attempt.transport?.let {
            runCatching { it.pause(attempt.operationId) }.getOrDefault(false)
        } == true
        if (paused) attempt.paused = true else stopAttempt(operation)
    }

    private fun resume(member: Member) = control.execute {
        if (member.finished || !member.paused) return@execute
        var operation = member.operation
        operation.result?.getOrNull()?.let { url ->
            operation.members.remove(member.id)
            member.finished = true
            notify { member.observer.onResult(Result.success(url)) }
            return@execute
        }
        val existing = operations[operation.key]
        if (existing != null && existing !== operation) {
            operation.members.remove(member.id)
            operation = existing
            member.operation = operation
            operation.members[member.id] = member
        } else {
            operations[operation.key] = operation
        }
        operation.result = null
        member.paused = false
        notify { member.observer.onResumed(operation.attempt == null) }
        control.execute { admit(operation) }
    }

    private fun cancel(member: Member) = control.execute {
        if (member.finished) return@execute
        member.finished = true
        val operation = member.operation
        operation.members.remove(member.id)
        if (operation.members.isEmpty()) {
            operations.remove(operation.key, operation)
            pending.remove(operation)
            stopAttempt(operation)
            releaseTurn(operation)
        } else if (!operation.hasActiveMembers) {
            stopAttempt(operation)
            pending.remove(operation)
            releaseTurn(operation)
        }
    }

    private fun releaseTurn(operation: Operation) {
        if (current !== operation) return
        current = pending.pollFirst()
        current?.let { next -> control.execute { admit(next) } }
    }

    private fun stopAttempt(operation: Operation) {
        val attempt = operation.attempt
        operation.attempt = null
        attempt?.job?.cancel()
    }

    private fun resumeTransport(attempt: Attempt): Boolean = runCatching {
        attempt.transport?.resume(attempt.operationId) == true
    }.getOrDefault(false)

    fun cancelAll() = control.execute {
        val active = operations.values.toList()
        operations.clear()
        pending.clear()
        current = null
        active.forEach { operation ->
            operation.members.values.forEach { it.finished = true }
            operation.members.clear()
            stopAttempt(operation)
        }
    }

    private fun notify(callback: () -> Unit) {
        runCatching(callback).onFailure {
            SceytLog.e("FileUploadCoordinator", "File transfer callback failed", it)
        }
    }

    private class Operation(val key: String, val sequential: Boolean, val shared: Boolean) {
        val members = mutableMapOf<String, Member>()
        val hasActiveMembers get() = members.values.any { !it.isPaused }
        var attempt: Attempt? = null
        var result: Result<String>? = null
    }

    private class Attempt(val operationId: String) {
        lateinit var job: Job
        var paused = false
        @Volatile var transport: FileTransferTransport? = null
        @Volatile var waitingForNetwork = false
        @Volatile var completed = false
    }

    private inner class Member(
        var operation: Operation,
        val id: String,
        val operationId: String,
        val prepare: suspend () -> FileUploadPreparation,
        val observer: FileTransferObserver,
    ) : FileTransferSubscription {
        @Volatile var paused = false
        @Volatile var finished = false
        override val isPaused get() = paused
        override val isFinished get() = finished

        override fun pause() = pause(this)
        override fun resume() = resume(this)
        override fun cancel() = cancel(this)
    }
}
