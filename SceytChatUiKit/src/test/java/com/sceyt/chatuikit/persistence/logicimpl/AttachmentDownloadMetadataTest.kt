package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
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
    private val transport = RecordingFileTransferProvider()

    @Before
    fun setUp() {
        previous = SceytChatUIKit.fileTransfer
        val destination = temporary.newFile()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            provider = this@AttachmentDownloadMetadataTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _ -> destination }
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previous
    }

    @Test
    fun `the download request carries its attachment and only the saved app object`() = runTest {
        val value =
            attachment(filePath = null, fileSize = 456L, state = TransferState.PendingDownload)
                .copy(metadata = """{"szw":1280,"enc":{"v":1},"file_transfer":{"key":"custom-key"}}""")
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator =
            AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        val call = transport.downloadCalls.single()
        assertThat(call.request.metadata)
            .isEqualTo(JsonParser.parseString("""{"key":"custom-key"}""").asJsonObject)
        assertThat(call.request.attachment).isEqualTo(value)
        assertThat(call.request.type).isEqualTo(FileTransferType.PLAIN)
        call.succeed(call.request.destinationFile.path)
        runCurrent()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Success::class.java)
    }

    @Test
    fun `encryption enabled requests an encrypted download even without metadata`() = runTest {
        SceytChatUIKit.fileTransfer.encryptionEnabled = true
        val value = attachment(filePath = null, state = TransferState.PendingDownload)
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator =
            AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        val call = transport.downloadCalls.single()
        assertThat(call.request.type).isEqualTo(FileTransferType.ENCRYPTED)
        assertThat(call.request.attachment).isEqualTo(value)
        assertThat(call.request.metadata.entrySet()).isEmpty()
        call.succeed(call.request.destinationFile.path)
        runCurrent()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Success::class.java)
    }

    @Test
    fun `the router passes custom encryption fields without interpreting them`() = runTest {
        val metadata =
            """{"file_transfer":{"enc":"app-format","appEncryption":{"key":"custom-key"}}}"""
        val value = attachment(filePath = null, state = TransferState.PendingDownload)
            .copy(metadata = metadata)
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator =
            AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        val call = transport.downloadCalls.single()
        assertThat(call.request.metadata)
            .isEqualTo(JsonParser.parseString(metadata).asJsonObject.getAsJsonObject("file_transfer"))
        call.succeed(call.request.destinationFile.path)
        runCurrent()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Success::class.java)
    }

    @Test
    fun `plain attachments can keep non JSON metadata`() = runTest {
        val value = attachment(filePath = null, state = TransferState.PendingDownload)
            .copy(metadata = "application metadata")
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator =
            AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        val call = transport.downloadCalls.single()
        assertThat(call.request.metadata.entrySet()).isEmpty()
        assertThat(call.request.attachment.metadata).isEqualTo("application metadata")
        call.succeed(call.request.destinationFile.path)
        runCurrent()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Success::class.java)
    }

    @Test
    fun `a malformed saved app object fails before reaching the transport`() = runTest {
        val value = attachment(filePath = null, state = TransferState.PendingDownload)
            .copy(metadata = """{"file_transfer":"broken"}""")
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator =
            AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)

        coordinator.download(value, transferTask(value), { finished.complete(it) })
        runCurrent()

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(finished.await()).isInstanceOf(SceytResponse.Error::class.java)
    }
}
