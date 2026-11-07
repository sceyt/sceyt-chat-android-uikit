package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sceyt.chat.fileencryption.AeadFileEncryptionProvider
import com.sceyt.chat.fileencryption.AttachmentCipher
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PreparedUpload
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
@Config(shadows = [ShadowEncryptionIdGenerator::class])
class ChatSdkFileEncryptionTest {
    private val file = File("/tmp/prepared-file.bin")
    private val token = "opaque-sdk-token"

    @Test
    fun `prepared attachment ID meets the SDK format and stays in metadata and checkpoint`() =
        runTest {
            val provider = mock<FileEncryptionProvider>()
            val prepared = upload()
            doAnswer {
                val id = it.getArgument<String>(1)
                require(
                    id.matches(Regex("[A-Za-z0-9_-]{22}")) && Base64.getUrlDecoder()
                        .decode(id).size == 16
                ) {
                    "uniqueId must be 16 bytes of base64url without padding"
                }
                val cipher = mockCipher("token:$id")
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
                val id = argumentCaptor<String>()
                verify(provider).prepareUpload(eq(file), id.capture())
                val metadata = FileEncryptionMetadata.write(
                    null, session.token,
                )

                assertThat(session.token).isEqualTo("token:" + id.firstValue)
                assertThat(FileEncryptionMetadata.read(metadata)).isEqualTo(session.token)
                assertThat(session.checkpoint().token).isEqualTo(session.token)
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
        assertThat(resumed.token).isEqualTo(token)
        assertThat(resumed.plaintextSize).isEqualTo(4L)
        assertThat(resumed.ciphertextSize).isEqualTo(8L)
        val checkpoint = argumentCaptor<SdkCheckpoint>()
        verify(provider).restoreUpload(eq(file), checkpoint.capture())
        assertThat(checkpoint.firstValue.cipher.token).isEqualTo(token)
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
        val checkpoint = FileEncryptionCheckpoint(token, "saved-state")

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
        val cipher = mockCipher(token)
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
        fun newUniqueId(): String = "gIGCg4SFhoeIiYqLjI2Ojw"

        @JvmStatic
        @Implementation
        fun restore(token: String): AttachmentCipher = mockCipher(token)
    }
}

private fun mockCipher(token: String): AttachmentCipher = mock<AttachmentCipher>().also {
    whenever(it.token).thenReturn(token)
}
