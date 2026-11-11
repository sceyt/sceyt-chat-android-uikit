package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sceyt.chat.fileencryption.AeadFileEncryptionProvider
import com.sceyt.chat.fileencryption.AttachmentCipher
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.fileencryption.RandomAccessStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.sceyt.chat.fileencryption.FileEncryptionCheckpoint as SdkCheckpoint

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowEncryptionIdGenerator::class, ShadowAttachmentCipher::class])
class ChatSdkFileEncryptionTest {
    private val file = File("/tmp/prepared-file.bin")
    private val attachmentKey = "opaque-attachment-key"

    @Test
    fun `prepared attachment ID meets the SDK format and stays in metadata and checkpoint`() =
        runTest {
            val provider = mock<FileEncryptionProvider>()
            val prepared = upload()
            doAnswer {
                val id = it.getArgument<ByteArray>(1)
                require(id.size == 16) { "attachmentId must contain 16 bytes" }
                val cipher = mockCipher("key:" + Base64.getUrlEncoder().withoutPadding().encodeToString(id))
                whenever(prepared.cipher).thenReturn(cipher)
                whenever(prepared.checkpoint).thenReturn(
                    SdkCheckpoint(
                        cipher,
                        "saved-state"
                    )
                )
                prepared
            }.whenever(provider).prepareUpload(eq(file), any())

            ChatSdkFileEncryption(provider).prepareEncryption(file).use { session ->
                val id = argumentCaptor<ByteArray>()
                verify(provider).prepareUpload(eq(file), id.capture())
                val metadata = FileEncryptionMetadata.write(
                    null, session.attachmentKey,
                )

                assertThat(session.attachmentKey).isEqualTo(prepared.cipher.attachmentKey)
                assertThat(FileEncryptionMetadata.read(metadata)).isEqualTo(session.attachmentKey)
                assertThat(session.checkpoint().attachmentKey).isEqualTo(session.attachmentKey)
            }
        }

    @Test
    fun `typed checkpoint survives storage and restores the same SDK session`() = runTest {
        val provider = mock<FileEncryptionProvider>()
        val first = upload()
        val restored = upload()
        whenever(provider.prepareUpload(eq(file), any())).thenReturn(first)
        whenever(provider.restoreUpload(eq(file), any())).thenReturn(restored)
        var checkpointData = "before-read"
        whenever(first.checkpoint).thenAnswer { SdkCheckpoint(first.cipher, checkpointData) }
        doAnswer {
            checkpointData = "after-read"
            byteArrayOf(3, 4)
        }.whenever(first).readCiphertextRange(2L, 2)
        whenever(restored.readCiphertextRange(2L, 2)).thenReturn(byteArrayOf(3, 4))
        val encryption: FileEncryption = ChatSdkFileEncryption(provider)

        val session = encryption.prepareEncryption(file)
        val bytes = session.read(2L, 2)
        val saved = Gson().fromJson(
            Gson().toJson(session.checkpoint()),
            FileEncryptionCheckpoint::class.java,
        )
        session.close()
        session.close()
        val resumed = encryption.restoreEncryption(file, saved)

        assertThat(resumed.read(2L, 2)).isEqualTo(bytes)
        assertThat(resumed.sourceId).isEqualTo(session.sourceId)
        assertThat(resumed.attachmentKey).isEqualTo(attachmentKey)
        assertThat(resumed.plaintextSize).isEqualTo(4L)
        assertThat(resumed.ciphertextSize).isEqualTo(8L)
        val checkpoint = argumentCaptor<SdkCheckpoint>()
        verify(provider).restoreUpload(eq(file), checkpoint.capture())
        assertThat(checkpoint.firstValue.cipher.attachmentKey).isEqualTo(attachmentKey)
        assertThat(checkpoint.firstValue.data).isEqualTo("after-read")
        verify(first, times(1)).close()
        resumed.close()
        verify(restored).close()
    }

    @Test
    fun `restore failure does not silently generate a replacement key`() = runTest {
        val provider = mock<FileEncryptionProvider>()
        val error = IOException("Source changed")
        whenever(provider.restoreUpload(eq(file), any())).thenThrow(error)
        val checkpoint = FileEncryptionCheckpoint(attachmentKey, ByteArray(72), "saved-state")

        val failure = runCatching {
            ChatSdkFileEncryption(provider).restoreEncryption(file, checkpoint)
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure).hasMessageThat().isEqualTo("Source changed")
        verify(provider, never()).prepareUpload(any(), any())
    }

    @Test
    fun `failed session construction closes the SDK handle off the caller dispatcher`() = runTest {
        val provider = mock<FileEncryptionProvider>()
        val prepared = upload()
        whenever(provider.prepareUpload(eq(file), any())).thenReturn(prepared)
        whenever(prepared.sourceId).thenThrow(IllegalStateException("Invalid native session"))
        var callerThread: Thread? = null
        var cleanupThread: Thread? = null
        doAnswer {
            cleanupThread = Thread.currentThread()
            Unit
        }.whenever(prepared).close()

        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { caller ->
            val failure = withContext(caller) {
                callerThread = Thread.currentThread()
                runCatching { ChatSdkFileEncryption(provider).prepareEncryption(file) }.exceptionOrNull()
            }

            assertThat(failure).hasMessageThat().isEqualTo("Invalid native session")
            assertThat(cleanupThread).isNotNull()
            assertThat(cleanupThread).isNotSameInstanceAs(callerThread)
            verify(prepared).close()
        }
    }

    @Test
    fun `cancellation during preparation closes a session returned by the SDK`() = runTest {
        val provider = mock<FileEncryptionProvider>()
        val prepared = upload()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        doAnswer {
            entered.countDown()
            try {
                check(release.await(5, TimeUnit.SECONDS))
            } catch (_: InterruptedException) {
                // Model a native allocation completing just as cancellation arrives.
            }
            prepared
        }.whenever(provider).prepareUpload(eq(file), any())
        val job = launch(Dispatchers.Default) {
            ChatSdkFileEncryption(provider).prepareEncryption(file)
        }
        try {
            withContext(Dispatchers.Default) {
                check(entered.await(5, TimeUnit.SECONDS))
            }
            job.cancel()
            release.countDown()
            job.join()

            assertThat(job.isCancelled).isTrue()
            verify(prepared).close()
        } finally {
            release.countDown()
            job.cancel()
        }
    }

    private fun upload(): PreparedUpload {
        val cipher = mockCipher(attachmentKey)
        return mock<PreparedUpload>().also {
            whenever(it.sourceId).thenReturn("same-ciphertext")
            whenever(it.plaintextSize).thenReturn(4L)
            whenever(it.ciphertextSize).thenReturn(8L)
            whenever(it.cipher).thenReturn(cipher)
        }
    }
}

/** Replaces the Android native generator for JVM tests, including calls on Dispatchers.IO. */
@Implements(AeadFileEncryptionProvider::class, isInAndroidSdk = false)
class ShadowEncryptionIdGenerator {
    companion object {
        @JvmStatic
        @Implementation
        fun newAttachmentId(): ByteArray = ByteArray(16) { it.toByte() }
    }
}

@Implements(AttachmentCipher::class, isInAndroidSdk = false)
class ShadowAttachmentCipher {
    companion object {
        var decrypt: ((RandomAccessStream) -> RandomAccessStream)? = null

        @JvmStatic
        @Implementation
        fun restore(attachmentKey: String, header: ByteArray): AttachmentCipher = mockCipher(attachmentKey, header)
    }
}

private fun mockCipher(attachmentKey: String, header: ByteArray = ByteArray(72)): AttachmentCipher =
    mock<AttachmentCipher>().also {
        whenever(it.attachmentKey).thenReturn(attachmentKey)
        whenever(it.header).thenReturn(header)
        whenever(it.newDecryptingRandomAccessStream(any())).thenAnswer { call ->
            checkNotNull(ShadowAttachmentCipher.decrypt)(call.getArgument(0))
        }
    }
