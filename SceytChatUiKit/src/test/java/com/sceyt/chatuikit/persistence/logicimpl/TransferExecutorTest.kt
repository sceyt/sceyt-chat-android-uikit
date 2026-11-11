package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileTransferProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
    fun `discard requested before preparation runs cleanup when it is registered`() {
        var cleaned = false
        val executor = TransferExecutor("upload:42", provider) { _, _ -> FileTransferResult.Success("url") }

        executor.discard()
        assertThat(cleaned).isFalse()
        executor.onDiscard { cleaned = true }

        assertThat(cleaned).isTrue()
    }

    @Test
    fun `discard runs an already registered cleanup`() {
        var cleaned = false
        val executor = TransferExecutor("upload:42", provider) { _, _ -> FileTransferResult.Success("url") }
        executor.onDiscard { cleaned = true }
        assertThat(cleaned).isFalse()

        executor.discard()

        assertThat(cleaned).isTrue()
    }
}
