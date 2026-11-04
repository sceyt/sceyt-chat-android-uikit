package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.UploadOperation
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.EncryptedUploadCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.FileUploadResult
import com.sceyt.chatuikit.filetransfer.FileUploadType
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.toUIKitInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedSdkUploadTest {
    private lateinit var previous: SceytChatUIKitFileTransfer
    private val info = FileEncryptionInfo(
        "sdk-format",
        FileEncryptionData("sdk-upload", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 32L),
    )

    @Before
    fun setUp() {
        previous = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer()
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previous
    }

    @Test
    fun `cancellation stops the SDK upload closes its session and ignores late callbacks`() = runTest {
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
            val result = async {
                DefaultFileTransferTransport().upload(
                    FileUploadRequest("upload:cancel", file, "file.bin", null, type = FileUploadType.ENCRYPTED),
                    progress::add,
                )
            }
            runCurrent()
            result.cancelAndJoin()
            progressCallback.onResult(0.5f)
            resultCallback.onResult("late-url", info)

            verify(operation).cancel()
            verify(source).close()
            assertThat(result.isCancelled).isTrue()
            assertThat(progress).isEmpty()
        }
    }

    @Test
    fun `encrypted upload prepares a session and returns URL with matching encryption information`() = runTest {
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
            val result = async {
                DefaultFileTransferTransport().upload(
                    FileUploadRequest("upload:1", file, "file.bin", null, type = FileUploadType.ENCRYPTED),
                    progress::add,
                )
            }
            runCurrent()
            progressCallback.onResult(0.4f)
            resultCallback.onResult("https://cdn.test/encrypted", info)
            progressCallback.onResult(0.8f)
            progressCallback.onResult(1f)

            assertThat(result.await()).isEqualTo(
                FileTransferResult.Success(FileUploadResult("https://cdn.test/encrypted", info.toUIKitInfo())),
            )
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

            val result = DefaultFileTransferTransport().upload(
                FileUploadRequest("upload:error", file, "file.bin", null, type = FileUploadType.ENCRYPTED),
            ) {}

            assertThat((result as FileTransferResult.Failure).error).isSameInstanceAs(error)
            verify(source).close()
        }
    }

    private suspend fun configureSession(source: PreparedUpload): File {
        val file = File("/tmp/native-upload.bin")
        whenever(source.sourceId).thenReturn("sdk-session")
        whenever(source.plaintextSize).thenReturn(32L)
        whenever(source.ciphertextSize).thenReturn(136L)
        whenever(source.encryptionInfo).thenReturn(info)
        val encryption = mock<FileEncryption>()
        val session = ChatSdkFileEncryption.EncryptionSession(source)
        whenever(encryption.prepareEncryption(file)).thenReturn(session)
        SceytChatUIKit.fileTransfer.encryption = encryption
        return file
    }
}
