package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal class FileDownloadCoordinator(
    private val scope: CoroutineScope,
    private val transport: () -> FileTransferTransport,
) {
    private val control = FileTransferControl()
    private val operations = mutableMapOf<String, Operation>()

    fun download(
        request: FileDownloadRequest,
        participantId: String,
        observer: FileTransferObserver,
        bind: (FileTransferSubscription) -> Unit,
    ) = control.execute {
        val operation = operations.getOrPut(request.operationId) { Operation(request) }
        val existing = operation.members[participantId]
        if (existing != null) {
            bind(existing)
            return@execute
        }

        val member = Member(operation, participantId, observer)
        operation.members[participantId] = member
        bind(member)
        notify { observer.onStarted() }
        control.execute { start(operation) }
    }

    private fun start(operation: Operation) {
        if (operation.attempt != null || operation.members.values.none { !it.isPaused }) return
        if (operations[operation.request.operationId] !== operation) return

        val attempt = Attempt()
        attempt.job = scope.launch(start = CoroutineStart.LAZY) {
            val result = try {
                val backend = transport()
                attempt.transport = backend
                val path = backend.download(operation.request) { event ->
                    control.execute { handleEvent(operation, attempt, event) }
                }.takeUnless { it.isNullOrBlank() }
                    ?: throw IllegalStateException("File download returned an empty local path")
                currentCoroutineContext().ensureActive()
                Result.success(path)
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
        control.start(attempt.job)
    }

    private fun handleEvent(operation: Operation, attempt: Attempt, event: FileTransferEvent) {
        if (operation.attempt !== attempt || operation.members.values.all { it.isPaused }) return

        when (event) {
            is FileTransferEvent.Progress -> {
                operation.members.values.filter { !it.isPaused }.forEach {
                    notify { it.observer.onProgress(event.progressPercent) }
                }
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
        operations.remove(operation.request.operationId, operation)
        operation.attempt = null
        if (result.isFailure && !attempt.waitingForNetwork) operation.request.destinationFile.delete()

        val members = operation.members.values.toList()
        operation.members.clear()
        members.forEach { it.finished = true }
        members.forEach { notify { it.observer.onResult(result) } }
    }

    private fun pause(member: Member) = control.execute {
        if (member.finished || member.paused) return@execute
        member.paused = true
        val operation = member.operation
        if (operation.members.values.all { it.isPaused } && operation.attempt?.completed != true) {
            val attempt = operation.attempt
            val paused = attempt?.job?.isActive == true && attempt.transport?.let {
                runCatching { it.pause(operation.request.operationId) }.getOrDefault(false)
            } == true
            if (!paused) stopAttempt(operation)
        }
        notify { member.observer.onPaused() }
    }

    private fun resume(member: Member) = control.execute {
        if (member.finished || !member.paused) return@execute
        val operation = member.operation
        val wasPaused = operation.members.values.all { it.isPaused }
        member.paused = false
        var restarted = false
        if (wasPaused && operation.attempt?.completed != true) {
            val attempt = operation.attempt
            val resumed = attempt?.job?.isActive == true && attempt.transport?.let {
                runCatching { it.resume(operation.request.operationId) }.getOrDefault(false)
            } == true
            if (!resumed) {
                stopAttempt(operation)
                restarted = true
            }
        }
        notify { member.observer.onResumed(restarted) }
        control.execute { start(operation) }
    }

    private fun cancel(member: Member) = control.execute {
        if (member.finished) return@execute
        member.finished = true
        val operation = member.operation
        operation.members.remove(member.id)
        if (operation.members.isEmpty()) {
            operations.remove(operation.request.operationId, operation)
            stopAttempt(operation)
        } else if (operation.members.values.all { it.isPaused }) {
            stopAttempt(operation)
        }
    }

    private fun stopAttempt(operation: Operation) {
        val attempt = operation.attempt
        operation.attempt = null
        attempt?.job?.cancel()
    }

    fun cancelAll() = control.execute {
        val active = operations.values.toList()
        operations.clear()
        active.forEach { operation ->
            operation.members.values.forEach { it.finished = true }
            operation.members.clear()
            stopAttempt(operation)
        }
    }

    private fun notify(callback: () -> Unit) {
        runCatching(callback).onFailure {
            SceytLog.e("FileDownloadCoordinator", "File transfer callback failed", it)
        }
    }

    private class Operation(val request: FileDownloadRequest) {
        val members = mutableMapOf<String, Member>()
        var attempt: Attempt? = null
    }

    private class Attempt {
        lateinit var job: Job
        @Volatile
        var transport: FileTransferTransport? = null
        @Volatile
        var waitingForNetwork = false
        @Volatile
        var completed = false
    }

    private inner class Member(
        val operation: Operation,
        val id: String,
        val observer: FileTransferObserver,
    ) : FileTransferSubscription {
        @Volatile
        var paused = false
        @Volatile
        var finished = false
        override val isPaused get() = paused
        override val isFinished get() = finished

        override fun pause() = pause(this)
        override fun resume() = resume(this)
        override fun cancel() = cancel(this)
    }
}
