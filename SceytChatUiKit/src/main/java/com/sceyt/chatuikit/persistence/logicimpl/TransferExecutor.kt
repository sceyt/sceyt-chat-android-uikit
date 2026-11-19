package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Executes a routed transfer and keeps its controls bound to the selected provider. */
internal class TransferExecutor(
    val operationId: String,
    private val provider: FileTransferProvider,
    private val execute: suspend (TransferExecutor, FileTransferCallback) -> FileTransferResult<String>,
) {
    var attachment: SceytAttachment? = null
        internal set

    @Volatile
    private var job: Job? = null

    val isActive get() = job?.isActive == true
    val isCancelled get() = job?.isCancelled == true

    @Volatile
    private var transferring = false

    @OptIn(DelicateCoroutinesApi::class)
    fun start(
        scope: CoroutineScope,
        previous: TransferExecutor?,
        pendingCleanup: Job? = null,
        block: suspend () -> Unit,
    ) {
        job = scope.launch(start = CoroutineStart.ATOMIC) {
            // Even a canceled startup must wait for the previous writer to stop.
            withContext(NonCancellable) {
                previous?.awaitStopped()
                if (previous != null && previous.operationId != operationId)
                    previous.cancel()
                pendingCleanup?.join()
            }
            currentCoroutineContext().ensureActive()
            block()
        }
    }

    /** Stops this execution while keeping the provider's resumable state. */
    fun stop() {
        job?.cancel()
    }

    /** Stops this execution before deleting the provider's retained session. */
    suspend fun cancel() {
        stop()
        withContext(NonCancellable) {
            awaitStopped()
            provider.cancel(operationId)
        }
    }

    private suspend fun awaitStopped() {
        job?.join()
    }

    suspend fun execute(callback: FileTransferCallback): FileTransferResult<String> =
        execute.invoke(this, callback)

    suspend fun <T> executeTransport(
        block: suspend () -> FileTransferResult<T>,
    ): FileTransferResult<T> {
        currentCoroutineContext().ensureActive()
        transferring = true
        return try {
            block()
        } finally {
            transferring = false
        }
    }

    fun pause(): Boolean = transferring && provider.pause(operationId)

    fun resume(): Boolean = transferring && provider.resume(operationId)
}
