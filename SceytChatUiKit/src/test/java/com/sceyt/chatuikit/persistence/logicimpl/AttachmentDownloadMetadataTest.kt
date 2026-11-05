package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileDownloadType
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentDownloadMetadataTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var previous: SceytChatUIKitFileTransfer
    private val transport = RecordingFileTransferTransport()

    @Before
    fun setUp() {
        previous = SceytChatUIKit.fileTransfer
        val destination = temporary.newFile()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            transport = this@AttachmentDownloadMetadataTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _ -> destination }
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previous
    }

    @Test
    fun `enc metadata is passed to the transport with the attachment plaintext size`() = runTest {
        val value = attachment(filePath = null, fileSize = 456L, state = TransferState.PendingDownload)
            .copy(metadata = """{"enc":{"v":1,"alg":"sdk-format","attachmentId":"id","key":"key"}}""")
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator = AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        val call = transport.downloadCalls.single()
        assertThat(call.request.type)
            .isEqualTo(FileDownloadType.Encrypted(FileEncryptionInfo("sdk-format", "id", "key"), value.fileSize))
        call.succeed(call.request.destinationFile.path)
        runCurrent()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Success::class.java)
    }

    @Test
    fun `malformed enc metadata fails before a plain download can start`() = runTest {
        val metadata = """{"enc":"invalid"}"""
        val value = attachment(filePath = null, state = TransferState.PendingDownload).copy(metadata = metadata)
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator = AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(finished.isCompleted).isTrue()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Error::class.java)
    }
}
