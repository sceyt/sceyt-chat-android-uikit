package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.UploadOperation
import com.sceyt.chat.fileencryption.AttachmentCipher
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.EncryptedUploadCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionSession
import com.sceyt.chatuikit.persistence.extensions.withMetadata
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import com.sceyt.chat.fileencryption.FileEncryptionCheckpoint as SdkCheckpoint

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedSdkUploadTest {
    private lateinit var previous: SceytChatUIKitFileTransfer
    private val token = "opaque-sdk-token"
    private var savedCheckpoint: FileEncryptionCheckpoint? = null
    private lateinit var checkpointConstruction: MockedConstruction<NativeUploadCheckpointStore>

    @Before
    fun setUp() {
        previous = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer()
        checkpointConstruction = Mockito.mockConstruction(NativeUploadCheckpointStore::class.java) { store, _ ->
            doAnswer { call ->
                savedCheckpoint?.takeIf { it.attachmentKey == call.getArgument<String>(1) }
            }.whenever(store).read(any(), any())
            doAnswer { call ->
                savedCheckpoint = call.getArgument(1)
                Unit
            }.whenever(store).save(any(), any())
            doAnswer { call ->
                val checkpoint = call.getArgument<FileEncryptionCheckpoint>(1)
                if (savedCheckpoint?.attachmentKey == checkpoint.attachmentKey) savedCheckpoint = checkpoint
                Unit
            }.whenever(store).update(any(), any())
            doAnswer {
                savedCheckpoint = null
                Unit
            }.whenever(store).remove(any())
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previous
        checkpointConstruction.close()
    }

    @Test
    fun `a new provider reuses the saved token during retry preparation`() = runTest {
        val file = configureSession(mock())
        val request = prepareRequest(DefaultFileTransferProvider(), file, "upload:retry")

        val data = DefaultFileTransferProvider().prepareUpload(request)

        assertThat(data).containsExactly("sceyt.enc", token)
        assertThat(savedCheckpoint?.data).isEqualTo("before-read")
        verify(SceytChatUIKit.fileTransfer.encryption, times(1)).prepareEncryption(file)
    }

    @Test
    fun `explicit upload cancellation removes the local checkpoint`() = runTest {
        val file = configureSession(mock())
        val provider = DefaultFileTransferProvider()
        prepareRequest(provider, file, "upload:discard")

        provider.cancel("upload:discard")

        assertThat(savedCheckpoint).isNull()
        verify(checkpointConstruction.constructed().single()).remove("upload:discard")
    }

    @Test
    fun `cancellation stops the SDK upload closes its session and ignores late callbacks`() =
        runTest {
            val client = mock<ChatClient>()
            val operation = mock<UploadOperation>()
            val source = mock<PreparedUpload>()
            val file = configureSession(source)
            lateinit var progressCallback: ProgressCallback
            lateinit var resultCallback: EncryptedUploadCallback
            doAnswer {
                progressCallback = it.getArgument(1)
                resultCallback = it.getArgument(2)
                operation
            }.whenever(client).uploadEncrypted(any(), any(), any())

            Mockito.mockStatic(ChatClient::class.java).use { static ->
                static.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(client)
                val progress = mutableListOf<Float>()
                val provider = DefaultFileTransferProvider()
                val request = prepareRequest(provider, file, "upload:cancel")
                val result = async {
                    provider.upload(request, progress::add)
                }
                runCurrent()
                result.cancelAndJoin()
                progressCallback.onResult(0.5f)
                resultCallback.onResult("late-url", source.cipher)

                verify(operation).cancel()
                verify(source).close()
                assertThat(result.isCancelled).isTrue()
                assertThat(progress).isEmpty()
            }
        }

    @Test
    fun `encrypted upload restores prepared parameters and returns only its URL`() =
        runTest {
            val client = mock<ChatClient>()
            val source = mock<PreparedUpload>()
            val file = configureSession(source)
            val operation = mock<UploadOperation>()
            lateinit var progressCallback: ProgressCallback
            lateinit var resultCallback: EncryptedUploadCallback
            doAnswer {
                assertThat(it.getArgument<PreparedUpload>(0)).isSameInstanceAs(source)
                progressCallback = it.getArgument(1)
                resultCallback = it.getArgument(2)
                operation
            }.whenever(client).uploadEncrypted(any(), any(), any())

            Mockito.mockStatic(ChatClient::class.java).use { static ->
                static.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(client)
                val progress = mutableListOf<Float>()
                val provider = DefaultFileTransferProvider()
                val request = prepareRequest(provider, file, "upload:1")
                val result = async {
                    provider.upload(request, progress::add)
                }
                runCurrent()
                progressCallback.onResult(0.4f)
                resultCallback.onResult("https://cdn.test/encrypted", source.cipher)
                progressCallback.onResult(0.8f)
                progressCallback.onResult(1f)

                assertThat(result.await()).isEqualTo(FileTransferResult.Success("https://cdn.test/encrypted"))
                assertThat(savedCheckpoint).isNull()
                assertThat(progress).containsExactly(40f)
                verify(SceytChatUIKit.fileTransfer.encryption).prepareEncryption(file)
                verify(source).close()
                verify(operation, never()).cancel()
            }
        }

    @Test
    fun `SDK failure closes the prepared session`() = runTest {
        val client = mock<ChatClient>()
        val source = mock<PreparedUpload>()
        val file = configureSession(source)
        val error = SceytException(1, "Upload rejected")
        doAnswer {
            it.getArgument<EncryptedUploadCallback>(2).onError(error)
            mock<UploadOperation>()
        }.whenever(client).uploadEncrypted(any(), any(), any())

        Mockito.mockStatic(ChatClient::class.java).use { static ->
            static.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(client)

            val provider = DefaultFileTransferProvider()
            val request = prepareRequest(provider, file, "upload:error")
            val result = provider.upload(request) {}

            assertThat((result as FileTransferResult.Failure).error).isSameInstanceAs(error)
            assertThat(savedCheckpoint?.data).isEqualTo("after-read")
            verify(source).close()
        }
    }

    private suspend fun configureSession(source: PreparedUpload): File {
        val file = File("/tmp/native-upload.bin")
        val cipher = mock<AttachmentCipher>()
        whenever(cipher.attachmentKey).thenReturn(token)
        whenever(cipher.header).thenReturn(ByteArray(72))
        whenever(source.sourceId).thenReturn("sdk-session")
        whenever(source.plaintextSize).thenReturn(32L)
        whenever(source.ciphertextSize).thenReturn(136L)
        whenever(source.cipher).thenReturn(cipher)
        whenever(source.checkpoint).thenReturn(SdkCheckpoint(cipher, "after-read"))
        val encryption = mock<FileEncryption>()
        val prepared = mock<FileEncryptionSession>()
        whenever(prepared.checkpoint()).thenReturn(FileEncryptionCheckpoint(token, ByteArray(72), "before-read"))
        val session = ChatSdkFileEncryption.EncryptionSession(source)
        whenever(encryption.prepareEncryption(file)).thenReturn(prepared)
        whenever(encryption.restoreEncryption(eq(file), any())).thenReturn(session)
        SceytChatUIKit.fileTransfer.encryption = encryption
        return file
    }

    private suspend fun prepareRequest(
        provider: DefaultFileTransferProvider,
        file: File,
        operationId: String,
    ): FileUploadRequest {
        val request = FileUploadRequest(
            operationId, file, attachment(filePath = file.path), FileTransferType.ENCRYPTED,
        )
        val data = provider.prepareUpload(request)
        assertThat(data).containsExactly("sceyt.enc", token)
        return request.copy(attachment = request.attachment.copy(metadata = null.withMetadata(data)))
    }
}
