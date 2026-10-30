package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.UploadOperation
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.sceyt_callbacks.EncryptedUploadCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedSdkUploadTest {
    @Test
    fun `cancellation stops the SDK upload and ignores late callbacks`() = runTest {
        val client = mock<ChatClient>()
        val operation = mock<UploadOperation>()
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
                    FileUploadRequest.Encrypted("upload:cancel", mock<PreparedUpload>(), "file.bin", null),
                    progress::add,
                )
            }
            runCurrent()
            result.cancelAndJoin()
            progressCallback.onResult(0.5f)
            resultCallback.onResult("late-url", null)

            verify(operation).cancel()
            assertThat(result.isCancelled).isTrue()
            assertThat(progress).isEmpty()
        }
    }

    @Test
    fun `encrypted upload uses SDK session and returns its URL`() = runTest {
        val client = mock<ChatClient>()
        val source = mock<PreparedUpload>()
        val info = FileEncryptionInfo(
            "sdk-format",
            FileEncryptionData("sdk-upload", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 32L),
        )
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
                    FileUploadRequest.Encrypted("upload:1", source, "file.bin", null),
                    progress::add,
                )
            }
            runCurrent()
            progressCallback.onResult(0.4f)
            resultCallback.onResult("https://cdn.test/encrypted", info)
            progressCallback.onResult(0.8f)
            progressCallback.onResult(1f)

            assertThat(result.await()).isEqualTo(FileTransferResult.Success("https://cdn.test/encrypted"))
            assertThat(progress).containsExactly(40f)
            verify(source, never()).close()
            verify(operation, never()).cancel()
        }
    }
}
