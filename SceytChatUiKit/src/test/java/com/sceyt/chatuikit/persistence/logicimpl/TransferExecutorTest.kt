package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileTransferProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TransferExecutorTest {
    private val provider = mock<FileTransferProvider>()

    @Test
    fun `controls reach the selected provider only while the transport is running`() = runTest {
        val result = CompletableDeferred<FileTransferResult<String>>()
        val progress = mutableListOf<Float>()
        whenever(provider.pause("download:42")).thenReturn(true)
        whenever(provider.resume("download:42")).thenReturn(true)
        val executor = TransferExecutor("download:42", provider) { transfer, callback ->
            transfer.executeTransport {
                callback.onProgress(25f)
                result.await()
            }
        }
        assertThat(executor.pause()).isFalse()
        assertThat(executor.resume()).isFalse()
        verifyNoInteractions(provider)

        val attempt = async { executor.execute { progress += it } }
        runCurrent()

        assertThat(executor.pause()).isTrue()
        assertThat(executor.resume()).isTrue()
        verify(provider).pause("download:42")
        verify(provider).resume("download:42")
        result.complete(FileTransferResult.Success("file"))
        assertThat(attempt.await()).isEqualTo(FileTransferResult.Success("file"))
        assertThat(progress).containsExactly(25f)
        assertThat(executor.pause()).isFalse()
        assertThat(executor.resume()).isFalse()
    }

    @Test
    fun `transport failures keep their cause and disable controls`() = runTest {
        val error = IOException("connection failed")
        val executor = TransferExecutor("upload:42", provider) { transfer, _ ->
            transfer.executeTransport { throw error }
        }

        assertThat(runCatching { executor.execute {} }.exceptionOrNull()).isSameInstanceAs(error)
        assertThat(executor.pause()).isFalse()
        assertThat(executor.resume()).isFalse()
        verifyNoInteractions(provider)
    }

    @Test
    fun `cancelling the transport disables its controls`() = runTest {
        val blocked = CompletableDeferred<FileTransferResult<String>>()
        val executor = TransferExecutor("download:42", provider) { transfer, _ ->
            transfer.executeTransport { blocked.await() }
        }
        val attempt = async { executor.execute {} }
        runCurrent()

        attempt.cancelAndJoin()

        assertThat(executor.pause()).isFalse()
        assertThat(executor.resume()).isFalse()
        verifyNoInteractions(provider)
    }

    @Test
    fun `cancel before execution cleans up the selected provider`() = runTest {
        val executor = TransferExecutor("upload:42", provider) { _, _ -> FileTransferResult.Success("url") }

        executor.cancel()

        verify(provider).cancel("upload:42")
    }

    @Test
    fun `cancel waits for the running provider to stop before cleanup`() = runTest {
        val releaseStop = CompletableDeferred<Unit>()
        val executor = TransferExecutor("upload:42", provider) { transfer, _ ->
            transfer.executeTransport {
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) { releaseStop.await() }
                }
            }
        }
        executor.start(this, previous = null) { executor.execute {} }
        runCurrent()

        val cancellation = launch { executor.cancel() }
        runCurrent()
        verifyNoInteractions(provider)
        releaseStop.complete(Unit)
        cancellation.join()

        verify(provider).cancel("upload:42")
    }

    @Test
    fun `cancel during startup waits for the previous execution without starting another upload`() = runTest {
        val releaseStop = CompletableDeferred<Unit>()
        val previous = TransferExecutor("upload:42", provider) { _, _ -> FileTransferResult.Success("url") }
        previous.start(this, previous = null) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { releaseStop.await() }
            }
        }
        runCurrent()
        previous.stop()
        var uploads = 0
        val replacement = TransferExecutor("upload:42", provider) { _, _ ->
            uploads++
            FileTransferResult.Success("url")
        }
        replacement.start(this, previous) { replacement.execute {} }
        runCurrent()

        val cancellation = launch { replacement.cancel() }
        runCurrent()
        verifyNoInteractions(provider)
        releaseStop.complete(Unit)
        cancellation.join()

        assertThat(uploads).isEqualTo(0)
        verify(provider).cancel("upload:42")
    }

    @Test
    fun `a replacement owner cleans up the previous provider before starting`() = runTest {
        val previousProvider = mock<FileTransferProvider>()
        val previous = TransferExecutor("upload:41", previousProvider) { _, _ -> FileTransferResult.Success("url") }
        previous.start(this, previous = null) { awaitCancellation() }
        runCurrent()
        previous.stop()
        val replacement = TransferExecutor("upload:42", provider) { _, _ -> FileTransferResult.Success("url") }
        var started = false

        replacement.start(this, previous) {
            verify(previousProvider).cancel("upload:41")
            started = true
        }
        runCurrent()

        assertThat(started).isTrue()
        verifyNoInteractions(provider)
    }
}
