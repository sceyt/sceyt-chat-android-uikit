package com.sceyt.chatuikit.persistence.extensions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class CoroutineExtensionsTest {

    @Test
    fun `returns successful block result`() = runTest {
        val result = runCatchingCancellable { "success" }

        assertThat(result.getOrNull()).isEqualTo("success")
    }

    @Test
    fun `captures failure while caller is active`() = runTest {
        val error = IOException("operation failed")

        val result = runCatchingCancellable<Unit> { throw error }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `captures operation cancellation while caller is active`() = runTest {
        val error = CancellationException("operation cancelled")

        val result = runCatchingCancellable<Unit> { throw error }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `propagates caller cancellation even when block throws another error`() = runTest {
        var continuedAfterCancellation = false
        val job = launch {
            runCatchingCancellable<Unit> {
                currentCoroutineContext().cancel()
                throw IOException("operation failed after cancellation")
            }
            continuedAfterCancellation = true
        }

        job.join()

        assertThat(job.isCancelled).isTrue()
        assertThat(continuedAfterCancellation).isFalse()
    }
}
