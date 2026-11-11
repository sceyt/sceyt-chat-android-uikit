package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.fileencryption.AttachmentCipher
import com.sceyt.chat.fileencryption.RandomAccessStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.GeneralSecurityException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAttachmentCipher::class])
class ChatSdkFileDecryptionTest {
    @get:Rule
    val temporary = TemporaryFolder()
    private val attachmentKey = "opaque-attachment-key"

    @Test
    fun `prepare and read use IO and pass the saved attachmentKey to the SDK`() = runTest {
        val file = temporary.newFile().apply { writeBytes(ByteArray(72) { it.toByte() }) }
        val reader = reader()
        var callerThread: Thread? = null
        var prepareThread: Thread? = null
        var readThread: Thread? = null
        open { source ->
            prepareThread = Thread.currentThread()
            assertThat(source.read(0, 72)).isEqualTo(file.readBytes())
            reader
        }
        doAnswer {
            readThread = Thread.currentThread()
            byteArrayOf(7, 8)
        }.whenever(reader).read(2, 2)

        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { caller ->
            withContext(caller) {
                callerThread = Thread.currentThread()
                val session = ChatSdkFileEncryption().prepareDecryption(file, attachmentKey)
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
        assertClosed(checkNotNull(openedSource))
    }

    @Test
    fun `failed native open closes the ciphertext source`() = runTest {
        val file = temporary.newFile().apply { writeBytes(ByteArray(72) { it.toByte() }) }
        var source: RandomAccessStream? = null
        open {
            source = it
            throw GeneralSecurityException("Wrong key")
        }

        val error = runCatching {
            ChatSdkFileEncryption().prepareDecryption(
                file,
                attachmentKey,
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(GeneralSecurityException::class.java)
        assertClosed(checkNotNull(source))
    }

    @Test
    fun `failed session construction closes both the native reader and source`() = runTest {
        val file = temporary.newFile().apply { writeBytes(ByteArray(72) { it.toByte() }) }
        val reader = reader()
        whenever(reader.size()).thenThrow(IllegalStateException("Invalid native handle"))
        open { reader }

        val error = runCatching {
            ChatSdkFileEncryption().prepareDecryption(
                file,
                attachmentKey,
            )
        }.exceptionOrNull()

        assertThat(error).hasMessageThat().isEqualTo("Invalid native handle")
        verify(reader).close()
        assertClosed(checkNotNull(openedSource))
    }

    @Test
    fun `cancel during native open closes a reader allocated as cancellation arrives`() = runTest {
        val file = temporary.newFile().apply { writeBytes(ByteArray(72) { it.toByte() }) }
        val reader = reader()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        open {
            entered.countDown()
            try {
                check(release.await(5, TimeUnit.SECONDS))
            } catch (_: InterruptedException) {
                // A native allocation can finish despite the thread interruption.
            }
            reader
        }
        val job = launch(Dispatchers.Default) {
            ChatSdkFileEncryption().prepareDecryption(
                file,
                attachmentKey,
            )
        }
        try {
            withContext(Dispatchers.IO) { check(entered.await(5, TimeUnit.SECONDS)) }
            job.cancel()
            release.countDown()
            job.join()

            assertThat(job.isCancelled).isTrue()
            verify(reader).close()
            assertClosed(checkNotNull(openedSource))
        } finally {
            release.countDown()
            job.cancel()
        }
    }

    @Test
    fun `cancel interrupts a blocking native read and allows the caller to close its session`() =
        runTest {
            val file = temporary.newFile().apply { writeBytes(ByteArray(72) { it.toByte() }) }
            val reader = reader()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            open { reader }
            doAnswer {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                byteArrayOf(7, 8)
            }.whenever(reader).read(0, 2)
            val session = ChatSdkFileEncryption().prepareDecryption(file, attachmentKey)
            val job = launch(Dispatchers.Default) { session.use { it.read(0, 2) } }
            try {
                withContext(Dispatchers.IO) { check(entered.await(5, TimeUnit.SECONDS)) }
                job.cancel()
                job.join()

                assertThat(job.isCancelled).isTrue()
                verify(reader).close()
                assertClosed(checkNotNull(openedSource))
            } finally {
                release.countDown()
                job.cancel()
            }
        }

    private var openedSource: RandomAccessStream? = null

    @After
    fun resetCipher() {
        ShadowAttachmentCipher.decrypt = null
    }

    private fun open(action: (RandomAccessStream) -> RandomAccessStream) {
        ShadowAttachmentCipher.decrypt = { source ->
            openedSource = source
            action(source).also { reader ->
                doAnswer { source.close() }.whenever(reader).close()
            }
        }
    }

    private fun reader(): RandomAccessStream = mock<RandomAccessStream>().also {
        whenever(it.size()).thenReturn(4L)
    }

    private fun assertClosed(source: RandomAccessStream) {
        assertThat(runCatching { source.read(0, 1) }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
    }
}
