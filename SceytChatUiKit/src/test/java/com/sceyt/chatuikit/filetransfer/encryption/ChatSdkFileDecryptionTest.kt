package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.fileencryption.CiphertextRangeSource
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PlaintextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.security.GeneralSecurityException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ChatSdkFileDecryptionTest {
    @get:Rule
    val temporary = TemporaryFolder()
    private val info = FileEncryptionInfo("test-format", "attachment-id", "test-file-key")

    @Test
    fun `prepare and read use IO and pass the original metadata size to the SDK`() = runTest {
        val file = temporary.newFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val provider = mock<FileEncryptionProvider>()
        val reader = reader()
        var callerThread: Thread? = null
        var prepareThread: Thread? = null
        var readThread: Thread? = null
        doAnswer {
            prepareThread = Thread.currentThread()
            val source = it.getArgument<CiphertextRangeSource>(0)
            assertThat(source.readCiphertextRange(0, 5)).isEqualTo(file.readBytes())
            reader
        }.whenever(provider).openDownload(any(), eq(info.toSdkInfo(4)))
        doAnswer {
            readThread = Thread.currentThread()
            byteArrayOf(7, 8)
        }.whenever(reader).read(2, 2)

        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { caller ->
            withContext(caller) {
                callerThread = Thread.currentThread()
                val session = ChatSdkFileEncryption(provider).prepareDecryption(file, info, 4)
                assertThat(session.plaintextSize).isEqualTo(4L)
                assertThat(session.read(2, 2)).isEqualTo(byteArrayOf(7, 8))
                session.close()
                session.close()
                assertThat(runCatching { session.read(0, 1) }.exceptionOrNull())
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }
        assertThat(prepareThread).isNotSameInstanceAs(callerThread)
        assertThat(readThread).isNotSameInstanceAs(callerThread)
        verify(reader, times(1)).close()
        val source = argumentCaptor<CiphertextRangeSource>()
        verify(provider).openDownload(source.capture(), eq(info.toSdkInfo(4)))
        assertClosed(source.firstValue)
    }

    @Test
    fun `failed native open closes the ciphertext source`() = runTest {
        val file = temporary.newFile().apply { writeText("ciphertext") }
        val provider = mock<FileEncryptionProvider>()
        var source: CiphertextRangeSource? = null
        doAnswer {
            source = it.getArgument(0)
            throw GeneralSecurityException("Wrong key")
        }.whenever(provider).openDownload(any(), any())

        val error = runCatching {
            ChatSdkFileEncryption(provider).prepareDecryption(
                file,
                info,
                4
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(GeneralSecurityException::class.java)
        assertClosed(checkNotNull(source))
    }

    @Test
    fun `failed session construction closes both the native reader and source`() = runTest {
        val file = temporary.newFile().apply { writeText("ciphertext") }
        val provider = mock<FileEncryptionProvider>()
        val reader = reader()
        whenever(reader.plaintextSize).thenThrow(IllegalStateException("Invalid native handle"))
        whenever(provider.openDownload(any(), any())).thenReturn(reader)

        val error = runCatching {
            ChatSdkFileEncryption(provider).prepareDecryption(
                file,
                info,
                4
            )
        }.exceptionOrNull()

        assertThat(error).hasMessageThat().isEqualTo("Invalid native handle")
        verify(reader).close()
        val source = argumentCaptor<CiphertextRangeSource>()
        verify(provider).openDownload(source.capture(), any())
        assertClosed(source.firstValue)
    }

    @Test
    fun `cancel during native open closes a reader allocated as cancellation arrives`() = runTest {
        val file = temporary.newFile().apply { writeText("ciphertext") }
        val provider = mock<FileEncryptionProvider>()
        val reader = reader()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        doAnswer {
            entered.countDown()
            try {
                check(release.await(5, TimeUnit.SECONDS))
            } catch (_: InterruptedException) {
                // A native allocation can finish despite the thread interruption.
            }
            reader
        }.whenever(provider).openDownload(any(), any())
        val job = launch(Dispatchers.Default) {
            ChatSdkFileEncryption(provider).prepareDecryption(
                file,
                info,
                4
            )
        }
        try {
            withContext(Dispatchers.IO) { check(entered.await(5, TimeUnit.SECONDS)) }
            job.cancel()
            release.countDown()
            job.join()

            assertThat(job.isCancelled).isTrue()
            verify(reader).close()
            val source = argumentCaptor<CiphertextRangeSource>()
            verify(provider).openDownload(source.capture(), any())
            assertClosed(source.firstValue)
        } finally {
            release.countDown()
            job.cancel()
        }
    }

    @Test
    fun `cancel interrupts a blocking native read and allows the caller to close its session`() =
        runTest {
            val file = temporary.newFile().apply { writeText("ciphertext") }
            val provider = mock<FileEncryptionProvider>()
            val reader = reader()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            whenever(provider.openDownload(any(), any())).thenReturn(reader)
            doAnswer {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                byteArrayOf(7, 8)
            }.whenever(reader).read(0, 2)
            val session = ChatSdkFileEncryption(provider).prepareDecryption(file, info, 4)
            val job = launch(Dispatchers.Default) { session.use { it.read(0, 2) } }
            try {
                withContext(Dispatchers.IO) { check(entered.await(5, TimeUnit.SECONDS)) }
                job.cancel()
                job.join()

                assertThat(job.isCancelled).isTrue()
                verify(reader).close()
                val source = argumentCaptor<CiphertextRangeSource>()
                verify(provider).openDownload(source.capture(), any())
                assertClosed(source.firstValue)
            } finally {
                release.countDown()
                job.cancel()
            }
        }

    private fun reader(): PlaintextReader = mock<PlaintextReader>().also {
        whenever(it.plaintextSize).thenReturn(4L)
    }

    private fun assertClosed(source: CiphertextRangeSource) {
        assertThat(runCatching { source.readCiphertextRange(0, 1) }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
    }
}
